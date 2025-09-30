package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class PersonalRecordTest {
    @Test
    fun personalRecord_creation_withAllFields_createsCorrectly() {
        val recordDate = LocalDateTime.of(2024, 1, 15, 10, 30)
        val previousDate = LocalDateTime.of(2024, 1, 1, 9, 0)

        val pr =
            PersonalRecord(
                id = "1",
                exerciseVariationId = "100",
                weight = 100f,
                reps = 5,
                rpe = 8.5f,
                recordDate = recordDate,
                previousWeight = 95f,
                previousReps = 5,
                previousDate = previousDate,
                improvementPercentage = 5.26f,
                recordType = PRType.WEIGHT,
                estimated1RM = 116.67f,
                notes = "Felt strong today",
                workoutId = "42",
            )

        assertThat(pr.id).isEqualTo("1")
        assertThat(pr.exerciseVariationId).isEqualTo("100")
        assertThat(pr.weight).isEqualTo(100f)
        assertThat(pr.reps).isEqualTo(5)
        assertThat(pr.rpe).isEqualTo(8.5f)
        assertThat(pr.recordDate).isEqualTo(recordDate)
        assertThat(pr.previousWeight).isEqualTo(95f)
        assertThat(pr.previousReps).isEqualTo(5)
        assertThat(pr.previousDate).isEqualTo(previousDate)
        assertThat(pr.improvementPercentage).isEqualTo(5.26f)
        assertThat(pr.recordType).isEqualTo(PRType.WEIGHT)
        assertThat(pr.volume).isEqualTo(500f) // 100 * 5
        assertThat(pr.estimated1RM).isEqualTo(116.67f)
        assertThat(pr.notes).isEqualTo("Felt strong today")
        assertThat(pr.workoutId).isEqualTo("42")
    }

    @Test
    fun personalRecord_creation_withMinimalFields_createsWithNulls() {
        val recordDate = LocalDateTime.now()

        val pr =
            PersonalRecord(
                exerciseVariationId = "50",
                weight = 50f,
                reps = 10,
                recordDate = recordDate,
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.WEIGHT,
            )

        assertThat(pr.id).isNotEmpty() // Auto-generated ID
        assertThat(pr.exerciseVariationId).isEqualTo("50")
        assertThat(pr.weight).isEqualTo(50f)
        assertThat(pr.reps).isEqualTo(10)
        assertThat(pr.rpe).isNull()
        assertThat(pr.recordDate).isEqualTo(recordDate)
        assertThat(pr.previousWeight).isNull()
        assertThat(pr.previousReps).isNull()
        assertThat(pr.previousDate).isNull()
        assertThat(pr.improvementPercentage).isEqualTo(0f)
        assertThat(pr.recordType).isEqualTo(PRType.WEIGHT)
        assertThat(pr.volume).isEqualTo(500f) // 50 * 10
        assertThat(pr.estimated1RM).isNull()
        assertThat(pr.notes).isNull()
        assertThat(pr.workoutId).isNull()
    }

    @Test
    fun personalRecord_volumeCalculation_isCorrect() {
        val pr1 =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 100f,
                reps = 10,
                recordDate = LocalDateTime.now(),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.ESTIMATED_1RM,
            )
        assertThat(pr1.volume).isEqualTo(1000f)

        val pr2 =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 225.5f,
                reps = 3,
                recordDate = LocalDateTime.now(),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.ESTIMATED_1RM,
            )
        assertThat(pr2.volume).isEqualTo(676.5f)

        val pr3 =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 0f,
                reps = 20,
                recordDate = LocalDateTime.now(),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.ESTIMATED_1RM,
            )
        assertThat(pr3.volume).isEqualTo(0f)
    }

    @Test
    fun personalRecord_allPRTypes_areValid() {
        val baseDate = LocalDateTime.now()

        val weightPR =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 100f,
                reps = 5,
                recordDate = baseDate,
                previousWeight = 95f,
                previousReps = 5,
                previousDate = baseDate.minusDays(7),
                improvementPercentage = 5.26f,
                recordType = PRType.WEIGHT,
            )
        assertThat(weightPR.recordType).isEqualTo(PRType.WEIGHT)

        val repsPR =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 80f,
                reps = 12,
                recordDate = baseDate,
                previousWeight = 80f,
                previousReps = 10,
                previousDate = baseDate.minusDays(7),
                improvementPercentage = 20f,
                recordType = PRType.WEIGHT,
            )
        assertThat(repsPR.recordType).isEqualTo(PRType.WEIGHT)

        val volumePR =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 85f,
                reps = 10,
                recordDate = baseDate,
                previousWeight = 80f,
                previousReps = 10,
                previousDate = baseDate.minusDays(7),
                improvementPercentage = 6.25f,
                recordType = PRType.ESTIMATED_1RM,
            )
        assertThat(volumePR.recordType).isEqualTo(PRType.ESTIMATED_1RM)

        val estimated1RMPR =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 90f,
                reps = 8,
                recordDate = baseDate,
                previousWeight = 85f,
                previousReps = 8,
                previousDate = baseDate.minusDays(7),
                improvementPercentage = 5.88f,
                recordType = PRType.ESTIMATED_1RM,
                estimated1RM = 113f,
            )
        assertThat(estimated1RMPR.recordType).isEqualTo(PRType.ESTIMATED_1RM)
    }

    @Test
    fun personalRecord_withHighImprovementPercentage_isValid() {
        val pr =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 100f,
                reps = 10,
                recordDate = LocalDateTime.now(),
                previousWeight = 50f,
                previousReps = 10,
                previousDate = LocalDateTime.now().minusDays(30),
                improvementPercentage = 100f, // Doubled the weight
                recordType = PRType.WEIGHT,
            )

        assertThat(pr.improvementPercentage).isEqualTo(100f)
        assertThat(pr.weight).isEqualTo(100f)
        assertThat(pr.previousWeight).isEqualTo(50f)
    }

    @Test
    fun personalRecord_withNegativeImprovementPercentage_isValid() {
        // This shouldn't normally happen for a PR, but testing edge case
        val pr =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 90f,
                reps = 5,
                recordDate = LocalDateTime.now(),
                previousWeight = 100f,
                previousReps = 5,
                previousDate = LocalDateTime.now().minusDays(7),
                improvementPercentage = -10f,
                recordType = PRType.WEIGHT,
            )

        assertThat(pr.improvementPercentage).isEqualTo(-10f)
    }

    @Test
    fun personalRecord_withRPE_validatesRange() {
        val pr1 =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 100f,
                reps = 5,
                rpe = 10f, // Maximum RPE
                recordDate = LocalDateTime.now(),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.WEIGHT,
            )
        assertThat(pr1.rpe).isEqualTo(10f)

        val pr2 =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 100f,
                reps = 5,
                rpe = 6.5f, // Mid-range RPE
                recordDate = LocalDateTime.now(),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.WEIGHT,
            )
        assertThat(pr2.rpe).isEqualTo(6.5f)

        val pr3 =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 100f,
                reps = 5,
                rpe = 0f, // Edge case - minimum
                recordDate = LocalDateTime.now(),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.WEIGHT,
            )
        assertThat(pr3.rpe).isEqualTo(0f)
    }

    @Test
    fun personalRecord_withLongNotes_storesCorrectly() {
        val longNotes =
            "This was an amazing PR! I felt incredibly strong today. " +
                "Perfect form throughout all reps. The warm-up was thorough and I made sure " +
                "to rest adequately between sets. This is a 10lb increase from my previous best. " +
                "I think the recent focus on technique and progressive overload is really paying off. " +
                "Next goal is to hit 105kg for 5 reps."

        val pr =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 100f,
                reps = 5,
                recordDate = LocalDateTime.now(),
                previousWeight = 95f,
                previousReps = 5,
                previousDate = LocalDateTime.now().minusWeeks(2),
                improvementPercentage = 5.26f,
                recordType = PRType.WEIGHT,
                notes = longNotes,
            )

        assertThat(pr.notes).isEqualTo(longNotes)
        assertThat(pr.notes).hasLength(longNotes.length)
    }

    @Test
    fun personalRecord_dateTimeFields_maintainPrecision() {
        val recordDate = LocalDateTime.of(2024, 3, 15, 14, 30, 45, 123456789)
        val previousDate = LocalDateTime.of(2024, 2, 28, 9, 15, 30, 987654321)

        val pr =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 100f,
                reps = 5,
                recordDate = recordDate,
                previousWeight = 95f,
                previousReps = 5,
                previousDate = previousDate,
                improvementPercentage = 5.26f,
                recordType = PRType.WEIGHT,
            )

        assertThat(pr.recordDate).isEqualTo(recordDate)
        assertThat(pr.previousDate).isEqualTo(previousDate)
        assertThat(pr.recordDate.year).isEqualTo(2024)
        assertThat(pr.recordDate.monthValue).isEqualTo(3)
        assertThat(pr.recordDate.dayOfMonth).isEqualTo(15)
        assertThat(pr.recordDate.hour).isEqualTo(14)
        assertThat(pr.recordDate.minute).isEqualTo(30)
        assertThat(pr.recordDate.second).isEqualTo(45)
    }

    @Test
    fun personalRecord_withEstimated1RM_storesCorrectly() {
        val pr =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 85f,
                reps = 8,
                recordDate = LocalDateTime.now(),
                previousWeight = 80f,
                previousReps = 8,
                previousDate = LocalDateTime.now().minusDays(14),
                improvementPercentage = 6.25f,
                recordType = PRType.ESTIMATED_1RM,
                estimated1RM = 106.25f, // 85 * 1.25 for 8 reps approximation
            )

        assertThat(pr.estimated1RM).isEqualTo(106.25f)
        assertThat(pr.recordType).isEqualTo(PRType.ESTIMATED_1RM)
    }

    @Test
    fun personalRecord_withWorkoutId_linksToWorkout() {
        val pr =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 100f,
                reps = 5,
                recordDate = LocalDateTime.now(),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.WEIGHT,
                workoutId = "999",
            )

        assertThat(pr.workoutId).isEqualTo("999")
    }

    @Test
    fun personalRecord_withZeroReps_calculatesZeroVolume() {
        val pr =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 100f,
                reps = 0,
                recordDate = LocalDateTime.now(),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.WEIGHT,
            )

        assertThat(pr.volume).isEqualTo(0f)
    }

    @Test
    fun personalRecord_withVeryHighWeight_handlesCorrectly() {
        val pr =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 500f, // Very heavy weight
                reps = 1,
                recordDate = LocalDateTime.now(),
                previousWeight = 495f,
                previousReps = 1,
                previousDate = LocalDateTime.now().minusMonths(1),
                improvementPercentage = 1.01f,
                recordType = PRType.WEIGHT,
            )

        assertThat(pr.weight).isEqualTo(500f)
        assertThat(pr.volume).isEqualTo(500f)
    }

    @Test
    fun personalRecord_withVeryHighReps_handlesCorrectly() {
        val pr =
            PersonalRecord(
                exerciseVariationId = "1",
                weight = 20f,
                reps = 100, // Very high reps for endurance
                recordDate = LocalDateTime.now(),
                previousWeight = 20f,
                previousReps = 90,
                previousDate = LocalDateTime.now().minusDays(7),
                improvementPercentage = 11.11f,
                recordType = PRType.WEIGHT,
            )

        assertThat(pr.reps).isEqualTo(100)
        assertThat(pr.volume).isEqualTo(2000f)
    }

    @Test
    fun personalRecord_equality_basedOnAllFields() {
        val date = LocalDateTime.now()
        val prevDate = date.minusDays(7)

        val pr1 =
            PersonalRecord(
                id = "1",
                exerciseVariationId = "100",
                weight = 100f,
                reps = 5,
                rpe = 8f,
                recordDate = date,
                previousWeight = 95f,
                previousReps = 5,
                previousDate = prevDate,
                improvementPercentage = 5.26f,
                recordType = PRType.WEIGHT,
                notes = "Good lift",
            )

        val pr2 =
            PersonalRecord(
                id = "1",
                exerciseVariationId = "100",
                weight = 100f,
                reps = 5,
                rpe = 8f,
                recordDate = date,
                previousWeight = 95f,
                previousReps = 5,
                previousDate = prevDate,
                improvementPercentage = 5.26f,
                recordType = PRType.WEIGHT,
                notes = "Good lift",
            )

        val pr3 =
            PersonalRecord(
                id = "2", // Different ID
                exerciseVariationId = "100",
                weight = 100f,
                reps = 5,
                rpe = 8f,
                recordDate = date,
                previousWeight = 95f,
                previousReps = 5,
                previousDate = prevDate,
                improvementPercentage = 5.26f,
                recordType = PRType.WEIGHT,
                notes = "Good lift",
            )

        assertThat(pr1).isEqualTo(pr2)
        assertThat(pr1).isNotEqualTo(pr3)
        assertThat(pr1.hashCode()).isEqualTo(pr2.hashCode())
    }

    @Test
    fun personalRecord_toString_includesKeyInfo() {
        val pr =
            PersonalRecord(
                id = "1",
                exerciseVariationId = "100",
                weight = 100f,
                reps = 5,
                recordDate = LocalDateTime.of(2024, 1, 15, 10, 0),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.WEIGHT,
            )

        val toString = pr.toString()
        assertThat(toString).contains("PersonalRecord")
        assertThat(toString).contains("id=1")
        assertThat(toString).contains("weight=100")
        assertThat(toString).contains("reps=5")
    }

    @Test
    fun personalRecord_copy_createsIndependentInstance() {
        val original =
            PersonalRecord(
                exerciseVariationId = "100",
                weight = 100f,
                reps = 5,
                recordDate = LocalDateTime.now(),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.WEIGHT,
            )

        val copy =
            original.copy(
                weight = 105f,
                improvementPercentage = 5f,
            )

        assertThat(copy.weight).isEqualTo(105f)
        assertThat(copy.improvementPercentage).isEqualTo(5f)
        assertThat(copy.exerciseVariationId).isEqualTo(original.exerciseVariationId)
        assertThat(copy.reps).isEqualTo(original.reps)
        // Note: volume is NOT recalculated with copy(), it keeps the original value
        // This is because volume is calculated in the constructor, not dynamically
        assertThat(copy.volume).isEqualTo(500f) // Still original volume
        assertThat(original.weight).isEqualTo(100f) // Original unchanged
        assertThat(original.volume).isEqualTo(500f) // Original volume unchanged
    }

    @Test
    fun personalRecord_volumeRecalculation_requiresNewInstance() {
        val original =
            PersonalRecord(
                exerciseVariationId = "100",
                weight = 100f,
                reps = 5,
                recordDate = LocalDateTime.now(),
                previousWeight = null,
                previousReps = null,
                previousDate = null,
                improvementPercentage = 0f,
                recordType = PRType.WEIGHT,
            )

        // To get recalculated volume, need to create a new instance
        val updated =
            PersonalRecord(
                exerciseVariationId = original.exerciseVariationId,
                weight = 105f,
                reps = original.reps,
                recordDate = original.recordDate,
                previousWeight = original.previousWeight,
                previousReps = original.previousReps,
                previousDate = original.previousDate,
                improvementPercentage = 5f,
                recordType = original.recordType,
            )

        assertThat(updated.weight).isEqualTo(105f)
        assertThat(updated.volume).isEqualTo(525f) // Correctly recalculated
        assertThat(original.volume).isEqualTo(500f) // Original unchanged
    }
}
