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

data class ExampleTemplate(
    val title: String,
    val goal: ProgrammeGoal,
    val frequency: Int,
    val duration: SessionDuration,
    val exampleText: String,
    val tags: List<String>
)

data class GuidedInputState(
    val selectedGoal: ProgrammeGoal? = null,
    val selectedFrequency: Int? = null,
    val selectedDuration: SessionDuration? = null,
    val inputText: String = "",
    val detectedElements: Set<DetectedElement> = emptySet(),
    val inputCompleteness: Float = 0f,
    val showExamples: Boolean = false,
    val availableChips: List<QuickAddChip> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val generationCount: Int = 0,
    val maxDailyGenerations: Int = 5
)