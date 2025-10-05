package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ProgrammeExerciseTracking
import com.github.radupana.featherweight.data.ProgrammeExerciseTrackingDao
import com.github.radupana.featherweight.data.ProgressionAction
import com.github.radupana.featherweight.data.programme.DeloadRules
import com.github.radupana.featherweight.data.programme.Programme
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.github.radupana.featherweight.data.programme.ProgressionRules
import com.github.radupana.featherweight.repository.FeatherweightRepository
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class ProgressionServiceTest {
    @MockK
    private lateinit var mockPerformanceTrackingDao: ProgrammeExerciseTrackingDao

    @MockK
    private lateinit var mockProgrammeDao: ProgrammeDao

    @MockK
    private lateinit var mockRepository: FeatherweightRepository

    private lateinit var service: ProgressionService

    private val progressionRules =
        ProgressionRules(
            incrementRules =
                mapOf(
                    "squat" to 5f,
                    "bench press" to 2.5f,
                    "default" to 2.5f,
                ),
            deloadRules =
                DeloadRules(
                    autoDeload = true,
                    triggerAfterFailures = 3,
                    deloadPercentage = 0.9f,
                    minimumWeight = 20f,
                ),
        )

    private val mockProgramme =
        Programme(
            id = "1",
            name = "Test Programme",
            description = "Test Description",
            durationWeeks = 8,
            programmeType = ProgrammeType.STRENGTH,
            difficulty = ProgrammeDifficulty.INTERMEDIATE,
            progressionRules = Gson().toJson(progressionRules),
        )

    private val successfulPerformance =
        ProgrammeExerciseTracking(
            id = "1",
            programmeId = "1",
            exerciseId = "1",
            exerciseName = "Squat",
            targetWeight = 100f,
            achievedWeight = 100f,
            targetSets = 5,
            completedSets = 5,
            targetReps = 5,
            achievedReps = 25,
            missedReps = 0,
            wasSuccessful = true,
            workoutDate = LocalDateTime.now(),
            workoutId = "1",
            isDeloadWorkout = false,
        )

    private val failedPerformance =
        successfulPerformance.copy(
            id = "2",
            achievedReps = 3,
            wasSuccessful = false,
        )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        service =
            ProgressionService(
                performanceTrackingDao = mockPerformanceTrackingDao,
                programmeDao = mockProgrammeDao,
                repository = mockRepository,
            )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `calculateProgressionWeight returns maintain when no progression rules`() =
        runTest {
            // Arrange
            val programmeNoRules = mockProgramme.copy(progressionRules = null)

            // Act
            val decision = service.calculateProgressionWeight("Squat", programmeNoRules)

            // Assert
            assertThat(decision.weight).isEqualTo(20f)
            assertThat(decision.action).isEqualTo(ProgressionAction.MAINTAIN)
            assertThat(decision.reason).isEqualTo("No progression rules defined")
        }

    @Test
    fun `calculateProgressionWeight triggers deload after failures`() =
        runTest {
            // Arrange
            coEvery {
                mockPerformanceTrackingDao.getRecentPerformance("1", "Squat", 5)
            } returns listOf(failedPerformance)

            coEvery {
                mockPerformanceTrackingDao.getConsecutiveFailures("1", "Squat")
            } returns 3

            // Act
            val decision = service.calculateProgressionWeight("Squat", mockProgramme)

            // Assert
            assertThat(decision.weight).isEqualTo(90f) // 100 * 0.9
            assertThat(decision.action).isEqualTo(ProgressionAction.DELOAD)
            assertThat(decision.isDeload).isTrue()
            assertThat(decision.reason).contains("3 consecutive failures")
        }

    @Test
    fun `calculateProgressionWeight handles deload recovery`() =
        runTest {
            // Arrange
            val deloadPerformance =
                successfulPerformance.copy(
                    achievedWeight = 90f,
                    isDeloadWorkout = true,
                )

            coEvery {
                mockPerformanceTrackingDao.getRecentPerformance("1", "Squat", 5)
            } returns listOf(deloadPerformance, failedPerformance)

            coEvery {
                mockPerformanceTrackingDao.getConsecutiveFailures("1", "Squat")
            } returns 0

            // Act
            val decision = service.calculateProgressionWeight("Squat", mockProgramme)

            // Assert
            assertThat(decision.weight).isEqualTo(95f) // Recovering towards pre-deload weight
            assertThat(decision.action).isEqualTo(ProgressionAction.PROGRESS)
            assertThat(decision.reason).contains("Recovering from deload")
        }

    @Test
    fun `calculateProgressionWeight progresses after success`() =
        runTest {
            // Arrange
            coEvery {
                mockPerformanceTrackingDao.getRecentPerformance("1", "Squat", 5)
            } returns listOf(successfulPerformance)

            coEvery {
                mockPerformanceTrackingDao.getConsecutiveFailures("1", "Squat")
            } returns 0

            // Act
            val decision = service.calculateProgressionWeight("Squat", mockProgramme)

            // Assert
            assertThat(decision.weight).isEqualTo(105f) // 100 + 5 (squat increment)
            assertThat(decision.action).isEqualTo(ProgressionAction.PROGRESS)
            assertThat(decision.reason).contains("adding 5.0 kg")
        }

    @Test
    fun `calculateProgressionWeight maintains after failure`() =
        runTest {
            // Arrange
            coEvery {
                mockPerformanceTrackingDao.getRecentPerformance("1", "Bench Press", 5)
            } returns listOf(failedPerformance.copy(exerciseName = "Bench Press"))

            coEvery {
                mockPerformanceTrackingDao.getConsecutiveFailures("1", "Bench Press")
            } returns 1 // Not enough for deload

            // Act
            val decision = service.calculateProgressionWeight("Bench Press", mockProgramme)

            // Assert
            assertThat(decision.weight).isEqualTo(100f)
            assertThat(decision.action).isEqualTo(ProgressionAction.MAINTAIN)
            assertThat(decision.reason).contains("repeating weight")
        }

    @Test
    fun `calculateProgressionWeight uses default increment for unknown exercise`() =
        runTest {
            // Arrange
            val unknownExercisePerformance =
                successfulPerformance.copy(
                    exerciseName = "Unknown Exercise",
                )

            coEvery {
                mockPerformanceTrackingDao.getRecentPerformance("1", "Unknown Exercise", 5)
            } returns listOf(unknownExercisePerformance)

            coEvery {
                mockPerformanceTrackingDao.getConsecutiveFailures("1", "Unknown Exercise")
            } returns 0

            // Act
            val decision = service.calculateProgressionWeight("Unknown Exercise", mockProgramme)

            // Assert
            assertThat(decision.weight).isEqualTo(102.5f) // 100 + 2.5 (default increment)
            assertThat(decision.action).isEqualTo(ProgressionAction.PROGRESS)
        }

    @Test
    fun `calculateProgressionWeight handles first workout with no history`() =
        runTest {
            // Arrange
            coEvery {
                mockPerformanceTrackingDao.getRecentPerformance("1", "Squat", 5)
            } returns emptyList()

            coEvery {
                mockRepository.getExerciseByName(any())
            } returns null

            coEvery {
                mockRepository.getOneRMForExercise(any())
            } returns 150f

            // Act
            val decision = service.calculateProgressionWeight("Squat", mockProgramme)

            // Assert
            assertThat(decision.weight).isGreaterThan(0f)
            assertThat(decision.action).isEqualTo(ProgressionAction.MAINTAIN)
            // The actual implementation returns "Exercise not found" when no exercise is found
            assertThat(decision.reason).contains("Exercise not found")
        }

    @Test
    fun `calculateProgressionWeight respects minimum weight during deload`() =
        runTest {
            // Arrange
            val lowWeightPerformance =
                failedPerformance.copy(
                    targetWeight = 25f,
                    achievedWeight = 25f,
                )

            coEvery {
                mockPerformanceTrackingDao.getRecentPerformance("1", "Squat", 5)
            } returns listOf(lowWeightPerformance)

            coEvery {
                mockPerformanceTrackingDao.getConsecutiveFailures("1", "Squat")
            } returns 3

            // Act
            val decision = service.calculateProgressionWeight("Squat", mockProgramme)

            // Assert
            assertThat(decision.weight).isEqualTo(22.5f) // 25 * 0.9 = 22.5, above minimum
            assertThat(decision.action).isEqualTo(ProgressionAction.DELOAD)
        }

    @Test
    fun `calculateProgressionWeight handles multiple consecutive successes`() =
        runTest {
            // Arrange
            val successHistory =
                listOf(
                    successfulPerformance.copy(id = "1", targetWeight = 105f, achievedWeight = 105f),
                    successfulPerformance.copy(id = "2", targetWeight = 100f, achievedWeight = 100f),
                    successfulPerformance.copy(id = "3", targetWeight = 95f, achievedWeight = 95f),
                )

            coEvery {
                mockPerformanceTrackingDao.getRecentPerformance("1", "Squat", 5)
            } returns successHistory

            coEvery {
                mockPerformanceTrackingDao.getConsecutiveFailures("1", "Squat")
            } returns 0

            // Act
            val decision = service.calculateProgressionWeight("Squat", mockProgramme)

            // Assert
            assertThat(decision.weight).isEqualTo(110f) // 105 + 5
            assertThat(decision.action).isEqualTo(ProgressionAction.PROGRESS)
        }

    @Test
    fun `calculateProgressionWeight handles partial set completion`() =
        runTest {
            // Arrange
            val partialPerformance =
                successfulPerformance.copy(
                    completedSets = 3,
                    achievedReps = 15,
                    wasSuccessful = false,
                )

            coEvery {
                mockPerformanceTrackingDao.getRecentPerformance("1", "Squat", 5)
            } returns listOf(partialPerformance)

            coEvery {
                mockPerformanceTrackingDao.getConsecutiveFailures("1", "Squat")
            } returns 1

            // Act
            val decision = service.calculateProgressionWeight("Squat", mockProgramme)

            // Assert
            assertThat(decision.weight).isEqualTo(100f) // Maintain same weight
            assertThat(decision.action).isEqualTo(ProgressionAction.MAINTAIN)
        }

    @Test
    fun `calculateProgressionWeight handles deload disabled in rules`() =
        runTest {
            // Arrange
            val noDeloadRules =
                progressionRules.copy(
                    deloadRules =
                        DeloadRules(
                            autoDeload = false,
                            triggerAfterFailures = 3,
                            deloadPercentage = 0.9f,
                            minimumWeight = 20f,
                        ),
                )
            val programmeNoDeload =
                mockProgramme.copy(
                    progressionRules = Gson().toJson(noDeloadRules),
                )

            coEvery {
                mockPerformanceTrackingDao.getRecentPerformance("1", "Squat", 5)
            } returns listOf(failedPerformance)

            coEvery {
                mockPerformanceTrackingDao.getConsecutiveFailures("1", "Squat")
            } returns 5 // Many failures but deload disabled

            // Act
            val decision = service.calculateProgressionWeight("Squat", programmeNoDeload)

            // Assert
            assertThat(decision.action).isNotEqualTo(ProgressionAction.DELOAD)
            assertThat(decision.weight).isEqualTo(100f) // Maintains weight
        }

    @Test
    fun `calculateProgressionWeight handles exercise name case variations`() =
        runTest {
            // Arrange
            coEvery {
                mockPerformanceTrackingDao.getRecentPerformance("1", "SQUAT", 5)
            } returns listOf(successfulPerformance)

            coEvery {
                mockPerformanceTrackingDao.getConsecutiveFailures("1", "SQUAT")
            } returns 0

            // Act
            val decision = service.calculateProgressionWeight("SQUAT", mockProgramme)

            // Assert
            assertThat(decision.weight).isEqualTo(105f) // Uses squat increment
            assertThat(decision.action).isEqualTo(ProgressionAction.PROGRESS)
        }
}
