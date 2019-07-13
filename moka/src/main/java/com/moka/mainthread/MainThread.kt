package com.moka.mainthread

import android.os.Looper

import java.lang.Thread.currentThread

object MainThread {

    fun checkNotOnMainThread() {
        try {
            if (onMainThread()) {
                throw illegalThreadStateExceptionExpectedNotMainThread()
            }
        } catch (e: Throwable) {
            val exception = illegalThreadStateExceptionExpectedNotMainThread()
            exception.initCause(e)
            throw exception
        }

    }

    fun checkOnMainThread() {
        try {
            if (!onMainThread()) {
                throw illegalThreadStateExceptionExpectedMainThread()
            }
        } catch (e: Throwable) {
            val exception = illegalThreadStateExceptionExpectedMainThread()
            exception.initCause(e)
            throw exception
        }

    }

    fun onMainThread(): Boolean {
        val myLooper = Looper.myLooper()
        val mainLooper = Looper.getMainLooper()
        return mainLooper == myLooper
    }

    private fun illegalThreadStateExceptionExpectedNotMainThread(): IllegalThreadStateException {
        return IllegalThreadStateException("Expected to NOT be on the Main Thread, but was on " + currentThread())
    }

    private fun illegalThreadStateExceptionExpectedMainThread(): IllegalThreadStateException {
        return IllegalThreadStateException("Expected to be on the Main Thread, but was on " + currentThread())
    }
}
