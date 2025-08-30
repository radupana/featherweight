package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SetLogTest {
    @Test
    fun setLog_withDefaultValues_hasCorrectDefaults() {
        // Act
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
            )

        // Assert
        assertThat(setLog.id).isEqualTo(0L)
        assertThat(setLog.exerciseLogId).isEqualTo(10L)
        assertThat(setLog.setOrder).isEqualTo(1)
        assertThat(setLog.targetReps).isNull()
        assertThat(setLog.targetWeight).isNull()
        assertThat(setLog.targetRpe).isNull()
        assertThat(setLog.actualReps).isEqualTo(0)
        assertThat(setLog.actualWeight).isEqualTo(0f)
        assertThat(setLog.actualRpe).isNull()
        assertThat(setLog.suggestedWeight).isNull()
        assertThat(setLog.suggestedReps).isNull()
        assertThat(setLog.suggestionSource).isNull()
        assertThat(setLog.suggestionConfidence).isNull()
        assertThat(setLog.calculationDetails).isNull()
        assertThat(setLog.tag).isNull()
        assertThat(setLog.notes).isNull()
        assertThat(setLog.isCompleted).isFalse()
        assertThat(setLog.completedAt).isNull()
    }

    @Test
    fun setLog_withAllValues_storesCorrectly() {
        // Arrange
        val timestamp = "2024-01-15T10:30:00"
        val suggestionJson = """{"source": "previous_performance"}"""
        val calculationJson = """{"formula": "brzycki", "estimated_1rm": 100}"""

        // Act
        val setLog =
            SetLog(
                id = 123L,
                exerciseLogId = 45L,
                setOrder = 3,
                targetReps = 10,
                targetWeight = 80f,
                targetRpe = 7.5f,
                actualReps = 8,
                actualWeight = 85f,
                actualRpe = 8.5f,
                suggestedWeight = 82.5f,
                suggestedReps = 9,
                suggestionSource = suggestionJson,
                suggestionConfidence = 0.85f,
                calculationDetails = calculationJson,
                tag = "warmup",
                notes = "Felt strong today",
                isCompleted = true,
                completedAt = timestamp,
            )

        // Assert
        assertThat(setLog.id).isEqualTo(123L)
        assertThat(setLog.exerciseLogId).isEqualTo(45L)
        assertThat(setLog.setOrder).isEqualTo(3)
        assertThat(setLog.targetReps).isEqualTo(10)
        assertThat(setLog.targetWeight).isEqualTo(80f)
        assertThat(setLog.targetRpe).isEqualTo(7.5f)
        assertThat(setLog.actualReps).isEqualTo(8)
        assertThat(setLog.actualWeight).isEqualTo(85f)
        assertThat(setLog.actualRpe).isEqualTo(8.5f)
        assertThat(setLog.suggestedWeight).isEqualTo(82.5f)
        assertThat(setLog.suggestedReps).isEqualTo(9)
        assertThat(setLog.suggestionSource).isEqualTo(suggestionJson)
        assertThat(setLog.suggestionConfidence).isEqualTo(0.85f)
        assertThat(setLog.calculationDetails).isEqualTo(calculationJson)
        assertThat(setLog.tag).isEqualTo("warmup")
        assertThat(setLog.notes).isEqualTo("Felt strong today")
        assertThat(setLog.isCompleted).isTrue()
        assertThat(setLog.completedAt).isEqualTo(timestamp)
    }

    @Test
    fun setLog_withZeroWeight_isValid() {
        // Act - Bodyweight exercise
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                actualReps = 15,
                actualWeight = 0f,
                isCompleted = true,
            )

        // Assert
        assertThat(setLog.actualWeight).isEqualTo(0f)
        assertThat(setLog.actualReps).isEqualTo(15)
        assertThat(setLog.isCompleted).isTrue()
    }

    @Test
    fun setLog_withHighRPE_isValid() {
        // Act
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                actualReps = 5,
                actualWeight = 100f,
                actualRpe = 10f, // Maximum RPE
            )

        // Assert
        assertThat(setLog.actualRpe).isEqualTo(10f)
    }

    @Test
    fun setLog_withLowRPE_isValid() {
        // Act
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                actualReps = 20,
                actualWeight = 50f,
                actualRpe = 1f, // Minimum RPE
            )

        // Assert
        assertThat(setLog.actualRpe).isEqualTo(1f)
    }

    @Test
    fun setLog_withFractionalRPE_isValid() {
        // Act - RPE can be in 0.5 increments
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                actualReps = 8,
                actualWeight = 75f,
                actualRpe = 7.5f,
            )

        // Assert
        assertThat(setLog.actualRpe).isEqualTo(7.5f)
    }

    @Test
    fun setLog_withHighConfidenceSuggestion_isValid() {
        // Act
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                suggestedWeight = 100f,
                suggestionConfidence = 1.0f, // Maximum confidence
            )

        // Assert
        assertThat(setLog.suggestionConfidence).isEqualTo(1.0f)
    }

    @Test
    fun setLog_withLowConfidenceSuggestion_isValid() {
        // Act
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                suggestedWeight = 100f,
                suggestionConfidence = 0.0f, // Minimum confidence
            )

        // Assert
        assertThat(setLog.suggestionConfidence).isEqualTo(0.0f)
    }

    @Test
    fun setLog_withTargetButNoActual_isNotCompleted() {
        // Act - Set with targets but not performed yet
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                targetReps = 10,
                targetWeight = 80f,
                targetRpe = 8f,
                actualReps = 0,
                actualWeight = 0f,
                isCompleted = false,
            )

        // Assert
        assertThat(setLog.targetReps).isEqualTo(10)
        assertThat(setLog.targetWeight).isEqualTo(80f)
        assertThat(setLog.targetRpe).isEqualTo(8f)
        assertThat(setLog.actualReps).isEqualTo(0)
        assertThat(setLog.actualWeight).isEqualTo(0f)
        assertThat(setLog.isCompleted).isFalse()
    }

    @Test
    fun setLog_withActualDifferentFromTarget_isValid() {
        // Act - User did different weight/reps than planned
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                targetReps = 10,
                targetWeight = 80f,
                actualReps = 8, // Did fewer reps
                actualWeight = 85f, // But with more weight
                isCompleted = true,
            )

        // Assert
        assertThat(setLog.targetReps).isEqualTo(10)
        assertThat(setLog.targetWeight).isEqualTo(80f)
        assertThat(setLog.actualReps).isEqualTo(8)
        assertThat(setLog.actualWeight).isEqualTo(85f)
        assertThat(setLog.isCompleted).isTrue()
    }

    @Test
    fun setLog_withTagWarmup_isValid() {
        // Act
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                tag = "warmup",
            )

        // Assert
        assertThat(setLog.tag).isEqualTo("warmup")
    }

    @Test
    fun setLog_withTagWorking_isValid() {
        // Act
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                tag = "working",
            )

        // Assert
        assertThat(setLog.tag).isEqualTo("working")
    }

    @Test
    fun setLog_withTagDropset_isValid() {
        // Act
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                tag = "dropset",
            )

        // Assert
        assertThat(setLog.tag).isEqualTo("dropset")
    }

    @Test
    fun setLog_withComplexCalculationDetails_storesJson() {
        // Arrange
        val complexJson =
            """
            {
                "formula": "brzycki",
                "estimated_1rm": 120.5,
                "percentage_used": 85,
                "previous_sets": [
                    {"weight": 100, "reps": 8},
                    {"weight": 105, "reps": 6}
                ],
                "confidence_factors": {
                    "recent_performance": 0.8,
                    "exercise_familiarity": 0.9,
                    "recovery_status": 0.7
                }
            }
            """.trimIndent()

        // Act
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                calculationDetails = complexJson,
            )

        // Assert
        assertThat(setLog.calculationDetails).isEqualTo(complexJson)
    }

    @Test
    fun setLog_copy_createsNewInstance() {
        // Arrange
        val original =
            SetLog(
                id = 1L,
                exerciseLogId = 10L,
                setOrder = 1,
                actualReps = 10,
                actualWeight = 100f,
            )

        // Act
        val copy =
            original.copy(
                actualReps = 12,
                actualWeight = 105f,
            )

        // Assert
        assertThat(copy.id).isEqualTo(1L)
        assertThat(copy.exerciseLogId).isEqualTo(10L)
        assertThat(copy.setOrder).isEqualTo(1)
        assertThat(copy.actualReps).isEqualTo(12)
        assertThat(copy.actualWeight).isEqualTo(105f)

        // Original should be unchanged
        assertThat(original.actualReps).isEqualTo(10)
        assertThat(original.actualWeight).isEqualTo(100f)
    }

    @Test
    fun setLog_equals_worksCorrectly() {
        // Arrange
        val set1 =
            SetLog(
                id = 1L,
                exerciseLogId = 10L,
                setOrder = 1,
                actualReps = 10,
                actualWeight = 100f,
            )

        val set2 =
            SetLog(
                id = 1L,
                exerciseLogId = 10L,
                setOrder = 1,
                actualReps = 10,
                actualWeight = 100f,
            )

        val set3 =
            SetLog(
                id = 2L, // Different ID
                exerciseLogId = 10L,
                setOrder = 1,
                actualReps = 10,
                actualWeight = 100f,
            )

        // Assert
        assertThat(set1).isEqualTo(set2)
        assertThat(set1).isNotEqualTo(set3)
    }

    @Test
    fun setLog_hashCode_isConsistent() {
        // Arrange
        val set1 =
            SetLog(
                id = 1L,
                exerciseLogId = 10L,
                setOrder = 1,
                actualReps = 10,
                actualWeight = 100f,
            )

        val set2 =
            SetLog(
                id = 1L,
                exerciseLogId = 10L,
                setOrder = 1,
                actualReps = 10,
                actualWeight = 100f,
            )

        // Assert
        assertThat(set1.hashCode()).isEqualTo(set2.hashCode())
    }

    // New tests for targetRpe functionality

    @Test
    fun setLog_withTargetRpe_storesCorrectly() {
        // Act
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                targetRpe = 7f,
            )

        // Assert
        assertThat(setLog.targetRpe).isEqualTo(7f)
        assertThat(setLog.actualRpe).isNull()
    }

    @Test
    fun setLog_withTargetRpeAndActualRpe_storesBoth() {
        // Act
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                targetRpe = 7f,
                actualRpe = 8.5f,
            )

        // Assert
        assertThat(setLog.targetRpe).isEqualTo(7f)
        assertThat(setLog.actualRpe).isEqualTo(8.5f)
    }

    @Test
    fun setLog_withFractionalTargetRpe_isValid() {
        // Act - RPE can be in 0.5 increments
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                targetRpe = 6.5f,
            )

        // Assert
        assertThat(setLog.targetRpe).isEqualTo(6.5f)
    }

    @Test
    fun setLog_withMinimumTargetRpe_isValid() {
        // Act
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                targetRpe = 1f,
            )

        // Assert
        assertThat(setLog.targetRpe).isEqualTo(1f)
    }

    @Test
    fun setLog_withMaximumTargetRpe_isValid() {
        // Act
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                targetRpe = 10f,
            )

        // Assert
        assertThat(setLog.targetRpe).isEqualTo(10f)
    }

    @Test
    fun setLog_withProgrammeTargets_hasAllTargets() {
        // Act - Programme workout with all targets specified
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                targetReps = 8,
                targetWeight = 100f,
                targetRpe = 8f,
            )

        // Assert
        assertThat(setLog.targetReps).isEqualTo(8)
        assertThat(setLog.targetWeight).isEqualTo(100f)
        assertThat(setLog.targetRpe).isEqualTo(8f)
    }

    @Test
    fun setLog_withActualRpeDifferentFromTarget_isValid() {
        // Act - User felt different RPE than planned
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                targetRpe = 7f, // Planned RPE 7
                actualRpe = 9f, // Actually felt like RPE 9
                actualReps = 8,
                actualWeight = 100f,
                isCompleted = true,
            )

        // Assert
        assertThat(setLog.targetRpe).isEqualTo(7f)
        assertThat(setLog.actualRpe).isEqualTo(9f)
        assertThat(setLog.isCompleted).isTrue()
    }

    @Test
    fun setLog_withOnlyTargetRpe_canBeUsedForBodyweightExercises() {
        // Act - Bodyweight exercise with RPE target
        val setLog =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                targetReps = 15,
                targetRpe = 8f,
                targetWeight = null, // No weight for bodyweight exercises
            )

        // Assert
        assertThat(setLog.targetReps).isEqualTo(15)
        assertThat(setLog.targetRpe).isEqualTo(8f)
        assertThat(setLog.targetWeight).isNull()
    }

    @Test
    fun setLog_copyWithTargetRpe_preservesValue() {
        // Arrange
        val original =
            SetLog(
                exerciseLogId = 10L,
                setOrder = 1,
                targetRpe = 7.5f,
                actualRpe = 8f,
            )

        // Act
        val copy =
            original.copy(
                actualRpe = 9f,
            )

        // Assert
        assertThat(copy.targetRpe).isEqualTo(7.5f) // Target unchanged
        assertThat(copy.actualRpe).isEqualTo(9f) // Actual updated
        assertThat(original.actualRpe).isEqualTo(8f) // Original unchanged
    }
}
