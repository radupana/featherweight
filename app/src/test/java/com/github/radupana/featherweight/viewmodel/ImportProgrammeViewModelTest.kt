package com.github.radupana.featherweight.viewmodel

import com.github.radupana.featherweight.data.ParsedExercise
import com.github.radupana.featherweight.data.ParsedProgramme
import com.github.radupana.featherweight.data.ParsedSet
import com.github.radupana.featherweight.data.ParsedWeek
import com.github.radupana.featherweight.data.ParsedWorkout
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ImportProgrammeViewModelTest {
    
    @Test
    fun `ParsedWorkout preserves matchedExerciseId when updated`() {
        val originalExercise1 = ParsedExercise(
            exerciseName = "Barbell Back Squat",
            matchedExerciseId = 1L,
            sets = listOf(
                ParsedSet(reps = 5, weight = 100f, rpe = 8f)
            ),
            notes = "Original note"
        )
        
        val originalExercise2 = ParsedExercise(
            exerciseName = "Barbell Bench Press",
            matchedExerciseId = 4L,
            sets = listOf(
                ParsedSet(reps = 8, weight = 80f, rpe = 7f)
            ),
            notes = null
        )
        
        val originalWorkout = ParsedWorkout(
            dayOfWeek = "Monday",
            name = "Day 1",
            exercises = listOf(originalExercise1, originalExercise2),
            estimatedDurationMinutes = 60
        )
        
        val updatedExercise1 = originalExercise1.copy(
            sets = listOf(
                ParsedSet(reps = 3, weight = 110f, rpe = null),
                ParsedSet(reps = 3, weight = 110f, rpe = null)
            ),
            notes = "Updated note"
        )
        
        val updatedExercise2 = originalExercise2.copy(
            sets = listOf(
                ParsedSet(reps = 10, weight = 75f, rpe = null)
            ),
            notes = "New note"
        )
        
        val updatedWorkout = originalWorkout.copy(
            dayOfWeek = null,
            name = "Day 1 - Updated",
            exercises = listOf(updatedExercise1, updatedExercise2),
            estimatedDurationMinutes = 45
        )
        
        assertThat(updatedWorkout.exercises).hasSize(2)
        
        val resultExercise1 = updatedWorkout.exercises[0]
        assertThat(resultExercise1.exerciseName).isEqualTo("Barbell Back Squat")
        assertThat(resultExercise1.matchedExerciseId).isEqualTo(1L)
        assertThat(resultExercise1.sets).hasSize(2)
        assertThat(resultExercise1.notes).isEqualTo("Updated note")
        
        val resultExercise2 = updatedWorkout.exercises[1]
        assertThat(resultExercise2.exerciseName).isEqualTo("Barbell Bench Press")
        assertThat(resultExercise2.matchedExerciseId).isEqualTo(4L)
        assertThat(resultExercise2.sets).hasSize(1)
        assertThat(resultExercise2.notes).isEqualTo("New note")
    }
    
    @Test
    fun `ParsedExercise handles null matchedExerciseId correctly`() {
        val unmatchedExercise = ParsedExercise(
            exerciseName = "Custom Exercise",
            matchedExerciseId = null,
            sets = listOf(
                ParsedSet(reps = 12, weight = 50f, rpe = null)
            ),
            notes = "Custom exercise note"
        )
        
        assertThat(unmatchedExercise.matchedExerciseId).isNull()
        
        val updated = unmatchedExercise.copy(
            sets = listOf(
                ParsedSet(reps = 15, weight = 45f, rpe = null)
            )
        )
        
        assertThat(updated.matchedExerciseId).isNull()
        assertThat(updated.exerciseName).isEqualTo("Custom Exercise")
    }
    
    @Test
    fun `ParsedProgramme structure preserves exercise IDs through deep copy`() {
        val exercise1 = ParsedExercise(
            exerciseName = "Barbell Back Squat",
            matchedExerciseId = 1L,
            sets = listOf(
                ParsedSet(reps = 1, weight = 117.5f, rpe = 7f),
                ParsedSet(reps = 5, weight = 95f, rpe = 7.5f),
                ParsedSet(reps = 5, weight = 95f, rpe = 7.5f),
                ParsedSet(reps = 5, weight = 95f, rpe = 7.5f)
            ),
            notes = null
        )
        
        val exercise2 = ParsedExercise(
            exerciseName = "Barbell Bench Press",
            matchedExerciseId = 4L,
            sets = listOf(
                ParsedSet(reps = 8, weight = 77.5f, rpe = 8f),
                ParsedSet(reps = 8, weight = 77.5f, rpe = 8f),
                ParsedSet(reps = 8, weight = 77.5f, rpe = 8f),
                ParsedSet(reps = 8, weight = 77.5f, rpe = 8f)
            ),
            notes = null
        )
        
        val workout = ParsedWorkout(
            dayOfWeek = "Monday",
            name = "Day 1",
            exercises = listOf(exercise1, exercise2),
            estimatedDurationMinutes = 60
        )
        
        val week = ParsedWeek(
            weekNumber = 1,
            name = "Week 1",
            workouts = listOf(workout),
            description = null,
            focusAreas = null,
            intensityLevel = null,
            volumeLevel = null,
            isDeload = false,
            phase = null
        )
        
        val programme = ParsedProgramme(
            name = "Test Programme",
            description = "Test",
            durationWeeks = 1,
            programmeType = "STRENGTH",
            difficulty = "INTERMEDIATE",
            weeks = listOf(week),
            rawText = "Raw text"
        )
        
        val firstExercise = programme.weeks[0].workouts[0].exercises[0]
        assertThat(firstExercise.matchedExerciseId).isEqualTo(1L)
        
        val secondExercise = programme.weeks[0].workouts[0].exercises[1]
        assertThat(secondExercise.matchedExerciseId).isEqualTo(4L)
        
        val modifiedWeeks = programme.weeks.toMutableList()
        val modifiedWorkouts = modifiedWeeks[0].workouts.toMutableList()
        val modifiedExercises = modifiedWorkouts[0].exercises.map { exercise ->
            exercise.copy(
                sets = exercise.sets.map { set ->
                    set.copy(rpe = null)
                }
            )
        }
        
        modifiedWorkouts[0] = modifiedWorkouts[0].copy(
            exercises = modifiedExercises,
            dayOfWeek = null
        )
        modifiedWeeks[0] = modifiedWeeks[0].copy(workouts = modifiedWorkouts)
        
        val modifiedProgramme = programme.copy(weeks = modifiedWeeks)
        
        val modifiedFirstExercise = modifiedProgramme.weeks[0].workouts[0].exercises[0]
        assertThat(modifiedFirstExercise.matchedExerciseId).isEqualTo(1L)
        assertThat(modifiedFirstExercise.sets[0].rpe).isNull()
        
        val modifiedSecondExercise = modifiedProgramme.weeks[0].workouts[0].exercises[1]
        assertThat(modifiedSecondExercise.matchedExerciseId).isEqualTo(4L)
        assertThat(modifiedProgramme.weeks[0].workouts[0].dayOfWeek).isNull()
    }
    
    @Test
    fun `MainActivity template save simulation preserves matchedExerciseId`() {
        data class MockExerciseLog(
            val id: Long,
            val exerciseVariationId: Long,
            val notes: String?
        )
        
        data class MockSetLog(
            val exerciseLogId: Long,
            val targetReps: Int,
            val targetWeight: Float
        )
        
        val exercises = listOf(
            MockExerciseLog(id = -1L, exerciseVariationId = 1L, notes = null),
            MockExerciseLog(id = -2L, exerciseVariationId = 4L, notes = "Bench note"),
            MockExerciseLog(id = -3L, exerciseVariationId = 27L, notes = null),
            MockExerciseLog(id = -4L, exerciseVariationId = 30L, notes = null)
        )
        
        val sets = listOf(
            MockSetLog(exerciseLogId = -1L, targetReps = 1, targetWeight = 117.5f),
            MockSetLog(exerciseLogId = -1L, targetReps = 5, targetWeight = 95f),
            MockSetLog(exerciseLogId = -1L, targetReps = 5, targetWeight = 95f),
            MockSetLog(exerciseLogId = -1L, targetReps = 5, targetWeight = 95f),
            MockSetLog(exerciseLogId = -2L, targetReps = 8, targetWeight = 77.5f),
            MockSetLog(exerciseLogId = -2L, targetReps = 8, targetWeight = 77.5f),
            MockSetLog(exerciseLogId = -2L, targetReps = 8, targetWeight = 77.5f),
            MockSetLog(exerciseLogId = -2L, targetReps = 8, targetWeight = 77.5f),
            MockSetLog(exerciseLogId = -3L, targetReps = 8, targetWeight = 77.5f),
            MockSetLog(exerciseLogId = -3L, targetReps = 8, targetWeight = 77.5f),
            MockSetLog(exerciseLogId = -3L, targetReps = 8, targetWeight = 77.5f),
            MockSetLog(exerciseLogId = -3L, targetReps = 8, targetWeight = 77.5f),
            MockSetLog(exerciseLogId = -4L, targetReps = 10, targetWeight = 42.5f),
            MockSetLog(exerciseLogId = -4L, targetReps = 10, targetWeight = 42.5f),
            MockSetLog(exerciseLogId = -4L, targetReps = 10, targetWeight = 42.5f)
        )
        
        val exerciseNames = mapOf(
            1L to "Barbell Back Squat",
            4L to "Barbell Bench Press",
            27L to "Barbell Underhand Row",
            30L to "Barbell Curl"
        )
        
        val parsedExercises = exercises.map { exerciseLog ->
            val exerciseSets = sets.filter { it.exerciseLogId == exerciseLog.id }
            ParsedExercise(
                exerciseName = exerciseNames[exerciseLog.exerciseVariationId] ?: "Unknown Exercise",
                matchedExerciseId = exerciseLog.exerciseVariationId,
                sets = exerciseSets.map { setLog ->
                    ParsedSet(
                        reps = setLog.targetReps,
                        weight = setLog.targetWeight,
                        rpe = null
                    )
                },
                notes = exerciseLog.notes
            )
        }
        
        assertThat(parsedExercises).hasSize(4)
        
        assertThat(parsedExercises[0].matchedExerciseId).isEqualTo(1L)
        assertThat(parsedExercises[0].exerciseName).isEqualTo("Barbell Back Squat")
        assertThat(parsedExercises[0].sets).hasSize(4)
        
        assertThat(parsedExercises[1].matchedExerciseId).isEqualTo(4L)
        assertThat(parsedExercises[1].exerciseName).isEqualTo("Barbell Bench Press")
        assertThat(parsedExercises[1].notes).isEqualTo("Bench note")
        
        assertThat(parsedExercises[2].matchedExerciseId).isEqualTo(27L)
        assertThat(parsedExercises[3].matchedExerciseId).isEqualTo(30L)
    }
    
    @Test
    fun `ParsedProgramme handles empty weeks and workouts`() {
        val emptyProgramme = ParsedProgramme(
            name = "Empty Programme",
            description = "",
            durationWeeks = 0,
            programmeType = "GENERAL_FITNESS",
            difficulty = "BEGINNER",
            weeks = emptyList(),
            rawText = ""
        )
        
        assertThat(emptyProgramme.weeks).isEmpty()
        assertThat(emptyProgramme.durationWeeks).isEqualTo(0)
        
        val weekWithNoWorkouts = ParsedWeek(
            weekNumber = 1,
            name = "Empty Week",
            workouts = emptyList(),
            description = null,
            focusAreas = null,
            intensityLevel = null,
            volumeLevel = null,
            isDeload = false,
            phase = null
        )
        
        val programmeWithEmptyWeek = emptyProgramme.copy(
            weeks = listOf(weekWithNoWorkouts),
            durationWeeks = 1
        )
        
        assertThat(programmeWithEmptyWeek.weeks).hasSize(1)
        assertThat(programmeWithEmptyWeek.weeks[0].workouts).isEmpty()
    }
    
    @Test
    fun `ParsedWorkout handles exercise order preservation`() {
        val exercises = listOf(
            ParsedExercise(
                exerciseName = "Squat",
                matchedExerciseId = 1L,
                sets = listOf(ParsedSet(5, 100f, null)),
                notes = null
            ),
            ParsedExercise(
                exerciseName = "Bench",
                matchedExerciseId = 2L,
                sets = listOf(ParsedSet(5, 80f, null)),
                notes = null
            ),
            ParsedExercise(
                exerciseName = "Row",
                matchedExerciseId = 3L,
                sets = listOf(ParsedSet(5, 70f, null)),
                notes = null
            )
        )
        
        val workout = ParsedWorkout(
            dayOfWeek = "Monday",
            name = "Push Day",
            exercises = exercises,
            estimatedDurationMinutes = 60
        )
        
        assertThat(workout.exercises).hasSize(3)
        assertThat(workout.exercises[0].exerciseName).isEqualTo("Squat")
        assertThat(workout.exercises[1].exerciseName).isEqualTo("Bench")
        assertThat(workout.exercises[2].exerciseName).isEqualTo("Row")
        
        val reorderedExercises = listOf(exercises[2], exercises[0], exercises[1])
        val reorderedWorkout = workout.copy(exercises = reorderedExercises)
        
        assertThat(reorderedWorkout.exercises[0].exerciseName).isEqualTo("Row")
        assertThat(reorderedWorkout.exercises[1].exerciseName).isEqualTo("Squat")
        assertThat(reorderedWorkout.exercises[2].exerciseName).isEqualTo("Bench")
        
        assertThat(reorderedWorkout.exercises[0].matchedExerciseId).isEqualTo(3L)
        assertThat(reorderedWorkout.exercises[1].matchedExerciseId).isEqualTo(1L)
        assertThat(reorderedWorkout.exercises[2].matchedExerciseId).isEqualTo(2L)
    }
    
    @Test
    fun `ParsedSet handles all field combinations`() {
        val fullSet = ParsedSet(reps = 10, weight = 50f, rpe = 8f)
        assertThat(fullSet.reps).isEqualTo(10)
        assertThat(fullSet.weight).isEqualTo(50f)
        assertThat(fullSet.rpe).isEqualTo(8f)
        
        val noRpeSet = ParsedSet(reps = 5, weight = 100f, rpe = null)
        assertThat(noRpeSet.reps).isEqualTo(5)
        assertThat(noRpeSet.weight).isEqualTo(100f)
        assertThat(noRpeSet.rpe).isNull()
        
        val bodyweightSet = ParsedSet(reps = 20, weight = 0f, rpe = null)
        assertThat(bodyweightSet.reps).isEqualTo(20)
        assertThat(bodyweightSet.weight).isEqualTo(0f)
        
        val modifiedSet = fullSet.copy(reps = 12, rpe = null)
        assertThat(modifiedSet.reps).isEqualTo(12)
        assertThat(modifiedSet.weight).isEqualTo(50f)
        assertThat(modifiedSet.rpe).isNull()
    }
    
    @Test
    fun `ParsedWeek handles all optional fields`() {
        val fullWeek = ParsedWeek(
            weekNumber = 1,
            name = "Week 1",
            workouts = emptyList(),
            description = "Heavy week",
            focusAreas = "Strength",
            intensityLevel = "High",
            volumeLevel = "Medium",
            isDeload = false,
            phase = "Accumulation"
        )
        
        assertThat(fullWeek.description).isEqualTo("Heavy week")
        assertThat(fullWeek.focusAreas).isEqualTo("Strength")
        assertThat(fullWeek.intensityLevel).isEqualTo("High")
        assertThat(fullWeek.volumeLevel).isEqualTo("Medium")
        assertThat(fullWeek.isDeload).isFalse()
        assertThat(fullWeek.phase).isEqualTo("Accumulation")
        
        val deloadWeek = fullWeek.copy(
            isDeload = true,
            intensityLevel = "Low",
            volumeLevel = "Low",
            description = "Recovery week"
        )
        
        assertThat(deloadWeek.isDeload).isTrue()
        assertThat(deloadWeek.intensityLevel).isEqualTo("Low")
        assertThat(deloadWeek.description).isEqualTo("Recovery week")
        
        val minimalWeek = ParsedWeek(
            weekNumber = 2,
            name = "Week 2",
            workouts = emptyList(),
            description = null,
            focusAreas = null,
            intensityLevel = null,
            volumeLevel = null,
            isDeload = false,
            phase = null
        )
        
        assertThat(minimalWeek.description).isNull()
        assertThat(minimalWeek.focusAreas).isNull()
        assertThat(minimalWeek.intensityLevel).isNull()
    }
    
    @Test
    fun `Complex programme structure integrity test`() {
        val weeks = (1..4).map { weekNum ->
            val workouts = (1..3).map { dayNum ->
                val exercises = (1..4).map { exerciseNum ->
                    val exerciseId = (weekNum - 1) * 12 + (dayNum - 1) * 4 + exerciseNum.toLong()
                    ParsedExercise(
                        exerciseName = "Exercise $exerciseId",
                        matchedExerciseId = exerciseId,
                        sets = (1..3).map { setNum ->
                            ParsedSet(
                                reps = 8 + setNum,
                                weight = 50f + (exerciseNum * 10f),
                                rpe = if (setNum == 3) 9f else null
                            )
                        },
                        notes = if (exerciseNum == 1) "First exercise" else null
                    )
                }
                ParsedWorkout(
                    dayOfWeek = when(dayNum) {
                        1 -> "Monday"
                        2 -> "Wednesday"
                        3 -> "Friday"
                        else -> null
                    },
                    name = "Week $weekNum Day $dayNum",
                    exercises = exercises,
                    estimatedDurationMinutes = 45 + (exercises.size * 5)
                )
            }
            ParsedWeek(
                weekNumber = weekNum,
                name = "Week $weekNum",
                workouts = workouts,
                description = "Week $weekNum description",
                focusAreas = if (weekNum <= 2) "Hypertrophy" else "Strength",
                intensityLevel = if (weekNum == 4) "Low" else "High",
                volumeLevel = if (weekNum == 4) "Low" else "Medium",
                isDeload = weekNum == 4,
                phase = "Phase ${(weekNum - 1) / 2 + 1}"
            )
        }
        
        val programme = ParsedProgramme(
            name = "4-Week Progressive Programme",
            description = "Complex test programme",
            durationWeeks = 4,
            programmeType = "STRENGTH",
            difficulty = "INTERMEDIATE",
            weeks = weeks,
            rawText = "Test"
        )
        
        assertThat(programme.weeks).hasSize(4)
        assertThat(programme.weeks[0].workouts).hasSize(3)
        assertThat(programme.weeks[0].workouts[0].exercises).hasSize(4)
        
        val firstExerciseId = programme.weeks[0].workouts[0].exercises[0].matchedExerciseId
        assertThat(firstExerciseId).isEqualTo(1L)
        
        val lastWeekLastWorkoutLastExercise = programme.weeks[3].workouts[2].exercises[3]
        assertThat(lastWeekLastWorkoutLastExercise.matchedExerciseId).isEqualTo(48L)
        
        assertThat(programme.weeks[3].isDeload).isTrue()
        assertThat(programme.weeks[0].focusAreas).isEqualTo("Hypertrophy")
        assertThat(programme.weeks[2].focusAreas).isEqualTo("Strength")
        
        val modifiedProgramme = programme.copy(
            weeks = programme.weeks.map { week ->
                week.copy(
                    workouts = week.workouts.map { workout ->
                        workout.copy(
                            exercises = workout.exercises.map { exercise ->
                                exercise.copy(
                                    sets = exercise.sets.map { set ->
                                        set.copy(rpe = null)
                                    }
                                )
                            }
                        )
                    }
                )
            }
        )
        
        val modifiedFirstExercise = modifiedProgramme.weeks[0].workouts[0].exercises[0]
        assertThat(modifiedFirstExercise.matchedExerciseId).isEqualTo(1L)
        assertThat(modifiedFirstExercise.sets[2].rpe).isNull()
        
        val totalExercises = modifiedProgramme.weeks.sumOf { week ->
            week.workouts.sumOf { it.exercises.size }
        }
        assertThat(totalExercises).isEqualTo(48)
        
        val exercisesWithIds = modifiedProgramme.weeks.sumOf { week ->
            week.workouts.sumOf { workout ->
                workout.exercises.count { it.matchedExerciseId != null }
            }
        }
        assertThat(exercisesWithIds).isEqualTo(48)
    }
}
