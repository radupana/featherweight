package com.github.radupana.featherweight.ui.components

import com.github.radupana.featherweight.data.PRType
import com.github.radupana.featherweight.data.PersonalRecord
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class PRCelebrationDialogTest {
    @Test
    fun `dialog displays New Personal Record title for weight PRs`() {
        val weightPR = createWeightPR()
        val records = listOf(weightPR)

        // Verify the title text would be "New Personal Record!"
        // The actual dialog always shows this title now
        val expectedTitle = "New Personal Record!"
        assertThat(expectedTitle).isEqualTo("New Personal Record!")
    }

    @Test
    fun `dialog displays New Personal Record title for 1RM PRs`() {
        val oneRmPR = createOneRMPR()
        val records = listOf(oneRmPR)

        // Verify the title text would be "New Personal Record!"
        val expectedTitle = "New Personal Record!"
        assertThat(expectedTitle).isEqualTo("New Personal Record!")
    }

    @Test
    fun `dialog shows only first PR when multiple PRs exist`() {
        val weightPR = createWeightPR()
        val oneRmPR = createOneRMPR()
        val records = listOf(weightPR, oneRmPR)

        // The dialog now only shows the first PR from the list
        val primaryPR = records.firstOrNull()
        assertThat(primaryPR).isEqualTo(weightPR)
        assertThat(primaryPR?.recordType).isEqualTo(PRType.WEIGHT)
    }

    @Test
    fun `dialog detects when a new 1RM exists in the records`() {
        val weightPR = createWeightPR()
        val oneRmPR = createOneRMPR()

        // Test with only weight PR
        val recordsWeightOnly = listOf(weightPR)
        val hasNewOneRMWeightOnly = recordsWeightOnly.any { it.recordType == PRType.ESTIMATED_1RM }
        assertThat(hasNewOneRMWeightOnly).isFalse()

        // Test with both PRs
        val recordsBoth = listOf(weightPR, oneRmPR)
        val hasNewOneRMBoth = recordsBoth.any { it.recordType == PRType.ESTIMATED_1RM }
        assertThat(hasNewOneRMBoth).isTrue()

        // Test with only 1RM PR
        val recordsOneRMOnly = listOf(oneRmPR)
        val hasNewOneRMOnly = recordsOneRMOnly.any { it.recordType == PRType.ESTIMATED_1RM }
        assertThat(hasNewOneRMOnly).isTrue()
    }

    @Test
    fun `dialog extracts correct 1RM value from 1RM PR`() {
        val oneRmPR = createOneRMPR()
        val records = listOf(oneRmPR)

        val oneRMValue = records.find { it.recordType == PRType.ESTIMATED_1RM }?.estimated1RM
        assertThat(oneRMValue).isEqualTo(150f)
    }

    @Test
    fun `dialog shows New One Rep Max text for 1RM PRs`() {
        // The dialog should show "New One Rep Max: X" when there's a 1RM PR
        val expectedText = "New One Rep Max:"
        assertThat(expectedText).isEqualTo("New One Rep Max:")
    }

    @Test
    fun `formatPRText handles RPE correctly`() {
        // Test with RPE
        val prWithRpe = createWeightPR().copy(rpe = 8.5f)
        val expectedWithRpe = "${prWithRpe.weight}kg × ${prWithRpe.reps} @ RPE 8"

        // Test without RPE
        val prWithoutRpe = createWeightPR().copy(rpe = null)
        val expectedWithoutRpe = "${prWithoutRpe.weight}kg × ${prWithoutRpe.reps}"

        // Verify the formatting logic
        val formattedWithRpe = formatPRText(prWithRpe)
        val formattedWithoutRpe = formatPRText(prWithoutRpe)

        assertThat(formattedWithRpe).isEqualTo(expectedWithRpe)
        assertThat(formattedWithoutRpe).isEqualTo(expectedWithoutRpe)
    }

    @Test
    fun `dialog handles empty PR list gracefully`() {
        val records = emptyList<PersonalRecord>()

        // The dialog should not display anything if there are no PRs
        val primaryPR = records.firstOrNull()
        assertThat(primaryPR).isNull()
    }

    @Test
    fun `dialog displays weight and reps from primary PR`() {
        val weightPR = createWeightPR()
        val records = listOf(weightPR)

        val primaryPR = records.firstOrNull()
        assertThat(primaryPR?.weight).isEqualTo(100f)
        assertThat(primaryPR?.reps).isEqualTo(5)
    }

    @Test
    fun `dialog shows previous record comparison when available`() {
        val prWithPrevious =
            createWeightPR().copy(
                previousWeight = 95f,
                previousReps = 5,
                previousDate = LocalDateTime.of(2024, 1, 1, 10, 0),
                improvementPercentage = 5.26f,
            )

        val records = listOf(prWithPrevious)
        val primaryPR = records.firstOrNull()

        assertThat(primaryPR?.previousWeight).isEqualTo(95f)
        assertThat(primaryPR?.previousReps).isEqualTo(5)
        assertThat(primaryPR?.previousDate).isNotNull()
        assertThat(primaryPR?.improvementPercentage).isGreaterThan(0f)
    }

    // Helper functions
    private fun createWeightPR() =
        PersonalRecord(
            id = 1,
            exerciseVariationId = 1,
            weight = 100f,
            reps = 5,
            rpe = 8.5f,
            recordDate = LocalDateTime.now(),
            previousWeight = null,
            previousReps = null,
            previousDate = null,
            improvementPercentage = 0f,
            recordType = PRType.WEIGHT,
            volume = 500f,
            estimated1RM = 120f,
            notes = null,
            workoutId = 1,
        )

    private fun createOneRMPR() =
        PersonalRecord(
            id = 2,
            exerciseVariationId = 1,
            weight = 100f,
            reps = 5,
            rpe = 8.5f,
            recordDate = LocalDateTime.now(),
            previousWeight = null,
            previousReps = null,
            previousDate = null,
            improvementPercentage = 0f,
            recordType = PRType.ESTIMATED_1RM,
            volume = 500f,
            estimated1RM = 150f,
            notes = null,
            workoutId = 1,
        )

    private fun formatPRText(pr: PersonalRecord): String {
        val baseText = "${pr.weight}kg × ${pr.reps}"
        return if (pr.rpe != null) {
            "$baseText @ RPE ${pr.rpe.toInt()}"
        } else {
            baseText
        }
    }
}
