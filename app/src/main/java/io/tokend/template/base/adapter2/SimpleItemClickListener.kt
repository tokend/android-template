package io.tokend.template.base.adapter2

import android.view.View

fun interface SimpleItemClickListener<T> {
    fun onViewClicked(view: View?, item: T)
}