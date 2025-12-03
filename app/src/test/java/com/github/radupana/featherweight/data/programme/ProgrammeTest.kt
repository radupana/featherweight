package com.github.radupana.featherweight.data.programme

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class ProgrammeTest {
    @Test
    fun programme_completedStatusWithoutCompletedAt_shouldThrowException() {
        assertFailsWith<IllegalArgumentException> {
            Programme(
                name = "Invalid Programme",
                description = null,
                durationWeeks = 8,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.BEGINNER,
                isActive = false,
                status = ProgrammeStatus.COMPLETED,
                startedAt = LocalDateTime.now(),
                completedAt = null, // Invalid: COMPLETED must have completedAt
            )
        }
    }

    @Test
    fun programme_notCompletedStatusWithCompletedAt_shouldThrowException() {
        assertFailsWith<IllegalArgumentException> {
            Programme(
                name = "Invalid Programme",
                description = null,
                durationWeeks = 8,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.BEGINNER,
                isActive = true,
                status = ProgrammeStatus.IN_PROGRESS,
                startedAt = LocalDateTime.now(),
                completedAt = LocalDateTime.now(), // Invalid: IN_PROGRESS cannot have completedAt
            )
        }
    }

    @Test
    fun programme_completedStatusCannotBeActive_shouldThrowException() {
        assertFailsWith<IllegalArgumentException> {
            Programme(
                name = "Invalid Programme",
                description = null,
                durationWeeks = 8,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.BEGINNER,
                isActive = true, // Invalid: COMPLETED cannot be active
                status = ProgrammeStatus.COMPLETED,
                startedAt = LocalDateTime.now().minusWeeks(8),
                completedAt = LocalDateTime.now(),
            )
        }
    }

    @Test
    fun programme_cancelledStatusCannotBeActive_shouldThrowException() {
        assertFailsWith<IllegalArgumentException> {
            Programme(
                name = "Cancelled Programme",
                description = null,
                durationWeeks = 12,
                programmeType = ProgrammeType.HYBRID,
                difficulty = ProgrammeDifficulty.ADVANCED,
                isActive = true, // Invalid: CANCELLED cannot be active
                status = ProgrammeStatus.CANCELLED,
                startedAt = LocalDateTime.now().minusWeeks(2),
            )
        }
    }

    @Test
    fun programme_notStartedStatusCannotHaveStartedAt_shouldThrowException() {
        assertFailsWith<IllegalArgumentException> {
            Programme(
                name = "Not Started Programme",
                description = null,
                durationWeeks = 6,
                programmeType = ProgrammeType.GENERAL_FITNESS,
                difficulty = ProgrammeDifficulty.NOVICE,
                isActive = false,
                status = ProgrammeStatus.NOT_STARTED,
                startedAt = LocalDateTime.now(), // Invalid: NOT_STARTED cannot have startedAt
            )
        }
    }

    @Test
    fun programme_inProgressStatusMustHaveStartedAt_shouldThrowException() {
        assertFailsWith<IllegalArgumentException> {
            Programme(
                name = "In Progress Programme",
                description = null,
                durationWeeks = 10,
                programmeType = ProgrammeType.OLYMPIC_LIFTING,
                difficulty = ProgrammeDifficulty.EXPERT,
                isActive = true,
                status = ProgrammeStatus.IN_PROGRESS,
                startedAt = null, // Invalid: IN_PROGRESS must have startedAt
            )
        }
    }

    @Test
    fun programme_withWeightCalculationRules_shouldSerializeCorrectly() {
        val rules =
            WeightCalculationRules(
                baseOn = WeightBasis.TRAINING_MAX,
                trainingMaxPercentage = 0.9f,
                roundingIncrement = 2.5f,
                minimumBarWeight = 20f,
                unit = WeightUnit.KG,
            )

        val programme =
            Programme(
                name = "Programme with Rules",
                description = null,
                durationWeeks = 8,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.INTERMEDIATE,
                weightCalculationRules = Programme.encodeWeightCalculationRules(rules),
            )

        val decodedRules = programme.getWeightCalculationRulesObject()
        assertThat(decodedRules).isNotNull()
        assertThat(decodedRules?.baseOn).isEqualTo(WeightBasis.TRAINING_MAX)
        assertThat(decodedRules?.trainingMaxPercentage).isEqualTo(0.9f)
    }

    @Test
    fun programme_withProgressionRules_shouldSerializeCorrectly() {
        val rules =
            ProgressionRules(
                type = ProgressionType.LINEAR,
                incrementRules = mapOf("squat" to 5f, "bench" to 2.5f),
                autoProgressionEnabled = true,
            )

        val programme =
            Programme(
                name = "Programme with Progression",
                description = null,
                durationWeeks = 12,
                programmeType = ProgrammeType.STRENGTH,
                difficulty = ProgrammeDifficulty.BEGINNER,
                progressionRules = Programme.encodeProgressionRules(rules),
            )

        val decodedRules = programme.getProgressionRulesObject()
        assertThat(decodedRules).isNotNull()
        assertThat(decodedRules?.type).isEqualTo(ProgressionType.LINEAR)
        assertThat(decodedRules?.incrementRules?.get("squat")).isEqualTo(5f)
        assertThat(decodedRules?.autoProgressionEnabled).isTrue()
    }

    // Note: Tests for invalid JSON parsing are omitted because they require Android's Log class
    // which is not available in unit tests. The implementation correctly returns null for invalid JSON.

    @Test
    fun programme_withNullRules_shouldReturnNull() {
        val programme =
            Programme(
                name = "Programme without Rules",
                description = null,
                durationWeeks = 8,
                programmeType = ProgrammeType.GENERAL_FITNESS,
                difficulty = ProgrammeDifficulty.BEGINNER,
                weightCalculationRules = null,
                progressionRules = null,
            )

        assertThat(programme.getWeightCalculationRulesObject()).isNull()
        assertThat(programme.getProgressionRulesObject()).isNull()
    }

    @Test
    fun programme_encodeWeightCalculationRules_shouldProduceValidJson() {
        val rules =
            WeightCalculationRules(
                baseOn = WeightBasis.TRAINING_MAX, // Use non-default value to ensure it's serialized
                trainingMaxPercentage = 0.85f,
                roundingIncrement = 5f,
                minimumBarWeight = 45f,
                unit = WeightUnit.LB,
            )

        val json = Programme.encodeWeightCalculationRules(rules)

        assertThat(json).isNotEmpty()
        assertThat(json).contains("TRAINING_MAX") // Non-default value should be included
        assertThat(json).contains("0.85")
        assertThat(json).contains("LB")

        val decoded = Json.decodeFromString<WeightCalculationRules>(json)
        assertThat(decoded.baseOn).isEqualTo(WeightBasis.TRAINING_MAX)
        assertThat(decoded.trainingMaxPercentage).isEqualTo(0.85f)
        assertThat(decoded.unit).isEqualTo(WeightUnit.LB)
    }

    @Test
    fun programme_encodeProgressionRules_shouldProduceValidJson() {
        val rules =
            ProgressionRules(
                type = ProgressionType.WAVE,
                cycleLength = 3,
                autoProgressionEnabled = false,
            )

        val json = Programme.encodeProgressionRules(rules)

        assertThat(json).isNotEmpty()
        assertThat(json).contains("WAVE")
        assertThat(json).contains("3")

        val decoded = Json.decodeFromString<ProgressionRules>(json)
        assertThat(decoded.type).isEqualTo(ProgressionType.WAVE)
        assertThat(decoded.cycleLength).isEqualTo(3)
    }
}
