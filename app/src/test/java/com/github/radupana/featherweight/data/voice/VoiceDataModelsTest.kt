package com.github.radupana.featherweight.data.voice

import com.github.radupana.featherweight.model.WeightUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VoiceDataModelsTest {
    // ParsedExerciseData Tests

    @Test
    fun `ParsedExerciseData needsMapping returns true when matchedExerciseId is null`() {
        val exercise =
            ParsedExerciseData(
                spokenName = "bench press",
                interpretedName = "Bench Press",
                matchedExerciseId = null,
                matchedExerciseName = null,
                sets = emptyList(),
                confidence = 0.8f,
                notes = null,
            )

        assertThat(exercise.needsMapping).isTrue()
    }

    @Test
    fun `ParsedExerciseData needsMapping returns false when matchedExerciseId is present`() {
        val exercise =
            ParsedExerciseData(
                spokenName = "bench press",
                interpretedName = "Bench Press",
                matchedExerciseId = "exercise-123",
                matchedExerciseName = "Barbell Bench Press",
                sets = emptyList(),
                confidence = 0.95f,
                notes = null,
            )

        assertThat(exercise.needsMapping).isFalse()
    }

    @Test
    fun `ParsedExerciseData stores all fields correctly`() {
        val sets =
            listOf(
                ParsedSetData(
                    setNumber = 1,
                    reps = 10,
                    weight = 60f,
                    weightUnit = WeightUnit.KG,
                    rpe = 8f,
                    isToFailure = false,
                    notes = null,
                ),
            )

        val exercise =
            ParsedExerciseData(
                spokenName = "squats",
                interpretedName = "Squat",
                matchedExerciseId = "squat-id",
                matchedExerciseName = "Barbell Back Squat",
                sets = sets,
                confidence = 0.92f,
                notes = "Go deep",
            )

        assertThat(exercise.spokenName).isEqualTo("squats")
        assertThat(exercise.interpretedName).isEqualTo("Squat")
        assertThat(exercise.matchedExerciseId).isEqualTo("squat-id")
        assertThat(exercise.matchedExerciseName).isEqualTo("Barbell Back Squat")
        assertThat(exercise.sets).hasSize(1)
        assertThat(exercise.confidence).isEqualTo(0.92f)
        assertThat(exercise.notes).isEqualTo("Go deep")
    }

    // ParsedSetData Tests

    @Test
    fun `ParsedSetData stores all fields correctly`() {
        val set =
            ParsedSetData(
                setNumber = 3,
                reps = 5,
                weight = 100f,
                weightUnit = WeightUnit.KG,
                rpe = 9.5f,
                isToFailure = true,
                notes = "PR attempt",
            )

        assertThat(set.setNumber).isEqualTo(3)
        assertThat(set.reps).isEqualTo(5)
        assertThat(set.weight).isEqualTo(100f)
        assertThat(set.weightUnit).isEqualTo(WeightUnit.KG)
        assertThat(set.rpe).isEqualTo(9.5f)
        assertThat(set.isToFailure).isTrue()
        assertThat(set.notes).isEqualTo("PR attempt")
    }

    @Test
    fun `ParsedSetData handles null rpe and notes`() {
        val set =
            ParsedSetData(
                setNumber = 1,
                reps = 12,
                weight = 50f,
                weightUnit = WeightUnit.LBS,
                rpe = null,
                isToFailure = false,
                notes = null,
            )

        assertThat(set.rpe).isNull()
        assertThat(set.notes).isNull()
    }

    @Test
    fun `ParsedSetData supports LB weight unit`() {
        val set =
            ParsedSetData(
                setNumber = 1,
                reps = 8,
                weight = 135f,
                weightUnit = WeightUnit.LBS,
                rpe = 7f,
                isToFailure = false,
                notes = null,
            )

        assertThat(set.weightUnit).isEqualTo(WeightUnit.LBS)
    }

    // ParsedVoiceWorkoutInput Tests

    @Test
    fun `ParsedVoiceWorkoutInput stores all fields correctly`() {
        val exercises =
            listOf(
                ParsedExerciseData(
                    spokenName = "bench",
                    interpretedName = "Bench Press",
                    matchedExerciseId = "bench-id",
                    matchedExerciseName = "Barbell Bench Press",
                    sets = emptyList(),
                    confidence = 0.9f,
                    notes = null,
                ),
            )
        val warnings = listOf("Could not parse weight for set 3")

        val input =
            ParsedVoiceWorkoutInput(
                transcription = "I did bench press 3 sets of 10",
                exercises = exercises,
                confidence = 0.85f,
                warnings = warnings,
            )

        assertThat(input.transcription).isEqualTo("I did bench press 3 sets of 10")
        assertThat(input.exercises).hasSize(1)
        assertThat(input.confidence).isEqualTo(0.85f)
        assertThat(input.warnings).hasSize(1)
        assertThat(input.warnings).contains("Could not parse weight for set 3")
    }

    @Test
    fun `ParsedVoiceWorkoutInput can have multiple exercises`() {
        val exercises =
            listOf(
                ParsedExerciseData(
                    spokenName = "squat",
                    interpretedName = "Squat",
                    matchedExerciseId = "squat-id",
                    matchedExerciseName = "Barbell Back Squat",
                    sets = emptyList(),
                    confidence = 0.9f,
                    notes = null,
                ),
                ParsedExerciseData(
                    spokenName = "deadlift",
                    interpretedName = "Deadlift",
                    matchedExerciseId = "deadlift-id",
                    matchedExerciseName = "Conventional Deadlift",
                    sets = emptyList(),
                    confidence = 0.88f,
                    notes = null,
                ),
            )

        val input =
            ParsedVoiceWorkoutInput(
                transcription = "Squats and deadlifts today",
                exercises = exercises,
                confidence = 0.89f,
                warnings = emptyList(),
            )

        assertThat(input.exercises).hasSize(2)
    }

    @Test
    fun `ParsedVoiceWorkoutInput can have empty warnings`() {
        val input =
            ParsedVoiceWorkoutInput(
                transcription = "Clean transcription",
                exercises = emptyList(),
                confidence = 0.95f,
                warnings = emptyList(),
            )

        assertThat(input.warnings).isEmpty()
    }

    // VoiceInputState Tests

    @Test
    fun `VoiceInputState Idle is singleton`() {
        val idle1 = VoiceInputState.Idle
        val idle2 = VoiceInputState.Idle

        assertThat(idle1).isSameInstanceAs(idle2)
    }

    @Test
    fun `VoiceInputState Preparing is singleton`() {
        val preparing1 = VoiceInputState.Preparing
        val preparing2 = VoiceInputState.Preparing

        assertThat(preparing1).isSameInstanceAs(preparing2)
    }

    @Test
    fun `VoiceInputState Listening is singleton`() {
        val listening1 = VoiceInputState.Listening
        val listening2 = VoiceInputState.Listening

        assertThat(listening1).isSameInstanceAs(listening2)
    }

    @Test
    fun `VoiceInputState Transcribing stores partial text`() {
        val transcribing = VoiceInputState.Transcribing(partialText = "I did bench...")

        assertThat(transcribing.partialText).isEqualTo("I did bench...")
    }

    @Test
    fun `VoiceInputState Transcribing handles null partial text`() {
        val transcribing = VoiceInputState.Transcribing(partialText = null)

        assertThat(transcribing.partialText).isNull()
    }

    @Test
    fun `VoiceInputState Parsing stores transcription`() {
        val parsing = VoiceInputState.Parsing(transcription = "Full transcription here")

        assertThat(parsing.transcription).isEqualTo("Full transcription here")
    }

    @Test
    fun `VoiceInputState Ready stores result`() {
        val result =
            ParsedVoiceWorkoutInput(
                transcription = "Test",
                exercises = emptyList(),
                confidence = 0.9f,
                warnings = emptyList(),
            )
        val ready = VoiceInputState.Ready(result = result)

        assertThat(ready.result).isEqualTo(result)
        assertThat(ready.result.transcription).isEqualTo("Test")
    }

    @Test
    fun `VoiceInputState Error stores message and canRetry`() {
        val error =
            VoiceInputState.Error(
                message = "Microphone access denied",
                canRetry = true,
            )

        assertThat(error.message).isEqualTo("Microphone access denied")
        assertThat(error.canRetry).isTrue()
    }

    @Test
    fun `VoiceInputState Error with non-retryable error`() {
        val error =
            VoiceInputState.Error(
                message = "API key invalid",
                canRetry = false,
            )

        assertThat(error.canRetry).isFalse()
    }

    @Test
    fun `VoiceInputState instances are distinguishable by type`() {
        val states: List<VoiceInputState> =
            listOf(
                VoiceInputState.Idle,
                VoiceInputState.Preparing,
                VoiceInputState.Listening,
                VoiceInputState.Transcribing("partial"),
                VoiceInputState.Parsing("full"),
                VoiceInputState.Ready(
                    ParsedVoiceWorkoutInput("t", emptyList(), 0.9f, emptyList()),
                ),
                VoiceInputState.Error("error", true),
            )

        assertThat(states.filterIsInstance<VoiceInputState.Idle>()).hasSize(1)
        assertThat(states.filterIsInstance<VoiceInputState.Preparing>()).hasSize(1)
        assertThat(states.filterIsInstance<VoiceInputState.Listening>()).hasSize(1)
        assertThat(states.filterIsInstance<VoiceInputState.Transcribing>()).hasSize(1)
        assertThat(states.filterIsInstance<VoiceInputState.Parsing>()).hasSize(1)
        assertThat(states.filterIsInstance<VoiceInputState.Ready>()).hasSize(1)
        assertThat(states.filterIsInstance<VoiceInputState.Error>()).hasSize(1)
    }

    @Test
    fun `VoiceInputState can be used in when expression`() {
        fun getStateName(state: VoiceInputState): String =
            when (state) {
                is VoiceInputState.Idle -> "idle"
                is VoiceInputState.Preparing -> "preparing"
                is VoiceInputState.Listening -> "listening"
                is VoiceInputState.Transcribing -> "transcribing"
                is VoiceInputState.Parsing -> "parsing"
                is VoiceInputState.Ready -> "ready"
                is VoiceInputState.Error -> "error"
            }

        assertThat(getStateName(VoiceInputState.Idle)).isEqualTo("idle")
        assertThat(getStateName(VoiceInputState.Preparing)).isEqualTo("preparing")
        assertThat(getStateName(VoiceInputState.Listening)).isEqualTo("listening")
        assertThat(getStateName(VoiceInputState.Transcribing("x"))).isEqualTo("transcribing")
        assertThat(getStateName(VoiceInputState.Parsing("y"))).isEqualTo("parsing")
    }
}
