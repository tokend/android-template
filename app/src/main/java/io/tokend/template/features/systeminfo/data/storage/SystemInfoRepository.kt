package io.tokend.template.features.systeminfo.data.storage

import io.reactivex.Single
import io.tokend.template.data.storage.persistence.ObjectPersistence
import io.tokend.template.data.storage.repository.SingleItemRepository
import io.tokend.template.features.systeminfo.data.model.SystemInfoRecord
import io.tokend.template.logic.providers.ApiProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.wallet.NetworkParams

class SystemInfoRepository(
    private val apiProvider: ApiProvider,
    persistence: ObjectPersistence<SystemInfoRecord>
) : SingleItemRepository<SystemInfoRecord>(persistence) {
    override fun getItem(): Single<SystemInfoRecord> {
        return apiProvider.getApi()
            .general
            .getSystemInfo()
            .toSingle()
            .map(::SystemInfoRecord)
    }

    fun getNetworkParams(): Single<NetworkParams> {
        return ensureData()
            .toSingle {
                item?.toNetworkParams()
                    ?: throw IllegalStateException("Missing network passphrase")
            }
    }
}