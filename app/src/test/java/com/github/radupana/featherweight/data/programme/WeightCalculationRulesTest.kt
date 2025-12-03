package com.github.radupana.featherweight.data.programme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WeightCalculationRulesTest {
    @Test
    fun progressionRules_withWaveProgression_shouldHandleCycles() {
        val rules =
            ProgressionRules(
                type = ProgressionType.WAVE,
                cycleLength = 3,
                weeklyPercentages =
                    listOf(
                        listOf(0.7f, 0.8f, 0.9f),
                        listOf(0.75f, 0.85f, 0.95f),
                        listOf(0.8f, 0.9f, 1.0f),
                    ),
                autoProgressionEnabled = false,
            )

        assertThat(rules.type).isEqualTo(ProgressionType.WAVE)
        assertThat(rules.cycleLength).isEqualTo(3)
        assertThat(rules.weeklyPercentages).hasSize(3)
        assertThat(rules.weeklyPercentages!![0]).hasSize(3)
        assertThat(rules.autoProgressionEnabled).isFalse()
    }

    @Test
    fun successCriteria_withAllParameters_shouldValidateCorrectly() {
        val criteria =
            SuccessCriteria(
                requiredSets = 5,
                requiredReps = 5,
                allowedMissedReps = 2,
                minRPE = 6f,
                maxRPE = 8f,
                techniqueRequirement = true,
            )

        assertThat(criteria.requiredSets).isEqualTo(5)
        assertThat(criteria.requiredReps).isEqualTo(5)
        assertThat(criteria.allowedMissedReps).isEqualTo(2)
        assertThat(criteria.minRPE).isEqualTo(6f)
        assertThat(criteria.maxRPE).isEqualTo(8f)
        assertThat(criteria.techniqueRequirement).isTrue()
    }

    @Test
    fun defaultIncrements_shouldContainCommonExercises() {
        val increments = defaultIncrements()

        assertThat(increments["squat"]).isEqualTo(5.0f)
        assertThat(increments["deadlift"]).isEqualTo(5.0f)
        assertThat(increments["bench press"]).isEqualTo(2.5f)
        assertThat(increments["overhead press"]).isEqualTo(2.5f)
        assertThat(increments["barbell row"]).isEqualTo(2.5f)
        assertThat(increments["default"]).isEqualTo(2.5f)
    }

    @Test
    fun defaultIncrements_lowerBodyExercises_shouldHaveLargerIncrements() {
        val increments = defaultIncrements()

        assertThat(increments["squat"]).isGreaterThan(increments["bench press"]!!)
        assertThat(increments["deadlift"]).isGreaterThan(increments["overhead press"]!!)
        assertThat(increments["leg press"]).isEqualTo(10.0f)
    }

    @Test
    fun `ProgressionRules should have correct default settings`() {
        val rules = ProgressionRules()

        assertThat(rules.type).isEqualTo(ProgressionType.LINEAR)
        assertThat(rules.autoProgressionEnabled).isTrue()
        assertThat(rules.successCriteria.techniqueRequirement).isTrue()
        assertThat(rules.successCriteria.allowedMissedReps).isEqualTo(0)
        assertThat(rules.deloadRules.triggerAfterFailures).isEqualTo(3)
        assertThat(rules.deloadRules.deloadPercentage).isEqualTo(0.85f)
        assertThat(rules.deloadRules.minimumWeight).isEqualTo(20f)
        assertThat(rules.deloadRules.autoDeload).isTrue()
    }

    @Test
    fun `defaultIncrements should have correct values for different exercises`() {
        val increments = defaultIncrements()

        // Lower body exercises should have larger increments
        assertThat(increments["squat"]).isEqualTo(5.0f)
        assertThat(increments["deadlift"]).isEqualTo(5.0f)
        assertThat(increments["front squat"]).isEqualTo(5.0f)
        assertThat(increments["romanian deadlift"]).isEqualTo(5.0f)
        assertThat(increments["leg press"]).isEqualTo(10.0f)

        // Upper body exercises should have smaller increments
        assertThat(increments["bench press"]).isEqualTo(2.5f)
        assertThat(increments["overhead press"]).isEqualTo(2.5f)
        assertThat(increments["incline bench press"]).isEqualTo(2.5f)
    }

    @Test
    fun `ProgressionRules can be configured for wave periodization`() {
        val wavePercentages =
            listOf(
                listOf(0.65f, 0.75f, 0.85f), // Week 1
                listOf(0.70f, 0.80f, 0.90f), // Week 2
                listOf(0.75f, 0.85f, 0.95f), // Week 3
            )

        val rules =
            ProgressionRules(
                type = ProgressionType.WAVE,
                cycleLength = 3,
                weeklyPercentages = wavePercentages,
                successCriteria =
                    SuccessCriteria(
                        requiredSets = null,
                        requiredReps = null,
                    ),
            )

        assertThat(rules.type).isEqualTo(ProgressionType.WAVE)
        assertThat(rules.cycleLength).isEqualTo(3)
        assertThat(rules.weeklyPercentages).hasSize(3)
        assertThat(rules.weeklyPercentages!![0]).containsExactly(0.65f, 0.75f, 0.85f).inOrder()
        assertThat(rules.weeklyPercentages!![1]).containsExactly(0.70f, 0.80f, 0.90f).inOrder()
        assertThat(rules.weeklyPercentages!![2]).containsExactly(0.75f, 0.85f, 0.95f).inOrder()
        assertThat(rules.successCriteria.requiredSets).isNull()
        assertThat(rules.successCriteria.requiredReps).isNull()
    }

    @Test
    fun weightCalculationRules_forBodyweightExercise_shouldNotUseBarWeight() {
        val rules =
            WeightCalculationRules(
                baseOn = WeightBasis.BODYWEIGHT,
                trainingMaxPercentage = 1.0f,
                roundingIncrement = 1f,
                minimumBarWeight = 0f,
                unit = WeightUnit.KG,
            )

        assertThat(rules.baseOn).isEqualTo(WeightBasis.BODYWEIGHT)
        assertThat(rules.minimumBarWeight).isEqualTo(0f)
    }

    @Test
    fun progressionRules_withRpeBased_shouldAllowAutoregulation() {
        val rules =
            ProgressionRules(
                type = ProgressionType.RPE_BASED,
                successCriteria =
                    SuccessCriteria(
                        minRPE = 7f,
                        maxRPE = 9f,
                        techniqueRequirement = true,
                    ),
                autoProgressionEnabled = true,
            )

        assertThat(rules.type).isEqualTo(ProgressionType.RPE_BASED)
        assertThat(rules.successCriteria.minRPE).isEqualTo(7f)
        assertThat(rules.successCriteria.maxRPE).isEqualTo(9f)
    }

    @Test
    fun suggestedWeight_withWideRepRange_shouldHandleVolumeWork() {
        val suggested =
            SuggestedWeight(
                weight = 50f,
                reps = 15..20,
                rpe = 6f,
                source = "Light volume work",
                confidence = 0.8f,
            )

        val repRange = suggested.reps.last - suggested.reps.first
        assertThat(repRange).isEqualTo(5)
        assertThat(suggested.rpe).isLessThan(7f)
    }

    @Test
    fun weightCalculationRules_dataClassEquality_shouldWorkCorrectly() {
        val rules1 =
            WeightCalculationRules(
                baseOn = WeightBasis.TRAINING_MAX,
                trainingMaxPercentage = 0.85f,
                roundingIncrement = 2.5f,
                minimumBarWeight = 20f,
                unit = WeightUnit.KG,
            )

        val rules2 =
            WeightCalculationRules(
                baseOn = WeightBasis.TRAINING_MAX,
                trainingMaxPercentage = 0.85f,
                roundingIncrement = 2.5f,
                minimumBarWeight = 20f,
                unit = WeightUnit.KG,
            )

        assertThat(rules1).isEqualTo(rules2)
        assertThat(rules1.hashCode()).isEqualTo(rules2.hashCode())
    }

    @Test
    fun progressionRules_withPercentageBased_shouldUsePercentages() {
        val rules =
            ProgressionRules(
                type = ProgressionType.PERCENTAGE_BASED,
                incrementRules = mapOf("squat" to 2.5f),
                autoProgressionEnabled = true,
            )

        assertThat(rules.type).isEqualTo(ProgressionType.PERCENTAGE_BASED)
        assertThat(rules.incrementRules["squat"]).isEqualTo(2.5f)
    }
}
