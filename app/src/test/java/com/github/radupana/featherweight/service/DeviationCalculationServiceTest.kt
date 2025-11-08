package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ExerciseLog
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.Workout
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.programme.DeviationType
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.data.programme.ProgrammeWeek
import com.github.radupana.featherweight.data.programme.ProgrammeWorkout
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class DeviationCalculationServiceTest {
    private lateinit var service: DeviationCalculationService
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseLogDao: ExerciseLogDao
    private lateinit var setLogDao: SetLogDao
    private lateinit var programmeDao: ProgrammeDao

    @Before
    fun setup() {
        workoutDao = mockk()
        exerciseLogDao = mockk()
        setLogDao = mockk()
        programmeDao = mockk()
        service =
            DeviationCalculationService(
                workoutDao = workoutDao,
                exerciseLogDao = exerciseLogDao,
                setLogDao = setLogDao,
                programmeDao = programmeDao,
            )
    }

    @Test
    fun calculateDeviations_nonProgrammeWorkout_returnsEmptyList() =
        runTest {
            coEvery { workoutDao.getWorkoutById("workout1") } returns
                Workout(
                    id = "workout1",
                    date = LocalDateTime.now(),
                    isProgrammeWorkout = false,
                )

            val deviations = service.calculateDeviations("workout1")

            assertThat(deviations).isEmpty()
        }

    @Test
    fun calculateDeviations_noProgrammeId_returnsEmptyList() =
        runTest {
            coEvery { workoutDao.getWorkoutById("workout1") } returns
                Workout(
                    id = "workout1",
                    date = LocalDateTime.now(),
                    isProgrammeWorkout = true,
                    programmeId = null,
                )

            val deviations = service.calculateDeviations("workout1")

            assertThat(deviations).isEmpty()
        }

    @Test
    fun calculateDeviations_exerciseSwap_createsSwapDeviation() =
        runTest {
            setupProgrammeWorkout(
                workoutId = "workout1",
                programmeId = "prog1",
                weekNumber = 1,
                dayNumber = 1,
                workoutStructure =
                    """
                    {
                        "day": 1,
                        "name": "Upper A",
                        "exercises": [
                            {
                                "name": "Barbell Bench Press",
                                "sets": 3,
                                "reps": 5,
                                "weights": [100.0, 100.0, 100.0]
                            }
                        ]
                    }
                    """.trimIndent(),
            )

            coEvery { exerciseLogDao.getExerciseLogsForWorkout("workout1") } returns
                listOf(
                    ExerciseLog(
                        id = "ex1",
                        workoutId = "workout1",
                        exerciseId = "dumbbell-bench",
                        exerciseOrder = 0,
                        isSwapped = true,
                        originalExerciseId = "barbell-bench",
                    ),
                )

            coEvery { setLogDao.getSetLogsForExercise("ex1") } returns
                listOf(
                    SetLog(
                        id = "set1",
                        exerciseLogId = "ex1",
                        setOrder = 1,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 100f,
                        isCompleted = true,
                    ),
                )

            val deviations = service.calculateDeviations("workout1")

            val swapDeviation = deviations.find { it.deviationType == DeviationType.EXERCISE_SWAP }
            assertThat(swapDeviation).isNotNull()
            assertThat(swapDeviation!!.deviationMagnitude).isEqualTo(1.0f)
            assertThat(swapDeviation.exerciseLogId).isEqualTo("ex1")
        }

    @Test
    fun calculateDeviations_volumeDeviation_above5Percent_createsDeviation() =
        runTest {
            setupProgrammeWorkout(
                workoutId = "workout1",
                programmeId = "prog1",
                weekNumber = 1,
                dayNumber = 1,
                workoutStructure =
                    """
                    {
                        "day": 1,
                        "name": "Upper A",
                        "exercises": [
                            {
                                "name": "Barbell Bench Press",
                                "sets": 3,
                                "reps": 5,
                                "weights": [100.0, 100.0, 100.0]
                            }
                        ]
                    }
                    """.trimIndent(),
            )

            coEvery { exerciseLogDao.getExerciseLogsForWorkout("workout1") } returns
                listOf(
                    ExerciseLog(
                        id = "ex1",
                        workoutId = "workout1",
                        exerciseId = "bench",
                        exerciseOrder = 0,
                        isSwapped = false,
                    ),
                )

            coEvery { setLogDao.getSetLogsForExercise("ex1") } returns
                listOf(
                    SetLog(
                        id = "set1",
                        exerciseLogId = "ex1",
                        setOrder = 1,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 80f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set2",
                        exerciseLogId = "ex1",
                        setOrder = 2,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 80f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set3",
                        exerciseLogId = "ex1",
                        setOrder = 3,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 80f,
                        isCompleted = true,
                    ),
                )

            val deviations = service.calculateDeviations("workout1")

            val volumeDeviation = deviations.find { it.deviationType == DeviationType.VOLUME_DEVIATION }
            assertThat(volumeDeviation).isNotNull()
            assertThat(volumeDeviation!!.deviationMagnitude).isEqualTo(-0.2f)
        }

    @Test
    fun calculateDeviations_volumeDeviation_below5Percent_noDeviation() =
        runTest {
            setupProgrammeWorkout(
                workoutId = "workout1",
                programmeId = "prog1",
                weekNumber = 1,
                dayNumber = 1,
                workoutStructure =
                    """
                    {
                        "day": 1,
                        "name": "Upper A",
                        "exercises": [
                            {
                                "name": "Barbell Bench Press",
                                "sets": 3,
                                "reps": 5,
                                "weights": [100.0, 100.0, 100.0]
                            }
                        ]
                    }
                    """.trimIndent(),
            )

            coEvery { exerciseLogDao.getExerciseLogsForWorkout("workout1") } returns
                listOf(
                    ExerciseLog(
                        id = "ex1",
                        workoutId = "workout1",
                        exerciseId = "bench",
                        exerciseOrder = 0,
                        isSwapped = false,
                    ),
                )

            coEvery { setLogDao.getSetLogsForExercise("ex1") } returns
                listOf(
                    SetLog(
                        id = "set1",
                        exerciseLogId = "ex1",
                        setOrder = 1,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 98f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set2",
                        exerciseLogId = "ex1",
                        setOrder = 2,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 98f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set3",
                        exerciseLogId = "ex1",
                        setOrder = 3,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 98f,
                        isCompleted = true,
                    ),
                )

            val deviations = service.calculateDeviations("workout1")

            val volumeDeviation = deviations.find { it.deviationType == DeviationType.VOLUME_DEVIATION }
            assertThat(volumeDeviation).isNull()
        }

    @Test
    fun calculateDeviations_intensityDeviation_createsDeviation() =
        runTest {
            setupProgrammeWorkout(
                workoutId = "workout1",
                programmeId = "prog1",
                weekNumber = 1,
                dayNumber = 1,
                workoutStructure =
                    """
                    {
                        "day": 1,
                        "name": "Upper A",
                        "exercises": [
                            {
                                "name": "Barbell Bench Press",
                                "sets": 3,
                                "reps": 5,
                                "weights": [100.0, 100.0, 100.0]
                            }
                        ]
                    }
                    """.trimIndent(),
            )

            coEvery { exerciseLogDao.getExerciseLogsForWorkout("workout1") } returns
                listOf(
                    ExerciseLog(
                        id = "ex1",
                        workoutId = "workout1",
                        exerciseId = "bench",
                        exerciseOrder = 0,
                        isSwapped = false,
                    ),
                )

            coEvery { setLogDao.getSetLogsForExercise("ex1") } returns
                listOf(
                    SetLog(
                        id = "set1",
                        exerciseLogId = "ex1",
                        setOrder = 1,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 110f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set2",
                        exerciseLogId = "ex1",
                        setOrder = 2,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 110f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set3",
                        exerciseLogId = "ex1",
                        setOrder = 3,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 110f,
                        isCompleted = true,
                    ),
                )

            val deviations = service.calculateDeviations("workout1")

            val intensityDeviation = deviations.find { it.deviationType == DeviationType.INTENSITY_DEVIATION }
            assertThat(intensityDeviation).isNotNull()
            assertThat(intensityDeviation!!.deviationMagnitude).isWithin(0.001f).of(0.1f)
        }

    @Test
    fun calculateDeviations_setCountDeviation_createsDeviation() =
        runTest {
            setupProgrammeWorkout(
                workoutId = "workout1",
                programmeId = "prog1",
                weekNumber = 1,
                dayNumber = 1,
                workoutStructure =
                    """
                    {
                        "day": 1,
                        "name": "Upper A",
                        "exercises": [
                            {
                                "name": "Barbell Bench Press",
                                "sets": 3,
                                "reps": 5,
                                "weights": [100.0, 100.0, 100.0]
                            }
                        ]
                    }
                    """.trimIndent(),
            )

            coEvery { exerciseLogDao.getExerciseLogsForWorkout("workout1") } returns
                listOf(
                    ExerciseLog(
                        id = "ex1",
                        workoutId = "workout1",
                        exerciseId = "bench",
                        exerciseOrder = 0,
                        isSwapped = false,
                    ),
                )

            coEvery { setLogDao.getSetLogsForExercise("ex1") } returns
                listOf(
                    SetLog(
                        id = "set1",
                        exerciseLogId = "ex1",
                        setOrder = 1,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 100f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set2",
                        exerciseLogId = "ex1",
                        setOrder = 2,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 100f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set3",
                        exerciseLogId = "ex1",
                        setOrder = 3,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 100f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set4",
                        exerciseLogId = "ex1",
                        setOrder = 4,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 100f,
                        isCompleted = true,
                    ),
                )

            val deviations = service.calculateDeviations("workout1")

            val setCountDeviation = deviations.find { it.deviationType == DeviationType.SET_COUNT_DEVIATION }
            assertThat(setCountDeviation).isNotNull()
            assertThat(setCountDeviation!!.deviationMagnitude).isWithin(0.001f).of(0.333f)
        }

    @Test
    fun calculateDeviations_repDeviation_createsDeviation() =
        runTest {
            setupProgrammeWorkout(
                workoutId = "workout1",
                programmeId = "prog1",
                weekNumber = 1,
                dayNumber = 1,
                workoutStructure =
                    """
                    {
                        "day": 1,
                        "name": "Upper A",
                        "exercises": [
                            {
                                "name": "Barbell Bench Press",
                                "sets": 3,
                                "reps": 5,
                                "weights": [100.0, 100.0, 100.0]
                            }
                        ]
                    }
                    """.trimIndent(),
            )

            coEvery { exerciseLogDao.getExerciseLogsForWorkout("workout1") } returns
                listOf(
                    ExerciseLog(
                        id = "ex1",
                        workoutId = "workout1",
                        exerciseId = "bench",
                        exerciseOrder = 0,
                        isSwapped = false,
                    ),
                )

            coEvery { setLogDao.getSetLogsForExercise("ex1") } returns
                listOf(
                    SetLog(
                        id = "set1",
                        exerciseLogId = "ex1",
                        setOrder = 1,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 3,
                        actualWeight = 100f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set2",
                        exerciseLogId = "ex1",
                        setOrder = 2,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 3,
                        actualWeight = 100f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set3",
                        exerciseLogId = "ex1",
                        setOrder = 3,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 3,
                        actualWeight = 100f,
                        isCompleted = true,
                    ),
                )

            val deviations = service.calculateDeviations("workout1")

            val repDeviation = deviations.find { it.deviationType == DeviationType.REP_DEVIATION }
            assertThat(repDeviation).isNotNull()
            assertThat(repDeviation!!.deviationMagnitude).isWithin(0.001f).of(-0.4f)
        }

    @Test
    fun calculateDeviations_rpeDeviation_createsDeviation() =
        runTest {
            setupProgrammeWorkout(
                workoutId = "workout1",
                programmeId = "prog1",
                weekNumber = 1,
                dayNumber = 1,
                workoutStructure =
                    """
                    {
                        "day": 1,
                        "name": "Upper A",
                        "exercises": [
                            {
                                "name": "Barbell Bench Press",
                                "sets": 3,
                                "reps": 5,
                                "weights": [100.0, 100.0, 100.0],
                                "rpeValues": [7.0, 7.0, 8.0]
                            }
                        ]
                    }
                    """.trimIndent(),
            )

            coEvery { exerciseLogDao.getExerciseLogsForWorkout("workout1") } returns
                listOf(
                    ExerciseLog(
                        id = "ex1",
                        workoutId = "workout1",
                        exerciseId = "bench",
                        exerciseOrder = 0,
                        isSwapped = false,
                    ),
                )

            coEvery { setLogDao.getSetLogsForExercise("ex1") } returns
                listOf(
                    SetLog(
                        id = "set1",
                        exerciseLogId = "ex1",
                        setOrder = 1,
                        targetReps = 5,
                        targetWeight = 100f,
                        targetRpe = 7f,
                        actualReps = 5,
                        actualWeight = 100f,
                        actualRpe = 9f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set2",
                        exerciseLogId = "ex1",
                        setOrder = 2,
                        targetReps = 5,
                        targetWeight = 100f,
                        targetRpe = 7f,
                        actualReps = 5,
                        actualWeight = 100f,
                        actualRpe = 9f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set3",
                        exerciseLogId = "ex1",
                        setOrder = 3,
                        targetReps = 5,
                        targetWeight = 100f,
                        targetRpe = 8f,
                        actualReps = 5,
                        actualWeight = 100f,
                        actualRpe = 9f,
                        isCompleted = true,
                    ),
                )

            val deviations = service.calculateDeviations("workout1")

            val rpeDeviation = deviations.find { it.deviationType == DeviationType.RPE_DEVIATION }
            assertThat(rpeDeviation).isNotNull()
            assertThat(rpeDeviation!!.deviationMagnitude).isWithin(0.001f).of(0.227f)
        }

    @Test
    fun calculateDeviations_multipleDeviations_createsAllDeviations() =
        runTest {
            setupProgrammeWorkout(
                workoutId = "workout1",
                programmeId = "prog1",
                weekNumber = 1,
                dayNumber = 1,
                workoutStructure =
                    """
                    {
                        "day": 1,
                        "name": "Upper A",
                        "exercises": [
                            {
                                "name": "Barbell Bench Press",
                                "sets": 3,
                                "reps": 5,
                                "weights": [100.0, 100.0, 100.0]
                            }
                        ]
                    }
                    """.trimIndent(),
            )

            coEvery { exerciseLogDao.getExerciseLogsForWorkout("workout1") } returns
                listOf(
                    ExerciseLog(
                        id = "ex1",
                        workoutId = "workout1",
                        exerciseId = "dumbbell-bench",
                        exerciseOrder = 0,
                        isSwapped = true,
                        originalExerciseId = "barbell-bench",
                    ),
                )

            coEvery { setLogDao.getSetLogsForExercise("ex1") } returns
                listOf(
                    SetLog(
                        id = "set1",
                        exerciseLogId = "ex1",
                        setOrder = 1,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 3,
                        actualWeight = 80f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set2",
                        exerciseLogId = "ex1",
                        setOrder = 2,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 3,
                        actualWeight = 80f,
                        isCompleted = true,
                    ),
                )

            val deviations = service.calculateDeviations("workout1")

            assertThat(deviations.map { it.deviationType })
                .containsAtLeast(
                    DeviationType.EXERCISE_SWAP,
                    DeviationType.VOLUME_DEVIATION,
                    DeviationType.INTENSITY_DEVIATION,
                    DeviationType.SET_COUNT_DEVIATION,
                    DeviationType.REP_DEVIATION,
                )
        }

    @Test
    fun calculateDeviations_incompleteSets_ignoresIncompleteSets() =
        runTest {
            setupProgrammeWorkout(
                workoutId = "workout1",
                programmeId = "prog1",
                weekNumber = 1,
                dayNumber = 1,
                workoutStructure =
                    """
                    {
                        "day": 1,
                        "name": "Upper A",
                        "exercises": [
                            {
                                "name": "Barbell Bench Press",
                                "sets": 3,
                                "reps": 5,
                                "weights": [100.0, 100.0, 100.0]
                            }
                        ]
                    }
                    """.trimIndent(),
            )

            coEvery { exerciseLogDao.getExerciseLogsForWorkout("workout1") } returns
                listOf(
                    ExerciseLog(
                        id = "ex1",
                        workoutId = "workout1",
                        exerciseId = "bench",
                        exerciseOrder = 0,
                        isSwapped = false,
                    ),
                )

            coEvery { setLogDao.getSetLogsForExercise("ex1") } returns
                listOf(
                    SetLog(
                        id = "set1",
                        exerciseLogId = "ex1",
                        setOrder = 1,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 5,
                        actualWeight = 100f,
                        isCompleted = true,
                    ),
                    SetLog(
                        id = "set2",
                        exerciseLogId = "ex1",
                        setOrder = 2,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 0,
                        actualWeight = 0f,
                        isCompleted = false,
                    ),
                    SetLog(
                        id = "set3",
                        exerciseLogId = "ex1",
                        setOrder = 3,
                        targetReps = 5,
                        targetWeight = 100f,
                        actualReps = 0,
                        actualWeight = 0f,
                        isCompleted = false,
                    ),
                )

            val deviations = service.calculateDeviations("workout1")

            val setCountDeviation = deviations.find { it.deviationType == DeviationType.SET_COUNT_DEVIATION }
            assertThat(setCountDeviation).isNotNull()
            assertThat(setCountDeviation!!.deviationMagnitude).isWithin(0.001f).of(-0.666f)
        }

    private fun setupProgrammeWorkout(
        workoutId: String,
        programmeId: String,
        weekNumber: Int,
        dayNumber: Int,
        workoutStructure: String,
    ) {
        coEvery { workoutDao.getWorkoutById(workoutId) } returns
            Workout(
                id = workoutId,
                date = LocalDateTime.now(),
                isProgrammeWorkout = true,
                programmeId = programmeId,
                weekNumber = weekNumber,
                dayNumber = dayNumber,
            )

        coEvery { programmeDao.getWeekById("week1") } returns
            ProgrammeWeek(
                id = "week1",
                programmeId = programmeId,
                weekNumber = weekNumber,
                name = "Week $weekNumber",
                description = null,
            )

        coEvery { programmeDao.getAllWorkoutsForProgramme(programmeId) } returns
            listOf(
                ProgrammeWorkout(
                    id = "pw1",
                    weekId = "week1",
                    dayNumber = dayNumber,
                    name = "Day $dayNumber",
                    description = null,
                    estimatedDuration = null,
                    workoutStructure = workoutStructure,
                ),
            )
    }
}
