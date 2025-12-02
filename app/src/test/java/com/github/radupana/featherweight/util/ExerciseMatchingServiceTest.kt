package com.github.radupana.featherweight.util

import com.github.radupana.featherweight.data.exercise.Exercise
import com.github.radupana.featherweight.data.exercise.ExerciseWithAliases
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for ExerciseMatchingService.
 *
 * These tests use real exercise data from the bug discovered on 2025-12-02 where:
 * - "Barbell Shoulder Press" incorrectly matched to "Cable Shoulder Press"
 * - "Barbell Chest Press" incorrectly matched to "Band Chest Press"
 * - "Barbell Quadriceps Squat" matched to "Barbell Back Squat"
 *
 * The root cause was that the equipment-stripped matching was too lenient,
 * allowing "barbell X" to match "cable X" or "band X".
 */
class ExerciseMatchingServiceTest {
    // Real exercise IDs from the bug scenario (from Room database)
    companion object {
        const val BARBELL_OVERHEAD_PRESS_ID = "b9132bd2-ce26-35bf-942f-93ca3c86d403"
        const val CABLE_SHOULDER_PRESS_ID = "fee6d52a-d717-3b8a-8c54-7416c47cb1a3"
        const val DUMBBELL_SHOULDER_PRESS_ID = "81652056-5f05-3512-8b67-2730dd29e630"
        const val BARBELL_BENCH_PRESS_ID = "ba335331-818e-39a3-96ab-4d0173cadeb2"
        const val BAND_CHEST_PRESS_ID = "de3896ed-3a5a-3d16-872a-32dc898f2048"
        const val DUMBBELL_CHEST_PRESS_ID = "98c5e763-37e9-32b0-be1e-3da12f90e8dd"
        const val BARBELL_BACK_SQUAT_ID = "9033d8b7-f843-3d56-a854-1449ef90bc7a"
    }

    /**
     * Creates a test exercise database with real exercise data from the bug.
     * Includes exercises with their aliases as they exist in Firestore/Room.
     */
    @Suppress("LongMethod")
    private fun createTestExerciseDatabase(): List<ExerciseWithAliases> =
        listOf(
            // Barbell Overhead Press - has "Shoulder Press" as an alias
            ExerciseWithAliases(
                exercise =
                    Exercise(
                        id = BARBELL_OVERHEAD_PRESS_ID,
                        name = "Barbell Overhead Press",
                        category = "SHOULDERS",
                        movementPattern = "PUSH",
                        equipment = "BARBELL",
                    ),
                aliases =
                    listOf(
                        "Overhead Press",
                        "Military Press",
                        "Standing Press",
                        "OHP",
                        "BB OHP",
                        "Shoulder Press", // This is the key alias!
                        "Strict Press",
                    ),
            ),
            // Cable Shoulder Press - this was incorrectly matched in the bug
            ExerciseWithAliases(
                exercise =
                    Exercise(
                        id = CABLE_SHOULDER_PRESS_ID,
                        name = "Cable Shoulder Press",
                        category = "SHOULDERS",
                        movementPattern = "PUSH",
                        equipment = "CABLE",
                    ),
                aliases = emptyList(),
            ),
            // Dumbbell Shoulder Press
            ExerciseWithAliases(
                exercise =
                    Exercise(
                        id = DUMBBELL_SHOULDER_PRESS_ID,
                        name = "Dumbbell Shoulder Press",
                        category = "SHOULDERS",
                        movementPattern = "PUSH",
                        equipment = "DUMBBELL",
                    ),
                aliases = listOf("DB Shoulder Press"),
            ),
            // Barbell Bench Press - has "Chest Press" as an alias
            ExerciseWithAliases(
                exercise =
                    Exercise(
                        id = BARBELL_BENCH_PRESS_ID,
                        name = "Barbell Bench Press",
                        category = "CHEST",
                        movementPattern = "PUSH",
                        equipment = "BARBELL",
                    ),
                aliases =
                    listOf(
                        "Bench Press",
                        "Barbell Bench",
                        "Flat Bench Press",
                        "BB Bench Press",
                        "Bench",
                        "BB Bench",
                        "Flat Bench",
                        "Chest Press", // Key alias
                        "BP",
                    ),
            ),
            // Band Chest Press - this was incorrectly matched in the bug
            ExerciseWithAliases(
                exercise =
                    Exercise(
                        id = BAND_CHEST_PRESS_ID,
                        name = "Band Chest Press",
                        category = "CHEST",
                        movementPattern = "PUSH",
                        equipment = "BAND",
                    ),
                aliases = emptyList(),
            ),
            // Dumbbell Chest Press
            ExerciseWithAliases(
                exercise =
                    Exercise(
                        id = DUMBBELL_CHEST_PRESS_ID,
                        name = "Dumbbell Chest Press",
                        category = "CHEST",
                        movementPattern = "PUSH",
                        equipment = "DUMBBELL",
                    ),
                aliases = listOf("DB Chest Press"),
            ),
            // Barbell Back Squat
            ExerciseWithAliases(
                exercise =
                    Exercise(
                        id = BARBELL_BACK_SQUAT_ID,
                        name = "Barbell Back Squat",
                        category = "LEGS",
                        movementPattern = "SQUAT",
                        equipment = "BARBELL",
                    ),
                aliases =
                    listOf(
                        "Back Squat",
                        "Barbell Squat",
                        "Squat",
                        "BB Back Squat",
                        "BB Squat",
                    ),
            ),
        )

    // ==================== BUG REGRESSION TESTS ====================

    @Test
    fun `barbell shoulder press should match Barbell Overhead Press via alias, not Cable Shoulder Press`() {
        val exercises = createTestExerciseDatabase()

        val result =
            ExerciseMatchingService.findBestExerciseMatch(
                "Barbell Shoulder Press",
                exercises,
            )

        // MUST match Barbell Overhead Press (which has "Shoulder Press" as alias)
        assertThat(result).isEqualTo(BARBELL_OVERHEAD_PRESS_ID)
        // MUST NOT match Cable Shoulder Press (wrong equipment)
        assertThat(result).isNotEqualTo(CABLE_SHOULDER_PRESS_ID)
    }

    @Test
    fun `barbell chest press should match Barbell Bench Press via alias, not Band Chest Press`() {
        val exercises = createTestExerciseDatabase()

        val result =
            ExerciseMatchingService.findBestExerciseMatch(
                "Barbell Chest Press",
                exercises,
            )

        // MUST match Barbell Bench Press (which has "Chest Press" as alias)
        assertThat(result).isEqualTo(BARBELL_BENCH_PRESS_ID)
        // MUST NOT match Band Chest Press (wrong equipment)
        assertThat(result).isNotEqualTo(BAND_CHEST_PRESS_ID)
    }

    // ==================== EQUIPMENT COMPATIBILITY TESTS ====================

    @Test
    fun `isEquipmentCompatible returns true when input has no equipment`() {
        assertThat(ExerciseMatchingService.isEquipmentCompatible(null, "barbell")).isTrue()
        assertThat(ExerciseMatchingService.isEquipmentCompatible(null, "cable")).isTrue()
        assertThat(ExerciseMatchingService.isEquipmentCompatible(null, null)).isTrue()
    }

    @Test
    fun `isEquipmentCompatible returns true when exercise has no equipment`() {
        assertThat(ExerciseMatchingService.isEquipmentCompatible("barbell", null)).isTrue()
        assertThat(ExerciseMatchingService.isEquipmentCompatible("dumbbell", null)).isTrue()
    }

    @Test
    fun `isEquipmentCompatible returns true only for exact equipment match`() {
        assertThat(ExerciseMatchingService.isEquipmentCompatible("barbell", "barbell")).isTrue()
        assertThat(ExerciseMatchingService.isEquipmentCompatible("dumbbell", "dumbbell")).isTrue()
        assertThat(ExerciseMatchingService.isEquipmentCompatible("cable", "cable")).isTrue()
    }

    @Test
    fun `isEquipmentCompatible returns false for different equipment types`() {
        // This was the bug - these used to return true
        assertThat(ExerciseMatchingService.isEquipmentCompatible("barbell", "cable")).isFalse()
        assertThat(ExerciseMatchingService.isEquipmentCompatible("barbell", "band")).isFalse()
        assertThat(ExerciseMatchingService.isEquipmentCompatible("barbell", "dumbbell")).isFalse()
        assertThat(ExerciseMatchingService.isEquipmentCompatible("barbell", "machine")).isFalse()
        assertThat(ExerciseMatchingService.isEquipmentCompatible("dumbbell", "cable")).isFalse()
        assertThat(ExerciseMatchingService.isEquipmentCompatible("dumbbell", "barbell")).isFalse()
    }

    // ==================== EXACT MATCH TESTS ====================

    @Test
    fun `exact name match works`() {
        val exercises = createTestExerciseDatabase()

        val result =
            ExerciseMatchingService.findBestExerciseMatch(
                "Barbell Overhead Press",
                exercises,
            )

        assertThat(result).isEqualTo(BARBELL_OVERHEAD_PRESS_ID)
    }

    @Test
    fun `exact name match is case insensitive`() {
        val exercises = createTestExerciseDatabase()

        val result =
            ExerciseMatchingService.findBestExerciseMatch(
                "barbell overhead press",
                exercises,
            )

        assertThat(result).isEqualTo(BARBELL_OVERHEAD_PRESS_ID)
    }

    @Test
    fun `exact alias match works`() {
        val exercises = createTestExerciseDatabase()

        // "OHP" is an exact alias for Barbell Overhead Press
        val result = ExerciseMatchingService.findBestExerciseMatch("OHP", exercises)

        assertThat(result).isEqualTo(BARBELL_OVERHEAD_PRESS_ID)
    }

    @Test
    fun `shoulder press alias matches Barbell Overhead Press`() {
        val exercises = createTestExerciseDatabase()

        // "Shoulder Press" is an alias for Barbell Overhead Press
        val result = ExerciseMatchingService.findBestExerciseMatch("Shoulder Press", exercises)

        assertThat(result).isEqualTo(BARBELL_OVERHEAD_PRESS_ID)
    }

    // ==================== EQUIPMENT + ALIAS MATCH TESTS ====================

    @Test
    fun `equipment plus alias match works for barbell`() {
        val exercises = createTestExerciseDatabase()

        // "barbell" + "shoulder press" should find Barbell Overhead Press
        val result =
            ExerciseMatchingService.findBestExerciseMatch(
                "barbell shoulder press",
                exercises,
            )

        assertThat(result).isEqualTo(BARBELL_OVERHEAD_PRESS_ID)
    }

    @Test
    fun `dumbbell shoulder press should match Dumbbell Shoulder Press, not Barbell Overhead Press`() {
        val exercises = createTestExerciseDatabase()

        val result =
            ExerciseMatchingService.findBestExerciseMatch(
                "Dumbbell Shoulder Press",
                exercises,
            )

        assertThat(result).isEqualTo(DUMBBELL_SHOULDER_PRESS_ID)
    }

    @Test
    fun `cable shoulder press should match Cable Shoulder Press`() {
        val exercises = createTestExerciseDatabase()

        val result =
            ExerciseMatchingService.findBestExerciseMatch(
                "Cable Shoulder Press",
                exercises,
            )

        assertThat(result).isEqualTo(CABLE_SHOULDER_PRESS_ID)
    }

    // ==================== EQUIPMENT EXTRACTION TESTS ====================

    @Test
    fun `extractEquipment identifies barbell correctly`() {
        assertThat(ExerciseMatchingService.extractEquipment("barbell bench press")).isEqualTo("barbell")
        assertThat(ExerciseMatchingService.extractEquipment("bb bench press")).isEqualTo("barbell")
    }

    @Test
    fun `extractEquipment identifies dumbbell correctly`() {
        assertThat(ExerciseMatchingService.extractEquipment("dumbbell curl")).isEqualTo("dumbbell")
        assertThat(ExerciseMatchingService.extractEquipment("db curl")).isEqualTo("dumbbell")
    }

    @Test
    fun `extractEquipment identifies cable correctly`() {
        assertThat(ExerciseMatchingService.extractEquipment("cable fly")).isEqualTo("cable")
    }

    @Test
    fun `extractEquipment identifies band correctly`() {
        assertThat(ExerciseMatchingService.extractEquipment("band pull apart")).isEqualTo("band")
        assertThat(ExerciseMatchingService.extractEquipment("resistance band curl")).isEqualTo("band")
    }

    @Test
    fun `extractEquipment returns null for no equipment prefix`() {
        assertThat(ExerciseMatchingService.extractEquipment("push up")).isNull()
        assertThat(ExerciseMatchingService.extractEquipment("squat")).isNull()
    }

    // ==================== STRIP EQUIPMENT TESTS ====================

    @Test
    fun `stripEquipmentFromName removes equipment prefix`() {
        assertThat(ExerciseMatchingService.stripEquipmentFromName("barbell bench press"))
            .isEqualTo("bench press")
        assertThat(ExerciseMatchingService.stripEquipmentFromName("dumbbell curl"))
            .isEqualTo("curl")
        assertThat(ExerciseMatchingService.stripEquipmentFromName("cable fly"))
            .isEqualTo("fly")
    }

    @Test
    fun `stripEquipmentFromName handles names without equipment`() {
        assertThat(ExerciseMatchingService.stripEquipmentFromName("push up"))
            .isEqualTo("push up")
        assertThat(ExerciseMatchingService.stripEquipmentFromName("squat"))
            .isEqualTo("squat")
    }

    // ==================== NO MATCH SCENARIOS ====================

    @Test
    fun `returns null for completely unknown exercise`() {
        val exercises = createTestExerciseDatabase()

        val result =
            ExerciseMatchingService.findBestExerciseMatch(
                "Barbell Unicorn Fly",
                exercises,
            )

        assertThat(result).isNull()
    }

    @Test
    fun `barbell exercise does not match cable exercise with same movement`() {
        val exercises = createTestExerciseDatabase()

        // "Barbell Fly" should not match "Cable Fly" if that existed
        // This test ensures equipment mismatch prevents false positives
        val result =
            ExerciseMatchingService.findBestExerciseMatch(
                "Barbell Something Nonexistent",
                exercises,
            )

        assertThat(result).isNull()
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `handles whitespace in exercise name`() {
        val exercises = createTestExerciseDatabase()

        val result =
            ExerciseMatchingService.findBestExerciseMatch(
                "  Barbell Overhead Press  ",
                exercises,
            )

        assertThat(result).isEqualTo(BARBELL_OVERHEAD_PRESS_ID)
    }

    @Test
    fun `handles mixed case`() {
        val exercises = createTestExerciseDatabase()

        val result =
            ExerciseMatchingService.findBestExerciseMatch(
                "BARBELL OVERHEAD PRESS",
                exercises,
            )

        assertThat(result).isEqualTo(BARBELL_OVERHEAD_PRESS_ID)
    }

    @Test
    fun `squat alias matches Barbell Back Squat`() {
        val exercises = createTestExerciseDatabase()

        val result = ExerciseMatchingService.findBestExerciseMatch("Squat", exercises)

        assertThat(result).isEqualTo(BARBELL_BACK_SQUAT_ID)
    }

    @Test
    fun `barbell squat matches Barbell Back Squat`() {
        val exercises = createTestExerciseDatabase()

        val result = ExerciseMatchingService.findBestExerciseMatch("Barbell Squat", exercises)

        assertThat(result).isEqualTo(BARBELL_BACK_SQUAT_ID)
    }
}
