package com.github.radupana.featherweight.service

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await

class RemoteConfigService {
    companion object {
        private const val TAG = "RemoteConfigService"
        private const val OPENAI_API_KEY = "openai_api_key"

        @Volatile
        private var instance: RemoteConfigService? = null

        fun getInstance(): RemoteConfigService =
            instance ?: synchronized(this) {
                instance ?: RemoteConfigService().also { instance = it }
            }
    }

    private val remoteConfig = Firebase.remoteConfig
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
            ),
        )
    }

    suspend fun initialize() {
        if (isInitialized) return

        try {
            remoteConfig.fetchAndActivate().await()
            isInitialized = true
            Log.d(TAG, "Remote config initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch remote config", e)
        }
    }

    suspend fun getOpenAIApiKey(): String? {
        if (!isInitialized) {
            initialize()
        }

        cachedApiKey?.let { return it }

        val key = remoteConfig.getString(OPENAI_API_KEY)
        return if (key.isNotEmpty() && key != "null" && key != "YOUR_API_KEY_HERE") {
            cachedApiKey = key
            key
        } else {
            Log.w(TAG, "OpenAI API key not found in remote config")
            null
        }
    }
}
