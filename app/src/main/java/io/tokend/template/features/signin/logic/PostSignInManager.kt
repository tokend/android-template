package io.tokend.template.features.signin.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toMaybe
import io.tokend.template.features.account.data.model.AccountRecord
import io.tokend.template.features.kyc.model.KycForm
import io.tokend.template.features.recovery.logic.SubmitKycRecoveryRequestUseCase
import io.tokend.template.features.signin.model.AccountType
import io.tokend.template.logic.TxManager
import io.tokend.template.logic.providers.AccountProvider
import io.tokend.template.logic.providers.ApiProvider
import io.tokend.template.logic.providers.RepositoryProvider
import io.tokend.template.logic.providers.WalletInfoProvider
import io.tokend.template.logic.session.Session
import io.tokend.template.util.errorhandler.ErrorLogger
import org.tokend.sdk.utils.extentions.isUnauthorized
import retrofit2.HttpException

class PostSignInManager(
    private val apiProvider: ApiProvider,
    private val accountProvider: AccountProvider,
    private val walletInfoProvider: WalletInfoProvider,
    private val repositoryProvider: RepositoryProvider,
    private val session: Session,
    private val errorLogger: ErrorLogger?,
    private val connectionStateProvider: (() -> Boolean)? = null,
    private val knownAccountType: AccountType? = null
) {
    class AuthMismatchException : Exception()

    private val isOnline: Boolean
        get() = connectionStateProvider?.invoke() ?: true

    /**
     * Updates all important repositories.
     */
    fun doPostSignIn(): Completable {
        val parallelActions = listOf(
            // Added actions will be performed simultaneously.

            repositoryProvider.activeKyc
                .run {
                    if (isOnline)
                        updateDeferred()
                    else
                        ensureData().doOnComplete {
                            if (!isFresh) {
                                update()
                            }
                        }
                },
        )
        val syncActions = listOf<Completable>(
            // Added actions will be performed on after another in
            // provided order.

            repositoryProvider.account
                .run {
                    if (isOnline)
                        updateDeferred()
                    else
                        ensureData()
                }
                .andThen(Completable.defer { sendEmptyKycRecoveryRequestIfNeeded() }),
        )

        val performParallelActions = Completable.merge(parallelActions)
        val performSyncActions = Completable.concat(syncActions)

        repositoryProvider.tfaFactors.invalidate()
        repositoryProvider.systemInfo.ensureData()
            .subscribeBy(onError = { errorLogger?.log(it) })

        /**
         * Send Firebase token to start receiving notifications somewhere here
         */

        return performSyncActions
            .andThen(performParallelActions)
            .onErrorResumeNext {
                if (it is HttpException && it.isUnauthorized())
                    Completable.error(AuthMismatchException())
                else
                    Completable.error(it)
            }
    }

    private fun sendEmptyKycRecoveryRequestIfNeeded(): Completable {
        if (!isOnline || repositoryProvider.account.item!!.kycRecoveryStatus
            != AccountRecord.KycRecoveryStatus.INITIATED
        ) {
            return Completable.complete()
        }

        return SubmitKycRecoveryRequestUseCase(
            form = KycForm.Empty,
            apiProvider = apiProvider,
            repositoryProvider = repositoryProvider,
            accountProvider = accountProvider,
            walletInfoProvider = walletInfoProvider,
            txManager = TxManager(apiProvider)
        )
            .perform()
    }

    /**
     * Include this to post sign in flow if your app requires account types.
     */
    private fun getOrLoadAccountType(): Single<AccountType> {
        return knownAccountType
            .toMaybe()
            .switchIfEmpty(
                FindOutAccountTypeUseCase(
                    phoneNumber = session.login,
                    checkWalletExistence = false,
                    repositoryProvider = repositoryProvider,
                    apiProvider = apiProvider
                )
                    .perform()
                    .map(FindOutAccountTypeUseCase.Result::type)
            )
    }
}