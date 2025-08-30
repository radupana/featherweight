package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class GlobalExerciseProgressTest {
    @Test
    fun globalExerciseProgress_creation_withAllFields_createsCorrectly() {
        val lastUpdated = LocalDateTime.of(2024, 1, 15, 10, 30)
        val lastPrDate = LocalDateTime.of(2024, 1, 10, 9, 0)
        val lastProgressionDate = LocalDateTime.of(2024, 1, 8, 11, 0)

        val progress =
            GlobalExerciseProgress(
                id = 1L,
                exerciseVariationId = 100L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = lastUpdated,
                recentAvgRpe = 8.5f,
                consecutiveStalls = 3,
                lastPrDate = lastPrDate,
                lastPrWeight = 105f,
                trend = ProgressTrend.IMPROVING,
                volumeTrend = VolumeTrend.INCREASING,
                totalVolumeLast30Days = 15000f,
                sessionsTracked = 25,
                bestSingleRep = 125f,
                best3Rep = 115f,
                best5Rep = 110f,
                best8Rep = 100f,
                lastSessionVolume = 2500f,
                avgSessionVolume = 2000f,
                weeksAtCurrentWeight = 2,
                lastProgressionDate = lastProgressionDate,
                failureStreak = 0,
                lastProgrammeWorkoutId = 50L,
                lastFreestyleWorkoutId = 45L,
            )

        assertThat(progress.id).isEqualTo(1L)
        assertThat(progress.exerciseVariationId).isEqualTo(100L)
        assertThat(progress.currentWorkingWeight).isEqualTo(100f)
        assertThat(progress.estimatedMax).isEqualTo(120f)
        assertThat(progress.lastUpdated).isEqualTo(lastUpdated)
        assertThat(progress.recentAvgRpe).isEqualTo(8.5f)
        assertThat(progress.consecutiveStalls).isEqualTo(3)
        assertThat(progress.lastPrDate).isEqualTo(lastPrDate)
        assertThat(progress.lastPrWeight).isEqualTo(105f)
        assertThat(progress.trend).isEqualTo(ProgressTrend.IMPROVING)
        assertThat(progress.volumeTrend).isEqualTo(VolumeTrend.INCREASING)
        assertThat(progress.totalVolumeLast30Days).isEqualTo(15000f)
        assertThat(progress.sessionsTracked).isEqualTo(25)
        assertThat(progress.bestSingleRep).isEqualTo(125f)
        assertThat(progress.best3Rep).isEqualTo(115f)
        assertThat(progress.best5Rep).isEqualTo(110f)
        assertThat(progress.best8Rep).isEqualTo(100f)
        assertThat(progress.lastSessionVolume).isEqualTo(2500f)
        assertThat(progress.avgSessionVolume).isEqualTo(2000f)
        assertThat(progress.weeksAtCurrentWeight).isEqualTo(2)
        assertThat(progress.lastProgressionDate).isEqualTo(lastProgressionDate)
        assertThat(progress.failureStreak).isEqualTo(0)
        assertThat(progress.lastProgrammeWorkoutId).isEqualTo(50L)
        assertThat(progress.lastFreestyleWorkoutId).isEqualTo(45L)
    }

    @Test
    fun globalExerciseProgress_creation_withMinimalFields_usesDefaults() {
        val lastUpdated = LocalDateTime.now()

        val progress =
            GlobalExerciseProgress(
                exerciseVariationId = 50L,
                currentWorkingWeight = 80f,
                estimatedMax = 100f,
                lastUpdated = lastUpdated,
            )

        assertThat(progress.id).isEqualTo(0L)
        assertThat(progress.exerciseVariationId).isEqualTo(50L)
        assertThat(progress.currentWorkingWeight).isEqualTo(80f)
        assertThat(progress.estimatedMax).isEqualTo(100f)
        assertThat(progress.lastUpdated).isEqualTo(lastUpdated)
        assertThat(progress.recentAvgRpe).isNull()
        assertThat(progress.consecutiveStalls).isEqualTo(0)
        assertThat(progress.lastPrDate).isNull()
        assertThat(progress.lastPrWeight).isNull()
        assertThat(progress.trend).isEqualTo(ProgressTrend.STALLING)
        assertThat(progress.volumeTrend).isNull()
        assertThat(progress.totalVolumeLast30Days).isEqualTo(0f)
        assertThat(progress.sessionsTracked).isEqualTo(0)
        assertThat(progress.bestSingleRep).isNull()
        assertThat(progress.best3Rep).isNull()
        assertThat(progress.best5Rep).isNull()
        assertThat(progress.best8Rep).isNull()
        assertThat(progress.lastSessionVolume).isNull()
        assertThat(progress.avgSessionVolume).isNull()
        assertThat(progress.weeksAtCurrentWeight).isEqualTo(0)
        assertThat(progress.lastProgressionDate).isNull()
        assertThat(progress.failureStreak).isEqualTo(0)
        assertThat(progress.lastProgrammeWorkoutId).isNull()
        assertThat(progress.lastFreestyleWorkoutId).isNull()
    }

    @Test
    fun globalExerciseProgress_allProgressTrendValues_areValid() {
        val baseDate = LocalDateTime.now()

        val improving =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = baseDate,
                trend = ProgressTrend.IMPROVING,
            )
        assertThat(improving.trend).isEqualTo(ProgressTrend.IMPROVING)

        val stalling =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = baseDate,
                trend = ProgressTrend.STALLING,
            )
        assertThat(stalling.trend).isEqualTo(ProgressTrend.STALLING)

        val declining =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = baseDate,
                trend = ProgressTrend.DECLINING,
            )
        assertThat(declining.trend).isEqualTo(ProgressTrend.DECLINING)
    }

    @Test
    fun globalExerciseProgress_allVolumeTrendValues_areValid() {
        val baseDate = LocalDateTime.now()

        val increasing =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = baseDate,
                volumeTrend = VolumeTrend.INCREASING,
            )
        assertThat(increasing.volumeTrend).isEqualTo(VolumeTrend.INCREASING)

        val maintaining =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = baseDate,
                volumeTrend = VolumeTrend.MAINTAINING,
            )
        assertThat(maintaining.volumeTrend).isEqualTo(VolumeTrend.MAINTAINING)

        val decreasing =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = baseDate,
                volumeTrend = VolumeTrend.DECREASING,
            )
        assertThat(decreasing.volumeTrend).isEqualTo(VolumeTrend.DECREASING)
    }

    @Test
    fun globalExerciseProgress_withHighConsecutiveStalls_tracksCorrectly() {
        val progress =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 80f,
                estimatedMax = 100f,
                lastUpdated = LocalDateTime.now(),
                consecutiveStalls = 10,
                weeksAtCurrentWeight = 5,
                trend = ProgressTrend.STALLING,
            )

        assertThat(progress.consecutiveStalls).isEqualTo(10)
        assertThat(progress.weeksAtCurrentWeight).isEqualTo(5)
        assertThat(progress.trend).isEqualTo(ProgressTrend.STALLING)
    }

    @Test
    fun globalExerciseProgress_withAllRepMaxes_storesCorrectly() {
        val progress =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 100f,
                estimatedMax = 130f,
                lastUpdated = LocalDateTime.now(),
                bestSingleRep = 130f,
                best3Rep = 120f,
                best5Rep = 110f,
                best8Rep = 100f,
            )

        assertThat(progress.bestSingleRep).isEqualTo(130f)
        assertThat(progress.best3Rep).isEqualTo(120f)
        assertThat(progress.best5Rep).isEqualTo(110f)
        assertThat(progress.best8Rep).isEqualTo(100f)
        // Validate that rep maxes follow expected strength curve (1RM > 3RM > 5RM > 8RM)
        assertThat(progress.bestSingleRep).isGreaterThan(progress.best3Rep!!)
        assertThat(progress.best3Rep).isGreaterThan(progress.best5Rep!!)
        assertThat(progress.best5Rep).isGreaterThan(progress.best8Rep!!)
    }

    @Test
    fun globalExerciseProgress_withVeryHighVolume_handlesCorrectly() {
        val progress =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = LocalDateTime.now(),
                totalVolumeLast30Days = 100000f, // Very high volume
                lastSessionVolume = 5000f,
                avgSessionVolume = 3500f,
                volumeTrend = VolumeTrend.INCREASING,
            )

        assertThat(progress.totalVolumeLast30Days).isEqualTo(100000f)
        assertThat(progress.lastSessionVolume).isEqualTo(5000f)
        assertThat(progress.avgSessionVolume).isEqualTo(3500f)
    }

    @Test
    fun globalExerciseProgress_withHighRPE_indicatesDifficulty() {
        val progress =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 100f,
                estimatedMax = 105f, // Close to max
                lastUpdated = LocalDateTime.now(),
                recentAvgRpe = 9.5f, // Very high RPE
            )

        assertThat(progress.recentAvgRpe).isEqualTo(9.5f)
        // Working weight is very close to estimated max (95%)
        val percentageOfMax = (progress.currentWorkingWeight / progress.estimatedMax) * 100
        assertThat(percentageOfMax).isGreaterThan(95f)
    }

    @Test
    fun globalExerciseProgress_withFailureStreak_tracksDecline() {
        val progress =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 80f,
                estimatedMax = 100f,
                lastUpdated = LocalDateTime.now(),
                failureStreak = 5,
                trend = ProgressTrend.DECLINING,
            )

        assertThat(progress.failureStreak).isEqualTo(5)
        assertThat(progress.trend).isEqualTo(ProgressTrend.DECLINING)
    }

    @Test
    fun globalExerciseProgress_dateTimeFields_maintainPrecision() {
        val lastUpdated = LocalDateTime.of(2024, 3, 15, 14, 30, 45, 123456789)
        val lastPrDate = LocalDateTime.of(2024, 3, 10, 10, 15, 30, 987654321)
        val lastProgressionDate = LocalDateTime.of(2024, 3, 5, 16, 45, 0, 0)

        val progress =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = lastUpdated,
                lastPrDate = lastPrDate,
                lastProgressionDate = lastProgressionDate,
            )

        assertThat(progress.lastUpdated).isEqualTo(lastUpdated)
        assertThat(progress.lastPrDate).isEqualTo(lastPrDate)
        assertThat(progress.lastProgressionDate).isEqualTo(lastProgressionDate)
        assertThat(progress.lastUpdated.year).isEqualTo(2024)
        assertThat(progress.lastUpdated.monthValue).isEqualTo(3)
        assertThat(progress.lastUpdated.dayOfMonth).isEqualTo(15)
    }

    @Test
    fun globalExerciseProgress_withManySessionsTracked_showsExperience() {
        val progress =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = LocalDateTime.now(),
                sessionsTracked = 100, // Many sessions
                avgSessionVolume = 2500f,
            )

        assertThat(progress.sessionsTracked).isEqualTo(100)
        assertThat(progress.avgSessionVolume).isEqualTo(2500f)
    }

    @Test
    fun globalExerciseProgress_workoutTypeTracking_maintainsBothTypes() {
        val progress =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = LocalDateTime.now(),
                lastProgrammeWorkoutId = 99L,
                lastFreestyleWorkoutId = 88L,
            )

        assertThat(progress.lastProgrammeWorkoutId).isEqualTo(99L)
        assertThat(progress.lastFreestyleWorkoutId).isEqualTo(88L)
    }

    @Test
    fun globalExerciseProgress_withZeroVolume_indicatesNoWork() {
        val progress =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = LocalDateTime.now(),
                totalVolumeLast30Days = 0f,
                lastSessionVolume = 0f,
                avgSessionVolume = 0f,
            )

        assertThat(progress.totalVolumeLast30Days).isEqualTo(0f)
        assertThat(progress.lastSessionVolume).isEqualTo(0f)
        assertThat(progress.avgSessionVolume).isEqualTo(0f)
    }

    @Test
    fun globalExerciseProgress_withRecentPR_showsImprovement() {
        val now = LocalDateTime.now()
        val recentPrDate = now.minusDays(2)

        val progress =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 110f,
                estimatedMax = 130f,
                lastUpdated = now,
                lastPrDate = recentPrDate,
                lastPrWeight = 110f,
                trend = ProgressTrend.IMPROVING,
                consecutiveStalls = 0,
            )

        assertThat(progress.lastPrDate).isEqualTo(recentPrDate)
        assertThat(progress.lastPrWeight).isEqualTo(110f)
        assertThat(progress.trend).isEqualTo(ProgressTrend.IMPROVING)
        assertThat(progress.consecutiveStalls).isEqualTo(0)
    }

    @Test
    fun globalExerciseProgress_equality_basedOnAllFields() {
        val date = LocalDateTime.now()

        val progress1 =
            GlobalExerciseProgress(
                id = 1L,
                exerciseVariationId = 100L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = date,
                recentAvgRpe = 8f,
                consecutiveStalls = 2,
                trend = ProgressTrend.IMPROVING,
            )

        val progress2 =
            GlobalExerciseProgress(
                id = 1L,
                exerciseVariationId = 100L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = date,
                recentAvgRpe = 8f,
                consecutiveStalls = 2,
                trend = ProgressTrend.IMPROVING,
            )

        val progress3 =
            GlobalExerciseProgress(
                id = 2L, // Different ID
                exerciseVariationId = 100L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = date,
                recentAvgRpe = 8f,
                consecutiveStalls = 2,
                trend = ProgressTrend.IMPROVING,
            )

        assertThat(progress1).isEqualTo(progress2)
        assertThat(progress1).isNotEqualTo(progress3)
        assertThat(progress1.hashCode()).isEqualTo(progress2.hashCode())
    }

    @Test
    fun globalExerciseProgress_toString_includesKeyInfo() {
        val progress =
            GlobalExerciseProgress(
                id = 1L,
                exerciseVariationId = 100L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = LocalDateTime.of(2024, 1, 15, 10, 0),
                trend = ProgressTrend.IMPROVING,
            )

        val toString = progress.toString()
        assertThat(toString).contains("GlobalExerciseProgress")
        assertThat(toString).contains("id=1")
        assertThat(toString).contains("currentWorkingWeight=100")
        assertThat(toString).contains("estimatedMax=120")
    }

    @Test
    fun globalExerciseProgress_copy_createsIndependentInstance() {
        val original =
            GlobalExerciseProgress(
                exerciseVariationId = 100L,
                currentWorkingWeight = 100f,
                estimatedMax = 120f,
                lastUpdated = LocalDateTime.now(),
                trend = ProgressTrend.STALLING,
            )

        val copy =
            original.copy(
                currentWorkingWeight = 105f,
                estimatedMax = 125f,
                trend = ProgressTrend.IMPROVING,
                consecutiveStalls = 0,
            )

        assertThat(copy.currentWorkingWeight).isEqualTo(105f)
        assertThat(copy.estimatedMax).isEqualTo(125f)
        assertThat(copy.trend).isEqualTo(ProgressTrend.IMPROVING)
        assertThat(copy.consecutiveStalls).isEqualTo(0)
        assertThat(copy.exerciseVariationId).isEqualTo(original.exerciseVariationId)
        assertThat(original.currentWorkingWeight).isEqualTo(100f)
        assertThat(original.trend).isEqualTo(ProgressTrend.STALLING)
    }

    @Test
    fun globalExerciseProgress_withLowRPE_indicatesEasyWork() {
        val progress =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = 60f,
                estimatedMax = 100f,
                lastUpdated = LocalDateTime.now(),
                recentAvgRpe = 5.5f, // Low RPE
            )

        assertThat(progress.recentAvgRpe).isEqualTo(5.5f)
        // Working weight is only 60% of max
        val percentageOfMax = (progress.currentWorkingWeight / progress.estimatedMax) * 100
        assertThat(percentageOfMax).isWithin(0.001f).of(60f)
    }

    @Test
    fun globalExerciseProgress_edgeCaseNegativeValues_handlesCorrectly() {
        // Edge case - negative values shouldn't happen in practice but testing data model
        val progress =
            GlobalExerciseProgress(
                exerciseVariationId = 1L,
                currentWorkingWeight = -10f, // Edge case
                estimatedMax = 100f,
                lastUpdated = LocalDateTime.now(),
                consecutiveStalls = -1, // Edge case
                failureStreak = -1, // Edge case
            )

        assertThat(progress.currentWorkingWeight).isEqualTo(-10f)
        assertThat(progress.consecutiveStalls).isEqualTo(-1)
        assertThat(progress.failureStreak).isEqualTo(-1)
    }
}
