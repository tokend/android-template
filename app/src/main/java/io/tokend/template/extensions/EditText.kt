package io.tokend.template.extensions

import android.text.Editable
import android.widget.EditText
import androidx.annotation.StringRes
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.disposables.Disposable
import io.tokend.template.view.util.SoftInputUtil

fun EditText.hasError(): Boolean {
    return error != null
}

fun EditText.setErrorAndFocus(@StringRes errorId: Int) {
    setErrorAndFocus(context.getString(errorId))
}

fun EditText.setErrorAndFocus(error: String) {
    this.error = error
    setSelection(text.length)
    requestFocus()
    SoftInputUtil.showSoftInputOnView(this)
}

fun EditText.onEditorAction(callback: () -> Unit) {
    this.setOnEditorActionListener { _, _, _ ->
        callback()
        true
    }
}

fun Editable?.getChars(): CharArray {
    val textLength = this?.length ?: return CharArray(0)

    val chars = CharArray(textLength)
    this.getChars(0, textLength, chars, 0)
    return chars
}

fun EditText.setText(chars: CharArray) {
    setText(chars, 0, chars.size)
}

/**
 * Function for validation input fields
 */
inline fun validateInput(inputLayout: TextInputLayout, inputView: TextInputEditText, crossinline body: () -> Unit): Disposable {
    return RxView.focusChanges(inputView)
        .skipInitialValue() // Listen for focus events.
        .map {
            if (!it) { // If view lost focus, lambda (our check logic) should be applied.
                body()
            }
            return@map it
        }
        .flatMap { hasFocus ->
            return@flatMap RxTextView.textChanges(inputView)
                .skipInitialValue()
                .map {
                    if (hasFocus && inputLayout.isErrorEnabled) inputLayout.isErrorEnabled = false
                } // Disable error when user typing.
                .skipWhile { hasFocus } // Don't react on text change events when we have a focus.
                .doOnEach { body() }
        }
        .subscribe { }
}