package com.moka

import android.view.View
import androidx.test.espresso.Root
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.typeTextIntoFocusedView
import androidx.test.espresso.assertion.ViewAssertions.matches
import com.moka.lib.actions.clickWithRetry
import com.moka.lib.actions.requestFocus
import com.moka.lib.matchers.hasWindowFocus
import org.hamcrest.Matcher

class MokaViewInteraction internal constructor(private val viewInteraction: ViewInteraction) : MokaInteraction() {

    fun click(): MokaViewInteraction {
        perform(requestFocus())
        loopMainThreadUntil(hasWindowFocus())
        perform(clickWithRetry())
        return this
    }

    fun type(text: String): MokaViewInteraction {
        perform(clickWithRetry())
        perform(typeTextIntoFocusedView(text))
        return this
    }

    fun pressImeActionButton(): MokaViewInteraction {
        perform(ViewActions.pressImeActionButton())
        return this
    }

    fun clearAndType(text: String): MokaViewInteraction {
        perform(clearText())
        return type(text)
    }

    fun setText(text: String): MokaViewInteraction {
        perform(com.moka.lib.actions.setText(text))
        return this
    }

    fun scrollTo(): MokaViewInteraction {
        perform(androidx.test.espresso.action.ViewActions.scrollTo())
        return this
    }

    fun inRoot(rootMatcher: Matcher<Root>): MokaViewInteraction {
        runWithRetry { viewInteraction.inRoot(rootMatcher) }
        return this
    }

    fun check(assertion: ViewAssertion): MokaViewInteraction {
        runWithRetry { viewInteraction.check(assertion) }
        return this
    }

    fun checkMatches(matcher: Matcher<View>): MokaViewInteraction {
        loopMainThreadUntil(matcher)
        runWithRetry { viewInteraction.check(matches(matcher)) }
        return this
    }

    override fun perform(vararg viewActions: ViewAction): MokaViewInteraction {
        return runWithRetryAndReturnSugarViewInteraction { viewInteraction.perform(*viewActions) }
    }

    private fun runWithRetryAndReturnSugarViewInteraction(action: () -> Unit): MokaViewInteraction {
        runWithRetry(action)
        return this
    }
}
