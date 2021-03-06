package io.tokend.template.features.signin.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import io.tokend.template.logic.credentials.model.WalletInfoRecord
import io.tokend.template.logic.credentials.persistence.CredentialsPersistence
import io.tokend.template.logic.credentials.persistence.WalletInfoPersistence
import io.tokend.template.logic.session.Session
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.wallet.Account

/**
 * Performs sign in with given credentials:
 * performs keypair loading and decryption,
 * sets up [Session], updates [CredentialsPersistence] if it is set.
 * If [CredentialsPersistence] contains saved credentials no network calls will be performed.
 *
 * @see PostSignInManager
 */
class SignInUseCase(
    private val login: String,
    private val password: CharArray,
    private val keyServer: KeyServer,
    private val session: Session,
    private val credentialsPersistence: CredentialsPersistence?,
    private val walletInfoPersistence: WalletInfoPersistence?,
    private val postSignInActions: (() -> Completable)?
) {
    private lateinit var walletInfo: WalletInfoRecord
    private lateinit var accounts: List<Account>

    private var ignoreCredentialsPersistence = false

    fun perform(): Completable {
        val scheduler = Schedulers.newThread()

        return getWalletInfo(login, password)
            .doOnSuccess { walletInfo ->
                this.walletInfo = walletInfo
            }
            .observeOn(scheduler)
            .flatMap {
                getAccountsFromWalletInfo()
            }
            .doOnSuccess { accounts ->
                this.accounts = accounts
            }
            .flatMap {
                updateProviders()
            }
            .doOnSuccess {
                walletInfo.eraseSeeds()
            }
            .flatMap {
                performPostSignIn()
            }
            .observeOn(scheduler)
            .doOnError { error ->
                // In this case it is possible that password had been changed
                // to the same one, so we can try one more time.
                if (error is PostSignInManager.AuthMismatchException) {
                    ignoreCredentialsPersistence = true
                }
            }
            .retry { attempt, error ->
                error is PostSignInManager.AuthMismatchException && attempt == 1
            }
            .ignoreElement()
    }

    private fun getWalletInfo(login: String, password: CharArray) = Single.defer {
        val networkRequest = keyServer
            .getWalletInfo(login, password)
            .toSingle()
            .map(::WalletInfoRecord)

        walletInfoPersistence
            ?.takeIf { !ignoreCredentialsPersistence }
            ?.loadWalletInfoMaybe(login, password)
            ?.switchIfEmpty(networkRequest)
            ?: networkRequest
    }

    private fun getAccountsFromWalletInfo(): Single<List<Account>> {
        return {
            walletInfo.getAccounts()
        }.toSingle().subscribeOn(Schedulers.computation())
    }

    private fun updateProviders(): Single<Boolean> {
        Companion.updateProviders(
            walletInfo, accounts, login, password,
            session, credentialsPersistence, walletInfoPersistence
        )
        return Single.just(true)
    }

    private fun performPostSignIn(): Single<Boolean> {
        return postSignInActions
            ?.invoke()
            ?.toSingleDefault(true)
            ?: Single.just(false)
    }

    companion object {
        fun updateProviders(
            walletInfo: WalletInfoRecord,
            accounts: List<Account>,
            login: String,
            password: CharArray,
            session: Session,
            credentialsPersistence: CredentialsPersistence?,
            walletInfoPersistence: WalletInfoPersistence?
        ) {
            session.login = login
            session.setWalletInfo(walletInfo)
            session.setAccounts(accounts)

            credentialsPersistence?.saveCredentials(login, password)
            walletInfoPersistence?.saveWalletInfo(walletInfo, login, password)
        }
    }
}