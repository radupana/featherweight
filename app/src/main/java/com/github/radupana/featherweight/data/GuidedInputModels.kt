package com.github.radupana.featherweight.data

enum class ProgrammeGoal(val displayName: String, val emoji: String) {
    BUILD_STRENGTH("Build Strength", "💪"),
    BUILD_MUSCLE("Build Muscle", "🏋️"),
    LOSE_FAT("Lose Fat", "🔥"),
    ATHLETIC_PERFORMANCE("Athletic Performance", "⚡"),
    CUSTOM("Custom", "🎯")
}

enum class SessionDuration(val displayName: String, val minutesRange: String) {
    QUICK("Quick", "30-45 min"),
    STANDARD("Standard", "45-60 min"),
    EXTENDED("Extended", "60-90 min"),
    LONG("Long", "90+ min")
}

enum class ExperienceLevel(val displayName: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
    ELITE("Elite")
}

enum class EquipmentAvailability(val displayName: String) {
    BARBELL_AND_RACK("Barbell & Rack"),
    FULL_GYM("Full Commercial Gym"),
    DUMBBELLS_ONLY("Dumbbells Only"),
    BODYWEIGHT("Bodyweight Only"),
    LIMITED("Limited Equipment")
}

// GenerationMode removed - only using simplified approach

enum class TrainingFrequency(val displayName: String, val daysPerWeek: Int) {
    TWO_DAYS("2x per week", 2),
    THREE_DAYS("3x per week", 3),
    FOUR_DAYS("4x per week", 4),
    FIVE_DAYS("5x per week", 5),
    SIX_DAYS("6x per week", 6)
}

enum class DetectedElement {
    GOAL,
    EXPERIENCE_LEVEL,
    CURRENT_LIFTS,
    SCHEDULE,
    EQUIPMENT,
    INJURIES,
    PREFERENCES
}

data class QuickAddChip(
    val text: String,
    val category: ChipCategory,
    val appendText: String
)

enum class ChipCategory {
    EXPERIENCE,
    EQUIPMENT,
    PROGRAMME_TYPE,
    TRAINING_STYLE
}

enum class WizardStep {
    QUICK_SETUP,    // Goal, Frequency, Duration
    ABOUT_YOU,      // Experience, Equipment
    CUSTOMIZE       // Custom Instructions
}

data class ExampleTemplate(
    val title: String,
    val goal: ProgrammeGoal,
    val frequency: Int,
    val duration: SessionDuration,
    val exampleText: String,
    val tags: List<String>
)

data class GuidedInputState(
    val currentStep: WizardStep = WizardStep.QUICK_SETUP,
    val selectedGoal: ProgrammeGoal? = null,
    val selectedFrequency: TrainingFrequency? = null,
    val selectedDuration: SessionDuration? = null,
    val selectedExperience: ExperienceLevel? = null,
    val selectedEquipment: EquipmentAvailability? = null,
    val customInstructions: String = "",
    val isLoading: Boolean = false,
    val loadingMessage: String = "Preparing your personalized programme...",
    val errorMessage: String? = null
) {
    fun isStepComplete(step: WizardStep): Boolean {
        return when (step) {
            WizardStep.QUICK_SETUP -> selectedGoal != null && selectedFrequency != null && selectedDuration != null
            WizardStep.ABOUT_YOU -> selectedExperience != null && selectedEquipment != null
            WizardStep.CUSTOMIZE -> true
        }
    }
    
    fun canGenerate(): Boolean {
        return selectedGoal != null && selectedFrequency != null && selectedDuration != null && 
               selectedExperience != null && selectedEquipment != null
    }
}