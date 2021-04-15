package io.tokend.template.view

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.core.content.res.ResourcesCompat
import io.tokend.template.R
import kotlinx.android.synthetic.main.progress_dialog_view.view.*

class ProgressDialog {
    var dialog: CustomDialog? = null

    fun show(context: Context): Dialog {
        return show(context, null)
    }

    fun show(context: Context, title: CharSequence?): Dialog {
        val inflater = (context as Activity).layoutInflater
        val view = inflater.inflate(R.layout.progress_dialog_view, null)

        if (title != null) {
            view.cp_title.text = title
        } else {
            view.cp_title.visibility = View.GONE
            view.cp_top_space.visibility = View.GONE
        }

        // Progress Bar Color
        setColorFilter(
            view.cp_pbar.indeterminateDrawable,
            ResourcesCompat.getColor(context.resources, R.color.primary, null)
        )

        dialog = CustomDialog(context)
        dialog?.setContentView(view)
        dialog?.show()
        return dialog!!
    }

    private fun setColorFilter(drawable: Drawable, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            drawable.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
        } else {
            @Suppress("DEPRECATION")
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }
    }

    class CustomDialog(context: Context) : Dialog(context, R.style.CustomDialogTheme) {
        init {
            // Set Semi-Transparent Color for Dialog Background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                window?.decorView?.setOnApplyWindowInsetsListener { _, insets ->
                    insets.consumeSystemWindowInsets()
                }
            }
        }
    }
}