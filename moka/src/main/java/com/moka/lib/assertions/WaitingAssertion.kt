package com.moka.lib.assertions

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.*
import androidx.test.espresso.action.ViewActions.actionWithAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.*
import timber.log.Timber

/**
 * It waits until a Matcher pass or the timeout elapse. In this way we can get rid of Thread.sleep in tests
 */
@Suppress("unused")
class WaitingAssertion {

    companion object {

        private fun waitForMatcher(timeoutInMs: Int, matcher: Matcher<View>): ViewAction {
            return actionWithAssertions(object : ViewAction {
                val endTime = System.currentTimeMillis() + timeoutInMs

                override fun getConstraints(): Matcher<View> {
                    return ViewMatchers.isAssignableFrom(View::class.java)
                }

                override fun getDescription(): String {
                    val description = StringDescription()
                    matcher.describeTo(description)
                    return String.format("wait until: %s", description)
                }

                override fun perform(uiController: UiController, view: View) {
                    var loopCount = 0
                    while (!(matcher.matches(view) || System.currentTimeMillis() >= endTime)) {
                        loopCount++
                        uiController.loopMainThreadForAtLeast(20)
                    }
                    val millis = endTime - System.currentTimeMillis()
                    Timber.d("${!(matcher.matches(view))} || ${System.currentTimeMillis() >= endTime} loopCount=$loopCount within $millis ms")
                }
            })
        }

        /**
         * Main checker. Checks to see, whether the view matches the matcher param
         */
        private fun checkAssertion(viewInteraction: ViewInteraction, matcher: Matcher<View>, timeoutInMs: Int) {
            viewInteraction
                .perform(waitForMatcher(timeoutInMs, matcher))
                .check(matches(matcher))
        }

        /**
         * Common checker. Checks to see, whether the view matches the matcher param
         */
        fun checkAssertion(viewId: Int, matcher: Matcher<View>, timeoutInMs: Int) {
            checkAssertion(Espresso.onView(ViewMatchers.withId(viewId)), matcher, timeoutInMs)
        }

        /**
         * Checks to see, whether the View is visible or not
         */
        fun assertVisibility(viewId: Int, visibility: Int, timeoutInMs: Int) {
            val isDisplayedMatcher = when (visibility) {
                View.VISIBLE -> ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
                View.GONE -> ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)
                else -> ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)
            }

            checkAssertion(Espresso.onView(ViewMatchers.withId(viewId)), isDisplayedMatcher, timeoutInMs)
        }

        /**
         * Checks to see, whether the View has some text
         */
        @Suppress("unused")
        fun assertText(viewId: Int, text: String, timeoutInMs: Int) {
            checkAssertion(Espresso.onView(ViewMatchers.withId(viewId)), withText(text), timeoutInMs)
        }

        /**
         * Checks to see, whether the RecyclerView has a specific amount of items
         * @param viewId an Id of a RecyclerView
         * @param count the minimum amount of items in the RecyclerView to check
         * @param timeoutInMs timeout
         */
        @Suppress("unused")
        fun assertAdapterMinimumItemsCount(viewId: Int, count: Int, timeoutInMs: Int) {
            val matcher = object : BaseMatcher<View>() {
                private var actualItems: Int = -1

                override fun describeTo(description: Description?) {
                    description?.appendText("Adapter item count: is '$actualItems' instead of '$count ($timeoutInMs ms)")
                }

                override fun describeMismatch(item: Any?, mismatchDescription: Description?) {
                    mismatchDescription?.appendText(
                        "Adapter count doesn't match. " +
                                "Required $count but found ${(item as RecyclerView?)?.adapter?.itemCount ?: -1} "
                    )
                }

                override fun matches(item: Any?): Boolean {
                    actualItems = (item as RecyclerView?)?.adapter?.itemCount ?: -1
                    return actualItems >= count
                }
            }

            checkAssertion(Espresso.onView(ViewMatchers.withId(viewId)), matcher, timeoutInMs)
        }

        @Suppress("unused")
        fun assertRecyclerAdapterItemsCount(viewId: Int, expectedCount: Int, matcherOperator: MatchOperator, timeoutInMs: Int) {
            val matcher = object : BaseMatcher<View>() {
                private var actualItems: Int = -1

                override fun describeTo(description: Description?) {
                    description?.appendText("Adapter item count: '$actualItems' instead of '$expectedCount' ($timeoutInMs) ms")
                }

                override fun describeMismatch(item: Any?, mismatchDescription: Description?) {
                    mismatchDescription?.appendText(
                        "Adapter count doesn't match. " +
                                "Required ${matcherOperator.name} $expectedCount but found ${(item as RecyclerView?)?.adapter?.itemCount ?: -1} "
                    )
                }

                override fun matches(item: Any?): Boolean {
                    actualItems = (item as RecyclerView?)?.adapter?.itemCount ?: -1
                    return when (matcherOperator) {
                        MatchOperator.IS -> actualItems == expectedCount
                        MatchOperator.LESS -> actualItems < expectedCount
                        MatchOperator.LESS_EQUAL -> actualItems <= expectedCount
                        MatchOperator.GREATER -> actualItems > expectedCount
                        MatchOperator.GREATER_EQUAL -> actualItems >= expectedCount
                    }
                }
            }

            checkAssertion(Espresso.onView(ViewMatchers.withId(viewId)), matcher, timeoutInMs)
        }

    }
}