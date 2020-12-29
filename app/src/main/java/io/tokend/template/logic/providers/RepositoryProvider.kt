package io.tokend.template.logic.providers

import io.tokend.template.features.account.data.storage.AccountRepository
import io.tokend.template.features.blobs.data.storage.BlobsRepository
import io.tokend.template.features.keyvalue.storage.KeyValueEntriesRepository
import io.tokend.template.features.kyc.storage.AccountKycFormsRepository
import io.tokend.template.features.kyc.storage.ActiveKycRepository
import io.tokend.template.features.kyc.storage.KycRequestStateRepository
import io.tokend.template.features.systeminfo.data.storage.SystemInfoRepository
import io.tokend.template.features.tfa.repository.TfaFactorsRepository

interface RepositoryProvider {
    val accountKycForms: AccountKycFormsRepository
    val systemInfo: SystemInfoRepository
    val tfaFactors: TfaFactorsRepository
    val account: AccountRepository
    val kycRequestState: KycRequestStateRepository
    val activeKyc: ActiveKycRepository
    val keyValueEntries: KeyValueEntriesRepository
    val blobs: BlobsRepository
}