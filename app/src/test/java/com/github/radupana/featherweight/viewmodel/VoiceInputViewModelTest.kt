package com.github.radupana.featherweight.viewmodel

import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseWithAliases
import com.github.radupana.featherweight.data.voice.ParsedExerciseData
import com.github.radupana.featherweight.data.voice.ParsedSetData
import com.github.radupana.featherweight.data.voice.ParsedVoiceWorkoutInput
import com.github.radupana.featherweight.data.voice.VoiceInputState
import com.github.radupana.featherweight.model.WeightUnit
import com.github.radupana.featherweight.service.ExerciseMatchResult
import com.github.radupana.featherweight.service.ExerciseMatchSuggestions
import com.github.radupana.featherweight.service.VoiceExerciseMatchingService
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VoiceInputViewModelTest {
    @Test
    fun `ConfirmableExercise holds parsed data and match suggestions`() {
        val parsedData = createTestParsedExercise("bench press", "Barbell Bench Press")
        val matchSuggestions =
            ExerciseMatchSuggestions(
                bestMatch =
                    ExerciseMatchResult(
                        exerciseId = "barbell-bench-press",
                        exerciseName = "Barbell Bench Press",
                        score = 1000,
                        isAutoMatch = true,
                    ),
                suggestions =
                    listOf(
                        ExerciseMatchResult(
                            exerciseId = "barbell-bench-press",
                            exerciseName = "Barbell Bench Press",
                            score = 1000,
                            isAutoMatch = true,
                        ),
                    ),
            )

        val confirmable =
            ConfirmableExercise(
                parsedData = parsedData,
                matchSuggestions = matchSuggestions,
                selectedExerciseId = "barbell-bench-press",
                selectedExerciseName = "Barbell Bench Press",
                isConfirmed = true,
            )

        assertThat(confirmable.parsedData.spokenName).isEqualTo("bench press")
        assertThat(confirmable.selectedExerciseId).isEqualTo("barbell-bench-press")
        assertThat(confirmable.isConfirmed).isTrue()
    }

    @Test
    fun `ConfirmableExercise can have null selectedExercise when no match`() {
        val parsedData = createTestParsedExercise("unknown exercise", "Unknown")
        val matchSuggestions =
            ExerciseMatchSuggestions(
                bestMatch = null,
                suggestions = emptyList(),
            )

        val confirmable =
            ConfirmableExercise(
                parsedData = parsedData,
                matchSuggestions = matchSuggestions,
                selectedExerciseId = null,
                selectedExerciseName = null,
                isConfirmed = false,
            )

        assertThat(confirmable.selectedExerciseId).isNull()
        assertThat(confirmable.isConfirmed).isFalse()
    }

    @Test
    fun `VoiceInputState Idle is default state`() {
        val state: VoiceInputState = VoiceInputState.Idle
        assertThat(state).isInstanceOf(VoiceInputState.Idle::class.java)
    }

    @Test
    fun `VoiceInputState Listening represents recording state`() {
        val state: VoiceInputState = VoiceInputState.Listening
        assertThat(state).isInstanceOf(VoiceInputState.Listening::class.java)
    }

    @Test
    fun `VoiceInputState Transcribing holds partial text`() {
        val state = VoiceInputState.Transcribing(partialText = "bench press...")
        assertThat(state.partialText).isEqualTo("bench press...")
    }

    @Test
    fun `VoiceInputState Parsing holds transcription`() {
        val state = VoiceInputState.Parsing(transcription = "bench press 3x8 at 100kg")
        assertThat(state.transcription).isEqualTo("bench press 3x8 at 100kg")
    }

    @Test
    fun `VoiceInputState Ready holds parsed result`() {
        val parsed = createTestParsedInput()
        val state = VoiceInputState.Ready(result = parsed)
        assertThat(state.result.exercises).hasSize(1)
    }

    @Test
    fun `VoiceInputState Error holds message and retry flag`() {
        val state =
            VoiceInputState.Error(
                message = "Network error",
                canRetry = true,
            )
        assertThat(state.message).isEqualTo("Network error")
        assertThat(state.canRetry).isTrue()
    }

    @Test
    fun `VoiceExerciseMatchingService finds match for exact exercise name`() {
        val service = VoiceExerciseMatchingService()
        val exercises = createTestExercises()

        val result =
            service.findMatchesForExercise(
                spokenName = "bench press",
                interpretedName = "Barbell Bench Press",
                allExercises = exercises,
            )

        assertThat(result.bestMatch).isNotNull()
        assertThat(result.bestMatch?.exerciseId).isEqualTo("barbell-bench-press")
    }

    @Test
    fun `VoiceExerciseMatchingService returns suggestions when no exact match`() {
        val service = VoiceExerciseMatchingService()
        val exercises = createTestExercises()

        val result =
            service.findMatchesForExercise(
                spokenName = "press",
                interpretedName = "Press",
                allExercises = exercises,
            )

        assertThat(result.suggestions).isNotEmpty()
    }

    @Test
    fun `ParsedSetData holds set information correctly`() {
        val setData =
            ParsedSetData(
                setNumber = 1,
                reps = 8,
                weight = 100f,
                weightUnit = WeightUnit.KG,
                rpe = 8.0f,
                isToFailure = false,
                notes = null,
            )

        assertThat(setData.setNumber).isEqualTo(1)
        assertThat(setData.reps).isEqualTo(8)
        assertThat(setData.weight).isEqualTo(100f)
        assertThat(setData.weightUnit).isEqualTo(WeightUnit.KG)
        assertThat(setData.rpe).isEqualTo(8.0f)
    }

    @Test
    fun `ParsedExerciseData needsMapping returns true when no match`() {
        val exerciseData =
            ParsedExerciseData(
                spokenName = "unknown",
                interpretedName = "Unknown",
                matchedExerciseId = null,
                matchedExerciseName = null,
                sets = emptyList(),
                confidence = 0.5f,
                notes = null,
            )

        assertThat(exerciseData.needsMapping).isTrue()
    }

    @Test
    fun `ParsedExerciseData needsMapping returns false when matched`() {
        val exerciseData =
            ParsedExerciseData(
                spokenName = "bench",
                interpretedName = "Barbell Bench Press",
                matchedExerciseId = "barbell-bench-press",
                matchedExerciseName = "Barbell Bench Press",
                sets = emptyList(),
                confidence = 0.95f,
                notes = null,
            )

        assertThat(exerciseData.needsMapping).isFalse()
    }

    @Test
    fun `ParsedVoiceWorkoutInput holds multiple exercises`() {
        val exercises =
            listOf(
                createTestParsedExercise("bench", "Barbell Bench Press"),
                createTestParsedExercise("curls", "Dumbbell Bicep Curl"),
            )

        val input =
            ParsedVoiceWorkoutInput(
                transcription = "bench 3x8 at 100, curls 3x12 at 25",
                exercises = exercises,
                confidence = 0.9f,
                warnings = emptyList(),
            )

        assertThat(input.exercises).hasSize(2)
        assertThat(input.confidence).isEqualTo(0.9f)
    }

    @Test
    fun `ParsedVoiceWorkoutInput can have warnings`() {
        val input =
            ParsedVoiceWorkoutInput(
                transcription = "some exercise",
                exercises = emptyList(),
                confidence = 0.3f,
                warnings = listOf("Could not identify exercise", "Weight unit assumed"),
            )

        assertThat(input.warnings).hasSize(2)
        assertThat(input.warnings).contains("Could not identify exercise")
    }

    private fun createTestParsedExercise(
        spokenName: String,
        interpretedName: String,
    ): ParsedExerciseData =
        ParsedExerciseData(
            spokenName = spokenName,
            interpretedName = interpretedName,
            matchedExerciseId = null,
            matchedExerciseName = null,
            sets =
                listOf(
                    ParsedSetData(
                        setNumber = 1,
                        reps = 8,
                        weight = 100f,
                        weightUnit = WeightUnit.KG,
                        rpe = null,
                        isToFailure = false,
                        notes = null,
                    ),
                ),
            confidence = 0.95f,
            notes = null,
        )

    private fun createTestParsedInput(): ParsedVoiceWorkoutInput =
        ParsedVoiceWorkoutInput(
            transcription = "bench press 3x8 at 100kg",
            exercises = listOf(createTestParsedExercise("bench press", "Barbell Bench Press")),
            confidence = 0.95f,
            warnings = emptyList(),
        )

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
                aliases = listOf("bicep curl", "curls"),
            ),
        )
}
