package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "training_analysis")
data class TrainingAnalysis(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val analysisDate: LocalDateTime,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val overallAssessment: String,
    val keyInsightsJson: String, // Store as JSON string
    val recommendationsJson: String, // Store as JSON string
    val warningsJson: String, // Store as JSON string
    val userId: Long = 1,
) {
    companion object {
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
            } catch (e: Exception) {
                emptyList()
            }

    val recommendations: List<String>
        get() =
            try {
                gson.fromJson(
                    recommendationsJson,
                    object : TypeToken<List<String>>() {}.type,
                ) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

    val warnings: List<String>
        get() =
            try {
                gson.fromJson(
                    warningsJson,
                    object : TypeToken<List<String>>() {}.type,
                ) ?: emptyList()
            } catch (e: Exception) {
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
