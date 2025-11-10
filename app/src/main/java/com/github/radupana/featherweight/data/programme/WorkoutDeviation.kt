package com.github.radupana.featherweight.data.programme

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.util.IdGenerator
import java.time.LocalDateTime

/**
 * Records a specific deviation from a prescribed programme workout.
 * Tracks what changed and by how much. Optional notes for user context.
 */
@Entity(
    tableName = "workout_deviations",
    indices = [
        Index(value = ["workoutId"]),
        Index(value = ["programmeId"]),
        Index(value = ["exerciseLogId"]),
        Index(value = ["deviationType"]),
        Index(value = ["timestamp"]),
        Index("userId"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class WorkoutDeviation(
    @PrimaryKey val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val workoutId: String,
    val programmeId: String,
    val exerciseLogId: String? = null,
    val deviationType: DeviationType,
    val deviationMagnitude: Float,
    val notes: String? = null,
    val timestamp: LocalDateTime,
)
