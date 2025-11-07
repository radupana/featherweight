@file:Suppress("TooGenericExceptionCaught")

package com.github.radupana.featherweight.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import com.github.radupana.featherweight.BuildConfig
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.service.RemoteConfigService
import com.github.radupana.featherweight.utils.InstallationIdProvider
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@Serializable
data class LogEvent(
    val level: String,
    val tag: String,
    val message: String,
    val timestamp: Long,
    val context: LogContext? = null,
    val throwable: ThrowableInfo? = null,
)

@Serializable
data class LogContext(
    val installationId: String? = null,
    val appVersion: String? = null,
    val deviceModel: String? = null,
    val androidVersion: String? = null,
    val screen: String? = null,
)

@Serializable
data class ThrowableInfo(
    val message: String,
    val stackTrace: String,
)

@Serializable
data class LogBatch(
    val events: List<LogEvent>,
)

@SuppressLint("StaticFieldLeak")
object CloudLogger {
    private const val TAG = "CloudLogger"
    private const val DEFAULT_BATCH_SIZE = 10
    private const val DEFAULT_BATCH_INTERVAL_MS = 30000L
    private const val MAX_QUEUE_SIZE = 1000

    private var context: Context? = null
    private var authManager: AuthenticationManager? = null
    private var remoteConfigService: RemoteConfigService? = null
    private var cloudFunctionUrl: String? = null

    private val logQueue = ConcurrentLinkedQueue<LogEvent>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

    private var isInitialized = false
    private var isBatchingStarted = false
    private var hasLoggedMissingUrl = false
    private var hasLoggedMissingToken = false

    fun initialize(
        context: Context,
        authManager: AuthenticationManager,
        remoteConfigService: RemoteConfigService,
    ) {
        this.context = context.applicationContext
        this.authManager = authManager
        this.remoteConfigService = remoteConfigService
        this.isInitialized = true

        fetchCloudFunctionUrl()

        if (!isBatchingStarted) {
            startBatchingCoroutine()
            isBatchingStarted = true
        }

        Log.d(TAG, "CloudLogger initialized")
    }

    private fun fetchCloudFunctionUrl() {
        cloudFunctionUrl = remoteConfigService?.getString("cloud_logging_function_url")
        if (cloudFunctionUrl.isNullOrBlank()) {
            if (!hasLoggedMissingUrl) {
                Log.i(TAG, "Cloud logging function URL not yet available from Remote Config")
                hasLoggedMissingUrl = true
            }
        } else {
            Log.i(TAG, "Cloud logging configured with URL: $cloudFunctionUrl")
        }
    }

    fun debug(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        log("DEBUG", tag, message, throwable)
    }

    fun info(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        log("INFO", tag, message, throwable)
    }

    fun warn(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        log("WARN", tag, message, throwable)
    }

    fun error(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        log("ERROR", tag, message, throwable)
    }

    private fun log(
        level: String,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        val localContext = context
        if (!isInitialized || localContext == null) {
            logToLogcat(level, tag, message, throwable)
            return
        }

        logToLogcat(level, tag, message, throwable)

        if (!shouldLog(level)) {
            return
        }

        if (!shouldSample(level)) {
            return
        }

        val event = createLogEvent(level, tag, message, throwable, localContext)
        enqueueEvent(event)
    }

    private fun logToLogcat(
        level: String,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        when (level) {
            "DEBUG" -> if (throwable != null) Log.d(tag, message, throwable) else Log.d(tag, message)
            "INFO" -> if (throwable != null) Log.i(tag, message, throwable) else Log.i(tag, message)
            "WARN" -> if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
            "ERROR" -> if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
        }
    }

    private fun shouldLog(level: String): Boolean {
        val config = remoteConfigService ?: return false

        if (!config.getBoolean("logging_enabled")) {
            return false
        }

        val minLevel = config.getString("logging_min_level").uppercase()
        val levelPriority =
            mapOf(
                "DEBUG" to 0,
                "INFO" to 1,
                "WARN" to 2,
                "ERROR" to 3,
            )

        val currentPriority = levelPriority[level] ?: 0
        val minPriority = levelPriority[minLevel] ?: 1

        return currentPriority >= minPriority
    }

    private fun shouldSample(level: String): Boolean {
        val config = remoteConfigService ?: return true

        val sampleRate =
            when (level) {
                "DEBUG" -> config.getDouble("logging_sample_rate_debug")
                "INFO" -> config.getDouble("logging_sample_rate_info")
                "WARN" -> 1.0
                "ERROR" -> 1.0
                else -> 1.0
            }

        return Random.nextDouble() < sampleRate
    }

    private fun createLogEvent(
        level: String,
        tag: String,
        message: String,
        throwable: Throwable?,
        context: Context,
    ): LogEvent {
        val logContext =
            LogContext(
                installationId = InstallationIdProvider.getId(context),
                appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            )

        val throwableInfo =
            throwable?.let {
                ThrowableInfo(
                    message = it.message ?: it.javaClass.simpleName,
                    stackTrace = it.stackTraceToString(),
                )
            }

        return LogEvent(
            level = level,
            tag = tag,
            message = message,
            timestamp = System.currentTimeMillis(),
            context = logContext,
            throwable = throwableInfo,
        )
    }

    private fun enqueueEvent(event: LogEvent) {
        if (logQueue.size >= MAX_QUEUE_SIZE) {
            Log.w(TAG, "Log queue full, dropping oldest event")
            logQueue.poll()
        }
        logQueue.offer(event)
    }

    private fun startBatchingCoroutine() {
        val config = remoteConfigService
        val batchSize = config?.getLong("logging_batch_size")?.toInt() ?: DEFAULT_BATCH_SIZE
        val batchInterval = config?.getLong("logging_batch_interval_ms") ?: DEFAULT_BATCH_INTERVAL_MS

        scope.launch {
            while (true) {
                delay(batchInterval)

                if (logQueue.size >= batchSize) {
                    flushLogs()
                }
            }
        }

        scope.launch {
            while (true) {
                delay(1000)
                if (logQueue.isNotEmpty() && logQueue.size >= batchSize) {
                    flushLogs()
                }
            }
        }
    }

    fun flushLogs() {
        if (logQueue.isEmpty()) {
            return
        }

        // Try to fetch URL again if we don't have it (Remote Config might have loaded)
        if (cloudFunctionUrl.isNullOrBlank()) {
            fetchCloudFunctionUrl()
        }

        val url = cloudFunctionUrl
        if (url.isNullOrBlank()) {
            // Silently skip - already logged once during initialization
            return
        }

        scope.launch {
            try {
                val events = mutableListOf<LogEvent>()
                while (events.size < 100 && logQueue.isNotEmpty()) {
                    logQueue.poll()?.let { events.add(it) }
                }

                if (events.isEmpty()) {
                    return@launch
                }

                sendBatch(events, url)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to flush logs", e)
            }
        }
    }

    private suspend fun sendBatch(
        events: List<LogEvent>,
        url: String,
    ) {
        try {
            val appCheckToken =
                getAppCheckToken() ?: run {
                    Log.w(TAG, "No App Check token available, cannot send logs")
                    return
                }

            val idToken =
                getIdToken() ?: run {
                    if (!hasLoggedMissingToken) {
                        Log.d(TAG, "No Firebase Auth token - user not signed in, logs will be local only")
                        hasLoggedMissingToken = true
                    }
                    return
                }

            hasLoggedMissingToken = false

            val batch = LogBatch(events = events)
            val jsonBody = json.encodeToString(batch)

            val request =
                Request
                    .Builder()
                    .url(url)
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $idToken")
                    .addHeader("X-Firebase-AppCheck", appCheckToken)
                    .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d(TAG, "Successfully sent ${events.size} log events to cloud")
            } else {
                Log.w(TAG, "Failed to send logs: ${response.code} ${response.message}")
            }
            response.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending log batch", e)
        }
    }

    private suspend fun getAppCheckToken(): String? =
        try {
            FirebaseAppCheck
                .getInstance()
                .getAppCheckToken(false)
                .await()
                .token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get App Check token", e)
            null
        }

    private suspend fun getIdToken(): String? {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            currentUser.getIdToken(false).await().token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ID token", e)
            null
        }
    }
}
