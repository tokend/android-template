package io.tokend.template.logic.providers

import io.tokend.template.features.account.data.storage.AccountRepository
import io.tokend.template.features.accountidentity.data.storage.AccountIdentitiesRepository
import io.tokend.template.features.assets.storage.AssetsRepository
import io.tokend.template.features.balances.storage.BalancesRepository
import io.tokend.template.features.blobs.data.storage.BlobsRepository
import io.tokend.template.features.keyvalue.storage.KeyValueEntriesRepository
import io.tokend.template.features.kyc.storage.AccountKycFormsRepository
import io.tokend.template.features.kyc.storage.ActiveKycRepository
import io.tokend.template.features.kyc.storage.KycRequestStateRepository
import io.tokend.template.features.systeminfo.data.storage.SystemInfoRepository
import io.tokend.template.features.tfa.repository.TfaFactorsRepository

/**
 * Provides SINGLETON instances of repositories
 * If you need parametrized repo use fun ...() getter and LruCache
 */
interface RepositoryProvider {
    val accountKycForms: AccountKycFormsRepository
    val accountIdentities: AccountIdentitiesRepository
    val systemInfo: SystemInfoRepository
    val tfaFactors: TfaFactorsRepository
    val account: AccountRepository
    val kycRequestState: KycRequestStateRepository
    val activeKyc: ActiveKycRepository
    val keyValueEntries: KeyValueEntriesRepository
    val blobs: BlobsRepository
    val assets: AssetsRepository
    val balances: BalancesRepository
}