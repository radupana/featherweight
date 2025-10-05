package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.SetLog
import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.data.profile.OneRMType
import com.github.radupana.featherweight.testutil.LogMock
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class OneRMServiceTest {
    private lateinit var service: OneRMService

    @Before
    fun setUp() {
        LogMock.setup()
        service = OneRMService()
    }

    // ===== calculateEstimated1RM Tests =====

    @Test
    fun `calculateEstimated1RM with 1 rep returns exact weight`() {
        val result =
            service.calculateEstimated1RM(
                weight = 100f,
                reps = 1,
                rpe = 10f,
            )

        assertThat(result).isEqualTo(100f)
    }

    @Test
    fun `calculateEstimated1RM with standard scaling uses Brzycki formula`() {
        // Brzycki: weight / (1.0278 - 0.0278 * reps)
        // 100 / (1.0278 - 0.0278 * 5) = 100 / 0.889 = 112.485
        val result =
            service.calculateEstimated1RM(
                weight = 100f,
                reps = 5,
                rpe = null,
                scalingType = RMScalingType.STANDARD,
            )

        assertThat(result).isWithin(0.1f).of(112.485f)
    }

    @Test
    fun `calculateEstimated1RM with weighted bodyweight scaling uses modified Epley`() {
        // Modified Epley: weight * (1 + reps * 0.035)
        // 50 * (1 + 10 * 0.035) = 50 * 1.35 = 67.5
        val result =
            service.calculateEstimated1RM(
                weight = 50f,
                reps = 10,
                rpe = null,
                scalingType = RMScalingType.WEIGHTED_BODYWEIGHT,
            )

        assertThat(result).isWithin(0.1f).of(67.5f)
    }

    @Test
    fun `calculateEstimated1RM with isolation scaling uses Lombardi formula`() {
        // Lombardi: weight * reps^0.10
        // 30 * 12^0.10 = 30 * 1.278 = 38.34
        val result =
            service.calculateEstimated1RM(
                weight = 30f,
                reps = 12,
                rpe = null,
                scalingType = RMScalingType.ISOLATION,
            )

        assertThat(result).isWithin(0.1f).of(38.46f)
    }

    @Test
    fun `calculateEstimated1RM with RPE adjusts for reps in reserve`() {
        // 5 reps @ RPE 8 = 7 total capacity (2 RIR)
        // 100 / (1.0278 - 0.0278 * 7) = 100 / 0.8332 = 120.05
        val result =
            service.calculateEstimated1RM(
                weight = 100f,
                reps = 5,
                rpe = 8f,
                scalingType = RMScalingType.STANDARD,
            )

        assertThat(result).isWithin(0.1f).of(120.05f)
    }

    @Test
    fun `calculateEstimated1RM returns null for zero reps`() {
        val result =
            service.calculateEstimated1RM(
                weight = 100f,
                reps = 0,
                rpe = null,
            )

        assertThat(result).isNull()
    }

    @Test
    fun `calculateEstimated1RM returns null for negative reps`() {
        val result =
            service.calculateEstimated1RM(
                weight = 100f,
                reps = -1,
                rpe = null,
            )

        assertThat(result).isNull()
    }

    @Test
    fun `calculateEstimated1RM returns null for reps over 15`() {
        val result =
            service.calculateEstimated1RM(
                weight = 100f,
                reps = 16,
                rpe = null,
            )

        assertThat(result).isNull()
    }

    @Test
    fun `calculateEstimated1RM returns null for RPE 6 or below`() {
        val result =
            service.calculateEstimated1RM(
                weight = 100f,
                reps = 5,
                rpe = 6f,
            )

        assertThat(result).isNull()
    }

    @Test
    fun `calculateEstimated1RM handles RPE 10 correctly`() {
        // RPE 10 = 0 RIR, so total capacity = actual reps
        val result =
            service.calculateEstimated1RM(
                weight = 100f,
                reps = 3,
                rpe = 10f,
                scalingType = RMScalingType.STANDARD,
            )

        // 100 / (1.0278 - 0.0278 * 3) = 100 / 0.9444 = 105.88
        assertThat(result).isWithin(0.1f).of(105.88f)
    }

    @Test
    fun `calculateEstimated1RM handles fractional RPE values`() {
        // 5 reps @ RPE 8.5 = 6.5 total capacity (1.5 RIR)
        val result =
            service.calculateEstimated1RM(
                weight = 100f,
                reps = 5,
                rpe = 8.5f,
                scalingType = RMScalingType.STANDARD,
            )

        // 100 / (1.0278 - 0.0278 * 6.5) = 100 / (1.0278 - 0.1807) = 100 / 0.8471 = 118.07
        assertThat(result).isWithin(0.1f).of(118.07f)
    }

    @Test
    fun `calculateEstimated1RM coerces negative RIR to zero`() {
        // RPE 11 would give -1 RIR, should be coerced to 0
        val result =
            service.calculateEstimated1RM(
                weight = 100f,
                reps = 5,
                rpe = 11f,
                scalingType = RMScalingType.STANDARD,
            )

        // Total capacity = 5 + 0 = 5
        assertThat(result).isWithin(0.1f).of(112.485f)
    }

    // ===== calculateConfidence Tests =====

    @Test
    fun `calculateConfidence returns 0 for zero reps`() {
        val confidence =
            service.calculateConfidence(
                reps = 0,
                rpe = 9f,
                percentOf1RM = 0.9f,
            )

        assertThat(confidence).isEqualTo(0f)
    }

    @Test
    fun `calculateConfidence returns 0 for negative reps`() {
        val confidence =
            service.calculateConfidence(
                reps = -1,
                rpe = 9f,
                percentOf1RM = 0.9f,
            )

        assertThat(confidence).isEqualTo(0f)
    }

    @Test
    fun `calculateConfidence gives highest score for 1 rep at RPE 10 and 100 percent load`() {
        val confidence =
            service.calculateConfidence(
                reps = 1,
                rpe = 10f,
                percentOf1RM = 1f,
            )

        // repScore = (16-1)/15 = 1.0 (weight 0.5)
        // rpeScore = (10-5)/5 = 1.0 (weight 0.3)
        // loadScore = 1.0 (weight 0.2)
        // Total = 0.5 + 0.3 + 0.2 = 1.0
        assertThat(confidence).isEqualTo(1f)
    }

    @Test
    fun `calculateConfidence uses default RPE score when RPE is null`() {
        val confidence =
            service.calculateConfidence(
                reps = 5,
                rpe = null,
                percentOf1RM = 0.8f,
            )

        // repScore = (16-5)/15 = 0.733 (weight 0.5)
        // rpeScore = 0.3 (default, weight 0.3)
        // loadScore = 0.8 (weight 0.2)
        // Total = 0.733*0.5 + 0.3*0.3 + 0.8*0.2 = 0.3665 + 0.09 + 0.16 = 0.6165
        assertThat(confidence).isWithin(0.01f).of(0.6165f)
    }

    @Test
    fun `calculateConfidence caps reps at 15 for scoring`() {
        val confidence20Reps =
            service.calculateConfidence(
                reps = 20,
                rpe = 9f,
                percentOf1RM = 0.5f,
            )

        val confidence15Reps =
            service.calculateConfidence(
                reps = 15,
                rpe = 9f,
                percentOf1RM = 0.5f,
            )

        assertThat(confidence20Reps).isEqualTo(confidence15Reps)
    }

    @Test
    fun `calculateConfidence handles low RPE values`() {
        val confidence =
            service.calculateConfidence(
                reps = 5,
                rpe = 5f,
                percentOf1RM = 0.8f,
            )

        // repScore = (16-5)/15 = 0.733
        // rpeScore = 0.3 (default since RPE 5 < MIN_RPE_FOR_ESTIMATE)
        // loadScore = 0.8
        // Total = 0.733*0.5 + 0.3*0.3 + 0.8*0.2 = 0.6165
        assertThat(confidence).isWithin(0.01f).of(0.6165f)
    }

    @Test
    fun `calculateConfidence clamps percentOf1RM between 0 and 1`() {
        val confidenceOver =
            service.calculateConfidence(
                reps = 5,
                rpe = 9f,
                percentOf1RM = 1.5f,
            )

        val confidenceAt1 =
            service.calculateConfidence(
                reps = 5,
                rpe = 9f,
                percentOf1RM = 1f,
            )

        assertThat(confidenceOver).isEqualTo(confidenceAt1)

        val confidenceNegative =
            service.calculateConfidence(
                reps = 5,
                rpe = 9f,
                percentOf1RM = -0.5f,
            )

        val confidenceAt0 =
            service.calculateConfidence(
                reps = 5,
                rpe = 9f,
                percentOf1RM = 0f,
            )

        assertThat(confidenceNegative).isEqualTo(confidenceAt0)
    }

    // ===== shouldUpdateOneRM Tests =====

    @Test
    fun `shouldUpdateOneRM returns true for valid set with no current estimate`() {
        val set =
            createSetLog(
                weight = 100f,
                reps = 5,
                rpe = 8f,
            )

        val shouldUpdate =
            service.shouldUpdateOneRM(
                set = set,
                currentEstimate = null,
                newEstimate = 120f,
            )

        assertThat(shouldUpdate).isTrue()
    }

    @Test
    fun `shouldUpdateOneRM returns true when new estimate is higher`() {
        val set =
            createSetLog(
                weight = 100f,
                reps = 3,
                rpe = 9f,
            )

        val shouldUpdate =
            service.shouldUpdateOneRM(
                set = set,
                currentEstimate = 105f,
                newEstimate = 110f,
            )

        assertThat(shouldUpdate).isTrue()
    }

    @Test
    fun `shouldUpdateOneRM returns false when new estimate is lower`() {
        val set =
            createSetLog(
                weight = 80f,
                reps = 10,
                rpe = 7f,
            )

        val shouldUpdate =
            service.shouldUpdateOneRM(
                set = set,
                currentEstimate = 120f,
                newEstimate = 100f,
            )

        assertThat(shouldUpdate).isFalse()
    }

    @Test
    fun `shouldUpdateOneRM returns false for zero reps`() {
        val set =
            createSetLog(
                weight = 100f,
                reps = 0,
                rpe = null,
            )

        val shouldUpdate =
            service.shouldUpdateOneRM(
                set = set,
                currentEstimate = null,
                newEstimate = 100f,
            )

        assertThat(shouldUpdate).isFalse()
    }

    @Test
    fun `shouldUpdateOneRM returns false for zero weight`() {
        val set =
            createSetLog(
                weight = 0f,
                reps = 10,
                rpe = 8f,
            )

        val shouldUpdate =
            service.shouldUpdateOneRM(
                set = set,
                currentEstimate = null,
                newEstimate = 100f,
            )

        assertThat(shouldUpdate).isFalse()
    }

    @Test
    fun `shouldUpdateOneRM returns false for reps over 15`() {
        val set =
            createSetLog(
                weight = 50f,
                reps = 20,
                rpe = 8f,
            )

        val shouldUpdate =
            service.shouldUpdateOneRM(
                set = set,
                currentEstimate = null,
                newEstimate = 60f,
            )

        assertThat(shouldUpdate).isFalse()
    }

    @Test
    fun `shouldUpdateOneRM returns false for low RPE`() {
        val set =
            createSetLog(
                weight = 100f,
                reps = 5,
                rpe = 5f,
            )

        val shouldUpdate =
            service.shouldUpdateOneRM(
                set = set,
                currentEstimate = null,
                newEstimate = 110f,
            )

        assertThat(shouldUpdate).isFalse()
    }

    @Test
    fun `shouldUpdateOneRM returns false when load percentage is too low`() {
        val set =
            createSetLog(
                weight = 50f, // Only 50% of current max
                reps = 5,
                rpe = 9f,
            )

        val shouldUpdate =
            service.shouldUpdateOneRM(
                set = set,
                currentEstimate = 100f,
                newEstimate = 60f,
            )

        assertThat(shouldUpdate).isFalse()
    }

    @Test
    fun `shouldUpdateOneRM returns true when load percentage is exactly 60 percent`() {
        val set =
            createSetLog(
                weight = 60f,
                reps = 5,
                rpe = 9f,
            )

        val shouldUpdate =
            service.shouldUpdateOneRM(
                set = set,
                currentEstimate = 100f,
                newEstimate = 101f, // New estimate should be higher than current
            )

        assertThat(shouldUpdate).isTrue()
    }

    @Test
    fun `shouldUpdateOneRM checks confidence threshold`() {
        // Low confidence scenario: high reps, no RPE, low load
        val set =
            createSetLog(
                weight = 40f,
                reps = 15,
                rpe = null,
            )

        val shouldUpdate =
            service.shouldUpdateOneRM(
                set = set,
                currentEstimate = 100f,
                newEstimate = 50f,
            )

        assertThat(shouldUpdate).isFalse()
    }

    // ===== createOneRMRecord Tests =====

    @Test
    fun `createOneRMRecord creates record with all provided data`() {
        val set =
            createSetLog(
                weight = 100f,
                reps = 5,
                rpe = 8f,
            )

        val record =
            service.createOneRMRecord(
                exerciseId = "123",
                set = set,
                estimate = 115f,
                confidence = 0.85f,
            )

        assertThat(record.exerciseId).isEqualTo("123")
        assertThat(record.mostWeightLifted).isEqualTo(100f)
        assertThat(record.mostWeightReps).isEqualTo(5)
        assertThat(record.mostWeightRpe).isEqualTo(8f)
        assertThat(record.oneRMEstimate).isEqualTo(115f)
        assertThat(record.oneRMConfidence).isEqualTo(0.85f)
        assertThat(record.oneRMType).isEqualTo(OneRMType.AUTOMATICALLY_CALCULATED)
        assertThat(record.context).isEqualTo("100kg × 5 @ RPE 8")
    }

    @Test
    fun `createOneRMRecord uses provided mostWeightData when available`() {
        val set =
            createSetLog(
                weight = 90f,
                reps = 8,
                rpe = 7f,
            )

        val mostWeightData =
            OneRMService.MostWeightData(
                weight = 105f,
                reps = 3,
                rpe = 9f,
                date = LocalDateTime.of(2024, 1, 1, 12, 0),
            )

        val record =
            service.createOneRMRecord(
                exerciseId = "123",
                set = set,
                estimate = 120f,
                confidence = 0.9f,
                mostWeightData = mostWeightData,
            )

        assertThat(record.mostWeightLifted).isEqualTo(105f)
        assertThat(record.mostWeightReps).isEqualTo(3)
        assertThat(record.mostWeightRpe).isEqualTo(9f)
        assertThat(record.mostWeightDate).isEqualTo(LocalDateTime.of(2024, 1, 1, 12, 0))
        assertThat(record.context).isEqualTo("90kg × 8 @ RPE 7")
    }

    @Test
    fun `createOneRMRecord handles null RPE in context`() {
        val set =
            createSetLog(
                weight = 100f,
                reps = 5,
                rpe = null,
            )

        val record =
            service.createOneRMRecord(
                exerciseId = "123",
                set = set,
                estimate = 112f,
                confidence = 0.7f,
            )

        assertThat(record.context).isEqualTo("100kg × 5")
        assertThat(record.mostWeightRpe).isNull()
    }

    // ===== buildContext Tests =====

    @Test
    fun `buildContext formats weight with proper decimals`() {
        val context =
            service.buildContext(
                weight = 102.5f,
                reps = 3,
                rpe = 8.5f,
            )

        assertThat(context).isEqualTo("102.5kg × 3 @ RPE 8.5")
    }

    @Test
    fun `buildContext handles whole number weights`() {
        val context =
            service.buildContext(
                weight = 100f,
                reps = 5,
                rpe = 8f,
            )

        assertThat(context).isEqualTo("100kg × 5 @ RPE 8")
    }

    @Test
    fun `buildContext omits RPE when null`() {
        val context =
            service.buildContext(
                weight = 80f,
                reps = 10,
                rpe = null,
            )

        assertThat(context).isEqualTo("80kg × 10")
    }

    @Test
    fun `buildContext preserves decimal RPE values`() {
        val context1 = service.buildContext(weight = 100f, reps = 5, rpe = 7.5f)
        assertThat(context1).isEqualTo("100kg × 5 @ RPE 7.5")

        val context2 = service.buildContext(weight = 100f, reps = 5, rpe = 8.5f)
        assertThat(context2).isEqualTo("100kg × 5 @ RPE 8.5")

        val context3 = service.buildContext(weight = 100f, reps = 5, rpe = 9.0f)
        assertThat(context3).isEqualTo("100kg × 5 @ RPE 9")
    }

    // ===== Edge Cases and Integration Tests =====

    @Test
    fun `calculateEstimated1RM handles very light weights`() {
        val result =
            service.calculateEstimated1RM(
                weight = 2.5f,
                reps = 10,
                rpe = 8f,
            )

        assertThat(result).isNotNull()
        assertThat(result).isGreaterThan(2.5f)
    }

    @Test
    fun `calculateEstimated1RM handles very heavy weights`() {
        val result =
            service.calculateEstimated1RM(
                weight = 500f,
                reps = 1,
                rpe = 10f,
            )

        assertThat(result).isEqualTo(500f)
    }

    @Test
    fun `complete flow from set to 1RM record`() {
        val set =
            createSetLog(
                weight = 100f,
                reps = 5,
                rpe = 8f,
            )

        val estimate =
            service.calculateEstimated1RM(
                weight = set.actualWeight,
                reps = set.actualReps,
                rpe = set.actualRpe,
                scalingType = RMScalingType.STANDARD,
            )

        assertThat(estimate).isNotNull()

        val confidence =
            service.calculateConfidence(
                reps = set.actualReps,
                rpe = set.actualRpe,
                percentOf1RM = set.actualWeight / (estimate ?: 100f),
            )

        assertThat(confidence).isGreaterThan(0.5f)

        val shouldUpdate =
            service.shouldUpdateOneRM(
                set = set,
                currentEstimate = null,
                newEstimate = estimate!!,
            )

        assertThat(shouldUpdate).isTrue()

        val record =
            service.createOneRMRecord(
                exerciseId = "123",
                set = set,
                estimate = estimate,
                confidence = confidence,
            )

        assertThat(record.oneRMEstimate).isEqualTo(estimate)
        assertThat(record.oneRMConfidence).isEqualTo(confidence)
    }

    @Test
    fun `calculateEstimated1RM with extreme RPE values`() {
        // Test RPE 6.1 (just above threshold)
        val result1 = service.calculateEstimated1RM(100f, 5, 6.1f)
        assertThat(result1).isNotNull()

        // Test RPE 5.9 (just below threshold)
        val result2 = service.calculateEstimated1RM(100f, 5, 5.9f)
        assertThat(result2).isNull()

        // Test RPE 10 (maximum)
        val result3 = service.calculateEstimated1RM(100f, 3, 10f)
        assertThat(result3).isNotNull()
    }

    @Test
    fun `shouldUpdateOneRM handles edge case of equal estimates`() {
        val set = createSetLog(100f, 5, 9f)

        val shouldUpdate =
            service.shouldUpdateOneRM(
                set = set,
                currentEstimate = 115f,
                newEstimate = 115f,
            )

        assertThat(shouldUpdate).isFalse()
    }

    @Test
    fun `confidence calculation with various rep ranges`() {
        // Test confidence decreases as reps increase
        val conf1 = service.calculateConfidence(1, 9f, 0.9f)
        val conf3 = service.calculateConfidence(3, 9f, 0.9f)
        val conf5 = service.calculateConfidence(5, 9f, 0.9f)
        val conf8 = service.calculateConfidence(8, 9f, 0.9f)
        val conf12 = service.calculateConfidence(12, 9f, 0.9f)
        val conf15 = service.calculateConfidence(15, 9f, 0.9f)

        assertThat(conf1).isGreaterThan(conf3)
        assertThat(conf3).isGreaterThan(conf5)
        assertThat(conf5).isGreaterThan(conf8)
        assertThat(conf8).isGreaterThan(conf12)
        assertThat(conf12).isGreaterThan(conf15)
    }

    @Test
    fun `buildContext handles extreme values`() {
        val context1 = service.buildContext(0.5f, 20, null)
        assertThat(context1).isEqualTo("0.5kg × 20")

        val context2 = service.buildContext(999.5f, 1, 10f)
        assertThat(context2).isEqualTo("999.5kg × 1 @ RPE 10")

        val context3 = service.buildContext(100f, 5, 0.5f)
        assertThat(context3).isEqualTo("100kg × 5 @ RPE 0.5")
    }

    @Test
    fun `calculateEstimated1RM with decimal RPE values gives correct progression`() {
        val weight = 100f
        val reps = 5

        val rpe80 = service.calculateEstimated1RM(weight, reps, 8.0f)
        val rpe85 = service.calculateEstimated1RM(weight, reps, 8.5f)
        val rpe90 = service.calculateEstimated1RM(weight, reps, 9.0f)
        val rpe95 = service.calculateEstimated1RM(weight, reps, 9.5f)

        assertThat(rpe80).isNotNull()
        assertThat(rpe85).isNotNull()
        assertThat(rpe90).isNotNull()
        assertThat(rpe95).isNotNull()

        assertThat(rpe85).isLessThan(rpe80)
        assertThat(rpe90).isLessThan(rpe85)
        assertThat(rpe95).isLessThan(rpe90)
    }

    @Test
    fun `calculateEstimated1RM with RPE 8 point 5 produces correct RIR`() {
        val weight = 120f
        val reps = 5

        val with85 = service.calculateEstimated1RM(weight, reps, 8.5f)
        val with8 = service.calculateEstimated1RM(weight, reps, 8.0f)
        val with9 = service.calculateEstimated1RM(weight, reps, 9.0f)

        assertThat(with85).isNotNull()
        assertThat(with85).isLessThan(with8)
        assertThat(with85).isGreaterThan(with9)

        val ratio85to8 = with85!! / with8!!
        val ratio9to85 = with9!! / with85
        assertThat(ratio85to8).isLessThan(1f)
        assertThat(ratio9to85).isLessThan(1f)
    }

    // ===== Helper Functions =====

    private fun createSetLog(
        weight: Float,
        reps: Int,
        rpe: Float? = null,
    ): SetLog =
        SetLog(
            id = "1",
            exerciseLogId = "1",
            setOrder = 1,
            targetReps = reps,
            targetWeight = weight,
            actualReps = reps,
            actualWeight = weight,
            actualRpe = rpe,
            isCompleted = true,
            completedAt = LocalDateTime.now().toString(),
            tag = null,
            notes = null,
        )
}
