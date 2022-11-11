package com.moka.utils

import android.app.Activity
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.test.runner.screenshot.ScreenCapture
import java.io.IOException
import java.util.*

object Screenshot {

    @RequiresApi(Build.VERSION_CODES.Q)
    fun takeScreenshot(contentResolver: ContentResolver, fileName: String, excludeArea: Rect? = null): String? {
        val screenCap = androidx.test.runner.screenshot.Screenshot.capture()
        return processScreenCapture(contentResolver, screenCap, fileName, excludeArea)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun takeScreenshot(contentResolver: ContentResolver, view: View, fileName: String, excludeArea: Rect? = null): String? {
        val screenCap = androidx.test.runner.screenshot.Screenshot.capture(view)
        return processScreenCapture(contentResolver, screenCap, fileName, excludeArea)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun takeScreenshot(activity: Activity, fileName: String, excludeArea: Rect? = null): String? {
        val screenCap = androidx.test.runner.screenshot.Screenshot.capture(activity)
        return processScreenCapture(activity.contentResolver, screenCap, fileName, excludeArea)
    }

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

    private fun processScreenCapture(contentResolver: ContentResolver, screenCapture: ScreenCapture, fileName: String, excludeArea: Rect? = null): String? {
        try {
            val stackTrace = Thread.currentThread().stackTrace

            // Depending of whether excludeArea param was passed or not
            val caller = if (stackTrace[5].fileName == "Method.java") {
                stackTrace[4]
            } else {
                stackTrace[5]
            }

            val automaticIdentifier =  caller.fileName + "-" + caller.methodName

            screenCapture.format = Bitmap.CompressFormat.PNG
            screenCapture.name = "$automaticIdentifier-$fileName"
            val list = ArrayList<ContentResolverScreenCaptureProcessor>(1)
            list.add(ContentResolverScreenCaptureProcessor(contentResolver, false, excludeArea))
            val processor: Set<ContentResolverScreenCaptureProcessor> = list.toSet()
            screenCapture.process(processor)
            return screenCapture.name + ".png"
        } catch (e: IOException) {
            e.printStackTrace()
            throw IllegalStateException(e)
        }
    }
}
