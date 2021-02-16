package io.tokend.template.extensions

import androidx.annotation.StringRes
import com.google.android.material.textfield.TextInputLayout
import io.tokend.template.view.util.SoftInputUtil

fun TextInputLayout.hasError(): Boolean {
    return error != null || isCounterEnabled && (editText?.text?.length ?: 0) > counterMaxLength
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