package io.tokend.template.logic.providers

import android.content.Context
import android.content.SharedPreferences
import com.fasterxml.jackson.databind.ObjectMapper
import io.tokend.template.data.storage.persistence.MemoryOnlyObjectPersistence
import io.tokend.template.data.storage.persistence.ObjectPersistenceOnPrefs
import io.tokend.template.data.storage.repository.MemoryOnlyRepositoryCache
import io.tokend.template.db.AppDatabase
import io.tokend.template.features.account.data.model.AccountRecord
import io.tokend.template.features.account.data.storage.AccountRepository
import io.tokend.template.features.accountidentity.data.storage.AccountIdentitiesRepository
import io.tokend.template.features.assets.storage.AssetsRepository
import io.tokend.template.features.balances.storage.BalancesRepository
import io.tokend.template.features.blobs.data.storage.BlobsRepository
import io.tokend.template.features.keyvalue.storage.KeyValueEntriesRepository
import io.tokend.template.features.kyc.storage.AccountKycFormsRepository
import io.tokend.template.features.kyc.storage.ActiveKycPersistence
import io.tokend.template.features.kyc.storage.ActiveKycRepository
import io.tokend.template.features.kyc.storage.KycRequestStateRepository
import io.tokend.template.features.systeminfo.data.model.SystemInfoRecord
import io.tokend.template.features.systeminfo.data.storage.SystemInfoRepository
import io.tokend.template.features.tfa.repository.TfaFactorsRepository
import io.tokend.template.features.urlconfig.providers.UrlConfigProvider

/**
 * @param context if not specified then android-related repositories
 * will be unavailable
 */
class AppRepositoryProvider(
    private val apiProvider: ApiProvider,
    private val walletInfoProvider: WalletInfoProvider,
    private val urlConfigProvider: UrlConfigProvider,
    private val accountProvider: AccountProvider,
    private val mapper: ObjectMapper,
    private val context: Context? = null,
    private val persistencePreferences: SharedPreferences? = null,
    private val database: AppDatabase? = null
) : RepositoryProvider {

    override val accountIdentities: AccountIdentitiesRepository by lazy {
        AccountIdentitiesRepository(apiProvider)
    }

    override val accountKycForms: AccountKycFormsRepository by lazy {
        AccountKycFormsRepository(keyValueEntries, apiProvider, mapper)
    }

    override val systemInfo: SystemInfoRepository by lazy {
        val persistence =
            if (persistencePreferences != null)
                ObjectPersistenceOnPrefs.forType(
                    persistencePreferences,
                    "system_info"
                )
            else
                MemoryOnlyObjectPersistence<SystemInfoRecord>()
        SystemInfoRepository(apiProvider, persistence)
    }

    override val tfaFactors: TfaFactorsRepository by lazy {
        TfaFactorsRepository(apiProvider, walletInfoProvider, MemoryOnlyRepositoryCache())
    }

    override val account: AccountRepository by lazy {
        val persistence =
            if (persistencePreferences != null)
                ObjectPersistenceOnPrefs.forType(
                    persistencePreferences,
                    "account_record"
                )
            else
                MemoryOnlyObjectPersistence<AccountRecord>()

        AccountRepository(apiProvider, walletInfoProvider, keyValueEntries, persistence)
    }

    override val keyValueEntries: KeyValueEntriesRepository by lazy {
        KeyValueEntriesRepository(apiProvider, MemoryOnlyRepositoryCache())
    }

    override val blobs: BlobsRepository by lazy {
        BlobsRepository(apiProvider, walletInfoProvider)
    }

    override val kycRequestState: KycRequestStateRepository by lazy {
        KycRequestStateRepository(apiProvider, walletInfoProvider, blobs, keyValueEntries)
    }

    override val activeKyc: ActiveKycRepository by lazy {
        val persistence = persistencePreferences?.let(::ActiveKycPersistence)
        ActiveKycRepository(account, blobs, persistence)
    }

    override val balances: BalancesRepository by lazy {
        BalancesRepository(
            apiProvider,
            walletInfoProvider,
            urlConfigProvider,
            mapper,
            conversionAssetCode = null,
            itemsCache = MemoryOnlyRepositoryCache()
        )
    }

    override val assets: AssetsRepository by lazy {
        AssetsRepository(
            null, apiProvider, urlConfigProvider,
            mapper, MemoryOnlyRepositoryCache()
        )
    }
}