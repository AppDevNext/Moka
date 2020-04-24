package com.moka.utils

import android.graphics.Bitmap
import java.io.IOException
import java.util.*

object Screenshot {

    fun takeScreenshot(fileName: String) {
        val screenCap = androidx.test.runner.screenshot.Screenshot.capture()
        try {
            val caller = Thread.currentThread().stackTrace[3]
            val automaticIdentifier =  caller.fileName + "-" + caller.methodName

            screenCap.format = Bitmap.CompressFormat.PNG
            screenCap.name = "$automaticIdentifier-$fileName"
            val list = ArrayList<FileNameScreenCaptureProcessor>(1)
            list.add(FileNameScreenCaptureProcessor(false))
            val processor: Set<FileNameScreenCaptureProcessor> = list.toMutableSet()
            processor.plus(FileNameScreenCaptureProcessor())
            screenCap.process(processor)
        } catch (e: IOException) {
            e.printStackTrace()
            throw IllegalStateException(e)
        }
    }

    fun takeScreenshotWithTimestamp(fileName: String) {
        val screenCap = androidx.test.runner.screenshot.Screenshot.capture()
        try {
            screenCap.format = Bitmap.CompressFormat.PNG
            screenCap.name = fileName
            val list = ArrayList<FileNameScreenCaptureProcessor>(1)
            list.add(FileNameScreenCaptureProcessor())
            val processor: Set<FileNameScreenCaptureProcessor> = list.toMutableSet()
            processor.plus(FileNameScreenCaptureProcessor())
            screenCap.process(processor)
        } catch (e: IOException) {
            e.printStackTrace()
            throw IllegalStateException(e)
        }
    }

}
