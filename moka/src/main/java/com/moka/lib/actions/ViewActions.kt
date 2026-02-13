@file:JvmName("ViewActions")

package com.moka.lib.actions

import android.annotation.SuppressLint
import android.graphics.Point
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.PerformException
import androidx.test.espresso.PerformException.Builder
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralLocation.BOTTOM_CENTER
import androidx.test.espresso.action.GeneralLocation.TOP_CENTER
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press.FINGER
import androidx.test.espresso.action.Swipe.SLOW
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.util.HumanReadables
import com.google.android.material.appbar.AppBarLayout
import com.moka.lib.actions.SpannableUtils.getSpannableWithText
import com.moka.lib.matchers.instanceOf
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import timber.log.Timber

/**
 * Returns a [ViewAction] that sets focus to the View.
 */
fun requestFocus(): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> = CoreMatchers.any(View::class.java)

        override fun getDescription() = "requestFocus"

        override fun perform(uiController: UiController, view: View) {
            view.requestFocus()
            uiController.loopMainThreadUntilIdle()
        }
    }
}

/**
 * Returns a [ViewAction] that slowly swipes from bottom to top.
 */
fun dragBottomToTop() = GeneralSwipeAction(SLOW, BOTTOM_CENTER, TOP_CENTER, FINGER)

/**
 * Returns a [ViewAction] that slowly swipes from top to bottom.
 */
fun dragTopToBottom() = GeneralSwipeAction(SLOW, TOP_CENTER, BOTTOM_CENTER, FINGER)

/**
 * Returns a [ViewAction] that clicks on the right compound drawable of an EditText.
 */
fun clickRightDrawable(): ViewAction {
    return object : ViewAction {

        @Suppress("MayBeConst")
        private val drawableRight = 2

        override fun getConstraints(): Matcher<View> {
            return allOf<View>(isAssignableFrom(EditText::class.java), object : BoundedMatcher<View, EditText>(EditText::class.java) {
                override fun matchesSafely(view: EditText): Boolean = view.requestFocusFromTouch() && view.compoundDrawables[drawableRight] != null

                override fun describeTo(description: Description) {
                    description.appendText(" and has drawable right set")
                }
            })
        }

        override fun getDescription() = "Click a compound drawable on the right side of a view"

        override fun perform(uiController: UiController, view: View) {
            val editText = view as EditText?
            if (editText != null && editText.requestFocusFromTouch()) {
                val drawableBounds = editText.compoundDrawables[drawableRight].bounds

                val clickPoint = Point(editText.right + drawableBounds.width() / 2,
                        (editText.pivotY + drawableBounds.height() / 2).toInt())

                if (editText.dispatchTouchEvent(MotionEvent.obtain(android.os.SystemClock.uptimeMillis(),
                                android.os.SystemClock.uptimeMillis(),
                                MotionEvent.ACTION_DOWN,
                                clickPoint.x.toFloat(),
                                clickPoint.y.toFloat(),
                                0))) {
                    editText.dispatchTouchEvent(MotionEvent.obtain(android.os.SystemClock.uptimeMillis(),
                            android.os.SystemClock.uptimeMillis(),
                            MotionEvent.ACTION_UP,
                            clickPoint.x.toFloat(),
                            clickPoint.y.toFloat(),
                            0))
                }
            }
        }
    }
}

/**
 * Returns a [ViewAction] that clicks on a clickable substring inside of a [TextView].
 * @param repeatCount The number of times we want to perform the click. Defaults to 1
 */
fun clickSpannableWithText(text: String, repeatCount: Int = 1): ViewAction {
    return object : ViewAction {

        override fun getConstraints() = isDisplayed()

        override fun getDescription() = "SpannableTextClickViewAction ( clicked() )"

        override fun perform(uiController: UiController, view: View) {
            if (view is TextView) {
                val clickableSpan = getSpannableWithText(text, view, ClickableSpan::class.java)
                for (i in 0 until repeatCount) {
                    checkNotNull(clickableSpan).onClick(view)
                }
            }
        }
    }

}

/**
 * Returns a [ViewAction] that tries clicking the [View] multiple times before failing.
 */
fun clickWithRetry(numberOfRetries: Int = 3): ViewAction {
    val click = androidx.test.espresso.action.ViewActions.click()
    val viewMatcher = isEnabled()
    return viewActionWithRetry(withAddedConstraints(click, viewMatcher), numberOfRetries)
}

/**
 * Returns a [ViewAction] that collapses an [AppBarLayout].
 */
fun collapseAppBarLayout(): ViewAction {
    return object : ViewAction {
        override fun getConstraints() = isAssignableFrom(AppBarLayout::class.java)

        override fun getDescription() = "Collapse App Bar Layout"

        override fun perform(uiController: UiController, view: View) {
            val appBarLayout = view as AppBarLayout
            appBarLayout.setExpanded(false)
        }
    }
}

/**
 * Returns a [ViewAction]  that sets the provided string into the [View].
 */
fun setText(text: String): ViewAction {
    return object : ViewAction {

        override fun getConstraints(): Matcher<View> = allOf(instanceOf(TextView::class.java), isDisplayed())

        override fun getDescription(): String = """setText("$text")"""

        override fun perform(uiController: UiController, view: View) {
            (view as TextView).text = text
        }
    }

}

private fun withAddedConstraints(click: ViewAction, viewMatcher: Matcher<View>): ViewAction {
    return object : ViewAction {
        override fun getConstraints() = allOf(click.constraints, viewMatcher)

        override fun getDescription() = click.description

        override fun perform(uiController: UiController, view: View) = click.perform(uiController, view)
    }
}

/**
 * Wraps the given [ViewAction] into another [ViewAction] that retries a given number of times before failing.
 */
fun viewActionWithRetry(viewAction: ViewAction, numberOfRetries: Int = 3): ViewAction {
    return object : ViewAction {

        override fun getConstraints(): Matcher<View> = viewAction.constraints

        // lint thinks that e.getCause() is not actually an exception here..
        @SuppressLint("ThrowableNotAtBeginning")
        override fun perform(uiController: UiController, view: View) {
            var exceptionForRethrow: PerformException? = null
            for (i in 0..numberOfRetries) {
                try {
                    if (i > 0) {
                        Timber.d("Try #%d performing %s on %s", i, viewAction, view)
                    }
                    viewAction.perform(uiController, view)
                    exceptionForRethrow = null // finally success, don't rethrow
                    Timber.d("successfully performed %s on %s", viewAction, view)
                    break // no exception so we can proceed
                } catch (e: PerformException) {
                    exceptionForRethrow = e
                    Timber.e(e.cause, "On try #%d, caught %s with cause %s while %s, ( notification drawer may not be closed )", i,
                            e, e.cause, description)
                    uiController.loopMainThreadForAtLeast(Math.pow(2.0, (i + 7).toDouble()).toInt().toLong())
                }

            }
            if (exceptionForRethrow != null) {
                throw exceptionForRethrow
            }
        }

        override fun getDescription(): String = viewAction.description
    }
}

/**
 * Returns a [ViewAction] that scrolls to a child inside a [NestedScrollView].
 * If you need to scroll inside a normal [android.widget.ScrollView] please use [android.support.test.espresso.action.ViewActions.scrollTo]
 */
fun scrollToChildInNestedScrollView(): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return allOf(withEffectiveVisibility(Visibility.VISIBLE), isDescendantOfA(instanceOf(NestedScrollView::class.java)))
        }

        override fun getDescription() = "Scrolling to child inside NestedScrollView"

        override fun perform(uiController: UiController, view: View) {
            uiController.loopMainThreadUntilIdle()

            val parent = findParent(view) ?: throw performException(view)

            parent.scrollTo(0, view.top)
            uiController.loopMainThreadUntilIdle()
        }

        private fun performException(view: View): PerformException {
            return Builder()
                    .withActionDescription(this.description)
                    .withViewDescription(HumanReadables.describe(view))
                    .withCause(RuntimeException("View is not a child of NestedScrollView")).build()
        }

        private fun findParent(view: View): NestedScrollView? {
            val parent = view.parent as View?
            if (parent != null) {
                return parent as? NestedScrollView ?: findParent(parent)
            }
            return null
        }
    }
}
