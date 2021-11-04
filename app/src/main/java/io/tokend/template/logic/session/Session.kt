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
    val login: String
        get() = getWalletInfo()?.login ?: ""

    /**
     * @returns true if session is expired and so sign out is required
     */
    var isExpired = false

    /**
     * @returns current Firebase token
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

        setWalletInfo(null)
        setAccounts(emptyList())
    }
}