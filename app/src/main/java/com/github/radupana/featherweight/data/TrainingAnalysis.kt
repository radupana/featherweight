package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.IdGenerator
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "training_analyses",
    indices = [androidx.room.Index("userId")],
)
data class TrainingAnalysis(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val analysisDate: LocalDateTime,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val overallAssessment: String,
    val keyInsightsJson: String, // Store as JSON string
    val recommendationsJson: String, // Store as JSON string
    val warningsJson: String, // Store as JSON string
) {
    companion object {
        private const val TAG = "TrainingAnalysis"
        private val gson = Gson() // Single instance for performance
    }

    // Helper properties to convert JSON strings
    val keyInsights: List<TrainingInsight>
        get() =
            try {
                gson.fromJson(
                    keyInsightsJson,
                    object : TypeToken<List<TrainingInsight>>() {}.type,
                ) ?: emptyList()
            } catch (e: JsonSyntaxException) {
                CloudLogger.warn(TAG, "Failed to parse key insights JSON", e)
                emptyList()
            }

    val recommendations: List<String>
        get() =
            try {
                gson.fromJson(
                    recommendationsJson,
                    object : TypeToken<List<String>>() {}.type,
                ) ?: emptyList()
            } catch (e: JsonSyntaxException) {
                CloudLogger.warn(TAG, "Failed to parse recommendations JSON", e)
                emptyList()
            }

    val warnings: List<String>
        get() =
            try {
                gson.fromJson(
                    warningsJson,
                    object : TypeToken<List<String>>() {}.type,
                ) ?: emptyList()
            } catch (e: JsonSyntaxException) {
                CloudLogger.warn(TAG, "Failed to parse warnings JSON", e)
                emptyList()
            }
}

data class TrainingInsight(
    val category: InsightCategory,
    val message: String,
    val severity: InsightSeverity,
)

enum class InsightCategory {
    VOLUME,
    INTENSITY,
    FREQUENCY,
    PROGRESSION,
    RECOVERY,
    CONSISTENCY,
    BALANCE,
    TECHNIQUE,
}

enum class InsightSeverity {
    SUCCESS,
    INFO,
    WARNING,
    CRITICAL,
}
