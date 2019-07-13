@file:JvmName("IntentMatchers")

package com.moka.matchers

import android.content.Intent
import android.os.Bundle
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

fun hasBundleExtra(bundleKey: String, bundle: Bundle): Matcher<Intent> {
    return object : TypeSafeMatcher<Intent>() {
        override fun describeTo(description: Description) {
            description.appendText("has bundle with key: $bundleKey and contents: $bundle")
        }

        public override fun matchesSafely(intent: Intent): Boolean {
            return bundle.toString() == intent.getBundleExtra(bundleKey).toString()
        }
    }
}
