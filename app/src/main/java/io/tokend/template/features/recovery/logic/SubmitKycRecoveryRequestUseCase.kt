package io.tokend.template.features.recovery.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.tokend.template.features.account.data.model.AccountRecord
import io.tokend.template.features.account.data.storage.AccountRepository
import io.tokend.template.features.kyc.model.KycForm
import io.tokend.template.features.signup.logic.WalletAccountsUtil
import io.tokend.template.logic.TxManager
import io.tokend.template.logic.providers.AccountProvider
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.RepositoryProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.blobs.model.Blob
import org.tokend.sdk.api.blobs.model.BlobType
import org.tokend.sdk.api.v3.requests.params.RequestsPageParamsV3
import org.tokend.sdk.factory.JsonApiTools
import org.tokend.sdk.keyserver.models.SignerData
import org.tokend.wallet.Account
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.PublicKeyFactory
import org.tokend.wallet.Transaction
import org.tokend.wallet.xdr.*

/**
 * Submits KYC recovery request with given [form].
 * Creates new reviewable request
 * or updates existing based on current KYC recovery status from [AccountRepository]
 *
 * Updates KYC recovery status in [AccountRepository] on success
 */
class SubmitKycRecoveryRequestUseCase(
    private val form: KycForm,
    private val apiProvider: ApiProvider,
    private val walletInfoProvider: WalletInfoProvider,
    private val accountProvider: AccountProvider,
    private val repositoryProvider: RepositoryProvider,
    private val txManager: TxManager
) {
    private val accountRepository: AccountRepository = repositoryProvider.account

    private lateinit var networkParams: NetworkParams
    private lateinit var formBlobId: String
    private var requestId: Long = 0L
    private lateinit var signersToSet: Collection<SignerData>
    private lateinit var resultMetaXdr: String
    private lateinit var newKycRecoveryStatus: AccountRecord.KycRecoveryStatus

    fun perform(): Completable {
        return getNetworkParams()
            .doOnSuccess { networkParams ->
                this.networkParams = networkParams
            }
            .flatMap {
                getFormBlobId()
            }
            .doOnSuccess { formBlobId ->
                this.formBlobId = formBlobId
            }
            .flatMap {
                getRequestId()
            }
            .doOnSuccess { requestId ->
                this.requestId = requestId
            }
            .flatMap {
                getSignersToSet()
            }
            .doOnSuccess { signersToSet ->
                this.signersToSet = signersToSet
            }
            .flatMap {
                getTransaction()
            }
            .flatMap { transaction ->
                txManager.submit(transaction)
            }
            .doOnSuccess { result ->
                this.resultMetaXdr = result.resultMetaXdr!!
            }
            .flatMap {
                getNewKycRecoveryStatus()
            }
            .doOnSuccess { newKycRecoveryStatus ->
                this.newKycRecoveryStatus = newKycRecoveryStatus
            }
            .doOnSuccess {
                updateRepositories()
            }
            .ignoreElement()
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider
            .systemInfo
            .getNetworkParams()
    }

    private fun getFormBlobId(): Single<String> {
        if (form is KycForm.Empty) {
            return "".toSingle()
        }

        val formJson = JsonApiTools.objectMapper.writeValueAsString(form)

        return repositoryProvider
            .blobs
            .create(Blob(BlobType.KYC_FORM, formJson))
            .map(Blob::id)
    }

    private fun getRequestId(): Single<Long> {
        val recoveryStatus = accountRepository.item?.kycRecoveryStatus

        if (setOf(
                AccountRecord.KycRecoveryStatus.INITIATED,
                AccountRecord.KycRecoveryStatus.PERMANENTLY_REJECTED,
                AccountRecord.KycRecoveryStatus.NONE
            ).contains(recoveryStatus)
        ) {
            return 0L.toSingle()
        }

        val signedApi = apiProvider.getSignedApi()

        val accountId = walletInfoProvider.getWalletInfo().accountId

        return signedApi.v3.requests
            .get(
                RequestsPageParamsV3(
                    requestor = accountId,
                    type = ReviewableRequestType.KYC_RECOVERY,
                    pagingParams = PagingParamsV2(
                        order = PagingOrder.DESC,
                        limit = 1
                    )
                )
            )
            .toSingle()
            .map { page ->
                page.items
                    .firstOrNull()
                    ?.id
                    ?.toLongOrNull()
                    ?: throw IllegalStateException("No request to update found")
            }
    }

    private fun getSignersToSet(): Single<Collection<SignerData>> {
        return WalletAccountsUtil.getSignersForNewWallet(
            orderedAccountIds = accountProvider.getAccounts().map(Account::accountId),
            keyValueRepository = repositoryProvider.keyValueEntries
        )
    }

    private fun getTransaction(): Single<Transaction> {
        val accountId = walletInfoProvider.getWalletInfo().accountId
        val account = accountProvider.getDefaultAccount()

        val operation = CreateKYCRecoveryRequestOp(
            requestID = requestId,
            targetAccount = PublicKeyFactory.fromAccountId(accountId),
            creatorDetails = """{"blob_id":"$formBlobId"}""",
            allTasks = null,
            signersData = signersToSet.map {
                UpdateSignerData(
                    publicKey = PublicKeyFactory.fromAccountId(it.id),
                    weight = it.weight,
                    identity = it.identity,
                    details = it.detailsJson ?: "{}",
                    roleID = it.roleId,
                    ext = EmptyExt.EmptyVersion()
                )
            }.toTypedArray(),
            ext = CreateKYCRecoveryRequestOp.CreateKYCRecoveryRequestOpExt.EmptyVersion()
        )

        return TxManager.createSignedTransaction(
            networkParams, accountId, account,
            Operation.OperationBody.CreateKycRecoveryRequest(operation)
        )
    }

    private fun getNewKycRecoveryStatus(): Single<AccountRecord.KycRecoveryStatus> {
        return try {
            val transactionMeta = TransactionMeta.fromBase64(resultMetaXdr)
                    as TransactionMeta.EmptyVersion

            val requiresReview = transactionMeta
                .operations
                .first()
                .changes
                .filterIsInstance(LedgerEntryChange.Created::class.java)
                .map(LedgerEntryChange.Created::created)
                .map(LedgerEntry::data)
                .filterIsInstance(LedgerEntry.LedgerEntryData.ReviewableRequest::class.java)
                .first()
                .reviewableRequest
                .tasks
                .pendingTasks != 0

            Single.just(
                if (requiresReview)
                    AccountRecord.KycRecoveryStatus.PENDING
                else
                    AccountRecord.KycRecoveryStatus.NONE
            )
        } catch (_: Exception) {
            AccountRecord.KycRecoveryStatus.PENDING.toSingle()
        }
    }

    private fun updateRepositories() {
        accountRepository.updateKycRecoveryStatus(newKycRecoveryStatus)
    }
}