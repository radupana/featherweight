package com.github.radupana.featherweight.domain

import com.github.radupana.featherweight.data.SetLog
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class ExerciseHistoryTest {
    
    @Test
    fun exerciseHistory_withAllFields_createsCorrectly() {
        // Arrange
        val lastWorkoutDate = LocalDateTime.of(2024, 6, 15, 10, 30)
        val set1 = SetLog(
            id = 1,
            exerciseLogId = 10,
            setOrder = 1,
            targetReps = 10,
            targetWeight = 50f,
            actualReps = 10,
            actualWeight = 50f,
            actualRpe = 7.5f,
            isCompleted = true
        )
        val set2 = SetLog(
            id = 2,
            exerciseLogId = 10,
            setOrder = 2,
            targetReps = 10,
            targetWeight = 50f,
            actualReps = 8,
            actualWeight = 50f,
            actualRpe = 8.5f,
            isCompleted = true
        )
        val set3 = SetLog(
            id = 3,
            exerciseLogId = 10,
            setOrder = 3,
            targetReps = 10,
            targetWeight = 50f,
            actualReps = 6,
            actualWeight = 50f,
            actualRpe = 9f,
            isCompleted = true
        )
        
        // Act
        val history = ExerciseHistory(
            exerciseVariationId = 5,
            lastWorkoutDate = lastWorkoutDate,
            sets = listOf(set1, set2, set3)
        )
        
        // Assert
        assertThat(history.exerciseVariationId).isEqualTo(5)
        assertThat(history.lastWorkoutDate).isEqualTo(lastWorkoutDate)
        assertThat(history.sets).hasSize(3)
        assertThat(history.sets[0]).isEqualTo(set1)
        assertThat(history.sets[1]).isEqualTo(set2)
        assertThat(history.sets[2]).isEqualTo(set3)
    }
    
    @Test
    fun exerciseHistory_withEmptySets_handlesEmptyList() {
        // Arrange
        val lastWorkoutDate = LocalDateTime.of(2024, 7, 1, 14, 0)
        
        // Act
        val history = ExerciseHistory(
            exerciseVariationId = 10,
            lastWorkoutDate = lastWorkoutDate,
            sets = emptyList()
        )
        
        // Assert
        assertThat(history.exerciseVariationId).isEqualTo(10)
        assertThat(history.lastWorkoutDate).isEqualTo(lastWorkoutDate)
        assertThat(history.sets).isEmpty()
    }
    
    @Test
    fun exerciseHistory_equality_worksCorrectly() {
        // Arrange
        val date = LocalDateTime.of(2024, 6, 15, 10, 0)
        val set = SetLog(
            id = 1,
            exerciseLogId = 10,
            setOrder = 1,
            actualReps = 10,
            actualWeight = 50f
        )
        
        val history1 = ExerciseHistory(
            exerciseVariationId = 5,
            lastWorkoutDate = date,
            sets = listOf(set)
        )
        
        val history2 = ExerciseHistory(
            exerciseVariationId = 5,
            lastWorkoutDate = date,
            sets = listOf(set)
        )
        
        val history3 = ExerciseHistory(
            exerciseVariationId = 6, // Different exercise
            lastWorkoutDate = date,
            sets = listOf(set)
        )
        
        // Assert
        assertThat(history1).isEqualTo(history2)
        assertThat(history1).isNotEqualTo(history3)
        assertThat(history1.hashCode()).isEqualTo(history2.hashCode())
    }
    
    @Test
    fun exerciseHistory_copy_createsIndependentCopy() {
        // Arrange
        val date = LocalDateTime.of(2024, 6, 15, 10, 0)
        val set1 = SetLog(
            id = 1,
            exerciseLogId = 10,
            setOrder = 1,
            actualReps = 10,
            actualWeight = 50f
        )
        val set2 = SetLog(
            id = 2,
            exerciseLogId = 10,
            setOrder = 2,
            actualReps = 8,
            actualWeight = 50f
        )
        
        val original = ExerciseHistory(
            exerciseVariationId = 5,
            lastWorkoutDate = date,
            sets = listOf(set1)
        )
        
        // Act
        val copy = original.copy(
            sets = listOf(set1, set2)
        )
        
        // Assert
        assertThat(copy.exerciseVariationId).isEqualTo(5) // Unchanged
        assertThat(copy.lastWorkoutDate).isEqualTo(date) // Unchanged
        assertThat(copy.sets).hasSize(2)
        assertThat(copy.sets).containsExactly(set1, set2)
        
        // Verify original is unchanged
        assertThat(original.sets).hasSize(1)
        assertThat(original.sets).containsExactly(set1)
    }
    
    @Test
    fun exerciseHistory_withIncompleteSets_includesAllSets() {
        // Arrange
        val date = LocalDateTime.of(2024, 6, 15, 10, 0)
        val completedSet = SetLog(
            id = 1,
            exerciseLogId = 10,
            setOrder = 1,
            actualReps = 10,
            actualWeight = 50f,
            isCompleted = true
        )
        val incompleteSet = SetLog(
            id = 2,
            exerciseLogId = 10,
            setOrder = 2,
            actualReps = 0,
            actualWeight = 0f,
            isCompleted = false
        )
        
        // Act
        val history = ExerciseHistory(
            exerciseVariationId = 5,
            lastWorkoutDate = date,
            sets = listOf(completedSet, incompleteSet)
        )
        
        // Assert
        assertThat(history.sets).hasSize(2)
        assertThat(history.sets[0].isCompleted).isTrue()
        assertThat(history.sets[1].isCompleted).isFalse()
    }
    
    @Test
    fun exerciseHistory_withDifferentDates_maintainsChronology() {
        // Arrange
        val earlierDate = LocalDateTime.of(2024, 6, 1, 10, 0)
        val laterDate = LocalDateTime.of(2024, 6, 15, 10, 0)
        
        val history1 = ExerciseHistory(
            exerciseVariationId = 5,
            lastWorkoutDate = earlierDate,
            sets = emptyList()
        )
        
        val history2 = ExerciseHistory(
            exerciseVariationId = 5,
            lastWorkoutDate = laterDate,
            sets = emptyList()
        )
        
        // Assert
        assertThat(history1.lastWorkoutDate.isBefore(history2.lastWorkoutDate)).isTrue()
    }
    
    @Test
    fun exerciseHistory_toString_includesAllFields() {
        // Arrange
        val date = LocalDateTime.of(2024, 6, 15, 10, 0)
        val set = SetLog(
            id = 1,
            exerciseLogId = 10,
            setOrder = 1,
            actualReps = 10,
            actualWeight = 50f
        )
        
        val history = ExerciseHistory(
            exerciseVariationId = 5,
            lastWorkoutDate = date,
            sets = listOf(set)
        )
        
        // Act
        val stringRepresentation = history.toString()
        
        // Assert
        assertThat(stringRepresentation).contains("exerciseVariationId=5")
        assertThat(stringRepresentation).contains("lastWorkoutDate=2024-06-15T10:00")
        assertThat(stringRepresentation).contains("sets")
    }
    
    @Test
    fun exerciseHistory_withMultipleSets_preservesOrder() {
        // Arrange
        val date = LocalDateTime.of(2024, 6, 15, 10, 0)
        val sets = (1..5).map { order ->
            SetLog(
                id = order.toLong(),
                exerciseLogId = 10,
                setOrder = order,
                actualReps = 10 - order + 1, // Decreasing reps
                actualWeight = 50f + (order * 5f), // Increasing weight
                isCompleted = true
            )
        }
        
        // Act
        val history = ExerciseHistory(
            exerciseVariationId = 5,
            lastWorkoutDate = date,
            sets = sets
        )
        
        // Assert
        assertThat(history.sets).hasSize(5)
        // Verify order is preserved
        assertThat(history.sets[0].setOrder).isEqualTo(1)
        assertThat(history.sets[0].actualReps).isEqualTo(10)
        assertThat(history.sets[0].actualWeight).isEqualTo(55f)
        
        assertThat(history.sets[4].setOrder).isEqualTo(5)
        assertThat(history.sets[4].actualReps).isEqualTo(6)
        assertThat(history.sets[4].actualWeight).isEqualTo(75f)
    }
}
