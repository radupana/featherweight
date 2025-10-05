package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.IdGenerator

@Entity(
    tableName = "set_logs",
    indices = [Index(value = ["exerciseLogId"]), Index("userId")],
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
    @PrimaryKey val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val exerciseLogId: String,
    val setOrder: Int,
    // Target (what programme says to do) - nullable for freestyle workouts
    val targetReps: Int? = null,
    val targetWeight: Float? = null,
    val targetRpe: Float? = null,
    // Actual performance (what user actually did) - THE SOURCE OF TRUTH
    val actualReps: Int = 0,
    val actualWeight: Float = 0f,
    val actualRpe: Float? = null,
    // Metadata
    val tag: String? = null,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: String? = null,
)
