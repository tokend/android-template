package io.tokend.template.logic.session

import io.tokend.template.logic.providers.AccountProvider
import io.tokend.template.logic.providers.WalletInfoProvider

/**
 * Holds session data
 */
class Session(
    walletInfoProvider: WalletInfoProvider,
    accountProvider: AccountProvider,
    private val sessionInfoStorage: SessionInfoStorage? = null
) : WalletInfoProvider by walletInfoProvider,
    AccountProvider by accountProvider {
    var login: String = ""

    /**
     * @returns true if session is expired and so sign out is required
     */
    var isExpired = false

    /**
     * @returns true if user type is guest, false if host
     */
    var isGuest: Boolean = true
        get() = sessionInfoStorage?.loadUserType() ?: true
        set(value) {
            field = value
            sessionInfoStorage?.saveUserType(value)
        }

    /**
     * @returns current firebase token
     */
    var currentFirebaseToken: String = ""
        get() = sessionInfoStorage?.loadFirebaseToken() ?: ""
        set(value) {
            field = value
            sessionInfoStorage?.saveFirebaseToken(value)
        }

    /**
     * Resets the session to the initial state, clears data
     */
    fun reset() {
        isExpired = false

        login = ""
        setWalletInfo(null)
        setAccounts(emptyList())
    }
}