package com.sample.app

import android.app.Application
import timber.log.Timber

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(PaybackDebugTree())
        }
    }
}

private class PaybackDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String? {
        return String.format("(%s:%s)", element.fileName, element.lineNumber)
    }
}