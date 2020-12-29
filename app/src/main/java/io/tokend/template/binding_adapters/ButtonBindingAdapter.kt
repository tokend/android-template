package io.tokend.template.binding_adapters

import android.widget.Button
import androidx.databinding.BindingAdapter

@BindingAdapter("isEnabled")
fun Button.isEnabled(isEnabled: Boolean) {
    this.isEnabled = isEnabled
}