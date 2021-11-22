package io.tokend.template.features.tfa.view

import android.content.Context
import android.text.InputType
import io.tokend.template.R
import io.tokend.template.util.errorhandler.ErrorHandler
import io.tokend.template.view.ToastManager
import org.tokend.sdk.tfa.NeedTfaException
import org.tokend.sdk.tfa.PasswordTfaOtpGenerator
import org.tokend.sdk.tfa.TfaVerifier

/**
 * TFA verification dialog requesting user's password and
 * forming OTP from it.
 */
class TfaPasswordDialog(
    context: Context,
    errorHandler: ErrorHandler,
    tfaVerifierInterface: TfaVerifier.Interface,
    private val tfaException: NeedTfaException,
    private val login: String,
    private val toastManager: ToastManager?
) : TfaDialog(context, errorHandler, tfaVerifierInterface) {
    override fun beforeDialogShow() {
        super.beforeDialogShow()

        inputEditText.apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        inputEditTextLayout.apply {
            hint = context.getString(R.string.password)
        }
    }

    override fun getOtp(input: CharArray): String {
        return PasswordTfaOtpGenerator().generate(tfaException, login, input)
    }

    override fun getMessage(): String {
        return context.getString(R.string.enter_your_password)
    }

    override fun getInvalidInputError(): String {
        return context.getString(R.string.error_invalid_password)
    }
}