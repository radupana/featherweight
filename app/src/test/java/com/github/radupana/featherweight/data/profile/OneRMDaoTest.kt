package com.github.radupana.featherweight.data.profile

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class OneRMDaoTest {
    private lateinit var dao: OneRMDao
    private lateinit var mockMax: UserExerciseMax
    private lateinit var testDate: LocalDateTime

    @Before
    fun setup() {
        dao = mockk()
        testDate = LocalDateTime.of(2024, 1, 1, 10, 0)
        mockMax = UserExerciseMax(
            id = 1L,
            exerciseVariationId = 1L,
            mostWeightLifted = 90f,
            mostWeightReps = 5,
            mostWeightRpe = 8f,
            mostWeightDate = testDate,
            oneRMEstimate = 100f,
            oneRMContext = "90kg × 5 @ RPE 8",
            oneRMConfidence = 0.9f,
            oneRMDate = testDate,
            oneRMType = OneRMType.AUTOMATICALLY_CALCULATED,
            notes = "Test note"
        )
    }

    @Test
    fun `insertExerciseMax_validMax_returnsId`() = runTest {
        // Arrange
        coEvery { dao.insertExerciseMax(any()) } returns 1L

        // Act
        val result = dao.insertExerciseMax(mockMax)

        // Assert
        assertThat(result).isEqualTo(1L)
        coVerify(exactly = 1) { dao.insertExerciseMax(mockMax) }
    }

    @Test
    fun `updateExerciseMax_validMax_updatesSuccessfully`() = runTest {
        // Arrange
        val updatedMax = mockMax.copy(oneRMEstimate = 105f)
        coEvery { dao.updateExerciseMax(updatedMax) } just runs

        // Act
        dao.updateExerciseMax(updatedMax)

        // Assert
        coVerify(exactly = 1) { dao.updateExerciseMax(updatedMax) }
    }

    @Test
    fun `deleteExerciseMax_validMax_deletesSuccessfully`() = runTest {
        // Arrange
        coEvery { dao.deleteExerciseMax(mockMax) } just runs

        // Act
        dao.deleteExerciseMax(mockMax)

        // Assert
        coVerify(exactly = 1) { dao.deleteExerciseMax(mockMax) }
    }

    @Test
    fun `deleteAllMaxesForExercise_validIds_deletesAll`() = runTest {
        // Arrange
        coEvery { dao.deleteAllMaxesForExercise(1L) } just runs

        // Act
        dao.deleteAllMaxesForExercise(1L)

        // Assert
        coVerify(exactly = 1) { dao.deleteAllMaxesForExercise(1L) }
    }

    @Test
    fun `getCurrentMax_existingMax_returnsLatest`() = runTest {
        // Arrange
        coEvery { dao.getCurrentMax(1L) } returns mockMax

        // Act
        val result = dao.getCurrentMax(1L)

        // Assert
        assertThat(result).isEqualTo(mockMax)
        assertThat(result?.oneRMEstimate).isEqualTo(100f)
    }

    @Test
    fun `getCurrentMax_noMax_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getCurrentMax(999L) } returns null

        // Act
        val result = dao.getCurrentMax(999L)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getCurrentMaxFlow_existingMax_emitsMax`() = runTest {
        // Arrange
        val flow: Flow<UserExerciseMax?> = flowOf(mockMax)
        every { dao.getCurrentMaxFlow(1L) } returns flow

        // Act
        val result = dao.getCurrentMaxFlow(1L).first()

        // Assert
        assertThat(result).isEqualTo(mockMax)
    }

    @Test
    fun `getCurrentMaxFlow_noMax_emitsNull`() = runTest {
        // Arrange
        val flow: Flow<UserExerciseMax?> = flowOf(null)
        every { dao.getCurrentMaxFlow(999L) } returns flow

        // Act
        val result = dao.getCurrentMaxFlow(999L).first()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getCurrentOneRMEstimate_existingEstimate_returnsValue`() = runTest {
        // Arrange
        coEvery { dao.getCurrentOneRMEstimate(1L) } returns 100f

        // Act
        val result = dao.getCurrentOneRMEstimate(1L)

        // Assert
        assertThat(result).isEqualTo(100f)
    }

    @Test
    fun `getCurrentOneRMEstimate_noEstimate_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getCurrentOneRMEstimate(999L) } returns null

        // Act
        val result = dao.getCurrentOneRMEstimate(999L)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getCurrentMaxesForExercises_multipleExercises_returnsLatestForEach`() = runTest {
        // Arrange
        val maxes = listOf(
            mockMax,
            mockMax.copy(id = 2, exerciseVariationId = 2L, oneRMEstimate = 150f)
        )
        coEvery { dao.getCurrentMaxesForExercises(listOf(1L, 2L)) } returns maxes

        // Act
        val result = dao.getCurrentMaxesForExercises(listOf(1L, 2L))

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result[0].oneRMEstimate).isEqualTo(100f)
        assertThat(result[1].oneRMEstimate).isEqualTo(150f)
    }

    @Test
    fun `getCurrentMaxesForExercises_noMaxes_returnsEmptyList`() = runTest {
        // Arrange
        coEvery { dao.getCurrentMaxesForExercises(listOf(999L)) } returns emptyList()

        // Act
        val result = dao.getCurrentMaxesForExercises(listOf(999L))

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `getMaxHistory_hasHistory_returnsFlowOfList`() = runTest {
        // Arrange
        val history = listOf(
            mockMax,
            mockMax.copy(id = 2, oneRMDate = testDate.minusDays(7), oneRMEstimate = 95f),
            mockMax.copy(id = 3, oneRMDate = testDate.minusDays(14), oneRMEstimate = 90f)
        )
        val flow: Flow<List<UserExerciseMax>> = flowOf(history)
        every { dao.getMaxHistory(1L) } returns flow

        // Act
        val result = dao.getMaxHistory(1L).first()

        // Assert
        assertThat(result).hasSize(3)
        assertThat(result[0].oneRMEstimate).isEqualTo(100f)
        assertThat(result[1].oneRMEstimate).isEqualTo(95f)
        assertThat(result[2].oneRMEstimate).isEqualTo(90f)
    }

    @Test
    fun `getMaxHistory_noHistory_returnsEmptyFlow`() = runTest {
        // Arrange
        val flow: Flow<List<UserExerciseMax>> = flowOf(emptyList())
        every { dao.getMaxHistory(999L) } returns flow

        // Act
        val result = dao.getMaxHistory(999L).first()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `upsertExerciseMax_newMax_inserts`() = runTest {
        // Arrange
        coEvery { dao.upsertExerciseMax(1L, 110f, "Test note") } just runs

        // Act
        dao.upsertExerciseMax(1L, 110f, "Test note")

        // Assert
        coVerify(exactly = 1) { 
            dao.upsertExerciseMax(1L, 110f, "Test note") 
        }
    }
}
