package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.util.CloudLogger

object ConfigServiceFactory {
    private const val TAG = "ConfigServiceFactory"

    @Volatile
    private var configService: ConfigService? = null

    @Volatile
    var isTestMode = false

    fun getConfigService(): ConfigService =
        configService ?: synchronized(this) {
            configService ?: if (isTestMode) {
                CloudLogger.debug(TAG, "Using test config service")
                TestConfigService()
            } else {
                try {
                    CloudLogger.debug(TAG, "Using remote config service")
                    RemoteConfigService.getInstance()
                } catch (e: IllegalStateException) {
                    CloudLogger.warn(TAG, "Failed to initialize RemoteConfigService, using test config", e)
                    TestConfigService()
                }
            }.also { configService = it }
        }
}

private class TestConfigService : ConfigService {
    override suspend fun getOpenAIApiKey(): String? = null
}
