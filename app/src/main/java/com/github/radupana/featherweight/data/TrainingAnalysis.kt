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
    indices = [
        androidx.room.Index("userId"),
        androidx.room.Index("analysisDate"),
        androidx.room.Index("periodStart"),
        androidx.room.Index("periodEnd"),
    ],
)
data class TrainingAnalysis(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val analysisDate: LocalDateTime,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val overallAssessment: String,
    val keyInsightsJson: String,
    val recommendationsJson: String,
    val warningsJson: String,
    val adherenceAnalysisJson: String? = null,
) {
    companion object {
        private const val TAG = "TrainingAnalysis"
        private val gson = Gson()
        private val insightsType = object : TypeToken<List<TrainingInsight>>() {}.type
        private val stringsType = object : TypeToken<List<String>>() {}.type
    }

    val keyInsights: List<TrainingInsight>
        get() = parseInsights(keyInsightsJson)

    val recommendations: List<String>
        get() = parseStrings(recommendationsJson, "recommendations")

    val warnings: List<String>
        get() = parseStrings(warningsJson, "warnings")

    val adherenceAnalysis: AdherenceAnalysis?
        get() = parseAdherence(adherenceAnalysisJson)

    private fun parseInsights(json: String): List<TrainingInsight> =
        try {
            gson.fromJson(json, insightsType) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            CloudLogger.warn(TAG, "Failed to parse key insights JSON", e)
            emptyList()
        }

    private fun parseStrings(
        json: String,
        field: String,
    ): List<String> =
        try {
            gson.fromJson(json, stringsType) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            CloudLogger.warn(TAG, "Failed to parse $field JSON", e)
            emptyList()
        }

    private fun parseAdherence(json: String?): AdherenceAnalysis? =
        json?.let {
            try {
                gson.fromJson(it, AdherenceAnalysis::class.java)
            } catch (e: JsonSyntaxException) {
                CloudLogger.warn(TAG, "Failed to parse adherence analysis JSON", e)
                null
            }
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

data class AdherenceAnalysis(
    val adherenceScore: Int,
    val scoreExplanation: String,
    val positivePatterns: List<String>,
    val negativePatterns: List<String>,
    val adherenceRecommendations: List<String>,
)
