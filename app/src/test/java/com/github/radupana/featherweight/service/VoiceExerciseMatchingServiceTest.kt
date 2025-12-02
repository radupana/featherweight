package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseWithAliases
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class VoiceExerciseMatchingServiceTest {
    private lateinit var service: VoiceExerciseMatchingService
    private lateinit var testExercises: List<ExerciseWithAliases>

    @Before
    fun setup() {
        service = VoiceExerciseMatchingService()
        testExercises = createTestExercises()
    }

    private fun createTestExercises(): List<ExerciseWithAliases> =
        listOf(
            ExerciseWithAliases(
                exercise =
                    Exercise(
                        id = "barbell-bench-press",
                        name = "Barbell Bench Press",
                        equipment = "Barbell",
                        category = "Compound",
                        movementPattern = "Horizontal Push",
                    ),
                aliases = listOf("bench press", "bench", "flat bench"),
            ),
            ExerciseWithAliases(
                exercise =
                    Exercise(
                        id = "dumbbell-bicep-curl",
                        name = "Dumbbell Bicep Curl",
                        equipment = "Dumbbell",
                        category = "Isolation",
                        movementPattern = "Curl",
                    ),
                aliases = listOf("bicep curl", "curls", "dumbbell curl"),
            ),
            ExerciseWithAliases(
                exercise =
                    Exercise(
                        id = "barbell-back-squat",
                        name = "Barbell Back Squat",
                        equipment = "Barbell",
                        category = "Compound",
                        movementPattern = "Squat",
                    ),
                aliases = listOf("squat", "squats", "back squat"),
            ),
            ExerciseWithAliases(
                exercise =
                    Exercise(
                        id = "barbell-deadlift",
                        name = "Barbell Deadlift",
                        equipment = "Barbell",
                        category = "Compound",
                        movementPattern = "Hip Hinge",
                    ),
                aliases = listOf("deadlift", "deads", "conventional deadlift"),
            ),
            ExerciseWithAliases(
                exercise =
                    Exercise(
                        id = "cable-tricep-pushdown",
                        name = "Cable Tricep Pushdown",
                        equipment = "Cable",
                        category = "Isolation",
                        movementPattern = "Elbow Extension",
                    ),
                aliases = listOf("tricep pushdown", "pushdowns", "cable pushdown"),
            ),
            ExerciseWithAliases(
                exercise =
                    Exercise(
                        id = "barbell-overhead-press",
                        name = "Barbell Overhead Press",
                        equipment = "Barbell",
                        category = "Compound",
                        movementPattern = "Vertical Push",
                    ),
                aliases = listOf("overhead press", "ohp", "shoulder press", "military press"),
            ),
        )

    @Test
    fun `findMatchesForExercise returns exact match with high confidence`() {
        val result =
            service.findMatchesForExercise(
                spokenName = "bench press",
                interpretedName = "Barbell Bench Press",
                allExercises = testExercises,
            )

        assertThat(result.bestMatch).isNotNull()
        assertThat(result.bestMatch?.exerciseId).isEqualTo("barbell-bench-press")
        assertThat(result.bestMatch?.isAutoMatch).isTrue()
        assertThat(result.bestMatch?.score).isAtLeast(VoiceExerciseMatchingService.AUTO_MATCH_THRESHOLD)
    }

    @Test
    fun `findMatchesForExercise matches via alias`() {
        val result =
            service.findMatchesForExercise(
                spokenName = "curls",
                interpretedName = "Bicep Curl",
                allExercises = testExercises,
            )

        assertThat(result.bestMatch).isNotNull()
        assertThat(result.bestMatch?.exerciseId).isEqualTo("dumbbell-bicep-curl")
    }

    @Test
    fun `findMatchesForExercise returns suggestions when no exact match`() {
        val result =
            service.findMatchesForExercise(
                spokenName = "press",
                interpretedName = "Press Exercise",
                allExercises = testExercises,
            )

        assertThat(result.suggestions).isNotEmpty()
        val suggestionIds = result.suggestions.map { it.exerciseId }
        assertThat(suggestionIds).containsAnyOf("barbell-bench-press", "barbell-overhead-press")
    }

    @Test
    fun `findMatchesForExercise returns empty when no match found`() {
        val result =
            service.findMatchesForExercise(
                spokenName = "xyz unknown exercise",
                interpretedName = "XYZ Unknown Exercise",
                allExercises = testExercises,
            )

        assertThat(result.bestMatch).isNull()
        assertThat(result.suggestions).isEmpty()
    }

    @Test
    fun `findMatchesForExercise limits suggestions to MAX_SUGGESTIONS`() {
        val result =
            service.findMatchesForExercise(
                spokenName = "barbell",
                interpretedName = "Barbell Exercise",
                allExercises = testExercises,
            )

        assertThat(result.suggestions.size).isAtMost(VoiceExerciseMatchingService.MAX_SUGGESTIONS)
    }

    @Test
    fun `findMatchesForExercise uses spokenName as fallback`() {
        val result =
            service.findMatchesForExercise(
                spokenName = "squats",
                interpretedName = "Unknown Movement",
                allExercises = testExercises,
            )

        assertThat(result.bestMatch).isNotNull()
        assertThat(result.bestMatch?.exerciseId).isEqualTo("barbell-back-squat")
    }

    @Test
    fun `findMatchesForExercise handles abbreviations via spoken name`() {
        val result =
            service.findMatchesForExercise(
                spokenName = "ohp",
                interpretedName = "OHP",
                allExercises = testExercises,
            )

        assertThat(result.bestMatch).isNotNull()
        assertThat(result.bestMatch?.exerciseId).isEqualTo("barbell-overhead-press")
    }

    @Test
    fun `findMatchesForExercise suggestions are sorted by score descending`() {
        val result =
            service.findMatchesForExercise(
                spokenName = "press",
                interpretedName = "Press",
                allExercises = testExercises,
            )

        if (result.suggestions.size >= 2) {
            for (i in 0 until result.suggestions.size - 1) {
                assertThat(result.suggestions[i].score)
                    .isAtLeast(result.suggestions[i + 1].score)
            }
        }
    }

    @Test
    fun `findMatchesForExercise handles equipment-specific matching`() {
        val result =
            service.findMatchesForExercise(
                spokenName = "cable tricep pushdown",
                interpretedName = "Cable Tricep Pushdown",
                allExercises = testExercises,
            )

        assertThat(result.bestMatch).isNotNull()
        assertThat(result.bestMatch?.exerciseId).isEqualTo("cable-tricep-pushdown")
        assertThat(result.bestMatch?.isAutoMatch).isTrue()
    }

    @Test
    fun `ExerciseMatchResult contains all required fields`() {
        val result =
            ExerciseMatchResult(
                exerciseId = "test-id",
                exerciseName = "Test Exercise",
                score = 850,
                isAutoMatch = true,
            )

        assertThat(result.exerciseId).isEqualTo("test-id")
        assertThat(result.exerciseName).isEqualTo("Test Exercise")
        assertThat(result.score).isEqualTo(850)
        assertThat(result.isAutoMatch).isTrue()
    }

    @Test
    fun `ExerciseMatchSuggestions can have null bestMatch`() {
        val suggestions =
            ExerciseMatchSuggestions(
                bestMatch = null,
                suggestions = emptyList(),
            )

        assertThat(suggestions.bestMatch).isNull()
        assertThat(suggestions.suggestions).isEmpty()
    }

    @Test
    fun `VoiceExerciseMatcher interface defines findMatchesForExercise method`() {
        val matcher =
            object : VoiceExerciseMatcher {
                override fun findMatchesForExercise(
                    spokenName: String,
                    interpretedName: String,
                    allExercises: List<ExerciseWithAliases>,
                ): ExerciseMatchSuggestions = ExerciseMatchSuggestions(null, emptyList())
            }

        assertThat(matcher).isNotNull()
    }
}
