package io.tokend.template.features.recovery.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.tokend.template.features.signup.logic.WalletAccountsUtil
import io.tokend.template.logic.credentials.persistence.CredentialsPersistence
import io.tokend.template.logic.providers.ApiProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.keyserver.models.WalletCreateResult
import org.tokend.wallet.Account

/**
 * Recovers user's password, first step of KYC recovery.
 * Clears [credentialsPersistence] on success if it's provided
 */
class RecoverPasswordUseCase(
    private val email: String,
    private val newPassword: CharArray,
    private val apiProvider: ApiProvider,
    private val credentialsPersistence: CredentialsPersistence?
) {
    private lateinit var newAccounts: List<Account>

    fun perform(): Completable {
        return generateNewAccounts()
            .doOnSuccess { newAccounts ->
                this.newAccounts = newAccounts
            }
            .flatMap {
                recoverPassword()
            }
            .doOnSuccess {
                clearSavedCredentials()
            }
            .ignoreElement()
    }

    private fun generateNewAccounts(): Single<List<Account>> {
        return WalletAccountsUtil.getAccountsForNewWallet()
    }

    private fun recoverPassword(): Single<WalletCreateResult> {
        return KeyServer(apiProvider.getApi().wallets)
            .recoverWalletPassword(
                email,
                newPassword,
                newAccounts
            )
            .toSingle()
    }

    private fun clearSavedCredentials() {
        credentialsPersistence?.clear(keepLogin = true)
    }
}