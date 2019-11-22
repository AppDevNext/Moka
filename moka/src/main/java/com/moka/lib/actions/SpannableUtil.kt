package com.moka.lib.actions

import android.text.Spannable
import android.text.style.URLSpan
import android.widget.TextView

internal object SpannableUtils {
    @JvmStatic
    fun <T> getSpansValues(view: TextView, spanType: Class<T>): List<String> {
        return (view.text as? Spannable)?.let { spannable ->
            val viewValue = spannable.toString()
            val spans = spannable.getSpans(0, spannable.length, spanType)

            return spans.map { span ->
                val start = spannable.getSpanStart(span)
                val end = spannable.getSpanEnd(span)

                viewValue.substring(start, end)
            }.toList()
        } ?: emptyList()
    }

    fun <T> getSpansInSpannable(s: Spannable, spanType: Class<T>): List<T> {
        return listOf<T>(*s.getSpans(0, s.length, spanType))
    }

    fun <T> removeSpans(view: TextView, spanType: Class<T>) {
        (view.text as? Spannable)?.let { spannable ->
            val spans = spannable.getSpans(0, spannable.length, spanType)
            spans.forEach { span ->
                spannable.removeSpan(span)
            }
        }
    }

    @JvmStatic
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

fun Spannable.getUrlSpans() = SpannableUtils.getSpansInSpannable(this, URLSpan::class.java)
