package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.util.IdGenerator
import java.time.LocalDateTime

@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(
            entity = Programme::class,
            parentColumns = ["id"],
            childColumns = ["programmeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("programmeId"),
        Index(value = ["status", "date"]),
        Index(value = ["isProgrammeWorkout", "status", "date"]),
        Index("userId"),
    ],
)
data class Workout(
    @PrimaryKey val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val date: LocalDateTime,
    val name: String? = null,
    val notes: String? = null,
    val notesUpdatedAt: LocalDateTime? = null,
    val programmeId: String? = null,
    val weekNumber: Int? = null,
    val dayNumber: Int? = null,
    val programmeWorkoutName: String? = null,
    val isProgrammeWorkout: Boolean = false,
    val status: WorkoutStatus = WorkoutStatus.NOT_STARTED,
    val durationSeconds: String? = null,
    val timerStartTime: LocalDateTime? = null,
    val timerElapsedSeconds: Int = 0,
    val isTemplate: Boolean = false,
    val fromTemplateId: String? = null,
)
