package io.tokend.template.features.kyc.storage

import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.tokend.template.extensions.mapSuccessful
import io.tokend.template.features.keyvalue.storage.KeyValueEntriesRepository
import io.tokend.template.features.kyc.model.KycForm
import io.tokend.template.features.logic.providers.ApiProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.integrations.kycprovider.model.generated.resources.KycResource

class AccountKycFormsRepository(
    private val keyValueEntriesRepository: KeyValueEntriesRepository,
    private val apiProvider: ApiProvider,
    private val objectMapper: ObjectMapper,
) {
    class NoKycAvailableException : Exception()

    private val itemsMap = mutableMapOf<String, KycForm>()

    fun getKyc(accountId: String): Single<KycForm> {
        return getKyc(setOf(accountId))
            .flatMapMaybe { it[accountId].toMaybe() }
            .switchIfEmpty(Single.error(NoKycAvailableException()))
    }

    fun getKyc(accountIds: Collection<String>): Single<Map<String, KycForm>> {
        val distinctIds = accountIds.distinct()

        val toRequest = distinctIds.filterNot(itemsMap::containsKey)

        if (toRequest.isEmpty()) {
            return Single.just(itemsMap)
        }

        val signedApi = apiProvider.getSignedApi()
            ?: return Single.error(IllegalStateException("No signed API instance found"))

        return toRequest
            .chunked(15)
            .map { accountsPart ->
                signedApi.integrations.kycProvider
                    .getByAccountIds(accountsPart.toSet())
                    .toSingle()
            }
            .let { Single.concat(it) }
            .collect({ mutableListOf<KycResource>() }, { res, it -> res.addAll(it) })
            .flatMap { kycResources ->
                keyValueEntriesRepository
                    .updateIfNotFreshDeferred()
                    .toSingle {
                        kycResources to keyValueEntriesRepository.itemsList
                    }
            }
            .map { (kycResources, keyValueEntries) ->
                val formsByAccountId = kycResources.mapSuccessful { kycResource ->
                    kycResource.accountId to KycForm.fromJson(
                        json = objectMapper.writeValueAsString(kycResource.details),
                        roleId = kycResource.role.toLong(),
                        keyValueEntries = keyValueEntries
                    )
                }

                itemsMap.apply { putAll(formsByAccountId) }
            }
    }
}