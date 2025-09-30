package com.github.radupana.featherweight.data.programme

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class ProgrammeCompletionModelsTest {
    @Test
    fun strengthImprovement_shouldCalculateCorrectly() {
        val improvement =
            StrengthImprovement(
                exerciseName = "Barbell Squat",
                startingMax = 100f,
                endingMax = 120f,
                improvementKg = 20f,
                improvementPercentage = 20f,
            )

        assertThat(improvement.exerciseName).isEqualTo("Barbell Squat")
        assertThat(improvement.startingMax).isEqualTo(100f)
        assertThat(improvement.endingMax).isEqualTo(120f)
        assertThat(improvement.improvementKg).isEqualTo(20f)
        assertThat(improvement.improvementPercentage).isEqualTo(20f)
    }

    @Test
    fun strengthImprovement_withNegativeImprovement_shouldHandleRegression() {
        val regression =
            StrengthImprovement(
                exerciseName = "Overhead Press",
                startingMax = 60f,
                endingMax = 55f,
                improvementKg = -5f,
                improvementPercentage = -8.33f,
            )

        assertThat(regression.improvementKg).isLessThan(0f)
        assertThat(regression.improvementPercentage).isLessThan(0f)
        assertThat(regression.endingMax).isLessThan(regression.startingMax)
    }

    @Test
    fun strengthImprovement_withZeroImprovement_shouldHandleStagnation() {
        val stagnation =
            StrengthImprovement(
                exerciseName = "Bench Press",
                startingMax = 80f,
                endingMax = 80f,
                improvementKg = 0f,
                improvementPercentage = 0f,
            )

        assertThat(stagnation.improvementKg).isEqualTo(0f)
        assertThat(stagnation.improvementPercentage).isEqualTo(0f)
        assertThat(stagnation.startingMax).isEqualTo(stagnation.endingMax)
    }

    @Test
    fun strengthImprovement_withLargeImprovement_shouldHandleSignificantGains() {
        val bigGains =
            StrengthImprovement(
                exerciseName = "Deadlift",
                startingMax = 140f,
                endingMax = 200f,
                improvementKg = 60f,
                improvementPercentage = 42.86f,
            )

        assertThat(bigGains.improvementKg).isEqualTo(60f)
        assertThat(bigGains.improvementPercentage).isWithin(0.01f).of(42.86f)
    }

    @Test
    fun programmeInsights_withAllData_shouldStoreCorrectly() {
        val insights =
            ProgrammeInsights(
                totalTrainingDays = 48,
                mostConsistentDay = "Monday",
                averageRestDaysBetweenWorkouts = 1.5f,
            )

        assertThat(insights.totalTrainingDays).isEqualTo(48)
        assertThat(insights.mostConsistentDay).isEqualTo("Monday")
        assertThat(insights.averageRestDaysBetweenWorkouts).isEqualTo(1.5f)
    }

    @Test
    fun programmeInsights_withNullMostConsistentDay_shouldHandleNoPattern() {
        val insights =
            ProgrammeInsights(
                totalTrainingDays = 10,
                mostConsistentDay = null,
                averageRestDaysBetweenWorkouts = 2.0f,
            )

        assertThat(insights.mostConsistentDay).isNull()
        assertThat(insights.totalTrainingDays).isEqualTo(10)
    }

    @Test
    fun programmeInsights_withHighRestDays_shouldHandleInfrequentTraining() {
        val insights =
            ProgrammeInsights(
                totalTrainingDays = 8,
                mostConsistentDay = "Saturday",
                averageRestDaysBetweenWorkouts = 6.0f,
            )

        assertThat(insights.averageRestDaysBetweenWorkouts).isEqualTo(6.0f)
        assertThat(insights.totalTrainingDays).isEqualTo(8)
    }

    @Test
    fun programmeCompletionStats_withFullData_shouldCalculateCorrectly() {
        val now = LocalDateTime.now()
        val twelveWeeksAgo = now.minusWeeks(12)

        val improvements =
            listOf(
                StrengthImprovement("Squat", 100f, 120f, 20f, 20f),
                StrengthImprovement("Bench", 80f, 90f, 10f, 12.5f),
                StrengthImprovement("Deadlift", 140f, 170f, 30f, 21.43f),
            )

        val insights = ProgrammeInsights(48, "Monday", 1.5f)

        val topExercises =
            listOf(
                ExerciseFrequency("Barbell Squat", 36),
                ExerciseFrequency("Barbell Bench Press", 36),
                ExerciseFrequency("Barbell Deadlift", 18),
            )

        val stats =
            ProgrammeCompletionStats(
                programmeId = "1",
                programmeName = "StrongLifts 5x5",
                startDate = twelveWeeksAgo,
                endDate = now,
                totalWorkouts = 36,
                completedWorkouts = 35,
                totalVolume = 250000f,
                averageWorkoutDuration = Duration.ofMinutes(75),
                totalPRs = 15,
                strengthImprovements = improvements,
                averageStrengthImprovement = 17.98f,
                insights = insights,
                topExercises = topExercises,
            )

        assertThat(stats.programmeId).isEqualTo("1")
        assertThat(stats.programmeName).isEqualTo("StrongLifts 5x5")
        assertThat(stats.completedWorkouts).isEqualTo(35)
        assertThat(stats.totalWorkouts).isEqualTo(36)
        assertThat(stats.totalVolume).isEqualTo(250000f)
        assertThat(stats.averageWorkoutDuration.toMinutes()).isEqualTo(75L)
        assertThat(stats.totalPRs).isEqualTo(15)
        assertThat(stats.strengthImprovements).hasSize(3)
        assertThat(stats.averageStrengthImprovement).isWithin(0.01f).of(17.98f)
        assertThat(stats.topExercises).hasSize(3)
        assertThat(stats.topExercises[0].exerciseName).isEqualTo("Barbell Squat")
        assertThat(stats.topExercises[0].frequency).isEqualTo(36)
    }

    @Test
    fun programmeCompletionStats_withZeroWorkouts_shouldHandleEmptyProgramme() {
        val now = LocalDateTime.now()
        val stats =
            ProgrammeCompletionStats(
                programmeId = "2",
                programmeName = "Custom Programme",
                startDate = now,
                endDate = now,
                totalWorkouts = 0,
                completedWorkouts = 0,
                totalVolume = 0f,
                averageWorkoutDuration = Duration.ZERO,
                totalPRs = 0,
                strengthImprovements = emptyList(),
                averageStrengthImprovement = 0f,
                insights = ProgrammeInsights(0, null, 0f),
            )

        assertThat(stats.completedWorkouts).isEqualTo(0)
        assertThat(stats.totalVolume).isEqualTo(0f)
        assertThat(stats.averageWorkoutDuration).isEqualTo(Duration.ZERO)
        assertThat(stats.strengthImprovements).isEmpty()
    }

    @Test
    fun programmeCompletionStats_withPartialCompletion_shouldCalculateCompletionRate() {
        val now = LocalDateTime.now()
        val stats =
            ProgrammeCompletionStats(
                programmeId = "3",
                programmeName = "5/3/1",
                startDate = now.minusWeeks(4),
                endDate = now,
                totalWorkouts = 12,
                completedWorkouts = 9,
                totalVolume = 75000f,
                averageWorkoutDuration = Duration.ofMinutes(60),
                totalPRs = 3,
                strengthImprovements =
                    listOf(
                        StrengthImprovement("Squat", 100f, 105f, 5f, 5f),
                    ),
                averageStrengthImprovement = 5f,
                insights = ProgrammeInsights(9, "Tuesday", 2.3f),
            )

        val completionRate = stats.completedWorkouts.toFloat() / stats.totalWorkouts
        assertThat(completionRate).isWithin(0.01f).of(0.75f)
        assertThat(stats.completedWorkouts).isLessThan(stats.totalWorkouts)
    }

    @Test
    fun programmeCompletionStats_withLongDuration_shouldHandleExtendedProgrammes() {
        val endDate = LocalDateTime.now()
        val startDate = endDate.minusMonths(6)

        val stats =
            ProgrammeCompletionStats(
                programmeId = "4",
                programmeName = "Long Term Strength",
                startDate = startDate,
                endDate = endDate,
                totalWorkouts = 72,
                completedWorkouts = 70,
                totalVolume = 500000f,
                averageWorkoutDuration = Duration.ofMinutes(90),
                totalPRs = 25,
                strengthImprovements =
                    listOf(
                        StrengthImprovement("Squat", 80f, 140f, 60f, 75f),
                        StrengthImprovement("Bench", 60f, 90f, 30f, 50f),
                        StrengthImprovement("Deadlift", 100f, 180f, 80f, 80f),
                    ),
                averageStrengthImprovement = 68.33f,
                insights = ProgrammeInsights(70, "Wednesday", 1.6f),
            )

        val durationDays = Duration.between(startDate, endDate).toDays()
        assertThat(durationDays).isGreaterThan(150)
        assertThat(stats.totalWorkouts).isEqualTo(72)
        assertThat(stats.strengthImprovements).hasSize(3)
    }

    @Test
    fun programmeCompletionStats_dataClassEquality_shouldWorkCorrectly() {
        val now = LocalDateTime.now()
        val insights = ProgrammeInsights(10, "Monday", 2f)

        val stats1 =
            ProgrammeCompletionStats(
                programmeId = "1",
                programmeName = "Test",
                startDate = now,
                endDate = now.plusWeeks(4),
                totalWorkouts = 12,
                completedWorkouts = 12,
                totalVolume = 50000f,
                averageWorkoutDuration = Duration.ofMinutes(60),
                totalPRs = 5,
                strengthImprovements = emptyList(),
                averageStrengthImprovement = 10f,
                insights = insights,
            )

        val stats2 =
            ProgrammeCompletionStats(
                programmeId = "1",
                programmeName = "Test",
                startDate = now,
                endDate = now.plusWeeks(4),
                totalWorkouts = 12,
                completedWorkouts = 12,
                totalVolume = 50000f,
                averageWorkoutDuration = Duration.ofMinutes(60),
                totalPRs = 5,
                strengthImprovements = emptyList(),
                averageStrengthImprovement = 10f,
                insights = insights,
            )

        assertThat(stats1).isEqualTo(stats2)
        assertThat(stats1.hashCode()).isEqualTo(stats2.hashCode())
    }

    @Test
    fun strengthImprovement_dataClassCopy_shouldWorkCorrectly() {
        val original =
            StrengthImprovement(
                exerciseName = "Squat",
                startingMax = 100f,
                endingMax = 120f,
                improvementKg = 20f,
                improvementPercentage = 20f,
            )

        val modified = original.copy(endingMax = 130f, improvementKg = 30f, improvementPercentage = 30f)

        assertThat(modified.exerciseName).isEqualTo(original.exerciseName)
        assertThat(modified.startingMax).isEqualTo(original.startingMax)
        assertThat(modified.endingMax).isEqualTo(130f)
        assertThat(modified.improvementKg).isEqualTo(30f)
        assertThat(modified.improvementPercentage).isEqualTo(30f)
    }

    @Test
    fun programmeInsights_withDifferentDays_shouldHandleAllWeekdays() {
        val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        weekdays.forEach { day ->
            val insights =
                ProgrammeInsights(
                    totalTrainingDays = 20,
                    mostConsistentDay = day,
                    averageRestDaysBetweenWorkouts = 2f,
                )

            assertThat(insights.mostConsistentDay).isEqualTo(day)
        }
    }

    @Test
    fun programmeCompletionStats_withHighVolume_shouldHandleLargeNumbers() {
        val stats =
            ProgrammeCompletionStats(
                programmeId = "5",
                programmeName = "High Volume",
                startDate = LocalDateTime.now().minusMonths(1),
                endDate = LocalDateTime.now(),
                totalWorkouts = 30,
                completedWorkouts = 30,
                totalVolume = 1_000_000f,
                averageWorkoutDuration = Duration.ofMinutes(120),
                totalPRs = 50,
                strengthImprovements = emptyList(),
                averageStrengthImprovement = 0f,
                insights = ProgrammeInsights(30, "Monday", 1f),
            )

        assertThat(stats.totalVolume).isEqualTo(1_000_000f)
        assertThat(stats.averageWorkoutDuration.toHours()).isEqualTo(2L)
    }

    @Test
    fun strengthImprovement_withFractionalPercentages_shouldHandlePrecision() {
        val improvement =
            StrengthImprovement(
                exerciseName = "Overhead Press",
                startingMax = 47.5f,
                endingMax = 52.5f,
                improvementKg = 5f,
                improvementPercentage = 10.526f,
            )

        assertThat(improvement.startingMax).isEqualTo(47.5f)
        assertThat(improvement.endingMax).isEqualTo(52.5f)
        assertThat(improvement.improvementPercentage).isWithin(0.001f).of(10.526f)
    }

    @Test
    fun exerciseFrequency_shouldStoreDataCorrectly() {
        val frequency =
            ExerciseFrequency(
                exerciseName = "Barbell Squat",
                frequency = 24,
            )

        assertThat(frequency.exerciseName).isEqualTo("Barbell Squat")
        assertThat(frequency.frequency).isEqualTo(24)
    }

    @Test
    fun exerciseFrequency_withZeroFrequency_shouldHandleUnusedExercise() {
        val frequency =
            ExerciseFrequency(
                exerciseName = "Cable Fly",
                frequency = 0,
            )

        assertThat(frequency.frequency).isEqualTo(0)
        assertThat(frequency.exerciseName).isEqualTo("Cable Fly")
    }

    @Test
    fun exerciseFrequency_dataClassEquality_shouldWorkCorrectly() {
        val freq1 = ExerciseFrequency("Deadlift", 18)
        val freq2 = ExerciseFrequency("Deadlift", 18)
        val freq3 = ExerciseFrequency("Deadlift", 20)

        assertThat(freq1).isEqualTo(freq2)
        assertThat(freq1).isNotEqualTo(freq3)
        assertThat(freq1.hashCode()).isEqualTo(freq2.hashCode())
    }
}
