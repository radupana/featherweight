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
    
    // Target (what programme says to do) - nullable for freestyle workouts
    val targetReps: Int = 0,
    val targetWeight: Float? = null,
    
    // Actual performance (what user actually did) - THE SOURCE OF TRUTH
    val actualReps: Int = 0,
    val actualWeight: Float = 0f,
    val actualRpe: Float? = null,
    
    // Intelligent suggestions with explanations
    val suggestedWeight: Float? = null,
    val suggestedReps: Int? = null,
    val suggestionSource: String? = null, // JSON with sources
    val suggestionConfidence: Float? = null, // 0.0-1.0
    val calculationDetails: String? = null, // JSON explaining the math
    
    // Metadata
    val tag: String? = null,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: String? = null,
)
