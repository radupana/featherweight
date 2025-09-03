package com.github.radupana.featherweight.service

import android.util.Log
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
        private const val BUGFENDER_API_KEY = "bugfender_api_key"

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
                BUGFENDER_API_KEY to "",
            ),
        )
    }

    override suspend fun initialize() {
        if (isInitialized) return

        try {
            remoteConfig.fetchAndActivate().await()
            isInitialized = true
            Log.d(TAG, "Remote config initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch remote config", e)
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
            Log.w(TAG, "OpenAI API key not found in remote config")
            null
        }
    }

    fun getBugfenderApiKey(): String? {
        if (!isInitialized) {
            return null
        }
        return remoteConfig.getString(BUGFENDER_API_KEY).takeIf { it.isNotEmpty() && it != "null" }
    }
    
    fun fetchAndActivate(onComplete: () -> Unit) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                isInitialized = true
                Log.d(TAG, "Remote config fetched and activated")
            } else {
                Log.e(TAG, "Failed to fetch remote config")
            }
            onComplete()
        }
    }
}
