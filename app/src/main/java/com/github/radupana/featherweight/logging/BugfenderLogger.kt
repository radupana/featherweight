package com.github.radupana.featherweight.logging

import android.content.Context
import android.util.Log
import com.bugfender.sdk.Bugfender
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object BugfenderLogger {
    private const val MAX_LOG_LENGTH = 10000
    private const val TRUNCATION_MARKER = "...[TRUNCATED]..."

    private val correlationIds = ConcurrentHashMap<String, String>()
    private var isInitialized = false

    fun initialize(
        context: Context,
        appKey: String,
        isDebug: Boolean = true,
    ) {
        if (isInitialized) return

        try {
            Bugfender.init(context, appKey, isDebug)
            Bugfender.enableLogcatLogging()
            Bugfender.enableCrashReporting()
            if (context is android.app.Application) {
                Bugfender.enableUIEventLogging(context)
            }

            Bugfender.setDeviceString("app_version", "${context.packageManager.getPackageInfo(context.packageName, 0).versionName}")
            Bugfender.setDeviceString("device_manufacturer", android.os.Build.MANUFACTURER)
            Bugfender.setDeviceString("device_model", android.os.Build.MODEL)
            Bugfender.setDeviceString("android_version", android.os.Build.VERSION.RELEASE)
            Bugfender.setDeviceInteger("android_sdk", android.os.Build.VERSION.SDK_INT)

            isInitialized = true
            i("BugfenderLogger", "Bugfender initialized - App: ${context.packageName}, Debug: $isDebug")
        } catch (e: Exception) {
            Log.e("BugfenderLogger", "Failed to initialize Bugfender", e)
        }
    }

    fun createCorrelationId(operation: String): String {
        val correlationId = UUID.randomUUID().toString()
        correlationIds[operation] = correlationId
        return correlationId
    }

    fun clearCorrelationId(operation: String) {
        correlationIds.remove(operation)
    }

    fun logUserAction(
        action: String,
        details: Map<String, Any>? = null,
    ) {
        val detailsStr = details?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: ""
        val message =
            if (detailsStr.isNotEmpty()) {
                "USER_ACTION: $action [$detailsStr]"
            } else {
                "USER_ACTION: $action"
            }
        i("UserAction", message)
    }

    fun logPerformance(
        tag: String,
        operation: String,
        durationMs: Long,
        details: Map<String, Any>? = null,
    ) {
        val detailsStr = details?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: ""
        val message =
            if (detailsStr.isNotEmpty()) {
                "PERFORMANCE: $operation completed in ${durationMs}ms [$detailsStr]"
            } else {
                "PERFORMANCE: $operation completed in ${durationMs}ms"
            }

        if (durationMs > 3000) {
            w(tag, message)
        } else {
            d(tag, message)
        }
    }

    fun logApiRequest(
        tag: String,
        url: String,
        method: String = "POST",
        requestBody: String? = null,
        headers: Map<String, String>? = null,
        correlationId: String? = null,
    ) {
        val logMessage =
            buildString {
                appendLine("=== API REQUEST ===")
                correlationId?.let { appendLine("CorrelationId: $it") }
                appendLine("URL: $url")
                appendLine("Method: $method")
                headers?.forEach { (key, value) ->
                    if (!key.contains("authorization", ignoreCase = true)) {
                        appendLine("Header: $key=$value")
                    }
                }
                requestBody?.let {
                    appendLine("Body: ${truncateIfNeeded(it)}")
                }
            }
        d(tag, logMessage)
    }

    fun logApiResponse(
        tag: String,
        url: String,
        statusCode: Int,
        responseBody: String? = null,
        responseTime: Long? = null,
        correlationId: String? = null,
    ) {
        val logMessage =
            buildString {
                appendLine("=== API RESPONSE ===")
                correlationId?.let { appendLine("CorrelationId: $it") }
                appendLine("URL: $url")
                appendLine("Status: $statusCode")
                responseTime?.let { appendLine("Time: ${it}ms") }
                responseBody?.let {
                    appendLine("Body: ${truncateIfNeeded(it)}")
                }
            }

        if (statusCode >= 400) {
            e(tag, logMessage)
        } else {
            d(tag, logMessage)
        }
    }

    fun v(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        Log.v(tag, message, throwable)
        if (!isInitialized) return
        sendToBugfender(0, tag, message, throwable)
    }

    fun d(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        Log.d(tag, message, throwable)
        if (!isInitialized) return
        sendToBugfender(1, tag, message, throwable)
    }

    fun i(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        Log.i(tag, message, throwable)
        if (!isInitialized) return
        sendToBugfender(2, tag, message, throwable)
    }

    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        Log.w(tag, message, throwable)
        if (!isInitialized) return
        sendToBugfender(3, tag, message, throwable)
    }

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        Log.e(tag, message, throwable)
        if (!isInitialized) return
        sendToBugfender(4, tag, message, throwable)
    }

    private fun sendToBugfender(
        level: Int,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        try {
            val fullMessage =
                if (throwable != null) {
                    "$message\n${throwable.stackTraceToString()}"
                } else {
                    message
                }

            val truncatedMessage = truncateIfNeeded(fullMessage)
            when (level) {
                0 -> Bugfender.t(tag, truncatedMessage) // Trace
                1 -> Bugfender.d(tag, truncatedMessage) // Debug
                2 -> Bugfender.i(tag, truncatedMessage) // Info
                3 -> Bugfender.w(tag, truncatedMessage) // Warning
                4 -> Bugfender.e(tag, truncatedMessage) // Error
                5 -> Bugfender.f(tag, truncatedMessage) // Fatal
                else -> Bugfender.d(tag, truncatedMessage)
            }
        } catch (e: Exception) {
            Log.e("BugfenderLogger", "Failed to send log to Bugfender", e)
        }
    }

    private fun truncateIfNeeded(message: String): String =
        if (message.length > MAX_LOG_LENGTH) {
            val halfLength = (MAX_LOG_LENGTH - TRUNCATION_MARKER.length) / 2
            message.take(halfLength) + TRUNCATION_MARKER + message.takeLast(halfLength)
        } else {
            message
        }
}