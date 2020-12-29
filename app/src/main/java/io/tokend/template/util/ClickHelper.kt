package io.tokend.template.util

import android.os.SystemClock
import android.view.View

class ClickHelper(val lockTimeMillis: Long) {

    var mLastClickTime = 0L
        private set

    private var onClickListener: ((view: View) -> Unit)? = null

    fun addViews(vararg views: View) {
        views.forEach {
            it.setOnClickListener(clickListener)
        }
    }

    fun setOnClickListener(listener: (view: View) -> Unit) {
        onClickListener = listener
    }

    fun removeOnClickListener() {
        onClickListener = null
    }

    fun canInvokeClick(onSuccess: () -> Unit) {
        if (SystemClock.elapsedRealtime() - mLastClickTime < lockTimeMillis) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()

        onSuccess.invoke()
    }

    private val clickListener = View.OnClickListener {
        if (SystemClock.elapsedRealtime() - mLastClickTime < lockTimeMillis) {
            return@OnClickListener
        }
        mLastClickTime = SystemClock.elapsedRealtime()

        onClickListener?.invoke(it)
    }
}