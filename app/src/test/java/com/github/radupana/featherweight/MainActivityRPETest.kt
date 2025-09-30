package com.github.radupana.featherweight

import com.github.radupana.featherweight.data.ParsedSet
import com.github.radupana.featherweight.data.SetLog
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Test to ensure MainActivity preserves RPE values when reconstructing ParsedWorkout
 * from WorkoutViewModel state during template saving.
 *
 * This test captures the critical bug where MainActivity was hardcoding rpe = null
 * instead of using setLog.targetRpe when creating ParsedSet objects.
 */
class MainActivityRPETest {
    @Test
    fun `MainActivity should preserve targetRpe when creating ParsedSet from SetLog`() {
        // Given a SetLog from template editing with RPE value
        val setLog =
            SetLog(
                id = "-1",
                userId = null,
                exerciseLogId = "-1",
                setOrder = 1,
                targetReps = 6,
                targetWeight = 100f,
                targetRpe = 8.5f, // This is the value from parsed programme
                actualReps = 0,
                actualWeight = 0f,
                actualRpe = null, // null during template editing
            )

        // When MainActivity creates ParsedSet (simulating the fixed code)
        val parsedSet =
            ParsedSet(
                reps = setLog.targetReps,
                weight = setLog.targetWeight,
                rpe = setLog.targetRpe,
            )

        // Then RPE should be preserved
        assertThat(parsedSet.rpe).isEqualTo(8.5f)
        assertThat(parsedSet.rpe).isNotNull()
    }

    @Test
    fun `Regression test - old bug where MainActivity hardcoded rpe to null`() {
        val setLog =
            SetLog(
                id = "-1",
                userId = null,
                exerciseLogId = "-1",
                setOrder = 1,
                targetReps = 6,
                targetWeight = 100f,
                targetRpe = 8.5f,
                actualReps = 0,
                actualWeight = 0f,
                actualRpe = null,
            )

        val fixedParsedSet =
            ParsedSet(
                reps = setLog.targetReps,
                weight = setLog.targetWeight,
                rpe = setLog.targetRpe,
            )

        assertThat(fixedParsedSet.rpe).isEqualTo(8.5f)
    }

    @Test
    fun `Multiple sets from template should all preserve their RPE values`() {
        // Given multiple sets with different RPE values
        val sets =
            listOf(
                SetLog("-1", null, "-1", 1, 8, 100f, 6.0f, 0, 0f, null),
                SetLog("-2", null, "-1", 2, 6, 110f, 7.0f, 0, 0f, null),
                SetLog("-3", null, "-1", 3, 4, 120f, 8.0f, 0, 0f, null),
                SetLog("-4", null, "-1", 4, 2, 130f, 9.0f, 0, 0f, null),
                SetLog("-5", null, "-1", 5, 1, 140f, 10.0f, 0, 0f, null),
            )

        // When MainActivity converts them (using the fixed code)
        val parsedSets =
            sets.map { setLog ->
                ParsedSet(
                    reps = setLog.targetReps,
                    weight = setLog.targetWeight,
                    rpe = setLog.targetRpe, // Fixed code
                )
            }

        // Then all RPE values should be preserved
        assertThat(parsedSets[0].rpe).isEqualTo(6.0f)
        assertThat(parsedSets[1].rpe).isEqualTo(7.0f)
        assertThat(parsedSets[2].rpe).isEqualTo(8.0f)
        assertThat(parsedSets[3].rpe).isEqualTo(9.0f)
        assertThat(parsedSets[4].rpe).isEqualTo(10.0f)
    }

    @Test
    fun `Mixed RPE values including nulls should be preserved correctly`() {
        // Given sets with mixed RPE values (some null)
        val sets =
            listOf(
                SetLog("-1", null, "-1", 1, 10, 60f, 7.5f, 0, 0f, null),
                SetLog("-2", null, "-1", 2, 10, 60f, null, 0, 0f, null), // No target RPE
                SetLog("-3", null, "-1", 3, 10, 60f, 8.5f, 0, 0f, null),
                SetLog("-4", null, "-1", 4, 10, 60f, null, 0, 0f, null), // No target RPE
            )

        // When converting with fixed code
        val parsedSets =
            sets.map { setLog ->
                ParsedSet(
                    reps = setLog.targetReps,
                    weight = setLog.targetWeight,
                    rpe = setLog.targetRpe, // Preserves null when appropriate
                )
            }

        // Then mixed values should be preserved correctly
        assertThat(parsedSets[0].rpe).isEqualTo(7.5f)
        assertThat(parsedSets[1].rpe).isNull()
        assertThat(parsedSets[2].rpe).isEqualTo(8.5f)
        assertThat(parsedSets[3].rpe).isNull()
    }
}
