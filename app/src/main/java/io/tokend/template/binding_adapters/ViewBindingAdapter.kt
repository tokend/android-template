package io.tokend.template.binding_adapters

import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("isEnabled")
fun TextView.isEnabled(isEnabled: Boolean) {
    this.isEnabled = isEnabled
}

@BindingAdapter("isVisible")
fun View.isVisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

@BindingAdapter("isInVisible")
fun View.isInVisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.INVISIBLE
}