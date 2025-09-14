package com.github.radupana.featherweight.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.data.programme.Programme
import java.time.LocalDateTime

@Entity(
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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String? = null,
    val date: LocalDateTime,
    val name: String? = null,
    val notes: String? = null,
    val notesUpdatedAt: LocalDateTime? = null,
    val programmeId: Long? = null,
    val weekNumber: Int? = null,
    val dayNumber: Int? = null,
    val programmeWorkoutName: String? = null,
    val isProgrammeWorkout: Boolean = false,
    val status: WorkoutStatus = WorkoutStatus.NOT_STARTED,
    val durationSeconds: Long? = null,
    val timerStartTime: LocalDateTime? = null,
    val timerElapsedSeconds: Int = 0,
    val isTemplate: Boolean = false,
    val fromTemplateId: Long? = null,
)
