package com.github.radupana.featherweight.domain

import com.github.radupana.featherweight.data.SetLog
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class ExerciseHistoryTest {
    @Test
    fun exerciseHistory_equality_worksCorrectly() {
        // Arrange
        val date = LocalDateTime.of(2024, 6, 15, 10, 0)
        val set =
            SetLog(
                id = "1",
                exerciseLogId = "10",
                setOrder = 1,
                actualReps = 10,
                actualWeight = 50f,
            )

        val history1 =
            ExerciseHistory(
                exerciseId = "5",
                lastWorkoutDate = date,
                sets = listOf(set),
            )

        val history2 =
            ExerciseHistory(
                exerciseId = "5",
                lastWorkoutDate = date,
                sets = listOf(set),
            )

        val history3 =
            ExerciseHistory(
                exerciseId = "6", // Different exercise
                lastWorkoutDate = date,
                sets = listOf(set),
            )

        // Assert
        assertThat(history1).isEqualTo(history2)
        assertThat(history1).isNotEqualTo(history3)
        assertThat(history1.hashCode()).isEqualTo(history2.hashCode())
    }

    @Test
    fun exerciseHistory_withIncompleteSets_includesAllSets() {
        // Arrange
        val date = LocalDateTime.of(2024, 6, 15, 10, 0)
        val completedSet =
            SetLog(
                id = "1",
                exerciseLogId = "10",
                setOrder = 1,
                actualReps = 10,
                actualWeight = 50f,
                isCompleted = true,
            )
        val incompleteSet =
            SetLog(
                id = "2",
                exerciseLogId = "10",
                setOrder = 2,
                actualReps = 0,
                actualWeight = 0f,
                isCompleted = false,
            )

        // Act
        val history =
            ExerciseHistory(
                exerciseId = "5",
                lastWorkoutDate = date,
                sets = listOf(completedSet, incompleteSet),
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

        val history1 =
            ExerciseHistory(
                exerciseId = "5",
                lastWorkoutDate = earlierDate,
                sets = emptyList(),
            )

        val history2 =
            ExerciseHistory(
                exerciseId = "5",
                lastWorkoutDate = laterDate,
                sets = emptyList(),
            )

        // Assert
        assertThat(history1.lastWorkoutDate.isBefore(history2.lastWorkoutDate)).isTrue()
    }

    @Test
    fun exerciseHistory_withMultipleSets_preservesOrder() {
        // Arrange
        val date = LocalDateTime.of(2024, 6, 15, 10, 0)
        val sets =
            (1..5).map { order ->
                SetLog(
                    id = order.toString(),
                    exerciseLogId = "10",
                    setOrder = order,
                    actualReps = 10 - order + 1, // Decreasing reps
                    actualWeight = 50f + (order * 5f), // Increasing weight
                    isCompleted = true,
                )
            }

        // Act
        val history =
            ExerciseHistory(
                exerciseId = "5",
                lastWorkoutDate = date,
                sets = sets,
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
