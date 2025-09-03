package com.github.radupana.featherweight.service

interface ConfigService {
    suspend fun initialize()
    suspend fun getOpenAIApiKey(): String?
}
