@file:JvmName("ViewMatchers")

package com.moka.matchers

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.AmbiguousViewMatcherException
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.viewpager.widget.ViewPager
import com.google.android.material.textfield.TextInputLayout
import com.moka.EspressoInternals
import com.moka.actions.SpannableUtils.getSpansValues
import com.moka.internals.TestAccessibilityEventListener.toPrintString
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.AllOf
import timber.log.Timber

/**
 * Returns a [Matcher] that matches if the examined object matches all of the specified matchers.
 */
fun <T> allOf(matcher1: Matcher<in T>, matcher2: Matcher<in T>): Matcher<T> {
    // this will avoid unchecked warnings elsewhere.
    return AllOf.allOf(matcher1, matcher2)
}

/**
 * Returns a [Matcher] that matches if the examined object matches all of the specified matchers.
 */
fun <T> allOf(matcher1: Matcher<in T>, matcher2: Matcher<in T>, matcher3: Matcher<in T>): Matcher<T> {
    // this will avoid unchecked warnings elsewhere.
    return AllOf.allOf(matcher1, matcher2, matcher3)
}

/**
 * Returns a [Matcher] that matches an [EditText] based on its current value.
 */
fun isEditTextValueEqualTo(content: String): Matcher<View> {
    return object : TypeSafeMatcher<View>() {

        override fun describeTo(description: Description) {
            description.appendText("Match EditText with value: " + content)
        }

        public override fun matchesSafely(view: View): Boolean = (view as? EditText)?.text?.toString()?.equals(content, true)
                ?: false
    }
}

/**
 * Returns a [Matcher] that matches a [TextView] based on its color.
 */
fun withTextColor(@ColorInt color: Int): Matcher<View> {
    return object : BoundedMatcher<View, TextView>(TextView::class.java) {
        override fun matchesSafely(item: TextView?): Boolean = color == item?.currentTextColor

        override fun describeTo(description: Description) {
            description.appendText("with text color: " + color)
        }
    }
}

/**
 * Returns a [Matcher] which accepts a view so long as it can be scrolled to satisfy
 * the input view matcher. Note that it also actually scrolls the [View].
 */
fun canBeScrolledSoIt(viewMatcher: Matcher<View>): Matcher<View> {
    return object : BaseMatcher<View>() {
        override fun matches(o: Any?): Boolean {
            if (o != null && o is View) {
                val matches = viewMatcher.matches(o)
                if (matches) {
                    return true
                }

                var parent: ViewParent? = o.parent
                while (parent != null) {
                    if (parent is ScrollView) {
                        parent.requestChildFocus(o, o)
                        EspressoInternals.waitForEspressoToIdle()
                        return viewMatcher.matches(o)
                    } else if (parent is NestedScrollView) {
                        parent.requestChildFocus(o, o)
                        EspressoInternals.waitForEspressoToIdle()
                        return viewMatcher.matches(o)
                    }
                    parent = parent.parent
                }
            }
            return false
        }

        override fun describeTo(description: Description) {
            description.appendText(" can be scrolled so it " + toPrintString<View>(viewMatcher))
        }
    }
}

/**
 * Returns a [Matcher] that matches a [View] if it has [android.view.Window] Focus
 */
fun hasWindowFocus(): Matcher<View> {
    return object : BaseMatcher<View>() {

        override fun matches(o: Any?): Boolean = (o as? View)?.hasWindowFocus() ?: false

        override fun describeTo(description: Description) {
            description.appendText(" has Window Focus ")
        }
    }
}

/**
 * Returns a [Matcher] that matches a String by a substring
 */
@JvmOverloads
fun contains(substring: String, ignoreCase: Boolean = false): Matcher<String> {
    return object : BaseMatcher<String>() {

        override fun matches(o: Any?): Boolean = o?.toString()?.contains(substring, ignoreCase = ignoreCase)
                ?: false

        override fun describeTo(description: Description) {
            description.appendText("contains " + substring)
        }
    }
}

/**
 * Returns a [Matcher] that matches a [TextView] if part of its content is a [ClickableSpan] hyperlink
 */
fun withClickableText(text: String): Matcher<View> {
    return object : BaseMatcher<View>() {

        override fun matches(o: Any?): Boolean = (o as? TextView)?.let { getSpansValues(it, ClickableSpan::class.java).contains(text) }
                ?: false

        override fun describeTo(description: Description) {
            description.appendText("is a clickable text")
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
            description.appendText("is instance of " + clazz)
        }
    }
}

/**
 * Returns a [Matcher] that matches a [View] when its visibility is set to [View.GONE].
 */
fun isGone(): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        public override fun matchesSafely(view: View): Boolean = view.visibility == View.GONE

        override fun describeTo(description: Description) {
            description.appendText("visiblility == GONE")
        }
    }
}

/**
 * Returns a [Matcher] that matches a [TextInputLayout] by the current error message.
 */
fun textInputHasErrorText(expectedErrorText: String): Matcher<View> {
    return object : BoundedMatcher<View, TextInputLayout>(TextInputLayout::class.java) {

        public override fun matchesSafely(view: TextInputLayout): Boolean {
            return when (view.error) {
                null -> false
                else -> view.error.toString() == expectedErrorText
            }
        }

        override fun describeTo(description: Description) {
            description.appendText(" has error text: " + expectedErrorText)
        }
    }
}

/**
 * Returns a [Matcher] that matches a [View] that is currently visible, but not necessarily displayed to the user on the screen.
 */
fun isVisibleButNotNecessarilyDisplayed(): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        public override fun matchesSafely(o: View): Boolean = o.visibility == View.VISIBLE

        override fun describeTo(description: Description) {
            description.appendText(" is visible but not necessarily displayed on the screen")
        }
    }
}

/**
 * Returns a [Matcher] that matches a [View] with an adapter that contain the given amount of elements.
 */
@Suppress("NestedBlockDepth")
fun <T : View> hasExactDataCount(exactCount: Int): Matcher<T> {
    return object : BaseMatcher<T>() {

        override fun matches(o: Any?): Boolean {
            if (o is AdapterView<*>) {
                o.adapter?.let {
                    val adapterCount = it.count
                    if (adapterCount != exactCount) {
                        Timber.w("Expected View %s to have item count %d, but actual count was %d", o, exactCount, adapterCount)
                        for (i in 0 until adapterCount) {
                            Timber.w("item[$i]: ${it.getItem(i)}")
                        }
                    } else {
                        return true
                    }
                }
            } else if (o is ViewPager) {
                o.adapter?.let {
                    val adapterCount = it.count
                    if (adapterCount != exactCount) {
                        Timber.w("Expected View %s to have item count %d, but actual count was %d", o, exactCount, adapterCount)
                    } else {
                        return true
                    }
                }
            }

            return false
        }

        override fun describeTo(description: Description) {
            description.appendText("is AdapterView with data count == $exactCount")
        }
    }

}

/**
 * Allows you to find a [View] based upon an id and an index in order to avoid the [AmbiguousViewMatcherException]
 *
 * Typically you'll need to use this when you have many views with the same id on the screen. This happens often when building UI's through
 * a loop of sorts and adding similar children to a ViewGroup.
 *
 * Example:
 * `onView(withIndex(withId(R.id.my_view), 2)).perform(click());`
 *
 * @param matcher the matcher that would typically find an ambiguous match
 * @param index the index
 * @return the matcher
 */
fun withIndex(matcher: Matcher<View>, index: Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        internal var currentIndex: Int = 0
        internal var viewObjHash: Int = 0

        @SuppressLint("DefaultLocale")
        override fun describeTo(description: Description) {
            description.appendText("with index: $index")
            matcher.describeTo(description)
        }

        public override fun matchesSafely(view: View): Boolean {
            if (matcher.matches(view) && currentIndex++ == index) {
                viewObjHash = view.hashCode()
            }
            return view.hashCode() == viewObjHash
        }
    }
}

/**
 * Returns a [Matcher] that matches a [DatePicker] by the selected date.
 */
fun date(year: Int, month: Int, day: Int): Matcher<View> {
    return object : BoundedMatcher<View, DatePicker>(DatePicker::class.java) {

        // DatePicker has zero based month
        public override fun matchesSafely(view: DatePicker): Boolean = view.dayOfMonth == day && view.month + 1 == month && view.year == year

        override fun describeTo(description: Description) {
            description.appendText("is datepicker with date $year/$month/$day")
        }
    }
}

/**
 * Returns a [Matcher] that matches an [ImageView] based on its drawable resource
 */
fun withDrawable(@DrawableRes expectedId: Int): Matcher<View> {
    return object : BoundedMatcher<View, ImageView>(ImageView::class.java) {
        private var resourceName: String? = null

        @Suppress("DEPRECATION")
        override fun matchesSafely(target: ImageView): Boolean {
            if (expectedId == EMPTY) {
                return target.drawable == null
            }
            if (expectedId == ANY) {
                return target.drawable != null
            }

            val resources = target.context.resources
            val expectedDrawable = resources.getDrawable(expectedId)
            resourceName = resources.getResourceEntryName(expectedId)

            if (expectedDrawable == null) {
                return false
            }

            val bitmap = getBitmap(target.drawable)
            val otherBitmap = getBitmap(expectedDrawable)
            return bitmap.sameAs(otherBitmap)
        }

        /**
         * With vector drawables is a bit more complicated to compare because you need to
         * create the Bitmap from the vector. So instead of merely calling ‘getBitmap()’ on
         * the drawable it’s better to create a bitmap from scratch and drawing the drawable
         * into it. Of course ‘getBitmap()’ from a VectorDrawable or a VectorDrawableCompat
         * doesn’t exist at all.
         *
         * @param drawable the drawable
         * @return the bitmap representation of the drawable
         */
        private fun getBitmap(drawable: Drawable): Bitmap {
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }

        override fun describeTo(description: Description) {
            description.appendText("with drawable from resource id: ")
            description.appendValue(expectedId)
            if (resourceName != null) {
                description.appendText("[")
                description.appendText(resourceName)
                description.appendText("]")
            }
        }
    }
}

/**
 * Returns a [Matcher] for a [ViewGroup] that matches a provided condition for a child of that [ViewGroup]
 */

fun hasChild(matcher: Matcher<View>): Matcher<View> {
    return object : BaseMatcher<View>() {
        override fun describeTo(description: Description?) {
            description?.appendText("is view that has child ${matcher}")
        }

        override fun matches(item: Any?): Boolean {
            val viewGroup = item as? ViewGroup
            viewGroup?.let {
                for (i in 0 until viewGroup.childCount) {
                    if (matcher.matches(viewGroup.getChildAt(i))) {
                        return true
                    }
                }
            }
            return false
        }
    }

}

/**
 * Returns a [Matcher] for a [ViewGroup] that matches a provided count of child views of that [ViewGroup]
 */

fun hasChildCount(count: Int): Matcher<View> {
    return object : BaseMatcher<View>() {
        override fun describeTo(description: Description?) {
            description?.appendText("is view has child count ${count}")
        }

        override fun matches(item: Any?): Boolean {
            val viewGroup = item as? ViewGroup
            viewGroup?.let {
                return count == viewGroup.childCount
            }
            return false
        }
    }

}

/**
 * Returns a [Matcher] that matches any [ImageView] with a [Drawable]
 */
fun withAnyDrawable() = withDrawable(ANY)

/**
 * Returns a [Matcher] that matches any [ImageView] with no [Drawable]
 */
fun withNoDrawable() = withDrawable(EMPTY)

/**
 * Returns a [Matcher] that matches a [TextView] by a substring
 */
fun containsText(substring: String?): Matcher<View> = withText(containsString(substring))

/**
 * Returns a [Matcher] that matches any [View] that is checkable and that has the correct checked state.
 */
fun withCheckBoxState(checked: Boolean): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("with checkbox state: $checked")
        }

        override fun matchesSafely(item: View): Boolean {
            if (item is Checkable) {
                return item.isChecked == checked
            }
            return false
        }
    }
}

/**
 * Returns a [Matcher] that matches an [AdapterView] by the amount of elements.
 */
@JvmOverloads
fun <T : AdapterView<*>> hasData(minData: Int = 1): Matcher<T> {
    return object : BaseMatcher<T>() {
        override fun matches(o: Any): Boolean {
            return if (o is AdapterView<*> && o.adapter != null) {
                o.adapter.count >= minData
            } else false
        }

        override fun describeTo(description: Description) {
            description.appendText("is AdapterView with data count >= $minData")
        }
    }
}

private const val EMPTY = -1
private const val ANY = -2
