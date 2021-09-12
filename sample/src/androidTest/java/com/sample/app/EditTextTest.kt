package com.sample.app

import android.Manifest
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.moka.EspressoMoka
import com.moka.utils.Screenshot
import com.moka.utils.ScreenshotActivityRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditTextTest {

    @get:Rule
    var activityScenarioRule = ScreenshotActivityRule(MainActivity::class.java)

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Test
    fun changeTextSameActivity() {
        Screenshot.takeScreenshot("Start")
        // Type text and then press the button.
        EspressoMoka.onView(ViewMatchers.withId(R.id.editTextUserInput)).perform(ViewActions.typeText(STRING_TO_BE_TYPED), ViewActions.closeSoftKeyboard())
        EspressoMoka.onView(ViewMatchers.withId(R.id.changeTextBt)).perform(ViewActions.click())
        // Check that the text was changed.
        EspressoMoka.onView(ViewMatchers.withId(R.id.textToBeChanged)).checkMatches(ViewMatchers.withText(STRING_TO_BE_TYPED))
        Screenshot.takeScreenshot("End")
    }

    companion object {
        private const val STRING_TO_BE_TYPED = "Espresso"
    }
}
