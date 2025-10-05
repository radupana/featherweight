package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class ProgrammeExerciseTrackingTest {
    @Test
    fun programmeExerciseTracking_creation_withAllFields_createsCorrectly() {
        val workoutDate = LocalDateTime.of(2024, 1, 15, 10, 30)

        val performance =
            ProgrammeExerciseTracking(
                id = "1",
                programmeId = "100",
                exerciseId = "1",
                exerciseName = "Barbell Bench Press",
                targetWeight = 100f,
                achievedWeight = 95f,
                targetSets = 5,
                completedSets = 5,
                targetReps = 5,
                achievedReps = 23,
                missedReps = 2,
                wasSuccessful = false,
                workoutDate = workoutDate,
                workoutId = "50",
                isDeloadWorkout = false,
                averageRpe = 8.5f,
            )

        assertThat(performance.id).isEqualTo("1")
        assertThat(performance.programmeId).isEqualTo("100")
        assertThat(performance.exerciseName).isEqualTo("Barbell Bench Press")
        assertThat(performance.targetWeight).isEqualTo(100f)
        assertThat(performance.achievedWeight).isEqualTo(95f)
        assertThat(performance.targetSets).isEqualTo(5)
        assertThat(performance.completedSets).isEqualTo(5)
        assertThat(performance.targetReps).isEqualTo(5)
        assertThat(performance.achievedReps).isEqualTo(23)
        assertThat(performance.missedReps).isEqualTo(2)
        assertThat(performance.wasSuccessful).isFalse()
        assertThat(performance.workoutDate).isEqualTo(workoutDate)
        assertThat(performance.workoutId).isEqualTo("50")
        assertThat(performance.isDeloadWorkout).isFalse()
        assertThat(performance.averageRpe).isEqualTo(8.5f)
    }

    @Test
    fun programmeExerciseTracking_creation_withMinimalFields_usesDefaults() {
        val workoutDate = LocalDateTime.now()

        val performance =
            ProgrammeExerciseTracking(
                programmeId = "50",
                exerciseId = "2",
                exerciseName = "Barbell Squat",
                targetWeight = 120f,
                achievedWeight = 120f,
                targetSets = 3,
                completedSets = 3,
                targetReps = null,
                achievedReps = 15,
                missedReps = 0,
                wasSuccessful = true,
                workoutDate = workoutDate,
                workoutId = "25",
            )

        assertThat(performance.id).isNotEmpty() // Auto-generated ID
        assertThat(performance.programmeId).isEqualTo("50")
        assertThat(performance.exerciseName).isEqualTo("Barbell Squat")
        assertThat(performance.targetWeight).isEqualTo(120f)
        assertThat(performance.achievedWeight).isEqualTo(120f)
        assertThat(performance.targetSets).isEqualTo(3)
        assertThat(performance.completedSets).isEqualTo(3)
        assertThat(performance.targetReps).isNull()
        assertThat(performance.achievedReps).isEqualTo(15)
        assertThat(performance.missedReps).isEqualTo(0)
        assertThat(performance.wasSuccessful).isTrue()
        assertThat(performance.workoutDate).isEqualTo(workoutDate)
        assertThat(performance.workoutId).isEqualTo("25")
        assertThat(performance.isDeloadWorkout).isFalse()
        assertThat(performance.averageRpe).isNull()
    }

    @Test
    fun programmeExerciseTracking_successfulWorkout_allTargetsMet() {
        val performance =
            ProgrammeExerciseTracking(
                programmeId = "10",
                exerciseId = "3",
                exerciseName = "Barbell Deadlift",
                targetWeight = 180f,
                achievedWeight = 180f,
                targetSets = 3,
                completedSets = 3,
                targetReps = 5,
                achievedReps = 15, // 3 sets x 5 reps
                missedReps = 0,
                wasSuccessful = true,
                workoutDate = LocalDateTime.now(),
                workoutId = "100",
            )

        assertThat(performance.wasSuccessful).isTrue()
        assertThat(performance.missedReps).isEqualTo(0)
        assertThat(performance.achievedWeight).isEqualTo(performance.targetWeight)
        assertThat(performance.completedSets).isEqualTo(performance.targetSets)
    }

    @Test
    fun programmeExerciseTracking_failedWorkout_missedReps() {
        val performance =
            ProgrammeExerciseTracking(
                programmeId = "10",
                exerciseId = "4",
                exerciseName = "Barbell Overhead Press",
                targetWeight = 60f,
                achievedWeight = 60f,
                targetSets = 5,
                completedSets = 5,
                targetReps = 5,
                achievedReps = 20, // Should have been 25 (5x5)
                missedReps = 5,
                wasSuccessful = false,
                workoutDate = LocalDateTime.now(),
                workoutId = "101",
            )

        assertThat(performance.wasSuccessful).isFalse()
        assertThat(performance.missedReps).isEqualTo(5)
        assertThat(performance.achievedReps).isLessThan(performance.targetSets * performance.targetReps!!)
    }

    @Test
    fun programmeExerciseTracking_deloadWorkout_tracksCorrectly() {
        val performance =
            ProgrammeExerciseTracking(
                programmeId = "10",
                exerciseId = "1",
                exerciseName = "Barbell Bench Press",
                targetWeight = 80f,
                achievedWeight = 80f,
                targetSets = 3,
                completedSets = 3,
                targetReps = 5,
                achievedReps = 15,
                missedReps = 0,
                wasSuccessful = true,
                workoutDate = LocalDateTime.now(),
                workoutId = "102",
                isDeloadWorkout = true,
            )

        assertThat(performance.isDeloadWorkout).isTrue()
        assertThat(performance.targetWeight).isEqualTo(80f) // Reduced weight
    }

    @Test
    fun programmeExerciseTracking_withHighRPE_indicatesDifficulty() {
        val performance =
            ProgrammeExerciseTracking(
                programmeId = "10",
                exerciseId = "5",
                exerciseName = "Barbell Row",
                targetWeight = 70f,
                achievedWeight = 70f,
                targetSets = 4,
                completedSets = 4,
                targetReps = 8,
                achievedReps = 32,
                missedReps = 0,
                wasSuccessful = true,
                workoutDate = LocalDateTime.now(),
                workoutId = "103",
                averageRpe = 9.5f,
            )

        assertThat(performance.averageRpe).isEqualTo(9.5f)
        assertThat(performance.wasSuccessful).isTrue()
        // High RPE despite success indicates near-maximal effort
    }

    @Test
    fun programmeExerciseTracking_partialCompletion_tracksIncomplete() {
        val performance =
            ProgrammeExerciseTracking(
                programmeId = "10",
                exerciseId = "6",
                exerciseName = "Dumbbell Curl",
                targetWeight = 15f,
                achievedWeight = 15f,
                targetSets = 4,
                completedSets = 2, // Only did 2 of 4 sets
                targetReps = 12,
                achievedReps = 24,
                missedReps = 0,
                wasSuccessful = false,
                workoutDate = LocalDateTime.now(),
                workoutId = "104",
            )

        assertThat(performance.completedSets).isLessThan(performance.targetSets)
        assertThat(performance.wasSuccessful).isFalse()
    }

    @Test
    fun programmeExerciseTracking_dateTime_maintainsPrecision() {
        val workoutDate = LocalDateTime.of(2024, 3, 15, 14, 30, 45, 123456789)

        val performance =
            ProgrammeExerciseTracking(
                programmeId = "10",
                exerciseId = "2",
                exerciseName = "Barbell Squat",
                targetWeight = 140f,
                achievedWeight = 140f,
                targetSets = 5,
                completedSets = 5,
                targetReps = 3,
                achievedReps = 15,
                missedReps = 0,
                wasSuccessful = true,
                workoutDate = workoutDate,
                workoutId = "105",
            )

        assertThat(performance.workoutDate).isEqualTo(workoutDate)
        assertThat(performance.workoutDate.year).isEqualTo(2024)
        assertThat(performance.workoutDate.monthValue).isEqualTo(3)
        assertThat(performance.workoutDate.dayOfMonth).isEqualTo(15)
        assertThat(performance.workoutDate.hour).isEqualTo(14)
        assertThat(performance.workoutDate.minute).isEqualTo(30)
        assertThat(performance.workoutDate.second).isEqualTo(45)
    }

    @Test
    fun programmeExerciseTracking_withVeryHighWeight_handlesCorrectly() {
        val performance =
            ProgrammeExerciseTracking(
                programmeId = "10",
                exerciseId = "2",
                exerciseName = "Barbell Squat",
                targetWeight = 500f, // Very heavy weight
                achievedWeight = 500f,
                targetSets = 1,
                completedSets = 1,
                targetReps = 1,
                achievedReps = 1,
                missedReps = 0,
                wasSuccessful = true,
                workoutDate = LocalDateTime.now(),
                workoutId = "107",
            )

        assertThat(performance.targetWeight).isEqualTo(500f)
        assertThat(performance.achievedWeight).isEqualTo(500f)
    }

    @Test
    fun programmeExerciseTracking_equality_basedOnAllFields() {
        val date = LocalDateTime.now()

        val performance1 =
            ProgrammeExerciseTracking(
                id = "1",
                programmeId = "10",
                exerciseId = "1",
                exerciseName = "Barbell Bench Press",
                targetWeight = 100f,
                achievedWeight = 100f,
                targetSets = 5,
                completedSets = 5,
                targetReps = 5,
                achievedReps = 25,
                missedReps = 0,
                wasSuccessful = true,
                workoutDate = date,
                workoutId = "50",
            )

        val performance2 =
            ProgrammeExerciseTracking(
                id = "1",
                programmeId = "10",
                exerciseId = "1",
                exerciseName = "Barbell Bench Press",
                targetWeight = 100f,
                achievedWeight = 100f,
                targetSets = 5,
                completedSets = 5,
                targetReps = 5,
                achievedReps = 25,
                missedReps = 0,
                wasSuccessful = true,
                workoutDate = date,
                workoutId = "50",
            )

        val performance3 =
            ProgrammeExerciseTracking(
                id = "2", // Different ID
                programmeId = "10",
                exerciseId = "1",
                exerciseName = "Barbell Bench Press",
                targetWeight = 100f,
                achievedWeight = 100f,
                targetSets = 5,
                completedSets = 5,
                targetReps = 5,
                achievedReps = 25,
                missedReps = 0,
                wasSuccessful = true,
                workoutDate = date,
                workoutId = "50",
            )

        assertThat(performance1).isEqualTo(performance2)
        assertThat(performance1).isNotEqualTo(performance3)
        assertThat(performance1.hashCode()).isEqualTo(performance2.hashCode())
    }

    @Test
    fun programmeExerciseTracking_toString_includesKeyInfo() {
        val performance =
            ProgrammeExerciseTracking(
                id = "1",
                programmeId = "10",
                exerciseId = "2",
                exerciseName = "Barbell Squat",
                targetWeight = 140f,
                achievedWeight = 140f,
                targetSets = 5,
                completedSets = 5,
                targetReps = 3,
                achievedReps = 15,
                missedReps = 0,
                wasSuccessful = true,
                workoutDate = LocalDateTime.of(2024, 1, 15, 10, 0),
                workoutId = "50",
            )

        val toString = performance.toString()
        assertThat(toString).contains("ProgrammeExerciseTracking")
        assertThat(toString).contains("id=1")
        assertThat(toString).contains("exerciseName=Barbell Squat")
        assertThat(toString).contains("targetWeight=140")
    }

    @Test
    fun programmeExerciseTracking_copy_createsIndependentInstance() {
        val original =
            ProgrammeExerciseTracking(
                programmeId = "10",
                exerciseId = "1",
                exerciseName = "Barbell Bench Press",
                targetWeight = 100f,
                achievedWeight = 100f,
                targetSets = 5,
                completedSets = 5,
                targetReps = 5,
                achievedReps = 25,
                missedReps = 0,
                wasSuccessful = true,
                workoutDate = LocalDateTime.now(),
                workoutId = "50",
            )

        val copy =
            original.copy(
                achievedWeight = 95f,
                achievedReps = 23,
                missedReps = 2,
                wasSuccessful = false,
            )

        assertThat(copy.achievedWeight).isEqualTo(95f)
        assertThat(copy.achievedReps).isEqualTo(23)
        assertThat(copy.missedReps).isEqualTo(2)
        assertThat(copy.wasSuccessful).isFalse()
        assertThat(copy.programmeId).isEqualTo(original.programmeId)
        assertThat(original.achievedWeight).isEqualTo(100f)
        assertThat(original.wasSuccessful).isTrue()
    }

    // Tests for ExerciseProgressionStatus data class

    @Test
    fun exerciseProgressionStatus_creation_withAllFields_createsCorrectly() {
        val lastSuccessDate = LocalDateTime.of(2024, 1, 10, 10, 0)
        val lastDeloadDate = LocalDateTime.of(2024, 1, 5, 9, 0)

        val status =
            ExerciseProgressionStatus(
                exerciseName = "Barbell Squat",
                currentWeight = 140f,
                consecutiveFailures = 2,
                lastSuccessDate = lastSuccessDate,
                lastDeloadDate = lastDeloadDate,
                totalDeloads = 1,
                isInDeloadCycle = false,
                suggestedAction = ProgressionAction.MAINTAIN,
            )

        assertThat(status.exerciseName).isEqualTo("Barbell Squat")
        assertThat(status.currentWeight).isEqualTo(140f)
        assertThat(status.consecutiveFailures).isEqualTo(2)
        assertThat(status.lastSuccessDate).isEqualTo(lastSuccessDate)
        assertThat(status.lastDeloadDate).isEqualTo(lastDeloadDate)
        assertThat(status.totalDeloads).isEqualTo(1)
        assertThat(status.isInDeloadCycle).isFalse()
        assertThat(status.suggestedAction).isEqualTo(ProgressionAction.MAINTAIN)
    }

    @Test
    fun exerciseProgressionStatus_withNoFailures_suggestsProgress() {
        val status =
            ExerciseProgressionStatus(
                exerciseName = "Barbell Bench Press",
                currentWeight = 100f,
                consecutiveFailures = 0,
                lastSuccessDate = LocalDateTime.now().minusDays(2),
                lastDeloadDate = null,
                totalDeloads = 0,
                isInDeloadCycle = false,
                suggestedAction = ProgressionAction.PROGRESS,
            )

        assertThat(status.consecutiveFailures).isEqualTo(0)
        assertThat(status.suggestedAction).isEqualTo(ProgressionAction.PROGRESS)
        assertThat(status.isInDeloadCycle).isFalse()
    }

    @Test
    fun exerciseProgressionStatus_withMultipleFailures_suggestsDeload() {
        val status =
            ExerciseProgressionStatus(
                exerciseName = "Barbell Overhead Press",
                currentWeight = 60f,
                consecutiveFailures = 3,
                lastSuccessDate = LocalDateTime.now().minusWeeks(3),
                lastDeloadDate = null,
                totalDeloads = 0,
                isInDeloadCycle = false,
                suggestedAction = ProgressionAction.DELOAD,
            )

        assertThat(status.consecutiveFailures).isEqualTo(3)
        assertThat(status.suggestedAction).isEqualTo(ProgressionAction.DELOAD)
    }

    @Test
    fun exerciseProgressionStatus_inDeloadCycle_tracksCorrectly() {
        val status =
            ExerciseProgressionStatus(
                exerciseName = "Barbell Deadlift",
                currentWeight = 160f, // Reduced from normal working weight
                consecutiveFailures = 0,
                lastSuccessDate = LocalDateTime.now(),
                lastDeloadDate = LocalDateTime.now().minusDays(7),
                totalDeloads = 2,
                isInDeloadCycle = true,
                suggestedAction = ProgressionAction.MAINTAIN,
            )

        assertThat(status.isInDeloadCycle).isTrue()
        assertThat(status.totalDeloads).isEqualTo(2)
        assertThat(status.currentWeight).isEqualTo(160f)
    }

    @Test
    fun exerciseProgressionStatus_suggestsReset_afterManyDeloads() {
        val status =
            ExerciseProgressionStatus(
                exerciseName = "Barbell Row",
                currentWeight = 50f,
                consecutiveFailures = 3,
                lastSuccessDate = LocalDateTime.now().minusMonths(2),
                lastDeloadDate = LocalDateTime.now().minusWeeks(1),
                totalDeloads = 5, // Many deloads
                isInDeloadCycle = true,
                suggestedAction = ProgressionAction.RESET,
            )

        assertThat(status.suggestedAction).isEqualTo(ProgressionAction.RESET)
        assertThat(status.totalDeloads).isEqualTo(5)
    }

    @Test
    fun exerciseProgressionStatus_suggestsTest1RM_whenAppropriate() {
        val status =
            ExerciseProgressionStatus(
                exerciseName = "Barbell Bench Press",
                currentWeight = 120f,
                consecutiveFailures = 0,
                lastSuccessDate = LocalDateTime.now(),
                lastDeloadDate = null,
                totalDeloads = 0,
                isInDeloadCycle = false,
                suggestedAction = ProgressionAction.TEST_1RM,
            )

        assertThat(status.suggestedAction).isEqualTo(ProgressionAction.TEST_1RM)
        assertThat(status.consecutiveFailures).isEqualTo(0)
    }

    // Tests for ProgressionAction enum

    @Test
    fun progressionAction_allValues_areValid() {
        val progress = ProgressionAction.PROGRESS
        val maintain = ProgressionAction.MAINTAIN
        val deload = ProgressionAction.DELOAD
        val reset = ProgressionAction.RESET
        val test1RM = ProgressionAction.TEST_1RM

        assertThat(progress).isEqualTo(ProgressionAction.PROGRESS)
        assertThat(maintain).isEqualTo(ProgressionAction.MAINTAIN)
        assertThat(deload).isEqualTo(ProgressionAction.DELOAD)
        assertThat(reset).isEqualTo(ProgressionAction.RESET)
        assertThat(test1RM).isEqualTo(ProgressionAction.TEST_1RM)

        // Verify all enum values are covered
        val allActions = ProgressionAction.entries.toTypedArray()
        assertThat(allActions).hasLength(5)
    }
}
