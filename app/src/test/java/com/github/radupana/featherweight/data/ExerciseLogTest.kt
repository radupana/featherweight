package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExerciseLogTest {
    
    @Test
    fun exerciseLog_withDefaultValues_hasCorrectDefaults() {
        // Act
        val exerciseLog = ExerciseLog(
            workoutId = 10L,
            exerciseVariationId = 5L,
            exerciseOrder = 1
        )
        
        // Assert
        assertThat(exerciseLog.id).isEqualTo(0L)
        assertThat(exerciseLog.workoutId).isEqualTo(10L)
        assertThat(exerciseLog.exerciseVariationId).isEqualTo(5L)
        assertThat(exerciseLog.exerciseOrder).isEqualTo(1)
        assertThat(exerciseLog.supersetGroup).isNull()
        assertThat(exerciseLog.notes).isNull()
        assertThat(exerciseLog.originalVariationId).isNull()
        assertThat(exerciseLog.isSwapped).isFalse()
    }
    
    @Test
    fun exerciseLog_withAllValues_storesCorrectly() {
        // Act
        val exerciseLog = ExerciseLog(
            id = 123L,
            workoutId = 45L,
            exerciseVariationId = 88L,
            exerciseOrder = 3,
            supersetGroup = 1,
            notes = "Focus on form today",
            originalVariationId = 77L,
            isSwapped = true
        )
        
        // Assert
        assertThat(exerciseLog.id).isEqualTo(123L)
        assertThat(exerciseLog.workoutId).isEqualTo(45L)
        assertThat(exerciseLog.exerciseVariationId).isEqualTo(88L)
        assertThat(exerciseLog.exerciseOrder).isEqualTo(3)
        assertThat(exerciseLog.supersetGroup).isEqualTo(1)
        assertThat(exerciseLog.notes).isEqualTo("Focus on form today")
        assertThat(exerciseLog.originalVariationId).isEqualTo(77L)
        assertThat(exerciseLog.isSwapped).isTrue()
    }
    
    @Test
    fun exerciseLog_withSuperset_hasSupersetGroup() {
        // Act - Two exercises in the same superset
        val exercise1 = ExerciseLog(
            workoutId = 10L,
            exerciseVariationId = 1L,
            exerciseOrder = 1,
            supersetGroup = 1
        )
        
        val exercise2 = ExerciseLog(
            workoutId = 10L,
            exerciseVariationId = 2L,
            exerciseOrder = 2,
            supersetGroup = 1
        )
        
        // Assert - Both have same superset group
        assertThat(exercise1.supersetGroup).isEqualTo(1)
        assertThat(exercise2.supersetGroup).isEqualTo(1)
        assertThat(exercise1.supersetGroup).isEqualTo(exercise2.supersetGroup)
    }
    
    @Test
    fun exerciseLog_withDifferentSupersetGroups_areDifferent() {
        // Act
        val superset1Exercise = ExerciseLog(
            workoutId = 10L,
            exerciseVariationId = 1L,
            exerciseOrder = 1,
            supersetGroup = 1
        )
        
        val superset2Exercise = ExerciseLog(
            workoutId = 10L,
            exerciseVariationId = 2L,
            exerciseOrder = 3,
            supersetGroup = 2
        )
        
        // Assert
        assertThat(superset1Exercise.supersetGroup).isNotEqualTo(superset2Exercise.supersetGroup)
    }
    
    @Test
    fun exerciseLog_withSwappedExercise_tracksOriginal() {
        // Act - Exercise was swapped from barbell to dumbbell variation
        val exerciseLog = ExerciseLog(
            workoutId = 10L,
            exerciseVariationId = 55L, // Current (swapped to)
            exerciseOrder = 1,
            originalVariationId = 33L, // Original (swapped from)
            isSwapped = true
        )
        
        // Assert
        assertThat(exerciseLog.exerciseVariationId).isEqualTo(55L)
        assertThat(exerciseLog.originalVariationId).isEqualTo(33L)
        assertThat(exerciseLog.isSwapped).isTrue()
    }
    
    @Test
    fun exerciseLog_withoutSwap_hasNoOriginal() {
        // Act
        val exerciseLog = ExerciseLog(
            workoutId = 10L,
            exerciseVariationId = 55L,
            exerciseOrder = 1,
            isSwapped = false
        )
        
        // Assert
        assertThat(exerciseLog.originalVariationId).isNull()
        assertThat(exerciseLog.isSwapped).isFalse()
    }
    
    @Test
    fun exerciseLog_withZeroOrder_isFirstExercise() {
        // Act
        val exerciseLog = ExerciseLog(
            workoutId = 10L,
            exerciseVariationId = 5L,
            exerciseOrder = 0 // First exercise
        )
        
        // Assert
        assertThat(exerciseLog.exerciseOrder).isEqualTo(0)
    }
    
    @Test
    fun exerciseLog_withHighOrder_isValidLaterExercise() {
        // Act - 20th exercise in a long workout
        val exerciseLog = ExerciseLog(
            workoutId = 10L,
            exerciseVariationId = 5L,
            exerciseOrder = 19
        )
        
        // Assert
        assertThat(exerciseLog.exerciseOrder).isEqualTo(19)
    }
    
    @Test
    fun exerciseLog_withLongNotes_storesFullText() {
        // Arrange
        val longNotes = "Today I'm focusing on time under tension. " +
                "Going to slow down the eccentric phase to 3 seconds " +
                "and pause at the bottom for 1 second. " +
                "Last week I felt some discomfort in my shoulder " +
                "so I'm being extra careful with form today. " +
                "Target is 3 sets of 8-10 reps with good control."
        
        // Act
        val exerciseLog = ExerciseLog(
            workoutId = 10L,
            exerciseVariationId = 5L,
            exerciseOrder = 1,
            notes = longNotes
        )
        
        // Assert
        assertThat(exerciseLog.notes).isEqualTo(longNotes)
    }
    
    @Test
    fun exerciseLog_copy_createsNewInstance() {
        // Arrange
        val original = ExerciseLog(
            id = 1L,
            workoutId = 10L,
            exerciseVariationId = 5L,
            exerciseOrder = 1,
            notes = "Original notes"
        )
        
        // Act
        val copy = original.copy(
            exerciseOrder = 2,
            notes = "Updated notes"
        )
        
        // Assert
        assertThat(copy.id).isEqualTo(1L)
        assertThat(copy.workoutId).isEqualTo(10L)
        assertThat(copy.exerciseVariationId).isEqualTo(5L)
        assertThat(copy.exerciseOrder).isEqualTo(2)
        assertThat(copy.notes).isEqualTo("Updated notes")
        
        // Original should be unchanged
        assertThat(original.exerciseOrder).isEqualTo(1)
        assertThat(original.notes).isEqualTo("Original notes")
    }
    
    @Test
    fun exerciseLog_equals_worksCorrectly() {
        // Arrange
        val log1 = ExerciseLog(
            id = 1L,
            workoutId = 10L,
            exerciseVariationId = 5L,
            exerciseOrder = 1
        )
        
        val log2 = ExerciseLog(
            id = 1L,
            workoutId = 10L,
            exerciseVariationId = 5L,
            exerciseOrder = 1
        )
        
        val log3 = ExerciseLog(
            id = 2L, // Different ID
            workoutId = 10L,
            exerciseVariationId = 5L,
            exerciseOrder = 1
        )
        
        // Assert
        assertThat(log1).isEqualTo(log2)
        assertThat(log1).isNotEqualTo(log3)
    }
    
    @Test
    fun exerciseLog_hashCode_isConsistent() {
        // Arrange
        val log1 = ExerciseLog(
            id = 1L,
            workoutId = 10L,
            exerciseVariationId = 5L,
            exerciseOrder = 1
        )
        
        val log2 = ExerciseLog(
            id = 1L,
            workoutId = 10L,
            exerciseVariationId = 5L,
            exerciseOrder = 1
        )
        
        // Assert
        assertThat(log1.hashCode()).isEqualTo(log2.hashCode())
    }
    
    @Test
    fun exerciseLog_forSameWorkout_haveSameWorkoutId() {
        // Act
        val exercise1 = ExerciseLog(
            workoutId = 100L,
            exerciseVariationId = 1L,
            exerciseOrder = 0
        )
        
        val exercise2 = ExerciseLog(
            workoutId = 100L,
            exerciseVariationId = 2L,
            exerciseOrder = 1
        )
        
        val exercise3 = ExerciseLog(
            workoutId = 100L,
            exerciseVariationId = 3L,
            exerciseOrder = 2
        )
        
        // Assert - All belong to same workout
        assertThat(exercise1.workoutId).isEqualTo(100L)
        assertThat(exercise2.workoutId).isEqualTo(100L)
        assertThat(exercise3.workoutId).isEqualTo(100L)
    }
    
    @Test
    fun exerciseLog_forDifferentWorkouts_haveDifferentWorkoutIds() {
        // Act
        val workout1Exercise = ExerciseLog(
            workoutId = 100L,
            exerciseVariationId = 1L,
            exerciseOrder = 0
        )
        
        val workout2Exercise = ExerciseLog(
            workoutId = 200L,
            exerciseVariationId = 1L, // Same exercise
            exerciseOrder = 0
        )
        
        // Assert
        assertThat(workout1Exercise.workoutId).isNotEqualTo(workout2Exercise.workoutId)
        assertThat(workout1Exercise.exerciseVariationId).isEqualTo(workout2Exercise.exerciseVariationId)
    }
    
    @Test
    fun exerciseLog_withTripleSuperset_allHaveSameGroup() {
        // Act - Three exercises in a giant set
        val exercise1 = ExerciseLog(
            workoutId = 10L,
            exerciseVariationId = 1L,
            exerciseOrder = 0,
            supersetGroup = 1
        )
        
        val exercise2 = ExerciseLog(
            workoutId = 10L,
            exerciseVariationId = 2L,
            exerciseOrder = 1,
            supersetGroup = 1
        )
        
        val exercise3 = ExerciseLog(
            workoutId = 10L,
            exerciseVariationId = 3L,
            exerciseOrder = 2,
            supersetGroup = 1
        )
        
        // Assert
        assertThat(exercise1.supersetGroup).isEqualTo(1)
        assertThat(exercise2.supersetGroup).isEqualTo(1)
        assertThat(exercise3.supersetGroup).isEqualTo(1)
    }
    
    @Test
    fun exerciseLog_toString_containsKeyInfo() {
        // Act
        val exerciseLog = ExerciseLog(
            id = 10L,
            workoutId = 100L,
            exerciseVariationId = 55L,
            exerciseOrder = 2
        )
        
        val stringRep = exerciseLog.toString()
        
        // Assert - toString should contain key identifying information
        assertThat(stringRep).contains("10") // id
        assertThat(stringRep).contains("100") // workoutId
        assertThat(stringRep).contains("55") // exerciseVariationId
        assertThat(stringRep).contains("2") // exerciseOrder
    }
}
