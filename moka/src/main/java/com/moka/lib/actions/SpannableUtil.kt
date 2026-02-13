package com.moka.lib.actions

import android.text.Spannable
import android.widget.TextView

internal object SpannableUtils {

    fun <T> getSpannableWithText(text: String, view: TextView, spanType: Class<T>): T? {
        return (view.text as? Spannable)?.let { spannable ->
            val viewValue = spannable.toString()
            val spans = spannable.getSpans(0, spannable.length, spanType)

            spans.firstOrNull { span ->
                val start = spannable.getSpanStart(span)
                val end = spannable.getSpanEnd(span)
                val textValue = viewValue.substring(start, end)

                textValue == text
            }
        }
    }

}
