package io.tokend.template.extensions

import android.os.SystemClock
import android.view.View

const val LOCK_TIME_MILLIS = 350

var mLastClickTime = 0L
    private set

fun canInvokeClick(onSuccess: () -> Unit = {}): Boolean {
    if (SystemClock.elapsedRealtime() - mLastClickTime < LOCK_TIME_MILLIS) {
        return false
    }
    mLastClickTime = SystemClock.elapsedRealtime()

    onSuccess.invoke()
    return true
}

fun View.singleOnClick(listener: (view: View) -> Unit) {
    this.setOnClickListener {
        if (SystemClock.elapsedRealtime() - mLastClickTime < LOCK_TIME_MILLIS) {
            return@setOnClickListener
        }
        mLastClickTime = SystemClock.elapsedRealtime()
        listener.invoke(this)
    }
}