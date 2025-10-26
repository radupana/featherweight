package com.github.radupana.featherweight.util

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
                LogLevel.DEBUG -> CloudLogger.debug(tag, fullMessage, throwable)
                LogLevel.WARNING -> CloudLogger.warn(tag, fullMessage, throwable)
                LogLevel.ERROR -> CloudLogger.error(tag, fullMessage, throwable)
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
