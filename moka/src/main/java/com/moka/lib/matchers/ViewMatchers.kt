package com.moka.lib.matchers

import android.view.View
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * Returns a [Matcher] that matches a String by a substring
 */
fun contains(substring: String, ignoreCase: Boolean = false): Matcher<String> {
    return object : BaseMatcher<String>() {

        override fun matches(o: Any?): Boolean =
            o?.toString()?.contains(substring, ignoreCase = ignoreCase) ?: false

        override fun describeTo(description: Description) {
            description.appendText("contains $substring")
        }
    }
}

/**
 * Returns a [Matcher] that matches a [View] based on its type.
 */
fun instanceOf(clazz: Class<*>): Matcher<View> {
    return object : BaseMatcher<View>() {

        override fun matches(o: Any?) = clazz.isInstance(o)

        override fun describeTo(description: Description) {
            description.appendText("is instance of $clazz")
        }
    }
}
