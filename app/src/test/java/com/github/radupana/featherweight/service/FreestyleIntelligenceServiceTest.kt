package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.GlobalExerciseProgress
import com.github.radupana.featherweight.data.GlobalExerciseProgressDao
import com.github.radupana.featherweight.data.ProgressTrend
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class FreestyleIntelligenceServiceTest {
    private lateinit var service: FreestyleIntelligenceService
    private lateinit var globalProgressDao: GlobalExerciseProgressDao

    @Before
    fun setup() {
        globalProgressDao = mockk(relaxed = true)
        service = FreestyleIntelligenceService(globalProgressDao)
    }

    @Test
    fun `getIntelligentSuggestions returns basic suggestions when no progress data exists`() =
        runTest {
            // Given
            coEvery { globalProgressDao.getProgressForExercise(100L) } returns null

            // When
            val result =
                service.getIntelligentSuggestions(
                    exerciseVariationId = 100L,
                    targetReps = 8,
                )

            // Then
            assertThat(result.suggestedWeight).isEqualTo(0f)
            assertThat(result.suggestedReps).isEqualTo(8)
            assertThat(result.suggestedRpe).isNull()
            assertThat(result.confidence).contains("Low")
            assertThat(result.reasoning).contains("No previous data")
            assertThat(result.alternativeSuggestions).isEmpty()
        }

    @Test
    fun `getIntelligentSuggestions suggests progressive overload when RPE is too easy`() =
        runTest {
            // Given
            val progress =
                createProgress(
                    currentWeight = 100f,
                    avgRpe = 6f,
                    consecutiveStalls = 0,
                    trend = ProgressTrend.IMPROVING,
                )
            coEvery { globalProgressDao.getProgressForExercise(100L) } returns progress

            // When
            val result =
                service.getIntelligentSuggestions(
                    exerciseVariationId = 100L,
                    targetReps = 5,
                )

            // Then
            assertThat(result.suggestedWeight).isEqualTo(105f) // 5% increase rounded to nearest 2.5kg
            assertThat(result.suggestedReps).isEqualTo(5)
            assertThat(result.suggestedRpe).isEqualTo(8f)
            assertThat(result.confidence).contains("High")
            assertThat(result.reasoning).contains("Progressive overload")
            assertThat(result.reasoning).contains("5%")
        }

    @Test
    fun `getIntelligentSuggestions suggests large increase when RPE is very low`() =
        runTest {
            // Given
            val progress =
                createProgress(
                    currentWeight = 100f,
                    avgRpe = 5.5f,
                    consecutiveStalls = 0,
                )
            coEvery { globalProgressDao.getProgressForExercise(100L) } returns progress

            // When
            val result =
                service.getIntelligentSuggestions(
                    exerciseVariationId = 100L,
                    targetReps = 5,
                )

            // Then
            assertThat(result.suggestedWeight).isEqualTo(107.5f) // 7.5% increase
            assertThat(result.reasoning).contains("8%") // Rounded percentage in message
        }

    @Test
    fun `getIntelligentSuggestions suggests deload after 3 consecutive stalls`() =
        runTest {
            // Given
            val progress =
                createProgress(
                    currentWeight = 100f,
                    avgRpe = 8.5f,
                    consecutiveStalls = 3,
                )
            coEvery { globalProgressDao.getProgressForExercise(100L) } returns progress

            // When
            val result =
                service.getIntelligentSuggestions(
                    exerciseVariationId = 100L,
                    targetReps = 5,
                )

            // Then
            assertThat(result.suggestedWeight).isEqualTo(85f) // 15% deload
            assertThat(result.suggestedRpe).isEqualTo(6f)
            assertThat(result.reasoning).contains("Deload recommended")
            assertThat(result.reasoning).contains("3 sessions")
            assertThat(result.alternativeSuggestions).hasSize(3)
        }

    @Test
    fun `getIntelligentSuggestions suggests deload alternatives correctly`() =
        runTest {
            // Given
            val progress =
                createProgress(
                    currentWeight = 100f,
                    avgRpe = 9f,
                    consecutiveStalls = 4,
                )
            coEvery { globalProgressDao.getProgressForExercise(100L) } returns progress

            // When
            val result =
                service.getIntelligentSuggestions(
                    exerciseVariationId = 100L,
                    targetReps = 5,
                )

            // Then
            val alternatives = result.alternativeSuggestions
            assertThat(alternatives).hasSize(3)

            // Check lower reps option
            assertThat(alternatives[0].reps).isEqualTo(3)
            assertThat(alternatives[0].weight).isEqualTo(100f)
            assertThat(alternatives[0].rpe).isEqualTo(7f)

            // Check higher reps option
            assertThat(alternatives[1].reps).isEqualTo(8)
            assertThat(alternatives[1].weight).isEqualTo(90f)
            assertThat(alternatives[1].rpe).isEqualTo(8f)

            // Check standard deload
            assertThat(alternatives[2].reps).isEqualTo(5)
            assertThat(alternatives[2].weight).isEqualTo(85f)
            assertThat(alternatives[2].rpe).isEqualTo(6f)
        }

    @Test
    fun `getIntelligentSuggestions suggests maintain when RPE is too hard`() =
        runTest {
            // Given
            val progress =
                createProgress(
                    currentWeight = 100f,
                    avgRpe = 9.2f,
                    consecutiveStalls = 1,
                )
            coEvery { globalProgressDao.getProgressForExercise(100L) } returns progress

            // When
            val result =
                service.getIntelligentSuggestions(
                    exerciseVariationId = 100L,
                    targetReps = 5,
                )

            // Then
            assertThat(result.suggestedWeight).isEqualTo(100f) // Maintain
            assertThat(result.suggestedRpe).isEqualTo(7f)
            assertThat(result.reasoning).contains("Maintain")
            assertThat(result.reasoning).contains("high effort")
        }

    @Test
    fun `getIntelligentSuggestions suggests decrease when RPE is very high`() =
        runTest {
            // Given
            val progress =
                createProgress(
                    currentWeight = 100f,
                    avgRpe = 9.7f,
                    consecutiveStalls = 0,
                )
            coEvery { globalProgressDao.getProgressForExercise(100L) } returns progress

            // When
            val result =
                service.getIntelligentSuggestions(
                    exerciseVariationId = 100L,
                    targetReps = 5,
                )

            // Then
            assertThat(result.suggestedWeight).isEqualTo(95f) // 5% decrease
            assertThat(result.reasoning).contains("Reducing weight by 5%")
        }

    @Test
    fun `getIntelligentSuggestions handles improving trend with optimal RPE`() =
        runTest {
            // Given
            val progress =
                createProgress(
                    currentWeight = 80f,
                    avgRpe = 8f,
                    consecutiveStalls = 0,
                    trend = ProgressTrend.IMPROVING,
                )
            coEvery { globalProgressDao.getProgressForExercise(100L) } returns progress

            // When
            val result =
                service.getIntelligentSuggestions(
                    exerciseVariationId = 100L,
                    targetReps = 5,
                )

            // Then
            assertThat(result.suggestedWeight).isEqualTo(82.5f) // 2.5% increase
            assertThat(result.reasoning).contains("Steady progress")
            assertThat(result.reasoning).contains("Small increase")
        }

    @Test
    fun `getIntelligentSuggestions handles stalling trend`() =
        runTest {
            // Given
            val progress =
                createProgress(
                    currentWeight = 100f,
                    avgRpe = 8f,
                    consecutiveStalls = 2,
                    trend = ProgressTrend.STALLING,
                )
            coEvery { globalProgressDao.getProgressForExercise(100L) } returns progress

            // When
            val result =
                service.getIntelligentSuggestions(
                    exerciseVariationId = 100L,
                    targetReps = 5,
                )

            // Then
            assertThat(result.suggestedWeight).isEqualTo(100f) // Maintain
            assertThat(result.reasoning).contains("Approaching a stall")
            assertThat(result.reasoning).contains("2 sessions")
        }

    @Test
    fun `getIntelligentSuggestions handles declining trend`() =
        runTest {
            // Given
            val progress =
                createProgress(
                    currentWeight = 100f,
                    avgRpe = 8f,
                    consecutiveStalls = 0,
                    trend = ProgressTrend.DECLINING,
                )
            coEvery { globalProgressDao.getProgressForExercise(100L) } returns progress

            // When
            val result =
                service.getIntelligentSuggestions(
                    exerciseVariationId = 100L,
                    targetReps = 5,
                )

            // Then
            assertThat(result.suggestedWeight).isEqualTo(95f) // 5% decrease
            assertThat(result.reasoning).contains("Performance declining")
            assertThat(result.reasoning).contains("Reduce weight by 5%")
        }

    @Test
    fun `getIntelligentSuggestions uses default reps when not specified`() =
        runTest {
            // Given
            val progress = createProgress(currentWeight = 100f)
            coEvery { globalProgressDao.getProgressForExercise(100L) } returns progress

            // When
            val result =
                service.getIntelligentSuggestions(
                    exerciseVariationId = 100L,
                    targetReps = null,
                )

            // Then
            assertThat(result.suggestedReps).isEqualTo(5) // Default
        }

    @Test
    fun `getIntelligentSuggestions generates alternatives with 1RM data`() =
        runTest {
            // Given
            val progress =
                createProgress(
                    currentWeight = 80f,
                    avgRpe = 8f,
                    estimatedMax = 100f,
                )
            coEvery { globalProgressDao.getProgressForExercise(100L) } returns progress

            // When
            val result =
                service.getIntelligentSuggestions(
                    exerciseVariationId = 100L,
                    targetReps = 5,
                )

            // Then
            val alternatives = result.alternativeSuggestions
            assertThat(alternatives).isNotEmpty()

            // Should have heavy double option
            val heavyDouble = alternatives.find { it.reps == 2 }
            assertThat(heavyDouble).isNotNull()
            assertThat(heavyDouble?.weight).isEqualTo(90f) // 90% of 1RM

            // Should have volume option
            val volumeWork = alternatives.find { it.reps == 8 }
            assertThat(volumeWork).isNotNull()
            assertThat(volumeWork?.weight).isEqualTo(70f) // 70% of 1RM
        }

    @Test
    fun `getIntelligentSuggestions generates rep range alternatives`() =
        runTest {
            // Given
            val progress =
                createProgress(
                    currentWeight = 100f,
                    avgRpe = 8f,
                    estimatedMax = 0f, // No 1RM data
                )
            coEvery { globalProgressDao.getProgressForExercise(100L) } returns progress

            // When
            val result =
                service.getIntelligentSuggestions(
                    exerciseVariationId = 100L,
                    targetReps = 5,
                )

            // Then
            val alternatives = result.alternativeSuggestions
            assertThat(alternatives).isNotEmpty()

            // Should have lower rep option (heavier)
            val lowerRep = alternatives.find { it.reps == 3 }
            assertThat(lowerRep).isNotNull()
            assertThat(lowerRep?.weight).isGreaterThan(100f)

            // Should have higher rep option (lighter)
            val higherRep = alternatives.find { it.reps == 8 }
            assertThat(higherRep).isNotNull()
            assertThat(higherRep?.weight).isLessThan(100f)
        }

    @Test
    fun `getSuggestionsForReps emits suggestions for each rep value`() =
        runTest {
            // Given
            val progress = createProgress(currentWeight = 100f, avgRpe = 8f)
            coEvery { globalProgressDao.getProgressForExercise(100L) } returns progress

            val repsFlow = flowOf(3, 5, 8)

            // When
            val results =
                service
                    .getSuggestionsForReps(
                        exerciseVariationId = 100L,
                        repsFlow = repsFlow,
                    ).toList()

            // Then
            assertThat(results).hasSize(3)
            assertThat(results[0].suggestedReps).isEqualTo(3)
            assertThat(results[1].suggestedReps).isEqualTo(5)
            assertThat(results[2].suggestedReps).isEqualTo(8)

            // Verify dao was called 3 times
            coVerify(exactly = 3) { globalProgressDao.getProgressForExercise(100L) }
        }

    @Test
    fun `roundToNearestPlate handles various weights correctly`() =
        runTest {
            // Given
            val progress1 = createProgress(currentWeight = 101f, avgRpe = 8f)
            val progress2 = createProgress(currentWeight = 102.4f, avgRpe = 8f)
            val progress3 = createProgress(currentWeight = 103.9f, avgRpe = 8f)

            coEvery { globalProgressDao.getProgressForExercise(101L) } returns progress1
            coEvery { globalProgressDao.getProgressForExercise(102L) } returns progress2
            coEvery { globalProgressDao.getProgressForExercise(103L) } returns progress3

            // When
            val result1 = service.getIntelligentSuggestions(101L, 5)
            val result2 = service.getIntelligentSuggestions(102L, 5)
            val result3 = service.getIntelligentSuggestions(103L, 5)

            // Then - weights should be rounded to nearest 2.5kg
            assertThat(result1.suggestedWeight % 2.5f).isEqualTo(0f)
            assertThat(result2.suggestedWeight % 2.5f).isEqualTo(0f)
            assertThat(result3.suggestedWeight % 2.5f).isEqualTo(0f)
        }

    @Test
    fun `analyzeRpeTrend categorizes RPE correctly`() =
        runTest {
            // Test TOO_EASY
            val progressEasy = createProgress(avgRpe = 6.5f)
            coEvery { globalProgressDao.getProgressForExercise(1L) } returns progressEasy
            val resultEasy = service.getIntelligentSuggestions(1L, 5)
            assertThat(resultEasy.reasoning).contains("capacity for more weight")

            // Test TOO_HARD
            val progressHard = createProgress(avgRpe = 9.5f)
            coEvery { globalProgressDao.getProgressForExercise(2L) } returns progressHard
            val resultHard = service.getIntelligentSuggestions(2L, 5)
            assertThat(resultHard.reasoning).contains("high effort")

            // Test OPTIMAL
            val progressOptimal = createProgress(avgRpe = 8f)
            coEvery { globalProgressDao.getProgressForExercise(3L) } returns progressOptimal
            val resultOptimal = service.getIntelligentSuggestions(3L, 5)
            assertThat(resultOptimal.suggestedRpe).isEqualTo(8f)

            // Test UNKNOWN
            val progressUnknown = createProgress(avgRpe = null)
            coEvery { globalProgressDao.getProgressForExercise(4L) } returns progressUnknown
            val resultUnknown = service.getIntelligentSuggestions(4L, 5)
            assertThat(resultUnknown.reasoning).doesNotContain("RPE")
        }

    @Test
    fun `confidence levels reflect data quality`() =
        runTest {
            // Test high confidence with many sessions and low RPE (progressive overload case)
            val progressMany = createProgress(sessionsTracked = 20, avgRpe = 6.5f)
            coEvery { globalProgressDao.getProgressForExercise(1L) } returns progressMany
            val resultMany = service.getIntelligentSuggestions(1L, 5)
            assertThat(resultMany.confidence).contains("High")
            assertThat(resultMany.confidence).contains("20 sessions")

            // Test medium confidence with trend data
            val progressTrend =
                createProgress(
                    sessionsTracked = 5,
                    avgRpe = 8f,
                    trend = ProgressTrend.IMPROVING,
                )
            coEvery { globalProgressDao.getProgressForExercise(2L) } returns progressTrend
            val resultTrend = service.getIntelligentSuggestions(2L, 5)
            assertThat(resultTrend.confidence).contains("Medium")

            // Test low confidence with no data
            coEvery { globalProgressDao.getProgressForExercise(3L) } returns null
            val resultNoData = service.getIntelligentSuggestions(3L, 5)
            assertThat(resultNoData.confidence).contains("Low")
        }

    @Test
    fun `weight adjustments respect percentage calculations`() =
        runTest {
            // Test 7.5% increase for very low RPE
            val progress1 = createProgress(currentWeight = 100f, avgRpe = 5.5f)
            coEvery { globalProgressDao.getProgressForExercise(1L) } returns progress1
            val result1 = service.getIntelligentSuggestions(1L, 5)
            assertThat(result1.suggestedWeight).isEqualTo(107.5f)

            // Test 5% increase for low RPE
            val progress2 = createProgress(currentWeight = 100f, avgRpe = 6.5f)
            coEvery { globalProgressDao.getProgressForExercise(2L) } returns progress2
            val result2 = service.getIntelligentSuggestions(2L, 5)
            assertThat(result2.suggestedWeight).isEqualTo(105f)

            // Test 2.5% standard progression
            val progress3 = createProgress(currentWeight = 100f, avgRpe = 7.5f)
            coEvery { globalProgressDao.getProgressForExercise(3L) } returns progress3
            val result3 = service.getIntelligentSuggestions(3L, 5)
            assertThat(result3.suggestedWeight).isEqualTo(102.5f)

            // Test 15% deload
            val progress4 = createProgress(currentWeight = 100f, consecutiveStalls = 3)
            coEvery { globalProgressDao.getProgressForExercise(4L) } returns progress4
            val result4 = service.getIntelligentSuggestions(4L, 5)
            assertThat(result4.suggestedWeight).isEqualTo(85f)

            // Test 5% decrease for declining
            val progress5 =
                createProgress(
                    currentWeight = 100f,
                    avgRpe = 8f,
                    trend = ProgressTrend.DECLINING,
                )
            coEvery { globalProgressDao.getProgressForExercise(5L) } returns progress5
            val result5 = service.getIntelligentSuggestions(5L, 5)
            assertThat(result5.suggestedWeight).isEqualTo(95f)
        }

    // Helper function to create test progress data
    private fun createProgress(
        currentWeight: Float = 100f,
        avgRpe: Float? = 8f,
        consecutiveStalls: Int = 0,
        trend: ProgressTrend = ProgressTrend.IMPROVING,
        estimatedMax: Float = 0f,
        sessionsTracked: Int = 10,
    ) = GlobalExerciseProgress(
        id = 1L,
        exerciseVariationId = 100L,
        currentWorkingWeight = currentWeight,
        estimatedMax = estimatedMax,
        recentAvgRpe = avgRpe,
        trend = trend,
        consecutiveStalls = consecutiveStalls,
        lastUpdated = LocalDateTime.now(),
        sessionsTracked = sessionsTracked,
        volumeTrend = null,
        totalVolumeLast30Days = 0f,
        bestSingleRep = null,
        best3Rep = null,
        best5Rep = null,
        best8Rep = null,
        lastSessionVolume = null,
        avgSessionVolume = null,
        weeksAtCurrentWeight = 0,
        lastProgressionDate = null,
        failureStreak = 0,
        lastProgrammeWorkoutId = null,
        lastFreestyleWorkoutId = null,
        lastPrDate = null,
        lastPrWeight = null,
    )
}
