package com.github.radupana.featherweight.service

import android.util.Log

object ConfigServiceFactory {
    private const val TAG = "ConfigServiceFactory"

    @Volatile
    private var configService: ConfigService? = null

    @Volatile
    var isTestMode = false

    fun getConfigService(): ConfigService =
        configService ?: synchronized(this) {
            configService ?: if (isTestMode) {
                Log.d(TAG, "Using test config service")
                TestConfigService()
            } else {
                try {
                    Log.d(TAG, "Using remote config service")
                    RemoteConfigService.getInstance()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to initialize RemoteConfigService, using test config", e)
                    TestConfigService()
                }
            }.also { configService = it }
        }
}

private class TestConfigService : ConfigService {
    override suspend fun getOpenAIApiKey(): String? = null
}
