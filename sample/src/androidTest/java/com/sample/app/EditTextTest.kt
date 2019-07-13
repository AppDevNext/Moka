package com.sample.app

import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.moka.EspressoMoka
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditTextTest {

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun changeText_sameActivity() {
        // Type text and then press the button.
        EspressoMoka.onView(ViewMatchers.withId(R.id.editTextUserInput)).perform(ViewActions.typeText(STRING_TO_BE_TYPED), ViewActions.closeSoftKeyboard())
        EspressoMoka.onView(ViewMatchers.withId(R.id.changeTextBt)).perform(ViewActions.click())
        // Check that the text was changed.
        EspressoMoka.onView(ViewMatchers.withId(R.id.textToBeChanged)).checkMatches(ViewMatchers.withText(STRING_TO_BE_TYPED))
    }

    companion object {
        private const val STRING_TO_BE_TYPED = "Espresso"
    }
}