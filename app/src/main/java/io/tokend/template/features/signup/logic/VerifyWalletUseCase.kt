package io.tokend.template.features.signup.logic

import io.reactivex.Completable
import io.tokend.template.features.signup.logic.VerifyWalletUseCase.InvalidCodeException
import io.tokend.template.logic.providers.ApiProvider
import org.tokend.rx.extensions.toCompletable
import org.tokend.sdk.utils.extentions.isBadRequest
import retrofit2.HttpException

/**
 * @see InvalidCodeException
 */
class VerifyWalletUseCase(
    private val code: String,
    private val walletId: String,
    private val apiProvider: ApiProvider
) {
    class InvalidCodeException : Exception()

    fun perform(): Completable {
        return apiProvider.getApi()
            .wallets
            .verify(
                walletId = walletId,
                token = code
            )
            .toCompletable()
            .onErrorResumeNext { error ->
                if (error is HttpException && error.isBadRequest())
                    Completable.error(InvalidCodeException())
                else
                    Completable.error(error)
            }
    }
}