package io.tokend.template.binding_adapters

import android.widget.TextView
import androidx.databinding.BindingAdapter
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("isEnabled")
fun TextView.isEnabled(isEnabled: Boolean) {
    this.isEnabled = isEnabled
}

@BindingAdapter("dayInWeekFormat")
fun TextView.formatDayInWeek(date: Date?) {
    if (date != null) {
        val locale = Locale.getDefault()
        this.text = SimpleDateFormat("EEEE", locale).format(date)
    }
}

@BindingAdapter("dayInWeekWithTimeFormat")
fun TextView.formatDayInWeekWithTime(date: Date?) {
    if (date != null) {
        val locale = Locale.getDefault()
        this.text = "${SimpleDateFormat("EEEE", locale).format(date)} \n ${
            SimpleDateFormat(
            "h:mm a",
            locale
        ).format(date)}"
    }
}

@BindingAdapter("compactDateFormat")
fun TextView.compactDateFormat(date: Date?) {
    if (date != null) {
        val locale = Locale.getDefault()
        this.text = SimpleDateFormat("dd MMM", locale).format(date)
    }
}