package com.github.radupana.featherweight.service

interface ConfigService {
    suspend fun getOpenAIApiKey(): String?
}
