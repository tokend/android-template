package io.tokend.template.features.accountidentity.data.storage

import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.tokend.template.features.accountidentity.data.model.AccountIdentityRecord
import io.tokend.template.logic.providers.ApiProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.model.AttributesEntity
import org.tokend.sdk.api.base.model.DataEntity
import org.tokend.sdk.api.identity.model.IdentityResource
import org.tokend.sdk.api.identity.params.IdentitiesPageParams
import retrofit2.HttpException
import java.net.HttpURLConnection

class AccountIdentitiesRepository(
    private val apiProvider: ApiProvider
) {
    class NoIdentityAvailableException : Exception()

    private val identities = mutableSetOf<AccountIdentityRecord>()
    private val notExistingIdentifiers = mutableSetOf<String>()

    /**
     * Loads account ID for given identifier.
     * Result will be cached.
     *
     * @param identifier - email or phone number
     *
     * @see NoIdentityAvailableException
     */
    fun getAccountIdByIdentifier(identifier: String): Single<String> {
        val formattedIdentifier = identifier.toLowerCase()
        val existing = identities.find {
            it.email == formattedIdentifier || it.phoneNumber == formattedIdentifier
        }?.accountId
        if (existing != null) {
            return Single.just(existing)
        }

        return getIdentity(IdentitiesPageParams(identifier = formattedIdentifier))
            .map(AccountIdentityRecord::accountId)
    }

    /**
     * Loads email for given account ID.
     * Result will be cached.
     *
     * @see NoIdentityAvailableException
     */
    fun getEmailByAccountId(accountId: String): Single<String> {
        val existing = identities.find { it.accountId == accountId }?.email
        if (existing != null) {
            return Single.just(existing)
        }

        return getIdentity(IdentitiesPageParams(address = accountId))
            .map(AccountIdentityRecord::email)
    }

    /**
     * Loads phone number for given account ID if it exists.
     * Result will be cached.
     *
     * @see NoIdentityAvailableException
     */
    fun getPhoneByAccountId(accountId: String): Maybe<String> {
        val existingIdentity = identities.find { it.accountId == accountId }

        if (existingIdentity != null) {
            return existingIdentity.phoneNumber.toMaybe()
        }

        return getIdentity(IdentitiesPageParams(address = accountId))
            .flatMapMaybe { it.phoneNumber.toMaybe() }
    }

    /**
     * Loads Telegram username for given account ID if it exists.
     * Result will be cached.
     *
     * @see NoIdentityAvailableException
     */
    fun getTelegramUsernameByAccountId(accountId: String): Maybe<String> {
        val existingIdentity = identities.find { it.accountId == accountId }

        if (existingIdentity != null) {
            return existingIdentity.telegramUsername.toMaybe()
        }

        return getIdentity(IdentitiesPageParams(address = accountId))
            .flatMapMaybe { it.telegramUsername.toMaybe() }
    }

    fun getEmailsByAccountIds(accountIds: Collection<String>): Single<Map<String, String>> {
        val toReturn = mutableMapOf<String, String>()
        val toRequest = mutableListOf<String>()

        val identitiesByAccountId = identities.associateBy(AccountIdentityRecord::accountId)

        accountIds
            .forEach { accountId ->
                val cached = identitiesByAccountId[accountId]
                if (cached != null) {
                    toReturn[accountId] = cached.email
                } else if (!notExistingIdentifiers.contains(accountId)) {
                    toRequest.add(accountId)
                }
            }

        if (toRequest.isEmpty()) {
            return Single.just(toReturn)
        }

        val signedApi = apiProvider.getSignedApi()

        return signedApi
            .identities
            .getForAccounts(toRequest)
            .toSingle()
            .map {
                it.map(::AccountIdentityRecord)
            }
            .map { identities ->
                this.identities.addAll(identities)
                toReturn.putAll(
                    identities
                        .associateBy(AccountIdentityRecord::accountId)
                        .mapValues { it.value.email }
                )
                toReturn
            }
    }

    private fun getIdentity(params: IdentitiesPageParams): Single<AccountIdentityRecord> {
        val identifier = params.identifier ?: params.address

        if (identifier != null && notExistingIdentifiers.contains(identifier)) {
            return Single.error(NoIdentityAvailableException())
        }

        return apiProvider
            .getApi()
            .identities
            .get(params)
            .toSingle()
            .map { detailsPage ->
                detailsPage.items.firstOrNull()
                    ?: throw NoIdentityAvailableException()
            }
            .onErrorResumeNext {
                if (it is HttpException && it.code() == HttpURLConnection.HTTP_NOT_FOUND)
                    Single.error(NoIdentityAvailableException())
                else
                    Single.error(it)
            }
            .map(::AccountIdentityRecord)
            .doOnSuccess { identities.add(it) }
            .doOnError {
                if (it is NoIdentityAvailableException && identifier != null) {
                    notExistingIdentifiers.add(identifier)
                }
            }
    }

    fun getCachedIdentity(accountId: String): AccountIdentityRecord? {
        return identities.find { it.accountId == accountId }
    }

    fun invalidateCachedIdentity(accountId: String) {
        identities.remove(getCachedIdentity(accountId))
    }

    fun createIdentity(phoneNumber: String): Single<AccountIdentityRecord> {
        return apiProvider.getApi().customRequests
            .post(
                url = "identities",
                body = DataEntity(AttributesEntity(mapOf("phone_number" to phoneNumber))),
                responseClass = IdentityResource::class.java
            )
            .toSingle()
            .map(::AccountIdentityRecord)
            .doOnSuccess { createdIdentity ->
                identities.add(createdIdentity)
                listOf(
                    createdIdentity.accountId,
                    createdIdentity.email,
                    createdIdentity.phoneNumber
                )
                    .forEach {
                        notExistingIdentifiers.remove(it)
                    }
            }
    }
}