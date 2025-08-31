package com.github.radupana.featherweight.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSearchUtilTest {
    @Test
    fun `scoreExerciseMatch returns 0 for blank query`() {
        val score =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "   ",
                aliases = emptyList(),
            )
        assertEquals(0, score)
    }

    @Test
    fun `scoreExerciseMatch returns highest score for exact name match`() {
        val score =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "barbell back squat",
                aliases = emptyList(),
            )
        assertEquals(1000, score)
    }

    @Test
    fun `scoreExerciseMatch returns high score for exact alias match`() {
        val score =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "back squat",
                aliases = listOf("Back Squat", "BB Squat"),
            )
        assertEquals(900, score)
    }

    @Test
    fun `scoreExerciseMatch handles case insensitive matching`() {
        val score =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "BARBELL BACK SQUAT",
                aliases = emptyList(),
            )
        assertEquals(1000, score)
    }

    @Test
    fun `scoreExerciseMatch returns higher score when name starts with query`() {
        val scoreStartsWith =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "barbell",
                aliases = emptyList(),
            )

        val scoreContains =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Dumbbell Barbell Mix",
                query = "barbell",
                aliases = emptyList(),
            )

        assertTrue("Starting with query should score higher", scoreStartsWith > scoreContains)
        assertEquals(900, scoreStartsWith) // starts with gets 900
        assertEquals(800, scoreContains) // contains gets 800
    }

    @Test
    fun `scoreExerciseMatch handles multi-word queries - all words must match`() {
        // Should match - both words present
        val matchScore =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "barbell squat",
                aliases = emptyList(),
            )
        assertTrue("Should match when all words present", matchScore > 0)

        // Should not match - "front" not in name
        val noMatchScore =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "barbell front squat",
                aliases = emptyList(),
            )
        assertEquals("Should not match when word missing", 0, noMatchScore)
    }

    @Test
    fun `scoreExerciseMatch gives bonus for matching all search words`() {
        // All words match
        val allWordsScore =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "barbell back",
                aliases = emptyList(),
            )

        // Multi-word matching gets bonus for matching all words
        assertTrue("Multi-word complete match should have good score", allWordsScore > 0)
    }

    @Test
    fun `scoreExerciseMatch handles partial word matching`() {
        val score =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "bar sq",
                aliases = emptyList(),
            )
        assertTrue("Should match partial words", score > 0)
    }

    @Test
    fun `scoreExerciseMatch searches aliases when word not in name`() {
        val score =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "BB squat",
                aliases = listOf("BB Back Squat", "Back Squat"),
            )
        assertTrue("Should find 'BB' in alias", score > 0)
    }

    @Test
    fun `exerciseMatches returns true for any non-zero score`() {
        assertTrue(
            ExerciseSearchUtil.exerciseMatches(
                exerciseName = "Barbell Back Squat",
                query = "squat",
                aliases = emptyList(),
            ),
        )

        assertFalse(
            ExerciseSearchUtil.exerciseMatches(
                exerciseName = "Barbell Back Squat",
                query = "dumbbell",
                aliases = emptyList(),
            ),
        )
    }

    @Test
    fun `filterAndSortExercises returns all exercises for blank query`() {
        val exercises =
            listOf(
                "Barbell Back Squat",
                "Dumbbell Curl",
                "Bench Press",
            )

        val results =
            ExerciseSearchUtil.filterAndSortExercises(
                exercises = exercises,
                query = "",
                nameExtractor = { it },
            )

        assertEquals(exercises, results)
    }

    @Test
    fun `filterAndSortExercises filters out non-matching exercises`() {
        val exercises =
            listOf(
                "Barbell Back Squat",
                "Barbell Front Squat",
                "Dumbbell Curl",
                "Bench Press",
            )

        val results =
            ExerciseSearchUtil.filterAndSortExercises(
                exercises = exercises,
                query = "squat",
                nameExtractor = { it },
            )

        assertEquals(2, results.size)
        assertTrue(results.contains("Barbell Back Squat"))
        assertTrue(results.contains("Barbell Front Squat"))
        assertFalse(results.contains("Dumbbell Curl"))
        assertFalse(results.contains("Bench Press"))
    }

    @Test
    fun `filterAndSortExercises sorts by relevance with exact match first`() {
        val exercises =
            listOf(
                "Barbell Back Squat",
                "Squat",
                "Front Squat",
                "Bulgarian Split Squat",
            )

        val results =
            ExerciseSearchUtil.filterAndSortExercises(
                exercises = exercises,
                query = "squat",
                nameExtractor = { it },
            )

        // Exact match should be first
        assertEquals("Squat", results[0])

        // All should be included since they all contain "squat"
        assertEquals(4, results.size)
    }

    @Test
    fun `filterAndSortExercises handles complex objects with name extractor`() {
        data class Exercise(
            val id: Int,
            val name: String,
        )

        val exercises =
            listOf(
                Exercise(1, "Barbell Back Squat"),
                Exercise(2, "Barbell Front Squat"),
                Exercise(3, "Dumbbell Curl"),
            )

        val results =
            ExerciseSearchUtil.filterAndSortExercises(
                exercises = exercises,
                query = "barbell squat",
                nameExtractor = { it.name },
            )

        assertEquals(2, results.size)
        assertEquals(1, results[0].id)
        assertEquals(2, results[1].id)
    }

    @Test
    fun `filterAndSortExercises uses aliases when provided`() {
        data class Exercise(
            val name: String,
            val aliases: List<String>,
        )

        val exercises =
            listOf(
                Exercise("Barbell Back Squat", listOf("BB Squat", "Back Squat")),
                Exercise("Barbell Overhead Press", listOf("OHP", "Military Press")),
                Exercise("Dumbbell Curl", listOf("DB Curl", "Bicep Curl")),
            )

        val results =
            ExerciseSearchUtil.filterAndSortExercises(
                exercises = exercises,
                query = "OHP",
                nameExtractor = { it.name },
                aliasExtractor = { it.aliases },
            )

        assertEquals(1, results.size)
        assertEquals("Barbell Overhead Press", results[0].name)
    }

    // Real-world test case from the bug report
    @Test
    fun `searching for 'Barbell Squat' finds 'Barbell Back Squat'`() {
        val exercises =
            listOf(
                "Barbell Back Squat",
                "Barbell Front Squat",
                "Barbell Hack Squat",
                "Barbell Anderson Squat",
                "Barbell Box Squat",
                "Barbell Landmine Squat",
                "Barbell Overhead Squat",
                "Barbell Paused Squat",
                "Dumbbell Bulgarian Split Squat",
                "Machine Leg Press",
            )

        val results =
            ExerciseSearchUtil.filterAndSortExercises(
                exercises = exercises,
                query = "Barbell Squat",
                nameExtractor = { it },
            )

        // Should find all exercises with both "Barbell" and "Squat"
        assertEquals(8, results.size)
        assertTrue(results.contains("Barbell Back Squat"))
        assertTrue(results.contains("Barbell Front Squat"))
        assertFalse(results.contains("Dumbbell Bulgarian Split Squat")) // no "Barbell"
        assertFalse(results.contains("Machine Leg Press")) // no "Squat" or "Barbell"
    }

    @Test
    fun `multi-word search requires all words to be present`() {
        val score1 =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "Barbell Back Squat Press", // "Press" not in name
                aliases = emptyList(),
            )
        assertEquals("Should not match when word is missing", 0, score1)

        val score2 =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "Back Barbell", // Both words present, different order
                aliases = emptyList(),
            )
        assertTrue("Should match when all words present in different order", score2 > 0)
    }
}
