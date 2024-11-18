package com.sample.app

import android.graphics.Bitmap
import androidx.test.core.app.takeScreenshot
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.captureToBitmap
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import java.io.IOException

/*
 * Illustrates usage of APIs to capture a bitmap from view and saving it to test storage.
 *
 * When this test is executed via gradle managed devices, the saved image files will be stored at
 * build/outputs/managed_device_android_test_additional_output/debugAndroidTest/managedDevice/nexusOneApi30/
 */
@RunWith(AndroidJUnit4::class)
class ADTTest {

    // a handy JUnit rule that stores the method name, so it can be used to generate unique screenshot files per test method
    @get:Rule
    var nameRule = TestName()

    @get:Rule
    val activityScenarioRule = activityScenarioRule<MainActivity>()

    /**
     * Captures and saves an image of the entire [MainActivity] contents.
     */
    @Test
    @Throws(IOException::class)
    fun saveActivityBitmap() {
        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap -> bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}") })

    }

    /**
     * Captures and saves an image of the 'Hello world' view.
     */
    @Test
    @Throws(IOException::class)
    fun saveViewBitmap() {
        onView(ViewMatchers.withText("Hello Espresso!"))
            .perform(captureToBitmap { bitmap: Bitmap -> bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}") })
    }

    /**
     * Captures and saves an image of the entire device screen to storage.
     */
    @Test
    @Throws(IOException::class)
    fun saveDeviceScreenBitmap() {
        Thread.sleep(500)
        takeScreenshot()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}")
    }

    @Test
    fun changeTextSameActivity() {
        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap -> bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-Start") })

        // Type text and then press the button.
        onView(ViewMatchers.withId(R.id.editTextUserInput)).perform(ViewActions.typeText(STRING_TO_BE_TYPED), ViewActions.closeSoftKeyboard())
        onView(ViewMatchers.withId(R.id.changeTextBt)).perform(ViewActions.click())
        // Check that the text was changed.
        onView(ViewMatchers.withId(R.id.textToBeChanged)).check(matches(ViewMatchers.withText(STRING_TO_BE_TYPED)))
        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap -> bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-End") })
    }

    companion object {
        private const val STRING_TO_BE_TYPED = "Espresso"
    }
}
