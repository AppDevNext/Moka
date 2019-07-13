package com.moka.debug

import android.os.Trace
import com.moka.BuildConfig

object DebugTrace {

    @JvmStatic
    fun beginSection(sectionName: String) {
        if (BuildConfig.DEBUG) {
            Trace.beginSection(sectionName)
        }
    }

    @JvmStatic
    fun endSection() {
        if (BuildConfig.DEBUG) {
            Trace.endSection()
        }
    }
}
