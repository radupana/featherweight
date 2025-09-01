package com.github.radupana.featherweight.data.programme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WeightCalculationRulesTest {

    @Test
    fun suggestedWeight_withAllParameters_shouldStoreCorrectly() {
        val suggested = SuggestedWeight(
            weight = 100f,
            reps = 3..5,
            rpe = 8.5f,
            source = "75% of 1RM",
            confidence = 0.9f
        )

        assertThat(suggested.weight).isEqualTo(100f)
        assertThat(suggested.reps).isEqualTo(3..5)
        assertThat(suggested.rpe).isEqualTo(8.5f)
        assertThat(suggested.source).isEqualTo("75% of 1RM")
        assertThat(suggested.confidence).isEqualTo(0.9f)
    }

    @Test
    fun suggestedWeight_withNullRpe_shouldHandleNoRpeData() {
        val suggested = SuggestedWeight(
            weight = 80f,
            reps = 8..10,
            rpe = null,
            source = "Last week + 2.5kg",
            confidence = 0.7f
        )

        assertThat(suggested.rpe).isNull()
        assertThat(suggested.weight).isEqualTo(80f)
    }

    @Test
    fun suggestedWeight_withSingleRepRange_shouldHandleFixedReps() {
        val suggested = SuggestedWeight(
            weight = 120f,
            reps = 1..1,
            rpe = 10f,
            source = "1RM attempt",
            confidence = 0.5f
        )

        assertThat(suggested.reps.first).isEqualTo(1)
        assertThat(suggested.reps.last).isEqualTo(1)
        assertThat(suggested.rpe).isEqualTo(10f)
    }

    @Test
    fun suggestedWeight_withLowConfidence_shouldIndicateUncertainty() {
        val suggested = SuggestedWeight(
            weight = 60f,
            reps = 12..15,
            rpe = 7f,
            source = "Estimated from similar exercise",
            confidence = 0.3f
        )

        assertThat(suggested.confidence).isLessThan(0.5f)
        assertThat(suggested.source).contains("Estimated")
    }

    @Test
    fun suggestedWeight_withHighConfidence_shouldIndicateCertainty() {
        val suggested = SuggestedWeight(
            weight = 90f,
            reps = 5..5,
            rpe = 8f,
            source = "Based on recent test",
            confidence = 0.95f
        )

        assertThat(suggested.confidence).isGreaterThan(0.9f)
    }

    @Test
    fun weightCalculationRules_withDefaults_shouldUseStandardValues() {
        val rules = WeightCalculationRules()

        assertThat(rules.baseOn).isEqualTo(WeightBasis.ONE_REP_MAX)
        assertThat(rules.trainingMaxPercentage).isEqualTo(0.9f)
        assertThat(rules.roundingIncrement).isEqualTo(2.5f)
        assertThat(rules.minimumBarWeight).isEqualTo(20f)
        assertThat(rules.unit).isEqualTo(WeightUnit.KG)
    }

    @Test
    fun weightCalculationRules_withCustomValues_shouldOverrideDefaults() {
        val rules = WeightCalculationRules(
            baseOn = WeightBasis.TRAINING_MAX,
            trainingMaxPercentage = 0.85f,
            roundingIncrement = 5f,
            minimumBarWeight = 45f,
            unit = WeightUnit.LB
        )

        assertThat(rules.baseOn).isEqualTo(WeightBasis.TRAINING_MAX)
        assertThat(rules.trainingMaxPercentage).isEqualTo(0.85f)
        assertThat(rules.roundingIncrement).isEqualTo(5f)
        assertThat(rules.minimumBarWeight).isEqualTo(45f)
        assertThat(rules.unit).isEqualTo(WeightUnit.LB)
    }

    @Test
    fun weightBasis_allEnumValues_shouldBeDefined() {
        val values = WeightBasis.values()
        
        assertThat(values).asList().contains(WeightBasis.ONE_REP_MAX)
        assertThat(values).asList().contains(WeightBasis.TRAINING_MAX)
        assertThat(values).asList().contains(WeightBasis.LAST_WORKOUT)
        assertThat(values).asList().contains(WeightBasis.FIXED_WEIGHT)
        assertThat(values).asList().contains(WeightBasis.BODYWEIGHT)
        assertThat(values).asList().contains(WeightBasis.PERCENTAGE_BASED)
        assertThat(values).hasLength(6)
    }

    @Test
    fun weightUnit_allEnumValues_shouldBeDefined() {
        val values = WeightUnit.values()
        
        assertThat(values).asList().contains(WeightUnit.KG)
        assertThat(values).asList().contains(WeightUnit.LB)
        assertThat(values).hasLength(2)
    }

    @Test
    fun progressionRules_withDefaults_shouldUseStandardValues() {
        val rules = ProgressionRules()

        assertThat(rules.type).isEqualTo(ProgressionType.LINEAR)
        assertThat(rules.incrementRules).isNotEmpty()
        assertThat(rules.successCriteria).isNotNull()
        assertThat(rules.deloadRules).isNotNull()
        assertThat(rules.cycleLength).isNull()
        assertThat(rules.weeklyPercentages).isNull()
        assertThat(rules.autoProgressionEnabled).isTrue()
    }

    @Test
    fun progressionRules_withWaveProgression_shouldHandleCycles() {
        val rules = ProgressionRules(
            type = ProgressionType.WAVE,
            cycleLength = 3,
            weeklyPercentages = listOf(
                listOf(0.7f, 0.8f, 0.9f),
                listOf(0.75f, 0.85f, 0.95f),
                listOf(0.8f, 0.9f, 1.0f)
            ),
            autoProgressionEnabled = false
        )

        assertThat(rules.type).isEqualTo(ProgressionType.WAVE)
        assertThat(rules.cycleLength).isEqualTo(3)
        assertThat(rules.weeklyPercentages).hasSize(3)
        assertThat(rules.weeklyPercentages!![0]).hasSize(3)
        assertThat(rules.autoProgressionEnabled).isFalse()
    }

    @Test
    fun progressionType_allEnumValues_shouldBeDefined() {
        val values = ProgressionType.values()
        
        assertThat(values).asList().contains(ProgressionType.LINEAR)
        assertThat(values).asList().contains(ProgressionType.DOUBLE)
        assertThat(values).asList().contains(ProgressionType.WAVE)
        assertThat(values).asList().contains(ProgressionType.PERCENTAGE_BASED)
        assertThat(values).asList().contains(ProgressionType.RPE_BASED)
        assertThat(values).asList().contains(ProgressionType.CUSTOM)
        assertThat(values).hasLength(6)
    }

    @Test
    fun successCriteria_withAllParameters_shouldValidateCorrectly() {
        val criteria = SuccessCriteria(
            requiredSets = 5,
            requiredReps = 5,
            allowedMissedReps = 2,
            minRPE = 6f,
            maxRPE = 8f,
            techniqueRequirement = true
        )

        assertThat(criteria.requiredSets).isEqualTo(5)
        assertThat(criteria.requiredReps).isEqualTo(5)
        assertThat(criteria.allowedMissedReps).isEqualTo(2)
        assertThat(criteria.minRPE).isEqualTo(6f)
        assertThat(criteria.maxRPE).isEqualTo(8f)
        assertThat(criteria.techniqueRequirement).isTrue()
    }

    @Test
    fun successCriteria_withNullValues_shouldHandleFlexibleCriteria() {
        val criteria = SuccessCriteria(
            requiredSets = null,
            requiredReps = null,
            allowedMissedReps = 0,
            minRPE = null,
            maxRPE = null,
            techniqueRequirement = false
        )

        assertThat(criteria.requiredSets).isNull()
        assertThat(criteria.requiredReps).isNull()
        assertThat(criteria.minRPE).isNull()
        assertThat(criteria.maxRPE).isNull()
        assertThat(criteria.techniqueRequirement).isFalse()
    }

    @Test
    fun deloadRules_withDefaults_shouldUseStandardValues() {
        val rules = DeloadRules()

        assertThat(rules.triggerAfterFailures).isEqualTo(3)
        assertThat(rules.deloadPercentage).isEqualTo(0.85f)
        assertThat(rules.minimumWeight).isEqualTo(20f)
        assertThat(rules.autoDeload).isTrue()
    }

    @Test
    fun deloadRules_withCustomValues_shouldOverrideDefaults() {
        val rules = DeloadRules(
            triggerAfterFailures = 2,
            deloadPercentage = 0.9f,
            minimumWeight = 45f,
            autoDeload = false
        )

        assertThat(rules.triggerAfterFailures).isEqualTo(2)
        assertThat(rules.deloadPercentage).isEqualTo(0.9f)
        assertThat(rules.minimumWeight).isEqualTo(45f)
        assertThat(rules.autoDeload).isFalse()
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
    fun progressionTemplates_strongLifts5x5_shouldHaveCorrectSettings() {
        val template = ProgressionTemplates.strongLifts5x5

        assertThat(template.type).isEqualTo(ProgressionType.LINEAR)
        assertThat(template.incrementRules["squat"]).isEqualTo(5.0f)
        assertThat(template.incrementRules["bench press"]).isEqualTo(2.5f)
        assertThat(template.successCriteria.requiredSets).isEqualTo(5)
        assertThat(template.successCriteria.requiredReps).isEqualTo(5)
        assertThat(template.deloadRules.triggerAfterFailures).isEqualTo(3)
        assertThat(template.deloadRules.deloadPercentage).isEqualTo(0.85f)
    }

    @Test
    fun progressionTemplates_startingStrength_shouldHaveCorrectSettings() {
        val template = ProgressionTemplates.startingStrength

        assertThat(template.type).isEqualTo(ProgressionType.LINEAR)
        assertThat(template.incrementRules["squat"]).isEqualTo(5.0f)
        assertThat(template.incrementRules["deadlift"]).isEqualTo(10.0f)
        assertThat(template.incrementRules["power clean"]).isEqualTo(2.5f)
        assertThat(template.successCriteria.requiredSets).isEqualTo(3)
        assertThat(template.successCriteria.requiredReps).isEqualTo(5)
        assertThat(template.successCriteria.allowedMissedReps).isEqualTo(0)
    }

    @Test
    fun progressionTemplates_fiveThreeOne_shouldHaveWaveSettings() {
        val template = ProgressionTemplates.fiveThreeOne

        assertThat(template.type).isEqualTo(ProgressionType.WAVE)
        assertThat(template.cycleLength).isEqualTo(3)
        assertThat(template.weeklyPercentages).hasSize(3)
        
        val week1 = template.weeklyPercentages!![0]
        assertThat(week1).containsExactly(0.65f, 0.75f, 0.85f).inOrder()
        
        val week2 = template.weeklyPercentages!![1]
        assertThat(week2).containsExactly(0.70f, 0.80f, 0.90f).inOrder()
        
        val week3 = template.weeklyPercentages!![2]
        assertThat(week3).containsExactly(0.75f, 0.85f, 0.95f).inOrder()
        
        assertThat(template.successCriteria.requiredSets).isNull()
        assertThat(template.successCriteria.requiredReps).isNull()
    }

    @Test
    fun weightCalculationRules_forBodyweightExercise_shouldNotUseBarWeight() {
        val rules = WeightCalculationRules(
            baseOn = WeightBasis.BODYWEIGHT,
            trainingMaxPercentage = 1.0f,
            roundingIncrement = 1f,
            minimumBarWeight = 0f,
            unit = WeightUnit.KG
        )

        assertThat(rules.baseOn).isEqualTo(WeightBasis.BODYWEIGHT)
        assertThat(rules.minimumBarWeight).isEqualTo(0f)
    }

    @Test
    fun progressionRules_withRpeBased_shouldAllowAutoregulation() {
        val rules = ProgressionRules(
            type = ProgressionType.RPE_BASED,
            successCriteria = SuccessCriteria(
                minRPE = 7f,
                maxRPE = 9f,
                techniqueRequirement = true
            ),
            autoProgressionEnabled = true
        )

        assertThat(rules.type).isEqualTo(ProgressionType.RPE_BASED)
        assertThat(rules.successCriteria.minRPE).isEqualTo(7f)
        assertThat(rules.successCriteria.maxRPE).isEqualTo(9f)
    }

    @Test
    fun suggestedWeight_withWideRepRange_shouldHandleVolumeWork() {
        val suggested = SuggestedWeight(
            weight = 50f,
            reps = 15..20,
            rpe = 6f,
            source = "Light volume work",
            confidence = 0.8f
        )

        val repRange = suggested.reps.last - suggested.reps.first
        assertThat(repRange).isEqualTo(5)
        assertThat(suggested.rpe).isLessThan(7f)
    }

    @Test
    fun weightCalculationRules_dataClassEquality_shouldWorkCorrectly() {
        val rules1 = WeightCalculationRules(
            baseOn = WeightBasis.TRAINING_MAX,
            trainingMaxPercentage = 0.85f,
            roundingIncrement = 2.5f,
            minimumBarWeight = 20f,
            unit = WeightUnit.KG
        )
        
        val rules2 = WeightCalculationRules(
            baseOn = WeightBasis.TRAINING_MAX,
            trainingMaxPercentage = 0.85f,
            roundingIncrement = 2.5f,
            minimumBarWeight = 20f,
            unit = WeightUnit.KG
        )
        
        assertThat(rules1).isEqualTo(rules2)
        assertThat(rules1.hashCode()).isEqualTo(rules2.hashCode())
    }

    @Test
    fun progressionRules_withPercentageBased_shouldUsePercentages() {
        val rules = ProgressionRules(
            type = ProgressionType.PERCENTAGE_BASED,
            incrementRules = mapOf("squat" to 2.5f),
            autoProgressionEnabled = true
        )

        assertThat(rules.type).isEqualTo(ProgressionType.PERCENTAGE_BASED)
        assertThat(rules.incrementRules["squat"]).isEqualTo(2.5f)
    }

    @Test
    fun deloadRules_withHighTriggerThreshold_shouldDelayDeload() {
        val rules = DeloadRules(
            triggerAfterFailures = 5,
            deloadPercentage = 0.8f,
            minimumWeight = 20f,
            autoDeload = true
        )

        assertThat(rules.triggerAfterFailures).isGreaterThan(3)
        assertThat(rules.deloadPercentage).isLessThan(0.85f)
    }
}