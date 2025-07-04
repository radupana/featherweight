package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Represents a data-driven insight about user's training progress
 */
@Entity(tableName = "progress_insights")
data class ProgressInsight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val insightType: InsightType,
    val title: String,
    val message: String,
    val data: String? = null, // JSON with supporting data
    val exerciseName: String? = null, // Specific exercise if insight is exercise-specific
    val priority: Int, // 1 = highest priority, 5 = lowest priority
    val generatedDate: LocalDateTime,
    val isRead: Boolean = false,
    val isActionable: Boolean = false, // Whether this insight suggests a specific action
    val actionType: String? = null // Type of action suggested (e.g., "deload", "increase_frequency")
)

/**
 * Types of insights the system can generate
 */
enum class InsightType {
    STRENGTH_PROGRESS,     // "Great progress on bench press this month!"
    PLATEAU_WARNING,       // "Your squat has plateaued - consider a deload"
    CONSISTENCY_PRAISE,    // "Excellent consistency this week!"
    AUTOREGULATION_FEEDBACK, // "Your RPE management looks spot on"
    VOLUME_ANALYSIS,       // "Your weekly volume is trending upward"
    TECHNIQUE_SUGGESTION,  // "Consider switching to pause bench for strength gains"
    RECOVERY_INSIGHT,      // "Your performance drops on back-to-back training days"
    PROGRAM_RECOMMENDATION // "You might benefit from switching to 5/3/1"
}

/**
 * Priority levels for insights
 */
object InsightPriority {
    const val CRITICAL = 1   // Immediate attention needed (injury risk, severe plateau)
    const val HIGH = 2       // Important for progress (plateau warning, program change)
    const val MEDIUM = 3     // Helpful feedback (good progress, technique tips)
    const val LOW = 4        // General observations (volume trends, consistency)
    const val INFO = 5       // Pure information (milestones reached)
}