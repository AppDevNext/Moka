package com.moka.utils

import android.app.Activity

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import timber.log.Timber

class ScreenshotRule : TestRule {

    private var className: String? = null
    private var methodName: String? = null

    fun take(activity: Activity, name: String) {
        val lineNumber = Thread.currentThread().stackTrace.find { it.className == className }?.lineNumber
        val simpleClassName = className?.takeLastWhile { it != '.' } // without package
        Timber.d("Screenshot ${activity.javaClass.simpleName} $simpleClassName.$methodName:$lineNumber..$name")
        Screenshot.takeScreenshotWithTimestamp("$name.$simpleClassName.$methodName.L$lineNumber")
    }

    override fun apply(base: Statement, description: Description): Statement {
        className = description.className
        methodName = description.methodName
        return base
    }
}
