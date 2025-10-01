package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class WorkoutDataModelTest {
    @Test
    fun `Workout stores all required fields`() {
        val now = LocalDateTime.now()
        val workout =
            Workout(
                id = "1",
                date = now,
                name = "Upper Body Day",
                status = WorkoutStatus.COMPLETED,
                durationSeconds = "3600",
                notes = "Felt strong today",
                isProgrammeWorkout = false,
                programmeId = null,
                programmeWorkoutName = null,
                weekNumber = null,
                dayNumber = null,
            )

        assertThat(workout.id).isEqualTo("1")
        assertThat(workout.date).isEqualTo(now)
        assertThat(workout.name).isEqualTo("Upper Body Day")
        assertThat(workout.status).isEqualTo(WorkoutStatus.COMPLETED)
        assertThat(workout.durationSeconds).isEqualTo("3600")
        assertThat(workout.notes).isEqualTo("Felt strong today")
        assertThat(workout.isProgrammeWorkout).isFalse()
    }

    @Test
    fun `Workout handles programme workout fields`() {
        val workout =
            Workout(
                id = "2",
                date = LocalDateTime.now(),
                status = WorkoutStatus.IN_PROGRESS,
                isProgrammeWorkout = true,
                programmeId = "100",
                programmeWorkoutName = "5/3/1 BBB - Week 2 Day 3",
                weekNumber = 2,
                dayNumber = 3,
            )

        assertThat(workout.isProgrammeWorkout).isTrue()
        assertThat(workout.programmeId).isEqualTo("100")
        assertThat(workout.programmeWorkoutName).isEqualTo("5/3/1 BBB - Week 2 Day 3")
        assertThat(workout.weekNumber).isEqualTo(2)
        assertThat(workout.dayNumber).isEqualTo(3)
    }

    @Test
    fun `Workout handles null optional fields`() {
        val workout =
            Workout(
                id = "3",
                date = LocalDateTime.now(),
                status = WorkoutStatus.NOT_STARTED,
            )

        assertThat(workout.name).isNull()
        assertThat(workout.durationSeconds).isNull()
        assertThat(workout.notes).isNull()
        assertThat(workout.programmeId).isNull()
        assertThat(workout.weekNumber).isNull()
        assertThat(workout.dayNumber).isNull()
    }

    @Test
    fun `WorkoutStatus enum has expected values`() {
        assertThat(WorkoutStatus.entries.toTypedArray()).hasLength(3)
        assertThat(WorkoutStatus.entries.toTypedArray()).asList().contains(WorkoutStatus.NOT_STARTED)
        assertThat(WorkoutStatus.entries.toTypedArray()).asList().contains(WorkoutStatus.IN_PROGRESS)
        assertThat(WorkoutStatus.entries.toTypedArray()).asList().contains(WorkoutStatus.COMPLETED)
    }

    @Test
    fun `Workout with different status transitions`() {
        val workout =
            Workout(
                id = "4",
                date = LocalDateTime.now(),
                status = WorkoutStatus.NOT_STARTED,
            )

        assertThat(workout.status).isEqualTo(WorkoutStatus.NOT_STARTED)

        val inProgressWorkout = workout.copy(status = WorkoutStatus.IN_PROGRESS)
        assertThat(inProgressWorkout.status).isEqualTo(WorkoutStatus.IN_PROGRESS)

        val completedWorkout =
            inProgressWorkout.copy(
                status = WorkoutStatus.COMPLETED,
                durationSeconds = "4500",
            )
        assertThat(completedWorkout.status).isEqualTo(WorkoutStatus.COMPLETED)
        assertThat(completedWorkout.durationSeconds).isEqualTo("4500")
    }

    @Test
    fun `Workout tracks timer fields`() {
        val startTime = LocalDateTime.now().minusHours(1)

        val workout =
            Workout(
                id = "5",
                date = LocalDateTime.now(),
                status = WorkoutStatus.IN_PROGRESS,
                timerStartTime = startTime,
                timerElapsedSeconds = 3600,
            )

        assertThat(workout.timerStartTime).isEqualTo(startTime)
        assertThat(workout.timerElapsedSeconds).isEqualTo(3600)
    }

    @Test
    fun `SetLog stores all required fields`() {
        val setLog =
            SetLog(
                id = "1",
                exerciseLogId = "100",
                setOrder = 0,
                targetReps = 10,
                targetWeight = 60f,
                actualReps = 10,
                actualWeight = 60f,
                actualRpe = 8f,
                notes = "Good form",
                isCompleted = true,
                completedAt = "2024-01-15T10:30:00",
            )

        assertThat(setLog.id).isEqualTo("1")
        assertThat(setLog.exerciseLogId).isEqualTo("100")
        assertThat(setLog.setOrder).isEqualTo(0)
        assertThat(setLog.targetReps).isEqualTo(10)
        assertThat(setLog.targetWeight).isEqualTo(60f)
        assertThat(setLog.actualReps).isEqualTo(10)
        assertThat(setLog.actualWeight).isEqualTo(60f)
        assertThat(setLog.actualRpe).isEqualTo(8f)
        assertThat(setLog.notes).isEqualTo("Good form")
        assertThat(setLog.isCompleted).isTrue()
        assertThat(setLog.completedAt).isEqualTo("2024-01-15T10:30:00")
    }

    @Test
    fun `SetLog handles incomplete sets`() {
        val setLog =
            SetLog(
                id = "2",
                exerciseLogId = "100",
                setOrder = 1,
                targetReps = 12,
                targetWeight = 50f,
                actualReps = 0,
                actualWeight = 0f,
                isCompleted = false,
            )

        assertThat(setLog.isCompleted).isFalse()
        assertThat(setLog.actualReps).isEqualTo(0)
        assertThat(setLog.actualWeight).isEqualTo(0f)
        assertThat(setLog.actualRpe).isNull()
        assertThat(setLog.completedAt).isNull()
    }

    @Test
    fun `SetLog validates RPE range`() {
        val validRpe =
            SetLog(
                id = "3",
                exerciseLogId = "100",
                setOrder = 0,
                actualRpe = 7.5f,
            )

        assertThat(validRpe.actualRpe).isEqualTo(7.5f)
        assertThat(validRpe.actualRpe).isAtLeast(1f)
        assertThat(validRpe.actualRpe).isAtMost(10f)
    }

    @Test
    fun `SetLog handles zero and negative weights correctly`() {
        val zeroWeight =
            SetLog(
                id = "4",
                exerciseLogId = "100",
                setOrder = 0,
                targetWeight = 0f,
                actualWeight = 0f,
            )

        assertThat(zeroWeight.targetWeight).isEqualTo(0f)
        assertThat(zeroWeight.actualWeight).isEqualTo(0f)
    }

    @Test
    fun `SetLog order maintains sequence`() {
        val sets =
            listOf(
                SetLog(id = "1", exerciseLogId = "100", setOrder = 0),
                SetLog(id = "2", exerciseLogId = "100", setOrder = 1),
                SetLog(id = "3", exerciseLogId = "100", setOrder = 2),
            )

        assertThat(sets[0].setOrder).isEqualTo(0)
        assertThat(sets[1].setOrder).isEqualTo(1)
        assertThat(sets[2].setOrder).isEqualTo(2)
        assertThat(sets.map { it.setOrder }).isInOrder()
    }

    @Test
    fun `ExerciseLog stores all required fields`() {
        val exerciseLog =
            ExerciseLog(
                id = "1",
                workoutId = "50",
                exerciseVariationId = "100",
                exerciseOrder = 0,
                notes = "Focus on form",
                supersetGroup = null,
                originalVariationId = null,
                isSwapped = false,
            )

        assertThat(exerciseLog.id).isEqualTo("1")
        assertThat(exerciseLog.workoutId).isEqualTo("50")
        assertThat(exerciseLog.exerciseVariationId).isEqualTo("100")
        assertThat(exerciseLog.exerciseOrder).isEqualTo(0)
        assertThat(exerciseLog.notes).isEqualTo("Focus on form")
        assertThat(exerciseLog.isSwapped).isFalse()
    }

    @Test
    fun `ExerciseLog handles superset grouping`() {
        val superset1 =
            ExerciseLog(
                id = "1",
                workoutId = "50",
                exerciseVariationId = "100",
                exerciseOrder = 0,
                supersetGroup = 1,
            )

        val superset2 =
            ExerciseLog(
                id = "2",
                workoutId = "50",
                exerciseVariationId = "101",
                exerciseOrder = 1,
                supersetGroup = 1,
            )

        assertThat(superset1.supersetGroup).isEqualTo(superset2.supersetGroup)
        assertThat(superset1.supersetGroup).isEqualTo(1)
    }

    @Test
    fun `ExerciseLog handles exercise swapping`() {
        val swappedExercise =
            ExerciseLog(
                id = "3",
                workoutId = "50",
                exerciseVariationId = "200", // New exercise
                exerciseOrder = 2,
                originalVariationId = "100", // Original exercise
                isSwapped = true,
            )

        assertThat(swappedExercise.isSwapped).isTrue()
        assertThat(swappedExercise.originalVariationId).isEqualTo("100")
        assertThat(swappedExercise.exerciseVariationId).isEqualTo("200")
    }

    @Test
    fun `ExerciseLog maintains exercise order`() {
        val exercises =
            listOf(
                ExerciseLog(id = "1", workoutId = "50", exerciseVariationId = "100", exerciseOrder = 0),
                ExerciseLog(id = "2", workoutId = "50", exerciseVariationId = "101", exerciseOrder = 1),
                ExerciseLog(id = "3", workoutId = "50", exerciseVariationId = "102", exerciseOrder = 2),
                ExerciseLog(id = "4", workoutId = "50", exerciseVariationId = "103", exerciseOrder = 3),
            )

        assertThat(exercises.map { it.exerciseOrder }).isInOrder()
        assertThat(exercises.all { it.workoutId == "50" }).isTrue()
    }

    @Test
    fun `PersonalRecord stores all PR types`() {
        val now = LocalDateTime.now()
        val weightPR =
            PersonalRecord(
                id = "1",
                exerciseVariationId = "100",
                weight = 150f,
                reps = 5,
                recordDate = now,
                previousWeight = 140f,
                previousReps = 5,
                previousDate = now.minusWeeks(1),
                improvementPercentage = 7.14f,
                recordType = PRType.WEIGHT,
                workoutId = "50",
            )

        val repsPR =
            PersonalRecord(
                id = "2",
                exerciseVariationId = "100",
                weight = 100f,
                reps = 20,
                recordDate = now,
                previousWeight = 100f,
                previousReps = 18,
                previousDate = now.minusWeeks(1),
                improvementPercentage = 11.11f,
                recordType = PRType.WEIGHT,
                workoutId = "51",
            )

        val volumePR =
            PersonalRecord(
                id = "3",
                exerciseVariationId = "100",
                weight = 120f,
                reps = 25,
                recordDate = now,
                previousWeight = 115f,
                previousReps = 24,
                previousDate = now.minusWeeks(1),
                improvementPercentage = 8.7f,
                recordType = PRType.ESTIMATED_1RM,
                workoutId = "52",
            )

        assertThat(weightPR.recordType).isEqualTo(PRType.WEIGHT)
        assertThat(repsPR.recordType).isEqualTo(PRType.WEIGHT)
        assertThat(volumePR.recordType).isEqualTo(PRType.ESTIMATED_1RM)
    }

    @Test
    fun `PRType enum has expected values`() {
        assertThat(PRType.values().toList()).containsExactly(
            PRType.WEIGHT,
            PRType.ESTIMATED_1RM,
        )
    }

    @Test
    fun `PersonalRecord calculates volume correctly`() {
        val pr =
            PersonalRecord(
                id = "1",
                exerciseVariationId = "100",
                weight = 100f,
                reps = 10,
                recordDate = LocalDateTime.now(),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.ESTIMATED_1RM,
            )

        assertThat(pr.volume).isEqualTo(1000f) // 100 * 10
    }

    @Test
    fun `GlobalExerciseProgress tracks exercise statistics`() {
        val now = LocalDateTime.now()
        val progress =
            GlobalExerciseProgress(
                id = "1",
                exerciseVariationId = "100",
                currentWorkingWeight = 90f,
                estimatedMax = 120f,
                lastUpdated = now,
                recentAvgRpe = 7.5f,
                consecutiveStalls = 0,
                lastPrDate = now.minusWeeks(2),
                lastPrWeight = 100f,
                trend = ProgressTrend.IMPROVING,
                volumeTrend = VolumeTrend.INCREASING,
                totalVolumeLast30Days = 45000f,
                sessionsTracked = 15,
                bestSingleRep = 120f,
                best3Rep = 110f,
                best5Rep = 100f,
                best8Rep = 90f,
            )

        assertThat(progress.exerciseVariationId).isEqualTo("100")
        assertThat(progress.currentWorkingWeight).isEqualTo(90f)
        assertThat(progress.estimatedMax).isEqualTo(120f)
        assertThat(progress.recentAvgRpe).isEqualTo(7.5f)
        assertThat(progress.consecutiveStalls).isEqualTo(0)
        assertThat(progress.trend).isEqualTo(ProgressTrend.IMPROVING)
        assertThat(progress.volumeTrend).isEqualTo(VolumeTrend.INCREASING)
        assertThat(progress.totalVolumeLast30Days).isEqualTo(45000f)
    }

    @Test
    fun `PersonalRecord handles estimated 1RM`() {
        val pr =
            PersonalRecord(
                id = "1",
                exerciseVariationId = "100",
                weight = 100f,
                reps = 5,
                recordDate = LocalDateTime.now(),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.ESTIMATED_1RM,
                estimated1RM = 116.7f, // Brzycki formula
            )

        assertThat(pr.recordType).isEqualTo(PRType.ESTIMATED_1RM)
        assertThat(pr.estimated1RM).isEqualTo(116.7f)
    }

    @Test
    fun `PendingOneRMUpdate contains all required fields`() {
        val now = LocalDateTime.now()
        val update =
            PendingOneRMUpdate(
                exerciseVariationId = "1",
                currentMax = 100f,
                suggestedMax = 110f,
                confidence = 0.92f,
                source = "3×100kg @ RPE 8",
                workoutDate = now,
            )

        assertThat(update.exerciseVariationId).isEqualTo("1")
        assertThat(update.currentMax).isEqualTo(100f)
        assertThat(update.suggestedMax).isEqualTo(110f)
        assertThat(update.confidence).isEqualTo(0.92f)
        assertThat(update.source).isEqualTo("3×100kg @ RPE 8")
        assertThat(update.workoutDate).isEqualTo(now)
        assertThat(update.confidence).isAtLeast(0f)
        assertThat(update.confidence).isAtMost(1f)
    }

    @Test
    fun `PersonalRecord handles null previous values for first record`() {
        val pr =
            PersonalRecord(
                id = "1",
                exerciseVariationId = "100",
                weight = 60f,
                reps = 12,
                recordDate = LocalDateTime.now(),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 100f, // First record = 100% improvement
                recordType = PRType.WEIGHT,
            )

        assertThat(pr.previousWeight).isNull()
        assertThat(pr.previousReps).isNull()
        assertThat(pr.previousDate).isNull()
        assertThat(pr.improvementPercentage).isEqualTo(100f)
    }

    @Test
    fun `ParseRequest tracks parsing status`() {
        val now = LocalDateTime.now()
        val pendingRequest =
            ParseRequest(
                id = "1",
                rawText = "Week 1 Day 1: Squat 5x5",
                status = ParseStatus.PROCESSING,
                createdAt = now,
            )

        assertThat(pendingRequest.status).isEqualTo(ParseStatus.PROCESSING)
        assertThat(pendingRequest.resultJson).isNull()
        assertThat(pendingRequest.error).isNull()

        val completedRequest =
            pendingRequest.copy(
                status = ParseStatus.COMPLETED,
                resultJson = "{ \"exercises\": [...] }",
                completedAt = now.plusMinutes(1),
            )

        assertThat(completedRequest.status).isEqualTo(ParseStatus.COMPLETED)
        assertThat(completedRequest.resultJson).isNotNull()
    }

    @Test
    fun `ParseStatus enum has expected values`() {
        assertThat(ParseStatus.values().toList()).containsExactly(
            ParseStatus.PROCESSING,
            ParseStatus.COMPLETED,
            ParseStatus.FAILED,
            ParseStatus.IMPORTED,
        )
    }

    @Test
    fun `PersonalRecord handles RPE field`() {
        val pr =
            PersonalRecord(
                id = "1",
                exerciseVariationId = "100",
                weight = 80f,
                reps = 8,
                rpe = 8.5f,
                recordDate = LocalDateTime.now(),
                previousWeight = 75f,
                previousReps = 8,
                previousDate = LocalDateTime.now().minusWeeks(1),
                improvementPercentage = 6.67f,
                recordType = PRType.WEIGHT,
            )

        assertThat(pr.rpe).isEqualTo(8.5f)
        assertThat(pr.rpe).isAtLeast(1f)
        assertThat(pr.rpe).isAtMost(10f)
    }

    @Test
    fun `ExerciseSwapHistory records swap details`() {
        val now = LocalDateTime.now()
        val swap =
            ExerciseSwapHistory(
                id = "1",
                originalExerciseId = "100",
                swappedToExerciseId = "200",
                swapDate = now,
                workoutId = "50",
                programmeId = "10",
            )

        assertThat(swap.originalExerciseId).isEqualTo("100")
        assertThat(swap.swappedToExerciseId).isEqualTo("200")
        assertThat(swap.workoutId).isEqualTo("50")
        assertThat(swap.programmeId).isEqualTo("10")
        assertThat(swap.swapDate).isEqualTo(now)
    }
}
