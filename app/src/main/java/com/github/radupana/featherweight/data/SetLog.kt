package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ExerciseLog::class,
            parentColumns = ["id"],
            childColumns = ["exerciseLogId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseLogId: Long,
    val setOrder: Int,
    
    // Legacy fields - maintain original order and defaults for compatibility
    val reps: Int,
    val weight: Float,
    val rpe: Float? = null,
    
    // Target (from programme or user intent) - new fields with defaults
    val targetReps: Int = reps, // Default to same as actual
    val targetWeight: Float? = if (weight > 0) weight else null, // Default to same as actual
    
    // Intelligent suggestions with explanations
    val suggestedWeight: Float? = null,
    val suggestedReps: Int? = null,
    val suggestionSource: String? = null, // JSON with sources
    val suggestionConfidence: Float? = null, // 0.0-1.0
    val calculationDetails: String? = null, // JSON explaining the math
    
    // Actual performance (what user actually did) - new fields with defaults
    val actualReps: Int = reps, // Default to same as legacy
    val actualWeight: Float = weight, // Default to same as legacy
    val actualRpe: Float? = rpe, // Default to same as legacy
    val tag: String? = null,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: String? = null,
)
