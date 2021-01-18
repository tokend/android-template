package io.tokend.template.features.signup.logic

import io.reactivex.Completable
import org.tokend.rx.extensions.toCompletable
import org.tokend.sdk.api.TokenDApi

/**
 * Requests verification sms resend
 *
 * @param walletId id of the related wallet
 */
class ResendVerificationSmsUseCase (
    private val walletId: String,
    private val api: TokenDApi
) {
    fun perform(): Completable {
        return api
            .wallets
            .requestVerification(walletId)
            .toCompletable()
    }
}