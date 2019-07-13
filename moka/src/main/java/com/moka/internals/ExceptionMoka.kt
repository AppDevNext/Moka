@file:JvmName("ExceptionSugar")

package com.moka.internals

import timber.log.Timber
import java.io.PrintStream
import java.io.PrintWriter

@Suppress("TooGenericExceptionThrown")
fun propagate(e: Throwable): RuntimeException {
    Timber.w(e, "Propagating Throwable: ${e.javaClass.simpleName} ${e.message}")
    if (e is RuntimeException) {
        throw e
    } else {
        throw RuntimeException(e)
    }
}

class ExceptionFromAnotherPlace(throwable: Throwable, thread: Any) : RuntimeException(message(throwable, thread), throwable) {

    override fun printStackTrace(err: PrintStream) = cause?.printStackTrace(err)
            ?: super.printStackTrace(err)

    override fun printStackTrace(err: PrintWriter) = cause?.printStackTrace(err)
            ?: super.printStackTrace(err)

    companion object {
        private fun message(throwable: Throwable, thread: Any): String {
            return "From $thread: ExecutionThread died with an exception ( ${throwable.javaClass} : ${throwable.message} ), attempting to restart."
        }
    }
}
