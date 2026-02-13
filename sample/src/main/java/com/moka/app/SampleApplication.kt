package com.moka.app

import androidx.multidex.MultiDexApplication
import com.moka.app.BuildConfig
import timber.log.Timber

class SampleApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(SampleDebugTree())
        }
    }
}

private class SampleDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String {
        return String.format("(%s:%s)", element.fileName, element.lineNumber)
    }
}