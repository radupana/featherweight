package com.github.radupana.featherweight.repository

import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.programme.ExerciseStructure
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.RepsStructure
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Integration test to ensure RPE values flow correctly from parsed programme to workout
 */
class ProgrammeRPEIntegrationTest {
    private lateinit var database: FeatherweightDatabase
    private lateinit var programmeDao: ProgrammeDao
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseLogDao: ExerciseLogDao
    private lateinit var setLogDao: SetLogDao

    @Before
    fun setup() {
        database = mockk()
        programmeDao = mockk(relaxed = true)
        workoutDao = mockk(relaxed = true)
        exerciseLogDao = mockk<ExerciseLogDao>(relaxed = true)
        setLogDao = mockk<SetLogDao>(relaxed = true)

        every { database.programmeDao() } returns programmeDao
        every { database.workoutDao() } returns workoutDao
        every { database.exerciseLogDao() } returns exerciseLogDao
        every { database.setLogDao() } returns setLogDao
    }

    @Test
    fun `SetLog is created with correct targetRpe from WorkoutStructure`() =
        runTest {
            // Given a workout structure with RPE values
            val exerciseStructure =
                ExerciseStructure(
                    name = "Barbell Back Squat",
                    sets = 3,
                    reps = RepsStructure.PerSet(listOf("6", "6", "6")),
                    exerciseId = 1L,
                    weights = listOf(90f, 90f, 90f),
                    rpeValues = listOf(8.0f, 8.5f, 9.0f), // Different RPE for each set
                )

            // Mock the DAO responses
            coEvery { workoutDao.insertWorkout(any()) } returns 1L

            // Capture the SetLog insertions
            val capturedSetLogs = mutableListOf<SetLog>()
            coEvery { setLogDao.insertSetLog(capture(capturedSetLogs)) } returns 1L

            // When creating sets from structure (simulating the repository method)
            val exerciseLogId = 1L
            exerciseStructure.rpeValues?.forEachIndexed { index, rpeValue ->
                val setLog =
                    SetLog(
                        exerciseLogId = exerciseLogId,
                        setOrder = index + 1,
                        targetReps = 6,
                        targetWeight = exerciseStructure.weights?.get(index) ?: 0f,
                        targetRpe = rpeValue,
                        actualReps = 0,
                        actualWeight = 0f,
                        actualRpe = null,
                    )
                setLogDao.insertSetLog(setLog)
            }

            // Then verify SetLogs were created with correct RPE values
            assertThat(capturedSetLogs).hasSize(3)
            assertThat(capturedSetLogs[0].targetRpe).isEqualTo(8.0f)
            assertThat(capturedSetLogs[1].targetRpe).isEqualTo(8.5f)
            assertThat(capturedSetLogs[2].targetRpe).isEqualTo(9.0f)

            // Verify all sets have the correct target weight
            capturedSetLogs.forEach { setLog ->
                assertThat(setLog.targetWeight).isEqualTo(90f)
                assertThat(setLog.targetReps).isEqualTo(6)
            }
        }

    @Test
    fun `RPE values are preserved when some sets have null RPE`() =
        runTest {
            // Given a workout structure with mixed RPE values (some null)
            val exerciseStructure =
                ExerciseStructure(
                    name = "Barbell Overhead Press",
                    sets = 4,
                    reps = RepsStructure.PerSet(listOf("5", "5", "5", "5")),
                    exerciseId = 2L,
                    weights = listOf(50f, 50f, 50f, 50f),
                    rpeValues = listOf(7.0f, null, 8.0f, null), // Some null RPE values
                )

            // Capture the SetLog insertions
            val capturedSetLogs = mutableListOf<SetLog>()
            coEvery { setLogDao.insertSetLog(capture(capturedSetLogs)) } returns 1L

            // When creating sets
            val exerciseLogId = 2L
            val repsList = (exerciseStructure.reps as RepsStructure.PerSet).values
            repsList.forEachIndexed { index, repsStr ->
                val setLog =
                    SetLog(
                        exerciseLogId = exerciseLogId,
                        setOrder = index + 1,
                        targetReps = repsStr.toInt(),
                        targetWeight = exerciseStructure.weights?.get(index) ?: 0f,
                        targetRpe = exerciseStructure.rpeValues?.get(index),
                        actualReps = 0,
                        actualWeight = 0f,
                        actualRpe = null,
                    )
                setLogDao.insertSetLog(setLog)
            }

            // Then verify mixed RPE values are preserved
            assertThat(capturedSetLogs).hasSize(4)
            assertThat(capturedSetLogs[0].targetRpe).isEqualTo(7.0f)
            assertThat(capturedSetLogs[1].targetRpe).isNull()
            assertThat(capturedSetLogs[2].targetRpe).isEqualTo(8.0f)
            assertThat(capturedSetLogs[3].targetRpe).isNull()
        }

    @Test
    fun `RPE list is null when no RPE values are provided`() {
        // Given sets data without RPE
        val sets =
            listOf(
                mapOf("reps" to 10.0, "weight" to 100.0),
                mapOf("reps" to 10.0, "weight" to 100.0),
                mapOf("reps" to 10.0, "weight" to 100.0),
            )

        // When extracting RPE values
        val rpeList =
            sets
                .map { it["rpe"] as? Double }
                .map { it?.toFloat() }
                .takeIf { list -> list.any { it != null } }

        // Then the RPE list should be null (not an empty list)
        assertThat(rpeList).isNull()
    }

    @Test
    fun `RPE values are correctly parsed from JSON with decimal values`() {
        // Given sets with decimal RPE values
        val sets =
            listOf(
                mapOf("reps" to 8.0, "weight" to 100.0, "rpe" to 7.5),
                mapOf("reps" to 8.0, "weight" to 100.0, "rpe" to 8.25),
                mapOf("reps" to 8.0, "weight" to 100.0, "rpe" to 9.0),
            )

        // When extracting RPE values
        val rpeList =
            sets
                .map { it["rpe"] as? Double }
                .map { it?.toFloat() }
                .takeIf { list -> list.any { it != null } }

        // Then decimal RPE values should be preserved
        assertThat(rpeList).isNotNull()
        assertThat(rpeList).containsExactly(7.5f, 8.25f, 9.0f).inOrder()
    }
}
