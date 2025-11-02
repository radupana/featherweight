package com.github.radupana.featherweight.service

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Firebase implementation of CloudFunctionCaller
 */
class FirebaseCloudFunctionCaller(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("europe-west2"),
) : CloudFunctionCaller {
    override suspend fun call(
        functionName: String,
        data: Map<String, Any>,
    ): Any? {
        val callable = functions.getHttpsCallable(functionName)
        callable.setTimeout(300, TimeUnit.SECONDS)
        return callable.call(data).await().data
    }
}
