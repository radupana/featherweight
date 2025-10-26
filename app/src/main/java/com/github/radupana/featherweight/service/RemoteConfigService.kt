package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.util.CloudLogger
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await

class RemoteConfigService(
    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig,
) : ConfigService {
    companion object {
        private const val TAG = "RemoteConfigService"
        private const val OPENAI_API_KEY = "openai_api_key"

        private const val LOGGING_ENABLED = "logging_enabled"
        private const val LOGGING_MIN_LEVEL = "logging_min_level"
        private const val LOGGING_SAMPLE_RATE_INFO = "logging_sample_rate_info"
        private const val LOGGING_SAMPLE_RATE_DEBUG = "logging_sample_rate_debug"
        private const val LOGGING_BATCH_SIZE = "logging_batch_size"
        private const val LOGGING_BATCH_INTERVAL_MS = "logging_batch_interval_ms"
        private const val CLOUD_LOGGING_FUNCTION_URL = "cloud_logging_function_url"

        @Volatile
        private var instance: RemoteConfigService? = null

        fun getInstance(): RemoteConfigService =
            instance ?: synchronized(this) {
                instance ?: RemoteConfigService().also { instance = it }
            }
    }

    private var isInitialized = false
    private var cachedApiKey: String? = null

    init {
        val configSettings =
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600
            }
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.setDefaultsAsync(
            mapOf(
                OPENAI_API_KEY to "",
                LOGGING_ENABLED to true,
                LOGGING_MIN_LEVEL to "INFO",
                LOGGING_SAMPLE_RATE_INFO to 0.1,
                LOGGING_SAMPLE_RATE_DEBUG to 0.01,
                LOGGING_BATCH_SIZE to 10L,
                LOGGING_BATCH_INTERVAL_MS to 30000L,
                CLOUD_LOGGING_FUNCTION_URL to "",
            ),
        )
    }

    suspend fun initialize() {
        if (isInitialized) return

        try {
            remoteConfig.fetchAndActivate().await()
            isInitialized = true
            CloudLogger.debug(TAG, "Remote config initialized successfully")
        } catch (e: com.google.firebase.remoteconfig.FirebaseRemoteConfigException) {
            CloudLogger.error(TAG, "Failed to fetch remote config", e)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            CloudLogger.error(TAG, "Remote config fetch timed out", e)
        } catch (e: java.io.IOException) {
            CloudLogger.error(TAG, "Network error fetching remote config", e)
        }
    }

    override suspend fun getOpenAIApiKey(): String? {
        if (!isInitialized) {
            initialize()
        }

        cachedApiKey?.let { return it }

        val key = remoteConfig.getString(OPENAI_API_KEY)
        return if (key.isNotEmpty() && key != "null" && key != "YOUR_API_KEY_HERE") {
            cachedApiKey = key
            key
        } else {
            CloudLogger.warn(TAG, "OpenAI API key not found in remote config")
            null
        }
    }

    fun getString(key: String): String = remoteConfig.getString(key)

    fun getBoolean(key: String): Boolean = remoteConfig.getBoolean(key)

    fun getDouble(key: String): Double = remoteConfig.getDouble(key)

    fun getLong(key: String): Long = remoteConfig.getLong(key)
}
