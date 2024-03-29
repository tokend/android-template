package io.tokend.template.features.tfa.view

import android.content.Context
import io.tokend.template.util.errorhandler.ErrorHandler
import io.tokend.template.view.ToastManager
import org.tokend.sdk.api.tfa.model.TfaFactor
import org.tokend.sdk.tfa.NeedTfaException
import org.tokend.sdk.tfa.TfaVerifier

class TfaDialogFactory(
    private val context: Context,
    private val errorHandler: ErrorHandler,
    private val toastManager: ToastManager?
) {
    /**
     * @return verification dialog for specified exception.
     * If there is no special dialog for given TFA factor type
     * then [TfaDefaultDialog] will be returned
     */
    @JvmOverloads
    fun getForException(
        tfaException: NeedTfaException,
        verifierInterface: TfaVerifier.Interface,
        login: String? = null
    ): TfaDialog? {
        return when (tfaException.factorType) {
            TfaFactor.Type.PASSWORD -> {
                if (login != null)
                    TfaPasswordDialog(
                        context, errorHandler, verifierInterface,
                        tfaException, login, toastManager
                    )
                else
                    null
            }
            TfaFactor.Type.TOTP -> TfaTotpDialog(context, errorHandler, verifierInterface)
            TfaFactor.Type.EMAIL -> TfaEmailOtpDialog(context, errorHandler, verifierInterface)
            TfaFactor.Type.PHONE -> TfaPhoneDialog(context, errorHandler, verifierInterface)
            else -> TfaDefaultDialog(context, errorHandler, verifierInterface)
        }
    }
}