package com.github.radupana.featherweight.data.programme

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class WorkoutDeviationTest {
    @Test
    fun workoutDeviation_creation_withAllFields_createsCorrectly() {
        val timestamp = LocalDateTime.of(2024, 1, 15, 10, 30)

        val deviation =
            WorkoutDeviation(
                id = "1",
                userId = "user123",
                workoutId = "workout1",
                programmeId = "programme1",
                exerciseLogId = "exerciseLog1",
                deviationType = DeviationType.EXERCISE_SWAP,
                deviationMagnitude = 0.25f,
                notes = "Barbell not available, used dumbbells",
                timestamp = timestamp,
            )

        assertThat(deviation.id).isEqualTo("1")
        assertThat(deviation.userId).isEqualTo("user123")
        assertThat(deviation.workoutId).isEqualTo("workout1")
        assertThat(deviation.programmeId).isEqualTo("programme1")
        assertThat(deviation.exerciseLogId).isEqualTo("exerciseLog1")
        assertThat(deviation.deviationType).isEqualTo(DeviationType.EXERCISE_SWAP)
        assertThat(deviation.deviationMagnitude).isEqualTo(0.25f)
        assertThat(deviation.notes).isEqualTo("Barbell not available, used dumbbells")
        assertThat(deviation.timestamp).isEqualTo(timestamp)
    }

    @Test
    fun workoutDeviation_creation_withMinimalFields_usesDefaults() {
        val timestamp = LocalDateTime.now()

        val deviation =
            WorkoutDeviation(
                workoutId = "workout2",
                programmeId = "programme2",
                deviationType = DeviationType.VOLUME_DEVIATION,
                deviationMagnitude = -0.15f,
                timestamp = timestamp,
            )

        assertThat(deviation.id).isNotEmpty()
        assertThat(deviation.userId).isNull()
        assertThat(deviation.workoutId).isEqualTo("workout2")
        assertThat(deviation.programmeId).isEqualTo("programme2")
        assertThat(deviation.exerciseLogId).isNull()
        assertThat(deviation.deviationType).isEqualTo(DeviationType.VOLUME_DEVIATION)
        assertThat(deviation.deviationMagnitude).isEqualTo(-0.15f)
        assertThat(deviation.notes).isNull()
        assertThat(deviation.timestamp).isEqualTo(timestamp)
    }

    @Test
    fun workoutDeviation_exerciseSwap_withContext() {
        val timestamp = LocalDateTime.now()

        val deviation =
            WorkoutDeviation(
                workoutId = "workout3",
                programmeId = "programme3",
                exerciseLogId = "exerciseLog3",
                deviationType = DeviationType.EXERCISE_SWAP,
                deviationMagnitude = 1.0f,
                notes = "Switched to incline press due to shoulder discomfort",
                timestamp = timestamp,
            )

        assertThat(deviation.deviationType).isEqualTo(DeviationType.EXERCISE_SWAP)
        assertThat(deviation.notes).contains("shoulder discomfort")
        assertThat(deviation.deviationMagnitude).isEqualTo(1.0f)
    }

    @Test
    fun workoutDeviation_volumeReduction_dueToFatigue() {
        val timestamp = LocalDateTime.now()

        val deviation =
            WorkoutDeviation(
                workoutId = "workout4",
                programmeId = "programme4",
                exerciseLogId = "exerciseLog4",
                deviationType = DeviationType.VOLUME_DEVIATION,
                deviationMagnitude = -0.3f,
                notes = "Feeling fatigued, reduced volume by 30%",
                timestamp = timestamp,
            )

        assertThat(deviation.deviationType).isEqualTo(DeviationType.VOLUME_DEVIATION)
        assertThat(deviation.deviationMagnitude).isEqualTo(-0.3f)
        assertThat(deviation.notes).contains("fatigued")
    }

    @Test
    fun workoutDeviation_intensityIncrease_intentional() {
        val timestamp = LocalDateTime.now()

        val deviation =
            WorkoutDeviation(
                workoutId = "workout5",
                programmeId = "programme5",
                exerciseLogId = "exerciseLog5",
                deviationType = DeviationType.INTENSITY_DEVIATION,
                deviationMagnitude = 0.1f,
                notes = "Felt strong, increased weight by 10%",
                timestamp = timestamp,
            )

        assertThat(deviation.deviationType).isEqualTo(DeviationType.INTENSITY_DEVIATION)
        assertThat(deviation.deviationMagnitude).isEqualTo(0.1f)
        assertThat(deviation.notes).contains("Felt strong")
    }

    @Test
    fun workoutDeviation_scheduleDeviation_gymBusy() {
        val timestamp = LocalDateTime.now()

        val deviation =
            WorkoutDeviation(
                workoutId = "workout6",
                programmeId = "programme6",
                deviationType = DeviationType.SCHEDULE_DEVIATION,
                deviationMagnitude = 1.0f,
                notes = "Gym too busy, moved workout to next day",
                timestamp = timestamp,
            )

        assertThat(deviation.deviationType).isEqualTo(DeviationType.SCHEDULE_DEVIATION)
        assertThat(deviation.notes).contains("Gym too busy")
        assertThat(deviation.exerciseLogId).isNull()
    }

    @Test
    fun workoutDeviation_rpeDeviation_autoRegulation() {
        val timestamp = LocalDateTime.now()

        val deviation =
            WorkoutDeviation(
                workoutId = "workout7",
                programmeId = "programme7",
                exerciseLogId = "exerciseLog7",
                deviationType = DeviationType.RPE_DEVIATION,
                deviationMagnitude = 0.15f,
                notes = "Auto-regulated intensity based on feel",
                timestamp = timestamp,
            )

        assertThat(deviation.deviationType).isEqualTo(DeviationType.RPE_DEVIATION)
        assertThat(deviation.notes).contains("Auto-regulated")
        assertThat(deviation.deviationMagnitude).isEqualTo(0.15f)
    }
}
