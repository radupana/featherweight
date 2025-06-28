package com.github.radupana.featherweight.data

import java.time.LocalDate
import java.util.*

data class GeneratedProgrammePreview(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val durationWeeks: Int,
    val daysPerWeek: Int,
    val focus: List<ProgrammeGoal>,
    val volumeLevel: VolumeLevel,
    val weeks: List<WeekPreview>,
    val validationResult: ValidationResult,
    val exerciseMatches: List<ExerciseMatchInfo>,
    val metadata: GenerationMetadata
)

data class WeekPreview(
    val weekNumber: Int,
    val workouts: List<WorkoutPreview>,
    val weeklyVolume: VolumeMetrics,
    val progressionNotes: String? = null
)

data class WorkoutPreview(
    val dayNumber: Int,
    val name: String,
    val exercises: List<ExercisePreview>,
    val estimatedDuration: Int,
    val targetRPE: Float? = null
)

data class ExercisePreview(
    val tempId: String,
    val exerciseName: String,
    val matchedExerciseId: Long? = null,
    val matchConfidence: Float,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val rpe: Float? = null,
    val restSeconds: Int,
    val notes: String? = null,
    val alternatives: List<ExerciseAlternative> = emptyList(),
    val muscleGroups: List<MuscleGroup> = emptyList(),
    val movementPattern: MovementPattern? = null
)

data class ExerciseAlternative(
    val exerciseId: Long,
    val name: String,
    val confidence: Float,
    val reason: String
)

data class ExerciseMatchInfo(
    val tempId: String,
    val originalName: String,
    val matches: List<com.github.radupana.featherweight.service.ExerciseMatch>
)

data class GenerationMetadata(
    val generatedAt: Long,
    val modelUsed: String,
    val tokensUsed: Int,
    val generationTimeMs: Long,
    val userInputSummary: String,
    val promptVersion: String = "1.0"
)

enum class VolumeLevel {
    LOW, MODERATE, HIGH, VERY_HIGH
}

enum class MuscleGroup(val displayName: String) {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    QUADS("Quadriceps"),
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    CALVES("Calves"),
    CORE("Core"),
    FOREARMS("Forearms")
}

enum class MovementPattern(val displayName: String) {
    HORIZONTAL_PUSH("Horizontal Push"),
    VERTICAL_PUSH("Vertical Push"),
    HORIZONTAL_PULL("Horizontal Pull"),
    VERTICAL_PULL("Vertical Pull"),
    SQUAT("Squat"),
    HINGE("Hip Hinge"),
    LUNGE("Lunge"),
    CARRY("Carry"),
    ROTATION("Rotation"),
    ISOLATION("Isolation")
}

data class VolumeMetrics(
    val totalSets: Int,
    val totalReps: Int,
    val muscleGroupVolume: Map<MuscleGroup, Int>,
    val movementPatternVolume: Map<MovementPattern, Int>,
    val estimatedTonnage: Float? = null
) {
    fun getVolumeLevel(): VolumeLevel {
        return when (totalSets) {
            in 0..15 -> VolumeLevel.LOW
            in 16..25 -> VolumeLevel.MODERATE
            in 26..35 -> VolumeLevel.HIGH
            else -> VolumeLevel.VERY_HIGH
        }
    }
}

// Validation models
data class ValidationResult(
    val warnings: List<ValidationWarning> = emptyList(),
    val errors: List<ValidationError> = emptyList(),
    val score: Float = 1.0f // 0.0 to 1.0
) {
    val isValid: Boolean get() = errors.isEmpty()
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
}

sealed class ValidationIssue {
    abstract val message: String
    abstract val severity: ValidationSeverity
    abstract val category: ValidationCategory
}

data class ValidationWarning(
    override val message: String,
    override val category: ValidationCategory,
    val suggestion: String? = null
) : ValidationIssue() {
    override val severity = ValidationSeverity.WARNING
}

data class ValidationError(
    override val message: String,
    override val category: ValidationCategory,
    val requiredAction: String,
    val isAutoFixable: Boolean = true
) : ValidationIssue() {
    override val severity = ValidationSeverity.ERROR
}

enum class ValidationSeverity {
    WARNING, ERROR
}

enum class ValidationCategory(val displayName: String) {
    VOLUME("Volume"),
    BALANCE("Muscle Balance"),
    PROGRESSION("Progression"),
    SAFETY("Safety"),
    DURATION("Duration"),
    EXERCISE_SELECTION("Exercise Selection"),
    RECOVERY("Recovery")
}

// Edit and regeneration models
sealed class QuickEditAction {
    data class SwapExercise(val tempId: String, val newExerciseName: String) : QuickEditAction()
    data class AdjustVolume(val factor: Float) : QuickEditAction()
    data class ShiftSchedule(val newDaysPerWeek: Int) : QuickEditAction()
    data class ChangeFocus(val newGoal: ProgrammeGoal) : QuickEditAction()
    object SimplifyForBeginner : QuickEditAction()
    object AddProgressiveOverload : QuickEditAction()
    data class UpdateExercise(
        val tempId: String,
        val sets: Int? = null,
        val repsMin: Int? = null,
        val repsMax: Int? = null,
        val rpe: Float? = null,
        val restSeconds: Int? = null
    ) : QuickEditAction()
}

enum class RegenerationMode(val displayName: String, val description: String) {
    FULL_REGENERATE("Start Over", "Regenerate the entire programme with the same input"),
    KEEP_STRUCTURE("Keep Structure", "Keep the weekly structure but change exercises"),
    ALTERNATIVE_APPROACH("Different Style", "Try a different programme approach"),
    FIX_VALIDATION_ERRORS("Fix Issues", "Address validation errors automatically"),
    MORE_VARIETY("Add Variety", "Include more exercise variations"),
    SIMPLER_VERSION("Simplify", "Create a simpler, more beginner-friendly version")
}

// Preview state
sealed class PreviewState {
    object Loading : PreviewState()
    data class Success(val preview: GeneratedProgrammePreview) : PreviewState()
    data class Activating(val preview: GeneratedProgrammePreview) : PreviewState()
    data class Error(val message: String, val canRetry: Boolean = true) : PreviewState()
}

// UI state for editing
data class ExerciseEditState(
    val tempId: String,
    val isEditing: Boolean = false,
    val showAlternatives: Boolean = false,
    val showResolution: Boolean = false
)