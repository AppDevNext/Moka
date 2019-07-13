package com.moka.actions

import android.graphics.Rect
import android.view.View
import android.widget.OverScroller
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.HumanReadables.getViewHierarchyErrorMessage
import com.moka.internals.Reflection.getFieldValue
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import timber.log.Timber

/**
 * This view action allows scrolling to occur on a NestedScrollView
 *
 *
 * Enables scrolling to the given view. Typically, scrollTo() can be used, but this
 * does not work with NestedScrollView.
 *
 *
 * See: https://issuetracker.google.com/issues/37087431
 *
 *
 * Source: https://gist.github.com/miszmaniac/12f720b7e898ece55d2464fe645e1f36
 */
class NestedScrollViewScrollToAction : ViewAction {

    override fun getConstraints(): Matcher<View> {
        return allOf(withEffectiveVisibility(Visibility.VISIBLE), isDescendantOfA(anyOf(
                isAssignableFrom(NestedScrollView::class.java))))
    }

    override fun perform(uiController: UiController, view: View) {
        uiController.loopMainThreadUntilIdle()
        if (isDisplayingAtLeast(90).matches(view)) {
            Timber.i("View is already displayed. Returning.")
            return
        }

        val parentScrollView = findScrollView(view)
        parentScrollView.requestLayout()

        uiController.loopMainThreadUntilIdle()

        val rect = Rect()
        view.getDrawingRect(rect)
        if (!/* immediate */view.requestRectangleOnScreen(rect, false)) {
            Timber.w("Scrolling to view was requested, but none of the parents scrolled.")
        }
        val mScroller = getFieldValue<OverScroller>(parentScrollView, NestedScrollView::class.java, "mScroller")

        // this is slower that I would like... should look for a faster way to scroll
        while (!mScroller.isFinished) {
            uiController.loopMainThreadUntilIdle()
        }

        if (!isDisplayingAtLeast(90).matches(view)) {
            throw PerformException.Builder()
                    .withActionDescription(this.description)
                    .withViewDescription(getViewHierarchyErrorMessage(view.rootView,
                            listOf(view),
                            "Scrolling to view was attempted, but the view is not displayed",
                            "NOT DISPLAYED 90 percent"))
                    .build()
        }
    }

    private fun findScrollView(view: View): View {
        val parent = view.parent as View
        if (parent != null) {
            return parent as? NestedScrollView ?: findScrollView(parent)
        }
        throw PerformException.Builder()
                .withActionDescription(this.description)
                .withViewDescription(HumanReadables.describe(view))
                .withCause(RuntimeException(
                        "Scrolling aborted due to not being NestedScrollView child"))
                .build()
    }

    override fun getDescription(): String {
        return "scroll to"
    }

    companion object {

        fun scrollTo(): NestedScrollViewScrollToAction {
            return NestedScrollViewScrollToAction()
        }
    }
}
