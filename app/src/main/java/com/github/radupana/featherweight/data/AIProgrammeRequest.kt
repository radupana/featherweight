package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "ai_programme_requests")
data class AIProgrammeRequest(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val status: GenerationStatus = GenerationStatus.PROCESSING,
    val requestPayload: String, // User selections as JSON
    val generatedProgrammeJson: String? = null,
    val errorMessage: String? = null,
    val attemptCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val workManagerId: String? = null
)

enum class GenerationStatus {
    PROCESSING, // Being processed by WorkManager
    COMPLETED,  // Successfully generated
    FAILED      // Failed after all retries
}