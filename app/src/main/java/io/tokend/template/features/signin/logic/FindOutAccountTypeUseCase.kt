package io.tokend.template.features.signin.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.tokend.template.features.accountidentity.data.storage.AccountIdentitiesRepository
import io.tokend.template.features.keyvalue.model.KeyValueEntryRecord
import io.tokend.template.features.signin.logic.FindOutAccountTypeUseCase.AccountDoesNotExistException
import io.tokend.template.features.signin.logic.FindOutAccountTypeUseCase.AccountHasUnknownType
import io.tokend.template.features.signin.model.AccountType
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.RepositoryProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.wallets.model.InvalidCredentialsException
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.utils.extentions.isServerError
import retrofit2.HttpException

/**
 * Used if registration in your app is limited by invitations (identity creation).
 *
 * Account type is not a KYC form type, it's rather an access group name.
 * For example, if invited account is pre-registered (role:unverified) or registered(role:general)
 * it has User type anyway, meaning that this account can use the app.
 *
 * @see AccountDoesNotExistException
 * @see AccountHasUnknownType
 */
class FindOutAccountTypeUseCase(
    private val phoneNumber: String,
    private val checkWalletExistence: Boolean,
    private val apiProvider: ApiProvider,
    private val repositoryProvider: RepositoryProvider
) {
    data class Result(
        val type: AccountType,
        val walletExists: Boolean
    )

    class AccountHasUnknownType(phoneNumber: String,
                                accountId: String,
                                roleId: Long) :
        IllegalStateException("Account $accountId of $phoneNumber has unknown type (role) $roleId")

    class AccountDoesNotExistException(phoneNumber: String) :
        IllegalStateException("There is no account for $phoneNumber")

    private lateinit var accountId: String
    private var roleId: Long = 0
    private var generalRoleId: Long = 0
    private var unverifiedRoleId: Long = 0
    private var walletExists: Boolean = false

    fun perform(): Single<Result> {
        return getAccountId()
            .doOnSuccess { accountId ->
                this.accountId = accountId
            }
            .flatMap {
                getRoleId()
            }
            .doOnSuccess { roleId ->
                this.roleId = roleId
            }
            .flatMap {
                getActualRoles()
            }
            .doOnSuccess { (generalRoleId, unverifiedRoleId) ->
                this.generalRoleId = generalRoleId
                this.unverifiedRoleId = unverifiedRoleId
            }
            .flatMap {
                getWalletExistence()
            }
            .doOnSuccess { walletExists ->
                this.walletExists = walletExists
            }
            .flatMap {
                getType()
            }
            .map { type ->
                Result(type, walletExists)
            }
    }

    private fun getAccountId(): Single<String> {
        return repositoryProvider.accountIdentities
            .getAccountIdByIdentifier(
                identifier = phoneNumber
            )
            .onErrorResumeNext { error ->
                if (error is AccountIdentitiesRepository.NoIdentityAvailableException)
                    Single.error(AccountDoesNotExistException(phoneNumber))
                else
                    Single.error(error)
            }
    }

    private fun getRoleId(): Single<Long> {
        return apiProvider.getApi().v3.accounts
            .getById(accountId)
            .toSingle()
            .map { it.role.id.toLong() }
    }

    private fun getActualRoles(): Single<Pair<Long, Long>> {
        return repositoryProvider.keyValueEntries
            .ensureEntries(setOf(
                AccountType.User.ROLE_KEY,
                ACCOUNT_ROLE_UNVERIFIED_KEY
            ))
            .map { keyValues ->
                val general = (keyValues.getValue(AccountType.User.ROLE_KEY) as KeyValueEntryRecord.Number)
                    .value
                val unverified = (keyValues.getValue(ACCOUNT_ROLE_UNVERIFIED_KEY) as KeyValueEntryRecord.Number)
                    .value

                Pair(general, unverified)
            }
    }

    private fun getWalletExistence(): Single<Boolean> {
        if (!checkWalletExistence) {
            return Single.just(true)
        }

        return KeyServer(apiProvider.getApi().wallets)
            .getLoginParams(
                login = phoneNumber,
                isRecovery = false
            )
            .toSingle()
            .map { true }
            .onErrorResumeNext { error ->
                if (error is InvalidCredentialsException
                    // Workaround for mobile ISP ads redirect on HTTP errors...
                    || error is HttpException && !error.isServerError())
                    Single.just(false)
                else
                    Single.error(error)
            }
    }

    private fun getType(): Single<AccountType> {
        return when (roleId) {
            // Implement your own role -> type mapping here

            generalRoleId,
            unverifiedRoleId ->
                AccountType.User(generalRoleId).toSingle()
            else ->
                Single.error(AccountHasUnknownType(phoneNumber, accountId, roleId))
        }
    }

    private companion object {
        private const val ACCOUNT_ROLE_UNVERIFIED_KEY = "account_role:unverified"
    }
}