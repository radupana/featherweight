package com.github.radupana.featherweight.data.programme

import androidx.room.*
import com.github.radupana.featherweight.data.exercise.ExerciseCategory
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Programme entity represents a multi-week training program
 */
@Entity(tableName = "programmes")
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
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val startedAt: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null,
    // User's 1RM values for percentage-based programs
    val squatMax: Float? = null,
    val benchMax: Float? = null,
    val deadliftMax: Float? = null,
    val ohpMax: Float? = null,
    // Weight calculation and progression rules (stored as JSON)
    val weightCalculationRules: String? = null, // JSON serialized WeightCalculationRules
    val progressionRules: String? = null, // JSON serialized ProgressionRules
    // Template name for template-based programmes (used when custom name is given)
    val templateName: String? = null
) {
    // Helper methods to serialize/deserialize rules
    fun getWeightCalculationRulesObject(): WeightCalculationRules? {
        return weightCalculationRules?.let {
            try {
                Json.decodeFromString<WeightCalculationRules>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun getProgressionRulesObject(): ProgressionRules? {
        return progressionRules?.let {
            try {
                Json.decodeFromString<ProgressionRules>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    companion object {
        fun encodeWeightCalculationRules(rules: WeightCalculationRules): String {
            return Json.encodeToString(rules)
        }
        
        fun encodeProgressionRules(rules: ProgressionRules): String {
            return Json.encodeToString(rules)
        }
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
    val progressionRules: String? = null // JSON serialized ProgressionRules
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
    val phase: String? = null // e.g., "Foundation", "Accumulation", "Peak"
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
