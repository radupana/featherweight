package com.github.radupana.featherweight.data.programme

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.util.CloudLogger
import com.github.radupana.featherweight.util.IdGenerator
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

/**
 * Programme entity represents a multi-week training program
 */
@Entity(
    tableName = "programmes",
    indices = [
        Index("completedAt"),
        Index("status"),
        Index(value = ["isActive", "completedAt"]),
        Index(value = ["status", "completedAt"]),
        Index("userId"),
    ],
)
data class Programme(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val name: String,
    val description: String?,
    val durationWeeks: Int,
    val programmeType: ProgrammeType,
    val difficulty: ProgrammeDifficulty,
    val isCustom: Boolean = false,
    val isActive: Boolean = false,
    val status: ProgrammeStatus = ProgrammeStatus.NOT_STARTED,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val startedAt: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null,
    val completionNotes: String? = null,
    val notesCreatedAt: LocalDateTime? = null,
    // User's 1RM values for percentage-based programs
    val squatMax: Float? = null,
    val benchMax: Float? = null,
    val deadliftMax: Float? = null,
    val ohpMax: Float? = null,
    // Weight calculation and progression rules (stored as JSON)
    val weightCalculationRules: String? = null,
    val progressionRules: String? = null,
    val templateName: String? = null,
    val immutableProgrammeJson: String? = null,
) {
    // Helper methods to serialize/deserialize rules
    fun getWeightCalculationRulesObject(): WeightCalculationRules? =
        weightCalculationRules?.let {
            try {
                Json.decodeFromString<WeightCalculationRules>(it)
            } catch (e: kotlinx.serialization.SerializationException) {
                CloudLogger.warn(TAG, "Failed to parse weight calculation rules JSON", e)
                null
            }
        }

    fun getProgressionRulesObject(): ProgressionRules? =
        progressionRules?.let {
            try {
                Json.decodeFromString<ProgressionRules>(it)
            } catch (e: kotlinx.serialization.SerializationException) {
                CloudLogger.warn(TAG, "Failed to parse progression rules JSON", e)
                null
            }
        }

    fun getImmutableProgrammeSnapshot(): ImmutableProgrammeSnapshot? =
        immutableProgrammeJson?.let {
            try {
                Json.decodeFromString<ImmutableProgrammeSnapshot>(it)
            } catch (e: kotlinx.serialization.SerializationException) {
                CloudLogger.warn(TAG, "Failed to parse immutable programme snapshot JSON", e)
                null
            }
        }

    companion object {
        private const val TAG = "Programme"

        fun encodeWeightCalculationRules(rules: WeightCalculationRules): String = Json.encodeToString(rules)

        fun encodeProgressionRules(rules: ProgressionRules): String = Json.encodeToString(rules)

        fun encodeImmutableProgrammeSnapshot(snapshot: ImmutableProgrammeSnapshot): String = Json.encodeToString(snapshot)
    }

    init {
        // Validate status and isActive consistency
        require(isValidStatusAndActiveState()) {
            "Invalid programme state: status=$status, isActive=$isActive. " +
                "Only NOT_STARTED and IN_PROGRESS programmes can have isActive=true"
        }

        // Validate completedAt consistency
        require((status == ProgrammeStatus.COMPLETED) == (completedAt != null)) {
            "Invalid programme state: status=$status but completedAt=$completedAt. " +
                "COMPLETED programmes must have completedAt set, others must not"
        }

        // Validate startedAt consistency
        require((status == ProgrammeStatus.NOT_STARTED) == (startedAt == null)) {
            "Invalid programme state: status=$status but startedAt=$startedAt. " +
                "NOT_STARTED programmes must not have startedAt set"
        }
    }

    private fun isValidStatusAndActiveState(): Boolean =
        when (status) {
            ProgrammeStatus.NOT_STARTED, ProgrammeStatus.IN_PROGRESS -> true // isActive can be true or false
            ProgrammeStatus.COMPLETED, ProgrammeStatus.CANCELLED -> !isActive // isActive must be false
        }
}

/**
 * Represents a specific week within a programme
 */
@Entity(
    tableName = "programme_weeks",
    foreignKeys = [
        ForeignKey(
            entity = Programme::class,
            parentColumns = ["id"],
            childColumns = ["programmeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("programmeId"), Index("userId")],
)
data class ProgrammeWeek(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val programmeId: String,
    val weekNumber: Int,
    val name: String?,
    val description: String?,
)

/**
 * Links a workout template to a programme week
 */
@Entity(
    tableName = "programme_workouts",
    foreignKeys = [
        ForeignKey(
            entity = ProgrammeWeek::class,
            parentColumns = ["id"],
            childColumns = ["weekId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("weekId"), Index("userId")],
)
data class ProgrammeWorkout(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val weekId: String,
    val dayNumber: Int, // 1-7
    val name: String,
    val description: String?,
    val estimatedDuration: Int?, // minutes
    val workoutStructure: String, // JSON representation of exercises, sets, reps, intensities
)

/**
 * Programme progress tracking
 */
@Entity(
    tableName = "programme_progress",
    foreignKeys = [
        ForeignKey(
            entity = Programme::class,
            parentColumns = ["id"],
            childColumns = ["programmeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("programmeId", unique = true), Index("userId")],
)
data class ProgrammeProgress(
    @PrimaryKey
    val id: String = IdGenerator.generateId(),
    val userId: String? = null,
    val programmeId: String,
    val currentWeek: Int,
    val currentDay: Int,
    val completedWorkouts: Int,
    val totalWorkouts: Int,
    val lastWorkoutDate: LocalDateTime?,
)

// Enums
enum class ProgrammeStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
}

enum class ProgrammeType {
    STRENGTH,
    POWERLIFTING,
    BODYBUILDING,
    GENERAL_FITNESS,
    OLYMPIC_LIFTING,
    HYBRID,
}

enum class ProgrammeDifficulty {
    BEGINNER,
    NOVICE,
    INTERMEDIATE,
    ADVANCED,
    EXPERT,
}

// Data classes for UI representation
data class ProgrammeWithDetails(
    val programme: Programme,
    val weeks: List<ProgrammeWeekWithWorkouts>,
    val progress: ProgrammeProgress?,
    val substitutions: List<Any> = emptyList(), // Placeholder for backwards compatibility
)

data class ProgrammeWeekWithWorkouts(
    val week: ProgrammeWeek,
    val workouts: List<ProgrammeWorkout>,
)
