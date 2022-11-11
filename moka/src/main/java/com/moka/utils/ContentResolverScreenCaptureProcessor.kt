package com.moka.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.ScreenCapture
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class ContentResolverScreenCaptureProcessor(private val contentResolver: ContentResolver, private var withFilename: Boolean = true, private val excludeArea: Rect? = null) : BasicScreenCaptureProcessor() {

    private val dateFormat: SimpleDateFormat

    init {
        mTag = "FileNameScreenCaptureProcessor"
        mFileNameDelimiter = "-"
        mDefaultFilenamePrefix = "screenshot"
        mDefaultScreenshotPath = File("/storage/self/primary/Download")
        dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    }

    override fun getFilename(prefix: String?): String {
        return if (withFilename)
            prefix + mFileNameDelimiter + dateFormat.format(Date())
        else
            prefix?.let { it } ?: run { dateFormat.format(Date()) }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Throws(IOException::class)
    override fun process(capture: ScreenCapture): String? {
        var filename = if (capture.name == null) defaultFilename else getFilename(capture.name)
        filename += "." + capture.format.toString().toLowerCase(Locale.ROOT)

        var bitmapToStore = capture.bitmap

        excludeArea?.let {
            bitmapToStore = bitmapToStore.copy(Bitmap.Config.ARGB_8888, true)
            bitmapToStore.setHasAlpha(true)
            val transparentIntArray = IntArray(bitmapToStore.width * bitmapToStore.height)
            bitmapToStore.setPixels(
                transparentIntArray,
                0,
                bitmapToStore.width,
                excludeArea.left,
                excludeArea.top,
                excludeArea.width(),
                excludeArea.height()
            )
        }

        storeScreenshot(contentResolver, filename, bitmapToStore, capture)

        return filename
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun storeScreenshot(
        contentResolver: ContentResolver,
        screenshotName: String,
        bitmap: Bitmap,
        capture: ScreenCapture
    ) {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Files.FileColumns.DISPLAY_NAME, screenshotName)
        contentValues.put(MediaStore.Files.FileColumns.IS_PENDING, screenshotName)
        contentValues.put(MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.Files.FileColumns.MEDIA_TYPE_NONE)
        contentValues.put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)

        contentValues.put(MediaStore.Files.FileColumns.IS_PENDING, 1)

        val uri: Uri? = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            val outputStream = contentResolver.openOutputStream(uri)
            outputStream?.use {
                bitmap.compress(capture.format, 100, it)
                it.flush()
            }

            contentValues.clear()
            contentValues.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)
        }
    }
}
