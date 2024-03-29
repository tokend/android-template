package io.tokend.template.features.balances.storage

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.tokend.template.data.storage.repository.MultipleItemsRepository
import io.tokend.template.data.storage.repository.RepositoryCache
import io.tokend.template.extensions.mapSuccessful
import io.tokend.template.extensions.tryOrNull
import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.assets.model.SimpleAsset
import io.tokend.template.features.balances.model.BalanceRecord
import io.tokend.template.features.systeminfo.data.storage.SystemInfoRepository
import io.tokend.template.features.urlconfig.providers.UrlConfigProvider
import io.tokend.template.logic.TxManager
import io.tokend.template.logic.providers.AccountProvider
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.v3.accounts.AccountsApiV3
import org.tokend.sdk.api.v3.balances.BalancesApi
import org.tokend.sdk.api.v3.balances.params.ConvertedBalancesParams
import org.tokend.sdk.utils.extentions.isNotFound
import org.tokend.wallet.*
import org.tokend.wallet.Transaction
import org.tokend.wallet.xdr.*
import org.tokend.wallet.xdr.op_extensions.CreateBalanceOp
import retrofit2.HttpException
import java.math.BigDecimal
import java.math.MathContext

class BalancesRepository(
    private val apiProvider: ApiProvider,
    private val walletInfoProvider: WalletInfoProvider,
    private val urlConfigProvider: UrlConfigProvider,
    private val mapper: ObjectMapper,
    private val conversionAssetCode: String?,
    itemsCache: RepositoryCache<BalanceRecord>
) : MultipleItemsRepository<BalanceRecord>(itemsCache) {

    var conversionAsset: Asset? = null
        private set

    override fun getItems(): Single<List<BalanceRecord>> {
        val signedApi = apiProvider.getSignedApi()
        val accountId = walletInfoProvider.getWalletInfo().accountId

        return if (conversionAssetCode != null)
            getConvertedBalances(
                signedApi.v3.balances,
                accountId,
                urlConfigProvider,
                mapper,
                conversionAssetCode
            )
                .onErrorResumeNext {
                    // It's back!
                    if (it is HttpException && it.isNotFound()) {
                        Log.e(
                            "BalancesRepo",
                            "This env is unable to convert balances into $conversionAssetCode"
                        )
                        getBalances(
                            signedApi.v3.accounts,
                            accountId,
                            urlConfigProvider,
                            mapper
                        )
                    } else
                        Single.error(it)
                }
        else
            getBalances(
                signedApi.v3.accounts,
                accountId,
                urlConfigProvider,
                mapper
            )
    }

    private fun getConvertedBalances(
        signedBalancesApi: BalancesApi,
        accountId: String,
        urlConfigProvider: UrlConfigProvider,
        mapper: ObjectMapper,
        conversionAssetCode: String
    ): Single<List<BalanceRecord>> {
        return signedBalancesApi
            .getConvertedBalances(
                accountId = accountId,
                assetCode = conversionAssetCode,
                params = ConvertedBalancesParams(
                    include = listOf(
                        ConvertedBalancesParams.Includes.BALANCE_ASSET,
                        ConvertedBalancesParams.Includes.STATES,
                        ConvertedBalancesParams.Includes.ASSET
                    )
                )
            )
            .toSingle()
            .map { convertedBalances ->
                val conversionAsset = SimpleAsset(convertedBalances.asset)
                convertedBalances.states.mapSuccessful {
                    BalanceRecord(
                        it, urlConfigProvider.getConfig(),
                        mapper, conversionAsset
                    )
                }
            }
    }

    private fun getBalances(
        signedAccountsApi: AccountsApiV3,
        accountId: String,
        urlConfigProvider: UrlConfigProvider,
        mapper: ObjectMapper
    ): Single<List<BalanceRecord>> {
        return signedAccountsApi
            .getBalances(accountId)
            .toSingle()
            .map { sourceList ->
                sourceList.mapSuccessful {
                    BalanceRecord(it, urlConfigProvider.getConfig(), mapper)
                }
            }
    }

    override fun broadcast() {
        if (conversionAssetCode != null) {
            conversionAsset = itemsCache.items
                .firstOrNull { it.conversionAsset != null }
                ?.conversionAsset
        }
        super.broadcast()
    }

    /**
     * Creates balance for given assets,
     * updates repository on complete
     */
    fun create(
        accountProvider: AccountProvider,
        systemInfoRepository: SystemInfoRepository,
        txManager: TxManager,
        vararg assets: String
    ): Completable {
        val accountId = walletInfoProvider.getWalletInfo().accountId
        val account = accountProvider.getDefaultAccount()

        return systemInfoRepository.getNetworkParams()
            .flatMap { netParams ->
                createBalanceCreationTransaction(netParams, accountId, account, assets)
            }
            .flatMap { transition ->
                txManager.submit(transition)
            }
            .flatMapCompletable {
                invalidate()
                updateDeferred()
            }
            .doOnSubscribe {
                isLoading = true
            }
            .doOnTerminate {
                isLoading = false
            }
    }

    private fun createBalanceCreationTransaction(
        networkParams: NetworkParams,
        sourceAccountId: String,
        signer: Account,
        assets: Array<out String>
    ): Single<Transaction> {
        return Single.defer {
            val operations = assets.map {
                CreateBalanceOp(sourceAccountId, it)
            }

            val transaction =
                TransactionBuilder(networkParams, PublicKeyFactory.fromAccountId(sourceAccountId))
                    .addOperations(operations.map(Operation.OperationBody::ManageBalance))
                    .build()

            transaction.addSignature(signer)

            Single.just(transaction)
        }.subscribeOn(Schedulers.computation())
    }

    fun updateBalance(
        balanceId: String,
        newAvailableAmount: BigDecimal
    ) {
        itemsList.find { it.id == balanceId }
            ?.also { updateBalance(it, newAvailableAmount) }
    }

    fun updateBalance(
        balance: BalanceRecord,
        newAvailableAmount: BigDecimal
    ) {
        balance.available = newAvailableAmount

        if (balance.conversionPrice != null) {
            val currentConvertedAmount = balance.convertedAmount
            if (currentConvertedAmount != null) {
                balance.convertedAmount =
                    newAvailableAmount.multiply(balance.conversionPrice, MathContext.DECIMAL64)
            }
        }

        itemsCache.update(balance)
        broadcast()
    }

    fun updateBalanceByDelta(
        balanceId: String,
        delta: BigDecimal
    ) {
        itemsList.find { it.id == balanceId }
            ?.also { updateBalanceByDelta(it, delta) }
    }

    fun updateBalanceByDelta(
        balance: BalanceRecord,
        delta: BigDecimal
    ) =
        updateBalance(balance, balance.available + delta)

    /**
     * Parses [TransactionMeta] from [transactionResultMetaXdr] string
     * and updates available amounts of affected balances
     */
    fun updateBalancesByTransactionResultMeta(
        transactionResultMetaXdr: String,
        networkParams: NetworkParams
    ): Boolean {
        val meta = tryOrNull {
            TransactionMeta.fromBase64(transactionResultMetaXdr) as TransactionMeta.EmptyVersion
        } ?: return false

        val balancesMap = itemsList.associateBy(BalanceRecord::id)

        val balancesToUpdate = meta.operations
            .map { it.changes.toList() }
            .flatten()
            .filterIsInstance(LedgerEntryChange.Updated::class.java)
            .map { it.updated.data }
            .filterIsInstance(LedgerEntry.LedgerEntryData.Balance::class.java)
            .map { it.balance }
            .mapNotNull { balanceEntry ->
                val id = balanceEntry.balanceID as? PublicKey.KeyTypeEd25519
                    ?: return@mapNotNull null

                val idString = Base32Check.encodeBalanceId(id.ed25519.wrapped)

                val balance = balancesMap[idString]
                    ?: return@mapNotNull null

                balance to networkParams.amountFromPrecised(balanceEntry.amount)
            }
            .takeIf(Collection<Any>::isNotEmpty)
            ?: return false

        balancesToUpdate.forEach { (balance, newAmount) ->
            updateBalance(balance, newAmount)
        }

        return true
    }
}