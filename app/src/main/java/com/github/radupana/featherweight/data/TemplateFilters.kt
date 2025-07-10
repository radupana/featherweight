package com.github.radupana.featherweight.data

enum class TemplateFilterCategory(val displayName: String) {
    GOAL("Goal"),
    EQUIPMENT("Equipment"),
    EXPERIENCE("Experience"),
    SPECIAL("Special"),
    FREQUENCY("Frequency"),
}

data class TemplateFilter(
    val category: TemplateFilterCategory,
    val value: String,
    val displayName: String,
)

object TemplateFilters {
    val allFilters =
        listOf(
            // Goal-based filters
            TemplateFilter(TemplateFilterCategory.GOAL, "strength", "Strength"),
            TemplateFilter(TemplateFilterCategory.GOAL, "hypertrophy", "Hypertrophy"),
            TemplateFilter(TemplateFilterCategory.GOAL, "fat_loss", "Fat Loss"),
            TemplateFilter(TemplateFilterCategory.GOAL, "athletic", "Athletic Performance"),
            // Equipment filters
            TemplateFilter(TemplateFilterCategory.EQUIPMENT, "full_gym", "Full Gym"),
            TemplateFilter(TemplateFilterCategory.EQUIPMENT, "home_gym", "Home Gym"),
            TemplateFilter(TemplateFilterCategory.EQUIPMENT, "dumbbells", "Dumbbells Only"),
            TemplateFilter(TemplateFilterCategory.EQUIPMENT, "minimal", "Minimal Equipment"),
            TemplateFilter(TemplateFilterCategory.EQUIPMENT, "bodyweight", "Bodyweight"),
            TemplateFilter(TemplateFilterCategory.EQUIPMENT, "kettlebells", "Kettlebells"),
            // Experience Level filters
            TemplateFilter(TemplateFilterCategory.EXPERIENCE, "beginner", "Beginner"),
            TemplateFilter(TemplateFilterCategory.EXPERIENCE, "intermediate", "Intermediate"),
            TemplateFilter(TemplateFilterCategory.EXPERIENCE, "advanced", "Advanced"),
            // Special filters
            TemplateFilter(TemplateFilterCategory.SPECIAL, "injury_recovery", "Injury Recovery"),
            TemplateFilter(TemplateFilterCategory.SPECIAL, "time_constrained", "Time Constrained"),
            TemplateFilter(TemplateFilterCategory.SPECIAL, "contest_prep", "Contest Prep"),
            TemplateFilter(TemplateFilterCategory.SPECIAL, "powerlifting", "Powerlifting"),
            TemplateFilter(TemplateFilterCategory.SPECIAL, "women", "Women's Training"),
            TemplateFilter(TemplateFilterCategory.SPECIAL, "senior", "Senior Fitness"),
            TemplateFilter(TemplateFilterCategory.SPECIAL, "youth", "Youth Training"),
            // Frequency filters
            TemplateFilter(TemplateFilterCategory.FREQUENCY, "2_3_days", "2-3 days/week"),
            TemplateFilter(TemplateFilterCategory.FREQUENCY, "4_5_days", "4-5 days/week"),
            TemplateFilter(TemplateFilterCategory.FREQUENCY, "6_plus_days", "6+ days/week"),
        )

    fun getFiltersByCategory(category: TemplateFilterCategory): List<TemplateFilter> {
        return allFilters.filter { it.category == category }
    }

    fun matchesFilter(
        template: ExampleTemplate,
        filter: TemplateFilter,
    ): Boolean {
        return when (filter.category) {
            TemplateFilterCategory.GOAL ->
                when (filter.value) {
                    "strength" -> template.goal == ProgrammeGoal.BUILD_STRENGTH
                    "hypertrophy" -> template.goal == ProgrammeGoal.BUILD_MUSCLE
                    "fat_loss" -> template.goal == ProgrammeGoal.LOSE_FAT
                    "athletic" -> template.goal == ProgrammeGoal.ATHLETIC_PERFORMANCE
                    else -> false
                }

            TemplateFilterCategory.EQUIPMENT -> {
                val lowerTags = template.tags.map { it.lowercase() }
                val lowerText = template.exampleText.lowercase()
                when (filter.value) {
                    "full_gym" -> lowerTags.contains("full gym") || lowerText.contains("full gym") || lowerText.contains("commercial gym")
                    "home_gym" -> lowerTags.contains("home gym") || lowerText.contains("home gym") || lowerText.contains("home with")
                    "dumbbells" -> lowerTags.contains("dumbbells") || lowerText.contains("dumbbell")
                    "minimal" -> lowerTags.contains("minimal equipment") || lowerText.contains("minimal equipment")
                    "bodyweight" -> lowerTags.contains("bodyweight") || lowerText.contains("bodyweight")
                    "kettlebells" -> lowerTags.contains("kettlebells") || lowerText.contains("kettlebell")
                    else -> false
                }
            }

            TemplateFilterCategory.EXPERIENCE -> {
                val lowerTags = template.tags.map { it.lowercase() }
                when (filter.value) {
                    "beginner" -> lowerTags.contains("beginner")
                    "intermediate" -> lowerTags.contains("intermediate")
                    "advanced" -> lowerTags.contains("advanced")
                    else -> false
                }
            }

            TemplateFilterCategory.SPECIAL -> {
                val lowerTags = template.tags.map { it.lowercase() }
                val lowerText = template.exampleText.lowercase()
                when (filter.value) {
                    "injury_recovery" -> lowerTags.contains("injury recovery") || lowerText.contains("injury")
                    "time_constrained" -> lowerTags.contains("time constrained") || lowerText.contains("busy") || template.duration == SessionDuration.QUICK
                    "contest_prep" -> lowerTags.contains("competition prep") || lowerTags.contains("contest prep") || lowerText.contains("competition")
                    "powerlifting" -> lowerTags.contains("powerlifting") || lowerText.contains("powerlifting")
                    "women" -> lowerTags.contains("women's training") || lowerTags.contains("women") || lowerText.contains("female")
                    "senior" -> lowerTags.contains("senior") || lowerText.contains("senior") || lowerText.contains("older adult")
                    "youth" -> lowerTags.contains("youth") || lowerText.contains("youth") || lowerText.contains("teen")
                    else -> false
                }
            }

            TemplateFilterCategory.FREQUENCY ->
                when (filter.value) {
                    "2_3_days" -> template.frequency in 2..3
                    "4_5_days" -> template.frequency in 4..5
                    "6_plus_days" -> template.frequency >= 6
                    else -> false
                }
        }
    }
}
