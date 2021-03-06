package io.tokend.template.features.tfa.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.textfield.TextInputLayout
import io.tokend.template.R
import io.tokend.template.extensions.getChars
import io.tokend.template.extensions.hasError
import io.tokend.template.extensions.onEditorAction
import io.tokend.template.extensions.setErrorAndFocus
import io.tokend.template.util.errorhandler.ErrorHandler
import io.tokend.template.view.util.LoadingIndicatorManager
import org.tokend.sdk.tfa.InvalidOtpException
import org.tokend.sdk.tfa.TfaVerifier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Abstract TFA verification dialog with code input.
 */
abstract class TfaDialog(
    protected val context: Context,
    protected val errorHandler: ErrorHandler,
    private val tfaVerifierInterface: TfaVerifier.Interface
) {
    protected val inputEditTextLayout: TextInputLayout
    protected val inputEditText: EditText
    protected val inputButtonImageView: AppCompatImageView
    protected val messageTextView: TextView
    protected lateinit var progress: ProgressBar
    protected val dialog: AlertDialog
    protected val verificationExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    protected val mainThreadHandler: Handler = Handler(Looper.getMainLooper())

    protected val loadingIndicator = LoadingIndicatorManager(
        showLoading = { progress.visibility = View.VISIBLE },
        hideLoading = { progress.visibility = View.GONE }
    )

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_tfa, null)

        progress = view.findViewById(R.id.progress)
        messageTextView = view.findViewById(R.id.tfa_message_text_view)
        inputEditTextLayout = view.findViewById(R.id.tfa_input_text_layout)
        inputEditText = view.findViewById(R.id.tfa_input_edit_text)
        inputButtonImageView = view.findViewById(R.id.tfa_input_button_image_view)

        dialog = AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setTitle(R.string.tfa_dialog_title)
            .setView(view)
            .setPositiveButton(R.string.continue_action, null)
            .setNegativeButton(R.string.cancel) { _, _ ->
                tfaVerifierInterface.cancelVerification()
            }
            .setOnCancelListener {
                tfaVerifierInterface.cancelVerification()
            }
            .also {
                extendDialogBuilder(it)
            }
            .create()
    }

    open fun show() {
        if (!dialog.isShowing) {
            beforeDialogShow()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            dialog.show()
            afterDialogShown()
        }
    }

    protected open fun extendDialogBuilder(builder: AlertDialog.Builder) {}

    protected open fun beforeDialogShow() {
        inputEditText.onEditorAction {
            tryToVerify()
        }
        messageTextView.text = getMessage()
    }

    protected open fun afterDialogShown() {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener {
                tryToVerify()
            }
    }

    protected open fun tryToVerify() {
        if (canVerify()) {
            verify()
        }
    }

    protected open fun canVerify(): Boolean {
        return !inputEditTextLayout.hasError() && !loadingIndicator.isLoading
    }

    protected open fun getOtp(input: CharArray): String {
        return input.joinToString("")
    }

    protected open fun verify() {
        loadingIndicator.show()

        val input = inputEditText.text.getChars()

        verificationExecutor.submit {
            tfaVerifierInterface.verify(
                otp = getOtp(input),
                onSuccess = {
                    mainThreadHandler.post(this::onSuccessfulVerification)
                },
                onError = { error ->
                    mainThreadHandler.post { onVerificationError(error) }
                }
            )
        }
    }

    protected open fun onSuccessfulVerification() {
        if (!dialog.isShowing) {
            return
        }

        loadingIndicator.hide()
        dialog.dismiss()
    }

    protected open fun onVerificationError(error: Throwable?) {
        if (!dialog.isShowing) {
            return
        }

        loadingIndicator.hide()
        if (error is InvalidOtpException) {
            inputEditTextLayout.setErrorAndFocus(getInvalidInputError())
        } else if (error != null) {
            errorHandler.handle(error)
        }
    }

    abstract fun getMessage(): CharSequence
    abstract fun getInvalidInputError(): String
}