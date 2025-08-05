package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.DetectedElement
import com.github.radupana.featherweight.data.ProgrammeGoal

class InputAnalyzer {
    companion object {
        private val EXPERIENCE_PATTERNS =
            listOf(
                "beginner",
                "new to",
                "just started",
                "first time",
                "never lifted",
                "intermediate",
                "2 years",
                "3 years",
                "few years",
                "some experience",
                "advanced",
                "experienced",
                "5+ years",
                "powerlifter",
                "bodybuilder",
            )

        private val LIFT_PATTERNS =
            listOf(
                "squat",
                "bench",
                "deadlift",
                "press",
                "max",
                "1rm",
                "pr",
                "current",
            )

        private val EQUIPMENT_PATTERNS =
            listOf(
                "home gym",
                "garage",
                "dumbbells",
                "barbell",
                "plates",
                "rack",
                "commercial gym",
                "planet fitness",
                "24 hour",
                "full gym",
            )

        private val INJURY_PATTERNS =
            listOf(
                "injury",
                "injured",
                "hurt",
                "pain",
                "rehab",
                "physical therapy",
                "avoid",
                "can't do",
                "bad knee",
                "shoulder issue",
                "back problem",
            )

        private val SCHEDULE_PATTERNS =
            listOf(
                "days",
                "times per week",
                "schedule",
                "busy",
                "limited time",
                "morning",
                "evening",
                "weekend",
                "weekday",
            )

        private val GOAL_KEYWORDS =
            mapOf(
                ProgrammeGoal.BUILD_STRENGTH to listOf("strength", "strong", "powerlifting", "1rm", "max", "heavy"),
                ProgrammeGoal.BUILD_MUSCLE to listOf("muscle", "mass", "size", "hypertrophy", "bodybuilding", "bulk"),
                ProgrammeGoal.LOSE_FAT to listOf("fat loss", "weight loss", "cut", "lean", "shred", "definition"),
                ProgrammeGoal.ATHLETIC_PERFORMANCE to listOf("athletic", "sport", "performance", "explosive", "agility", "speed"),
            )
    }

    fun analyzeInput(
        text: String,
        selectedGoal: ProgrammeGoal?,
    ): Set<DetectedElement> {
        val lowercaseText = text.lowercase()
        val detected = mutableSetOf<DetectedElement>()

        // Check for goal (either selected or mentioned in text)
        if (selectedGoal != null) {
            detected.add(DetectedElement.GOAL)
        } else {
            GOAL_KEYWORDS.forEach { (_, keywords) ->
                if (keywords.any { lowercaseText.contains(it) }) {
                    detected.add(DetectedElement.GOAL)
                    return@forEach
                }
            }
        }

        // Check for experience level
        if (EXPERIENCE_PATTERNS.any { lowercaseText.contains(it) }) {
            detected.add(DetectedElement.EXPERIENCE_LEVEL)
        }

        // Check for current lifts
        if (LIFT_PATTERNS.any { lowercaseText.contains(it) }) {
            detected.add(DetectedElement.CURRENT_LIFTS)
        }

        // Check for equipment mentions
        if (EQUIPMENT_PATTERNS.any { lowercaseText.contains(it) }) {
            detected.add(DetectedElement.EQUIPMENT)
        }

        // Check for injury/limitation mentions
        if (INJURY_PATTERNS.any { lowercaseText.contains(it) }) {
            detected.add(DetectedElement.INJURIES)
        }

        // Check for schedule mentions
        if (SCHEDULE_PATTERNS.any { lowercaseText.contains(it) }) {
            detected.add(DetectedElement.SCHEDULE)
        }

        // General preferences check
        if (lowercaseText.contains("prefer") ||
            lowercaseText.contains("like") ||
            lowercaseText.contains("focus") ||
            lowercaseText.contains("emphasize")
        ) {
            detected.add(DetectedElement.PREFERENCES)
        }

        return detected
    }

    fun calculateCompleteness(
        detectedElements: Set<DetectedElement>,
        hasGoal: Boolean,
        hasFrequency: Boolean,
        textLength: Int,
    ): Float {
        var score = 0f
        val maxScore = 10f

        // Goal selection or detection (2 points)
        if (hasGoal) score += 2f

        // Frequency selection (1 point)
        if (hasFrequency) score += 1f

        // Text length (2 points max)
        score += minOf(2f, textLength / 50f)

        // Detected elements (5 points max)
        score += minOf(5f, detectedElements.size.toFloat())

        return (score / maxScore).coerceIn(0f, 1f)
    }

    fun generateSuggestions(detectedElements: Set<DetectedElement>): List<String> {
        val suggestions = mutableListOf<String>()

        if (!detectedElements.contains(DetectedElement.EXPERIENCE_LEVEL)) {
            suggestions.add("Consider mentioning your training experience")
        }

        if (!detectedElements.contains(DetectedElement.CURRENT_LIFTS)) {
            suggestions.add("Adding current lifts helps personalization")
        }

        if (!detectedElements.contains(DetectedElement.EQUIPMENT)) {
            suggestions.add("Equipment access affects exercise selection")
        }

        if (!detectedElements.contains(DetectedElement.SCHEDULE)) {
            suggestions.add("Mention your weekly schedule constraints")
        }

        return suggestions
    }

    fun getContextualChips(
        goal: ProgrammeGoal?,
        detectedElements: Set<DetectedElement>,
        usedChips: Set<String> = emptySet(),
    ): List<com.github.radupana.featherweight.data.QuickAddChip> {
        val chips = mutableListOf<com.github.radupana.featherweight.data.QuickAddChip>()

        // Experience chips
        if (!detectedElements.contains(DetectedElement.EXPERIENCE_LEVEL)) {
            chips.addAll(
                listOf(
                    com.github.radupana.featherweight.data.QuickAddChip(
                        "+Beginner",
                        com.github.radupana.featherweight.data.ChipCategory.EXPERIENCE,
                        "I'm a beginner",
                    ),
                    com.github.radupana.featherweight.data.QuickAddChip(
                        "+Intermediate",
                        com.github.radupana.featherweight.data.ChipCategory.EXPERIENCE,
                        "I'm intermediate level",
                    ),
                    com.github.radupana.featherweight.data.QuickAddChip(
                        "+Advanced",
                        com.github.radupana.featherweight.data.ChipCategory.EXPERIENCE,
                        "I'm an advanced lifter",
                    ),
                ),
            )
        }

        // Equipment chips
        if (!detectedElements.contains(DetectedElement.EQUIPMENT)) {
            chips.addAll(
                listOf(
                    com.github.radupana.featherweight.data.QuickAddChip(
                        "+Home Gym",
                        com.github.radupana.featherweight.data.ChipCategory.EQUIPMENT,
                        "I train in a home gym",
                    ),
                    com.github.radupana.featherweight.data.QuickAddChip(
                        "+Full Gym",
                        com.github.radupana.featherweight.data.ChipCategory.EQUIPMENT,
                        "I have access to a full commercial gym",
                    ),
                    com.github.radupana.featherweight.data.QuickAddChip(
                        "+Dumbbells Only",
                        com.github.radupana.featherweight.data.ChipCategory.EQUIPMENT,
                        "I only have dumbbells available",
                    ),
                ),
            )
        }

        // Goal-specific programme types
        goal?.let { selectedGoal ->
            when (selectedGoal) {
                ProgrammeGoal.BUILD_STRENGTH -> {
                    chips.addAll(
                        listOf(
                            com.github.radupana.featherweight.data.QuickAddChip(
                                "+5/3/1",
                                com.github.radupana.featherweight.data.ChipCategory.PROGRAMME_TYPE,
                                "I like 5/3/1 style programmes",
                            ),
                            com.github.radupana.featherweight.data.QuickAddChip(
                                "+Linear",
                                com.github.radupana.featherweight.data.ChipCategory.PROGRAMME_TYPE,
                                "I prefer linear progression",
                            ),
                        ),
                    )
                }

                ProgrammeGoal.BUILD_MUSCLE -> {
                    chips.addAll(
                        listOf(
                            com.github.radupana.featherweight.data.QuickAddChip(
                                "+PPL",
                                com.github.radupana.featherweight.data.ChipCategory.PROGRAMME_TYPE,
                                "I like push/pull/legs splits",
                            ),
                            com.github.radupana.featherweight.data.QuickAddChip(
                                "+Upper/Lower",
                                com.github.radupana.featherweight.data.ChipCategory.PROGRAMME_TYPE,
                                "I prefer upper/lower splits",
                            ),
                        ),
                    )
                }

                else -> {}
            }
        }

        // Filter out used chips and limit to 6 chips to avoid UI clutter
        return chips.filterNot { usedChips.contains(it.text) }.take(6)
    }
}
