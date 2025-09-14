package com.github.radupana.featherweight.util

import android.util.Log

object ExceptionLogger {
    fun logException(
        tag: String,
        message: String,
        throwable: Throwable,
        level: LogLevel = LogLevel.ERROR,
    ) {
        val fullMessage = "$message: ${throwable.message}"
        try {
            when (level) {
                LogLevel.DEBUG -> Log.d(tag, fullMessage, throwable)
                LogLevel.WARNING -> Log.w(tag, fullMessage, throwable)
                LogLevel.ERROR -> Log.e(tag, fullMessage, throwable)
            }
        } catch (
            @Suppress("TooGenericExceptionCaught", "SwallowedException") e: RuntimeException,
        ) {
            println("[$tag] $fullMessage")
            println(throwable.stackTraceToString())
        }
    }

    fun logNonCritical(
        tag: String,
        message: String,
        throwable: Throwable,
    ) {
        logException(tag, message, throwable, LogLevel.DEBUG)
    }

    enum class LogLevel {
        DEBUG,
        WARNING,
        ERROR,
    }
}
