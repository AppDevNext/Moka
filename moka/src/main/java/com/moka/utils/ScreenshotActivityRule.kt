package com.moka.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import androidx.test.espresso.Espresso
import androidx.test.espresso.base.DefaultFailureHandler
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * It captures on error case a screenshot
 */
open class ScreenshotActivityRule<T : Activity>(activityClass: Class<T>) : NoAnimationActivityRule<T>(activityClass) {

    private val screenshotRule = ScreenshotRule()

    override fun apply(base: Statement, description: Description?): Statement {
        if (description != null) {
            val context = InstrumentationRegistry.getInstrumentation().context
            Espresso.setFailureHandler { throwable, matcher ->
                @Suppress("ConstantConditionIf")
                if (DUMP_THREADS) {
                    this@ScreenshotActivityRule.dumpThreads()
                }
                if (description.getAnnotation(Test::class.java).expected != throwable.javaClass) {
                    this@ScreenshotActivityRule.activity?.let {
                        screenshotRule.take(it, "failed")
                    }
                }
                DefaultFailureHandler(context).handle(throwable, matcher)
            }
            screenshotRule.apply(base, description)
        }
        return super.apply(base, description)
    }

    @SuppressLint("LogNotTimber")
    private fun dumpThreads() {
        val activeCount = Thread.activeCount()
        val threads = arrayOfNulls<Thread>(activeCount)
        Thread.enumerate(threads)
        threads.forEach {
            if (it != null) {
                Log.e("dumpThreads", it.name + ": " + it.state)
                for (stackTraceElement in it.stackTrace) {
                    Log.e("dumpThreads", "\t" + stackTraceElement)
                }
            }
        }
    }

    companion object {

        private const val DUMP_THREADS = false
    }
}
