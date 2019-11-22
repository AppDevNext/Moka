package com.moka.lib.actions

import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.util.HumanReadables
import com.google.android.material.appbar.AppBarLayout
import com.moka.lib.internals.Reflection.getFieldValue
import org.hamcrest.Matcher

fun collapseAppBar() = AppBarLayoutCollapseAction()

class AppBarLayoutCollapseAction : ViewAction {
    override fun getDescription(): String = "Collapse AppBarLayout"

    override fun getConstraints(): Matcher<View> = isAssignableFrom(View::class.java)

    override fun perform(uiController: UiController, view: View) {
        val rootView = view.rootView as ViewGroup
        val appBarLayout = findAppBarLayoutDescendant(rootView)
                ?: throw PerformException.Builder()
                        .withViewDescription(HumanReadables.getViewHierarchyErrorMessage(rootView, null, "no AppBarLayout found", null))
                        .build()
        appBarLayout.collapseWithoutAnimation()

        uiController.loopMainThreadUntilIdle()
        while (appBarLayout.hasPendingAction()) {
            uiController.loopMainThreadForAtLeast(60)
        }
    }

    private fun findAppBarLayoutDescendant(rootView: ViewGroup): AppBarLayout? {
        return (0..rootView.childCount)
                .map { rootView.getChildAt(it) }
                .mapNotNull {
                    when (it) {
                        is AppBarLayout -> it
                        is ViewGroup -> findAppBarLayoutDescendant(it)
                        else -> null
                    }
                }
                .singleOrNull()
    }
}

private const val PENDING_ACTION_NONE = 0x0
private fun AppBarLayout.collapseWithoutAnimation() {
    this.setExpanded(false, false)
}

private fun AppBarLayout.hasPendingAction(): Boolean {
    val pendingAction: Int = getFieldValue(this, "mPendingAction")
    return pendingAction != PENDING_ACTION_NONE
}
