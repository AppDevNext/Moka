package com.moka

import android.view.View
import androidx.test.espresso.*
import androidx.test.espresso.util.HumanReadables.getViewHierarchyErrorMessage
import com.moka.EspressoInternals.waitForEspressoToIdle
import com.moka.EspressoMokaRunner.runOnMainSyncDelayed
import com.moka.internals.TestAccessibilityEventListener.toPrintString
import com.moka.internals.propagate
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import timber.log.Timber
import java.lang.System.currentTimeMillis

@Suppress("UnnecessaryParentheses")
abstract class MokaInteraction {
    fun <T : View> loopMainThreadUntil(viewMatcher: Matcher<T>) = perform(LoopUntil(viewMatcher))

    abstract fun perform(vararg viewActions: ViewAction): MokaViewInteraction

    protected fun <T> runWithRetry(action: () -> T): T {
        try {
            try {
                return action()
            } catch (e: RuntimeException) {
                Timber.w(e, "Exception while performing %s, going to retry", action)
                when {
                    shouldWaitAndTryAgain(e) -> {
                        // Sometimes we just didn't wait long enough for the UI to update with the new View hierarchy
                        waitForEspressoToIdle()
                        runOnMainSyncDelayed({ Timber.w("Stalling because of exception: %s", e.message) }, 20)
                        waitForEspressoToIdle()
                        Timber.w(e, "Retrying failed action after a bit of waiting... ")
                        return action()
                    }
                    else -> throw e
                }
            }
        } catch (e: Exception) {
            throw propagate(e)
        }
    }

    private fun shouldWaitAndTryAgain(e: RuntimeException): Boolean {
        return hasCause(e, NoMatchingViewException::class.java) || hasCause(e, InjectEventSecurityException::class.java)
    }

    private fun hasCause(e: Throwable?, clazz: Class<*>): Boolean {
        return e != null && (clazz.isInstance(e) || e.cause != null) && hasCause(e.cause, clazz)
    }

    private fun shouldTryToResumeActivity(e: Throwable): Boolean {
        val cause = e.cause
        return (e is PerformException && cause?.javaClass != RuntimeException::class.java)
                || e is NoActivityResumedException
                || e.message?.contains("Waited for the root") ?: false
                || (cause != null && shouldTryToResumeActivity(cause))
    }

    private class LoopUntil<T : View>(private val viewMatcher: Matcher<T>) : ViewAction {

        override fun getConstraints(): Matcher<View> {
            return object : BaseMatcher<View>() {
                override fun matches(o: Any): Boolean = o is View

                override fun describeTo(description: Description) {
                    description.appendText("Is instance of View")
                }
            }
        }

        override fun getDescription(): String = "looping the main thread until ${toPrintString(viewMatcher)}"

        @Suppress("TooGenericExceptionThrown")
        override fun perform(uiController: UiController, view: View) {
            val startTime = currentTimeMillis()
            var timeSinceStart = currentTimeMillis() - startTime
            var timedOut = timeSinceStart > TIME_OUT
            var count = 0
            while (!viewMatcher.matches(view) && !timedOut) {
                count++
                uiController.loopMainThreadUntilIdle()
                if (count < 5) {
                    for (i in 0 until count) { // linear back off if it doesn't match right away...
                        uiController.loopMainThreadUntilIdle()
                    }
                }
                timeSinceStart = currentTimeMillis() - startTime
                timedOut = timeSinceStart > TIME_OUT
            }
            if (timedOut) {
                throw RuntimeException(
                        getViewHierarchyErrorMessage(
                                view.rootView,
                                listOf(view),
                                "Timed out after ${(TIME_OUT / 1000)} seconds, while looping until $view meets criteria $viewMatcher", "**PROBLEM**"
                        )
                )
            }
        }

        companion object {
            private const val TIME_OUT: Long = 30000
        }
    }
}
