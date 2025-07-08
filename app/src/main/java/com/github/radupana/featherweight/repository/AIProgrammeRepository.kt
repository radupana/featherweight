package com.github.radupana.featherweight.repository

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.github.radupana.featherweight.data.AIProgrammeRequest
import com.github.radupana.featherweight.data.AIProgrammeRequestDao
import com.github.radupana.featherweight.data.GenerationStatus
import com.github.radupana.featherweight.worker.ProgrammeGenerationWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.util.UUID
import java.util.concurrent.TimeUnit

@Serializable
data class SimpleRequest(
    val userInput: String,
    val selectedGoal: String? = null,
    val selectedFrequency: Int? = null,
    val selectedDuration: String? = null,
    val selectedExperience: String? = null,
    val selectedEquipment: String? = null,
    val generationMode: String
)

class AIProgrammeRepository(
    private val dao: AIProgrammeRequestDao,
    private val workManager: WorkManager
) {
    private val json = Json { encodeDefaults = true }
    
    suspend fun createGenerationRequest(
        userInput: String,
        selectedGoal: String? = null,
        selectedFrequency: Int? = null,
        selectedDuration: String? = null,
        selectedExperience: String? = null,
        selectedEquipment: String? = null,
        generationMode: String
    ): String {
        // Create request payload
        val simpleRequest = SimpleRequest(
            userInput = userInput,
            selectedGoal = selectedGoal,
            selectedFrequency = selectedFrequency,
            selectedDuration = selectedDuration,
            selectedExperience = selectedExperience,
            selectedEquipment = selectedEquipment,
            generationMode = generationMode
        )
        
        val requestPayload = json.encodeToString(simpleRequest)
        
        // Create database entry
        val request = AIProgrammeRequest(
            requestPayload = requestPayload
        )
        dao.insert(request)
        
        // Create and enqueue work
        val workRequest = ProgrammeGenerationWorker.createWorkRequest(request.id, requestPayload)
        workManager.enqueue(workRequest)
        
        // Update with WorkManager ID
        dao.updateWorkManagerId(request.id, workRequest.id.toString())
        
        return request.id
    }
    
    fun getAllRequests(): Flow<List<AIProgrammeRequest>> = dao.getAllRequests()
    
    suspend fun getRequestById(id: String): AIProgrammeRequest? = dao.getRequestById(id)
    
    suspend fun retryGeneration(requestId: String) {
        val request = dao.getRequestById(requestId) ?: return
        
        // Reset status to processing
        dao.updateStatus(requestId, GenerationStatus.PROCESSING)
        
        // Create new work request
        val workRequest = ProgrammeGenerationWorker.createWorkRequest(requestId, request.requestPayload)
        workManager.enqueue(workRequest)
        
        // Update WorkManager ID
        dao.updateWorkManagerId(requestId, workRequest.id.toString())
    }
    
    suspend fun deleteRequest(id: String) {
        dao.delete(id)
    }
    
    suspend fun cleanupStaleRequests() {
        // Get requests older than 1 hour that are still processing
        val staleRequests = dao.getRequestsOlderThan(
            GenerationStatus.PROCESSING,
            System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
        )
        
        for (request in staleRequests) {
            if (request.workManagerId != null) {
                try {
                    // Check WorkManager status
                    val workInfo = workManager.getWorkInfoById(UUID.fromString(request.workManagerId)).get()
                    
                    if (workInfo?.state?.isFinished == true) {
                        // Work finished but DB wasn't updated
                        val errorMessage = when (workInfo.state) {
                            WorkInfo.State.FAILED -> "Generation failed"
                            WorkInfo.State.CANCELLED -> "Generation was cancelled"
                            else -> "Generation timed out"
                        }
                        dao.updateStatus(request.id, GenerationStatus.FAILED, errorMessage)
                    }
                } catch (e: Exception) {
                    // If we can't check WorkManager, mark as failed
                    dao.updateStatus(request.id, GenerationStatus.FAILED, "Generation timed out")
                }
            } else {
                // No WorkManager ID means something went wrong
                dao.updateStatus(request.id, GenerationStatus.FAILED, "Generation was not properly started")
            }
        }
    }
}