package io.tokend.template.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Simplifies interaction with [ToastManager]
 */
class ToastManager(
    private val context: Context
) {
    /**
     * Shows a toast with [Toast.LENGTH_SHORT] length and given text
     */
    fun short(text: String?) {
        text?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shows a toast with [Toast.LENGTH_SHORT] length and given text
     */
    fun short(@StringRes text: Int) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    /**
     * Shows a toast with [Toast.LENGTH_LONG] length and given text
     */
    fun long(text: String?) {
        text?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shows a toast with [Toast.LENGTH_LONG] length and given text
     */
    fun long(@StringRes text: Int) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    fun makeFun() {
        var (frames, frame) = listOf(
            listOf("~(•◡•~)", "\\(•◡•)/", "(~•◡•)~") to 1,
            listOf("( ͡° ͜ʖ ͡°)", "( ͡~ ͜ʖ ͡°)") to 0,
            listOf(
                "      \uD83D\uDE82",
                "   \uD83D\uDE82\uD83D\uDE83",
                "\uD83D\uDE82\uD83D\uDE83\uD83D\uDE83",
                "\uD83D\uDE83\uD83D\uDE83\uD83D\uDE83",
                "\uD83D\uDE83\uD83D\uDE83   ",
                "\uD83D\uDE83      ",
                "         "
            ) to 0
        ).random()
        var forward = true

        val toast = Toast.makeText(context, frames[frame], Toast.LENGTH_SHORT).apply {
            setGravity(Gravity.CENTER, 0, 0)
            show()
        }

        val mainThreadHandler = Handler(Looper.getMainLooper())
        val start = System.currentTimeMillis()

        Thread {
            while (System.currentTimeMillis() - start < 2000) {
                Thread.sleep(320)
                frame += if (forward) 1 else -1
                if (frame == frames.size - 1) {
                    forward = false
                } else if (frame == 0) {
                    forward = true
                }
                mainThreadHandler.post { toast.setText(frames[frame]) }
            }
        }.start()
    }
}