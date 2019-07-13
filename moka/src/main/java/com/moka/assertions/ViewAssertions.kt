@file:JvmName("ViewAssertions")

package com.moka.assertions

import android.view.View
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.moka.matchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import timber.log.Timber

/**
 * Returns a [ViewAssertion] that asserts that a [View] exists in the view hierarchy.
 */
fun exists(): ViewAssertion {
    return ViewAssertion { view, noViewFoundException ->
        if (view == null || noViewFoundException != null) {
            if (noViewFoundException != null) {
                Timber.e(noViewFoundException, "Throw and Assertion error")
            }
            throw AssertionError("Expecting view to exist but it was not found.")
        }
    }
}

/**
 * Returns a [ViewAssertion] that asserts that a child [View] contains the given substring.
 */
fun hasChildWithSubstring(substring: String): ViewAssertion {
    return matches(hasDescendant(withText(containsString(substring))))
}

/**
 * Returns a [ViewAssertion] that asserts that any child [View] that matches the given matcher is displayed.
 */
fun descendantIsDisplayed(viewMatcher: Matcher<View>): ViewAssertion {
    return matches(hasDescendant(allOf<View>(isDisplayed(), viewMatcher)))
}

/**
 * Returns a [ViewAssertion] that asserts that any child [View] that matches the given matcher is not
 * displayed.
 */
fun descendantIsNotDisplayed(viewMatcher: Matcher<View>): ViewAssertion {
    return matches(not<View>(hasDescendant(allOf<View>(isDisplayed(), viewMatcher))))
}
