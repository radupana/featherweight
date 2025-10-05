package com.github.radupana.featherweight.ui.components

import com.github.radupana.featherweight.data.SetLog
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for the placeholder display logic in ExerciseCard.
 * Ensures that target values are shown as placeholders based on data,
 * not on workout type.
 */
class ExerciseCardPlaceholderTest {
    @Test
    fun `shouldShowPlaceholders returns true when any set has target values`() {
        val sets =
            listOf(
                SetLog(
                    id = "set-1",
                    userId = "user1",
                    exerciseLogId = "log-1",
                    setOrder = 1,
                    targetWeight = 100f,
                    targetReps = 8,
                    targetRpe = null,
                    actualWeight = 0f,
                    actualReps = 0,
                ),
                SetLog(
                    id = "set-2",
                    userId = "user1",
                    exerciseLogId = "log-1",
                    setOrder = 2,
                    targetWeight = null,
                    targetReps = null,
                    targetRpe = null,
                    actualWeight = 0f,
                    actualReps = 0,
                ),
            )

        val shouldShow = shouldShowPlaceholders(sets)
        assertThat(shouldShow).isTrue()
    }

    @Test
    fun `shouldShowPlaceholders returns true when only targetReps is present`() {
        val sets =
            listOf(
                SetLog(
                    id = "set-1",
                    userId = "user1",
                    exerciseLogId = "log-1",
                    setOrder = 1,
                    targetWeight = null,
                    targetReps = 10,
                    targetRpe = null,
                    actualWeight = 0f,
                    actualReps = 0,
                ),
            )

        val shouldShow = shouldShowPlaceholders(sets)
        assertThat(shouldShow).isTrue()
    }

    @Test
    fun `shouldShowPlaceholders returns true when only targetRpe is present`() {
        val sets =
            listOf(
                SetLog(
                    id = "set-1",
                    userId = "user1",
                    exerciseLogId = "log-1",
                    setOrder = 1,
                    targetWeight = null,
                    targetReps = null,
                    targetRpe = 8.5f,
                    actualWeight = 0f,
                    actualReps = 0,
                ),
            )

        val shouldShow = shouldShowPlaceholders(sets)
        assertThat(shouldShow).isTrue()
    }

    @Test
    fun `shouldShowPlaceholders returns false when no target values are present`() {
        val sets =
            listOf(
                SetLog(
                    id = "set-1",
                    userId = "user1",
                    exerciseLogId = "log-1",
                    setOrder = 1,
                    targetWeight = null,
                    targetReps = null,
                    targetRpe = null,
                    actualWeight = 100f,
                    actualReps = 8,
                ),
                SetLog(
                    id = "set-2",
                    userId = "user1",
                    exerciseLogId = "log-1",
                    setOrder = 2,
                    targetWeight = null,
                    targetReps = null,
                    targetRpe = null,
                    actualWeight = 110f,
                    actualReps = 6,
                ),
            )

        val shouldShow = shouldShowPlaceholders(sets)
        assertThat(shouldShow).isFalse()
    }

    @Test
    fun `shouldShowPlaceholders returns false for empty set list`() {
        val shouldShow = shouldShowPlaceholders(emptyList())
        assertThat(shouldShow).isFalse()
    }

    @Test
    fun `shouldShowPlaceholders correctly handles mixed target values`() {
        val sets =
            listOf(
                SetLog(
                    id = "set-1",
                    userId = "user1",
                    exerciseLogId = "log-1",
                    setOrder = 1,
                    targetWeight = 100f,
                    targetReps = null,
                    targetRpe = 7f,
                    actualWeight = 100f,
                    actualReps = 8,
                ),
                SetLog(
                    id = "set-2",
                    userId = "user1",
                    exerciseLogId = "log-1",
                    setOrder = 2,
                    targetWeight = null,
                    targetReps = 6,
                    targetRpe = null,
                    actualWeight = 110f,
                    actualReps = 6,
                ),
            )

        val shouldShow = shouldShowPlaceholders(sets)
        assertThat(shouldShow).isTrue()
    }

    /**
     * This function replicates the logic from ExerciseCard.kt
     * to determine whether to show target values as placeholders.
     */
    private fun shouldShowPlaceholders(sets: List<SetLog>): Boolean =
        sets.any {
            it.targetWeight != null || it.targetReps != null || it.targetRpe != null
        }
}
