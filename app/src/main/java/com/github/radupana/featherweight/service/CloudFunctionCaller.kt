package com.github.radupana.featherweight.service

/**
 * Interface for calling cloud functions
 * Allows for easier testing by abstracting Firebase dependencies
 */
interface CloudFunctionCaller {
    suspend fun call(
        functionName: String,
        data: Map<String, Any>,
    ): Any?
}
