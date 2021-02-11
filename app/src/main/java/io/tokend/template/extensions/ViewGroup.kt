package io.tokend.template.extensions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

fun ViewGroup.inflate(layoutRes: Int, attach: Boolean = false): View? =
    LayoutInflater.from(context).inflate(layoutRes, this, attach)
        ?: throw IllegalArgumentException("ViewHolder not found, view = null")