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
    fun `scoreExerciseMatch handles multi-word queries with full match`() {
        val matchScore =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "barbell squat",
                aliases = emptyList(),
            )
        assertTrue("Should match when all words present", matchScore > 0)
    }

    @Test
    fun `scoreExerciseMatch handles partial multi-word match when at least 50 percent of words match`() {
        val partialMatchScore =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "barbell front squat",
                aliases = emptyList(),
            )
        assertTrue("Should match when 66% of words present (2 out of 3)", partialMatchScore > 0)
    }

    @Test
    fun `scoreExerciseMatch returns zero when less than 50 percent of words match`() {
        val noMatchScore =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "dumbbell front press cable",
                aliases = emptyList(),
            )
        assertEquals("Should not match when only 1 of 4 words present (25%)", 0, noMatchScore)
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
    fun `searching for 'Barbell Squat' finds exercises with at least 50 percent word match`() {
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

        assertEquals(9, results.size)
        assertTrue(results.contains("Barbell Back Squat"))
        assertTrue(results.contains("Barbell Front Squat"))
        assertTrue(results.contains("Dumbbell Bulgarian Split Squat"))
        assertFalse(results.contains("Machine Leg Press"))
    }

    @Test
    fun `multi-word search with partial match scores proportionally`() {
        val score1 =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "Barbell Back Squat Press",
                aliases = emptyList(),
            )
        assertTrue("Should match when 75% of words present (3 out of 4)", score1 > 0)

        val score2 =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "Back Barbell",
                aliases = emptyList(),
            )
        assertTrue("Should match when all words present in different order", score2 > 0)
    }

    @Test
    fun `scoreExerciseMatch returns non-zero score for similar exercises with partial word match`() {
        val score =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Back Squat",
                query = "Barbell High-Bar Squat",
                aliases = emptyList(),
            )
        assertTrue("Should match similar exercises (2 of 3 words = 66%)", score > 0)
        assertTrue("Score should be below auto-propose threshold", score < 600)
    }

    @Test
    fun `scoreExerciseMatch partial match scores lower than full match`() {
        val fullMatchScore =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Bench Press",
                query = "Barbell Bench Press",
                aliases = emptyList(),
            )

        val partialMatchScore =
            ExerciseSearchUtil.scoreExerciseMatch(
                exerciseName = "Barbell Bench Press",
                query = "Barbell Incline Bench Press",
                aliases = emptyList(),
            )

        assertTrue("Partial match should score > 0", partialMatchScore > 0)
        assertTrue("Full match should score higher than partial match", fullMatchScore > partialMatchScore)
    }
}
