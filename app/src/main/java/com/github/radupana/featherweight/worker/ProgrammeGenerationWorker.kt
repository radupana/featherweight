package com.github.radupana.featherweight.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GenerationStatus
import com.github.radupana.featherweight.repository.SimpleRequest
import com.github.radupana.featherweight.service.AIProgrammeRequest
import com.github.radupana.featherweight.service.AIProgrammeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import java.net.SocketTimeoutException

class ProgrammeGenerationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    companion object {
        const val KEY_REQUEST_ID = "request_id"
        const val KEY_REQUEST_PAYLOAD = "request_payload"
        const val MAX_RETRY_COUNT = 3
        const val TIMEOUT_MILLIS = 300_000L // 5 minutes

        fun createWorkRequest(
            requestId: String,
            requestPayload: String,
        ): OneTimeWorkRequest {
            val inputData =
                workDataOf(
                    KEY_REQUEST_ID to requestId,
                    KEY_REQUEST_PAYLOAD to requestPayload,
                )

            return OneTimeWorkRequestBuilder<ProgrammeGenerationWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                ).setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    java.util.concurrent.TimeUnit.SECONDS,
                ).addTag("programme_generation")
                .addTag(requestId)
                .build()
        }
    }

    private val database = FeatherweightDatabase.getDatabase(applicationContext)
    private val aiRequestDao = database.aiProgrammeRequestDao()
    private val aiService = AIProgrammeService(applicationContext)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val requestId = inputData.getString(KEY_REQUEST_ID) ?: return@withContext Result.failure()
            val requestPayload = inputData.getString(KEY_REQUEST_PAYLOAD) ?: return@withContext Result.failure()

            try {
                // Increment attempt count
                aiRequestDao.incrementAttemptCount(requestId)
                aiRequestDao.getRequestById(requestId) ?: return@withContext Result.failure()

                // Parse the request payload
                val simpleRequest = json.decodeFromString<SimpleRequest>(requestPayload)

                // Get exercise database
                val exercises = database.exerciseVariationDao().getAllExerciseVariations()
                val exerciseNames = exercises.map { it.name }

                // Create AI request
                val aiRequest =
                    AIProgrammeRequest(
                        userInput = simpleRequest.userInput,
                        exerciseDatabase = exerciseNames,
                        user1RMs = simpleRequest.user1RMs,
                    )

                // Make the API call with timeout
                val response =
                    withTimeout(TIMEOUT_MILLIS) {
                        aiService.generateProgramme(aiRequest)
                    }

                if (response.success && response.programme != null) {
                    // Save the generated programme
                    val programmeJson = json.encodeToString(response.programme)
                    aiRequestDao.saveGeneratedProgramme(requestId, programmeJson)

                    Result.success()
                } else if (response.clarificationNeeded != null) {
                    // Handle clarification needed case
                    aiRequestDao.updateStatusWithClarification(
                        requestId,
                        GenerationStatus.NEEDS_CLARIFICATION,
                        response.clarificationNeeded,
                    )

                    Result.success()
                } else {
                    val errorMessage = response.error ?: "Unknown error"
                    aiRequestDao.updateStatus(requestId, GenerationStatus.FAILED, errorMessage)

                    Result.failure()
                }
            } catch (e: SocketTimeoutException) {
                // Check if we should retry
                if (runAttemptCount < MAX_RETRY_COUNT - 1) {
                    Result.retry()
                } else {
                    aiRequestDao.updateStatus(requestId, GenerationStatus.FAILED, "Request timed out after $MAX_RETRY_COUNT attempts")
                    Result.failure()
                }
            } catch (e: Exception) {
                aiRequestDao.updateStatus(requestId, GenerationStatus.FAILED, e.message ?: "Unknown error")
                Result.failure()
            }
        }
}
