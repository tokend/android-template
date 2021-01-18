package io.tokend.template.features.signin.logic

import io.tokend.template.features.signin.model.AccountType
import io.tokend.template.logic.providers.AccountProvider
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.RepositoryProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import io.tokend.template.logic.session.Session
import io.tokend.template.util.ConnectionStateUtil
import io.tokend.template.util.errorhandler.ErrorLogger

class PostSignInManagerFactory(
    private val apiProvider: ApiProvider,
    private val accountProvider: AccountProvider,
    private val walletInfoProvider: WalletInfoProvider,
    private val repositoryProvider: RepositoryProvider,
    private val connectionStateUtil: ConnectionStateUtil,
    private val session: Session,
    private val errorLogger: ErrorLogger
) {
    fun get(knownAccountType: AccountType? = null): PostSignInManager {
        return PostSignInManager(apiProvider, accountProvider,
            walletInfoProvider, repositoryProvider, session, errorLogger,
            connectionStateUtil::isOnline, knownAccountType)
    }
}