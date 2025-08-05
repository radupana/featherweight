package com.github.radupana.featherweight.data.programme

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
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
    ],
)
data class Programme(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
    val weightCalculationRules: String? = null, // JSON serialized WeightCalculationRules
    val progressionRules: String? = null, // JSON serialized ProgressionRules
    // Template name for template-based programmes (used when custom name is given)
    val templateName: String? = null,
) {
    // Helper methods to serialize/deserialize rules
    fun getWeightCalculationRulesObject(): WeightCalculationRules? =
        weightCalculationRules?.let {
            try {
                Json.decodeFromString<WeightCalculationRules>(it)
            } catch (e: Exception) {
                null
            }
        }

    fun getProgressionRulesObject(): ProgressionRules? =
        progressionRules?.let {
            try {
                Json.decodeFromString<ProgressionRules>(it)
            } catch (e: Exception) {
                null
            }
        }

    companion object {
        fun encodeWeightCalculationRules(rules: WeightCalculationRules): String = Json.encodeToString(rules)

        fun encodeProgressionRules(rules: ProgressionRules): String = Json.encodeToString(rules)
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
 * Programme template for pre-defined programs
 */
@Entity(tableName = "programme_templates")
data class ProgrammeTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val durationWeeks: Int,
    val programmeType: ProgrammeType,
    val difficulty: ProgrammeDifficulty,
    val author: String,
    val requiresMaxes: Boolean = false,
    val allowsAccessoryCustomization: Boolean = false,
    val jsonStructure: String, // JSON representation of the programme structure
    val weightCalculationRules: String? = null, // JSON serialized WeightCalculationRules
    val progressionRules: String? = null, // JSON serialized ProgressionRules
)

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
    indices = [Index("programmeId")],
)
data class ProgrammeWeek(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val programmeId: Long,
    val weekNumber: Int,
    val name: String?,
    val description: String?,
    val focusAreas: String?, // JSON array of muscle groups or movement patterns
    val intensityLevel: String? = null, // "low", "moderate", "high", "very_high"
    val volumeLevel: String? = null, // "low", "moderate", "high", "very_high"
    val isDeload: Boolean = false,
    val phase: String? = null, // e.g., "Foundation", "Accumulation", "Peak"
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
    indices = [Index("weekId")],
)
data class ProgrammeWorkout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weekId: Long,
    val dayNumber: Int, // 1-7
    val name: String,
    val description: String?,
    val estimatedDuration: Int?, // minutes
    val workoutStructure: String, // JSON representation of exercises, sets, reps, intensities
)

/**
 * Exercise substitution rules for flexibility
 */
@Entity(
    tableName = "exercise_substitutions",
    foreignKeys = [
        ForeignKey(
            entity = Programme::class,
            parentColumns = ["id"],
            childColumns = ["programmeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("programmeId")],
)
data class ExerciseSubstitution(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val programmeId: Long,
    val originalExerciseName: String,
    val substitutionCategory: ExerciseCategory,
    val substitutionCriteria: String?, // JSON with equipment, movement pattern requirements
    val isUserDefined: Boolean = false,
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
    indices = [Index("programmeId", unique = true)],
)
data class ProgrammeProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val programmeId: Long,
    val currentWeek: Int,
    val currentDay: Int,
    val completedWorkouts: Int,
    val totalWorkouts: Int,
    val lastWorkoutDate: LocalDateTime?,
    val adherencePercentage: Float,
    val strengthProgress: String?, // JSON tracking of lift improvements
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
    val substitutions: List<ExerciseSubstitution>,
)

data class ProgrammeWeekWithWorkouts(
    val week: ProgrammeWeek,
    val workouts: List<ProgrammeWorkout>,
)
