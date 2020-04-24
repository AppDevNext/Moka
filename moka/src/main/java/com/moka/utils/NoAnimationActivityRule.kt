package com.moka.utils

import android.app.Activity
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import timber.log.Timber

open class NoAnimationActivityRule<T : Activity>(activityClass: Class<T>) : ActivityTestRule<T>(activityClass) {

    private val device by lazy { UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()) }

    override fun launchActivity(startIntent: Intent?): T {
        Timber.d(device.executeShellCommand("settings put global window_animation_scale 0"))
        Timber.d(device.executeShellCommand("settings put global transition_animation_scale 0"))
        Timber.d(device.executeShellCommand("settings put global animator_duration_scale 0"))
        Timber.d(device.executeShellCommand("settings put system window_animation_scale 0"))
        Timber.d(device.executeShellCommand("settings put system transition_animation_scale 0"))
        Timber.d(device.executeShellCommand("settings put system animator_duration_scale 0"))
        return super.launchActivity(startIntent)
    }
}