package com.sample.app

import android.os.Bundle
import androidx.test.espresso.IdlingPolicies
import androidx.test.runner.AndroidJUnitRunner
import com.moka.EspressoMokaRunner
import com.moka.lib.debug.DebugTrace.beginSection
import com.moka.lib.debug.DebugTrace.endSection
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SampleAndroidJUnitRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle) {
        EspressoMokaRunner.onCreate(arguments)
        super.onCreate(arguments)
        IdlingPolicies.setMasterPolicyTimeout(15, TimeUnit.SECONDS)
        IdlingPolicies.setIdlingResourceTimeout(15, TimeUnit.SECONDS)
    }

    override fun onStart() {
        EspressoMokaRunner.onStart()
        super.onStart()
    }

    override fun waitForIdleSync() {
        super.waitForIdleSync()
        EspressoMokaRunner.waitForIdleSync()
    }

    override fun onException(obj: Any, e: Throwable): Boolean {
        beginSection(e.message + " SampleAndroidJUnitRunner.onException")
        return try {
            super.onException(obj, e)
            Timber.e(e, "%s", obj.toString())
            EspressoMokaRunner.onException(obj, e)
        } finally {
            endSection()
        }
    }
}