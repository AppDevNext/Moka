package com.moka.utils

import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileNameScreenCaptureProcessor(private var withFilename: Boolean = true) : BasicScreenCaptureProcessor() {

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
}
