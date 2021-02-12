package io.tokend.template.extensions

import android.text.Editable
import android.widget.EditText
import androidx.annotation.StringRes
import com.google.android.material.textfield.TextInputLayout
import io.tokend.template.view.util.SoftInputUtil

fun TextInputLayout.hasError(): Boolean {
    return error != null || isCounterEnabled && (editText?.text?.length ?: 0) > counterMaxLength
}

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

fun TextInputLayout.setErrorAndFocus(@StringRes errorId: Int) {
    setErrorAndFocus(context.getString(errorId))
}

fun TextInputLayout.setErrorAndFocus(error: String) {
    isErrorEnabled = true
    this.error = error
    editText?.apply {
        setSelection(text.length)
        requestFocus()
        SoftInputUtil.showSoftInputOnView(this)
    }
}

fun TextInputLayout.clearError() {
    error = null
    isErrorEnabled = false
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