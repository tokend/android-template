package io.tokend.template.extensions

import android.content.*
import android.net.Uri
import io.tokend.template.R

var Context.clipboardText: CharSequence?
    set(value) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (value != null) {
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    getString(R.string.app_name),
                    value
                )
            )
        }
    }
    get() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (!clipboard.hasPrimaryClip()) {
            return null
        }
        if (clipboard.primaryClipDescription
                ?.run {
                    hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                            || hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
                } != true
        ) {
            return null
        }
        val primaryClip = clipboard.primaryClip
            ?: return null
        return primaryClip
            .takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.text
    }

fun Context.browse(uri: String): Boolean =
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()