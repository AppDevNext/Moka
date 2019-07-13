package com.moka

import android.view.View
import androidx.test.espresso.*
import androidx.test.espresso.action.AdapterViewProtocol
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import com.moka.lib.actions.clickWithRetry
import org.hamcrest.Matcher

class MokaDataInteraction internal constructor(private val dataInteraction: DataInteraction) : MokaInteraction() {

    fun click(): MokaDataInteraction {
        perform(clickWithRetry())
        return this
    }

    fun type(text: String): MokaDataInteraction {
        perform(typeText(text))
        return this
    }

    fun clearAndType(text: String): MokaDataInteraction {
        perform(clearText())
        return type(text)
    }

    fun setText(text: String): MokaDataInteraction {
        perform(com.moka.lib.actions.setText(text))
        return this
    }

    fun onChildView(childMatcher: Matcher<View>): MokaDataInteraction {
        runWithRetry { dataInteraction.onChildView(childMatcher) }
        return this
    }

    fun inRoot(rootMatcher: Matcher<Root>): MokaDataInteraction {
        runWithRetry { dataInteraction.inRoot(rootMatcher) }
        return this
    }

    fun inAdapterView(adapterMatcher: Matcher<View>): MokaDataInteraction {
        runWithRetry { dataInteraction.inAdapterView(adapterMatcher) }
        return this
    }

    fun atPosition(atPosition: Int): MokaDataInteraction {
        runWithRetry { dataInteraction.atPosition(atPosition) }
        return this
    }

    fun usingAdapterViewProtocol(adapterViewProtocol: AdapterViewProtocol): MokaDataInteraction {
        runWithRetry { dataInteraction.usingAdapterViewProtocol(adapterViewProtocol) }
        return this
    }

    fun check(assertion: ViewAssertion): MokaDataInteraction {
        runWithRetry { dataInteraction.check(assertion) }
        return this
    }

    fun checkMatches(matcher: Matcher<View>): MokaDataInteraction {
        loopMainThreadUntil(matcher)
        runWithRetry { dataInteraction.check(matches(matcher)) }
        return this
    }

    override fun perform(vararg viewActions: ViewAction): MokaViewInteraction {
        return runWithRetryAndReturnSugarViewInteraction { dataInteraction.perform(*viewActions) }
    }

    private fun runWithRetryAndReturnSugarViewInteraction(action: () -> ViewInteraction): MokaViewInteraction {
        return MokaViewInteraction(runWithRetry(action))
    }
}
