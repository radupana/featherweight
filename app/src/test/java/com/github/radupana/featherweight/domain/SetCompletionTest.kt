package com.github.radupana.featherweight.domain

import com.github.radupana.featherweight.data.SetLog
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class SetCompletionTest {
    
    @Test
    fun `completed set has actual values filled`() {
        val completedSet = SetLog(
            exerciseLogId = 1L,
            setOrder = 1,
            targetReps = 10,
            targetWeight = 100f,
            actualReps = 10,
            actualWeight = 100f,
            actualRpe = 8f,
            isCompleted = true,
            completedAt = LocalDateTime.now().toString()
        )
        
        assertThat(completedSet.isCompleted).isTrue()
        assertThat(completedSet.actualReps).isGreaterThan(0)
        assertThat(completedSet.actualWeight).isGreaterThan(0f)
        assertThat(completedSet.completedAt).isNotNull()
    }
    
    @Test
    fun `incomplete set has zero actual values`() {
        val incompleteSet = SetLog(
            exerciseLogId = 1L,
            setOrder = 1,
            targetReps = 10,
            targetWeight = 100f,
            actualReps = 0,
            actualWeight = 0f,
            isCompleted = false,
            completedAt = null
        )
        
        assertThat(incompleteSet.isCompleted).isFalse()
        assertThat(incompleteSet.actualReps).isEqualTo(0)
        assertThat(incompleteSet.actualWeight).isEqualTo(0f)
        assertThat(incompleteSet.completedAt).isNull()
    }
    
    @Test
    fun `set with RPE tracking`() {
        val setWithRpe = SetLog(
            exerciseLogId = 1L,
            setOrder = 1,
            targetReps = 8,
            targetWeight = 80f,
            actualReps = 8,
            actualWeight = 80f,
            actualRpe = 7.5f, // RPE 7.5
            isCompleted = true
        )
        
        assertThat(setWithRpe.actualRpe).isNotNull()
        assertThat(setWithRpe.actualRpe!! >= 1f && setWithRpe.actualRpe <= 10f).isTrue()
        assertThat(setWithRpe.actualRpe).isEqualTo(7.5f)
    }
    
    @Test
    fun `set with intelligent suggestions`() {
        val suggestedSet = SetLog(
            exerciseLogId = 1L,
            setOrder = 1,
            targetReps = 10,
            targetWeight = null, // No target, using suggestion
            suggestedWeight = 85f,
            suggestedReps = 10,
            suggestionSource = "{\"source\": \"previous_performance\", \"confidence\": 0.85}",
            suggestionConfidence = 0.85f,
            actualReps = 0,
            actualWeight = 0f,
            isCompleted = false
        )
        
        assertThat(suggestedSet.suggestedWeight).isNotNull()
        assertThat(suggestedSet.suggestedWeight).isEqualTo(85f)
        assertThat(suggestedSet.suggestionConfidence).isGreaterThan(0.5f)
        assertThat(suggestedSet.suggestionSource).contains("previous_performance")
    }
    
    @Test
    fun `set order is maintained`() {
        val set1 = SetLog(
            exerciseLogId = 1L,
            setOrder = 1,
            targetReps = 10,
            targetWeight = 100f,
            actualReps = 10,
            actualWeight = 100f,
            isCompleted = true
        )
        
        val set2 = SetLog(
            exerciseLogId = 1L,
            setOrder = 2,
            targetReps = 10,
            targetWeight = 100f,
            actualReps = 9,
            actualWeight = 100f,
            isCompleted = true
        )
        
        val set3 = SetLog(
            exerciseLogId = 1L,
            setOrder = 3,
            targetReps = 10,
            targetWeight = 100f,
            actualReps = 8,
            actualWeight = 100f,
            isCompleted = true
        )
        
        val sets = listOf(set1, set2, set3)
        val sortedSets = sets.sortedBy { it.setOrder }
        
        assertThat(sortedSets[0].setOrder).isEqualTo(1)
        assertThat(sortedSets[1].setOrder).isEqualTo(2)
        assertThat(sortedSets[2].setOrder).isEqualTo(3)
        
        // Verify reps decrease as fatigue sets in
        assertThat(sortedSets[0].actualReps).isGreaterThan(sortedSets[2].actualReps)
    }
    
    @Test
    fun `set with notes and tags`() {
        val annotatedSet = SetLog(
            exerciseLogId = 1L,
            setOrder = 1,
            targetReps = 5,
            targetWeight = 120f,
            actualReps = 5,
            actualWeight = 120f,
            actualRpe = 9f,
            notes = "Felt heavy but completed all reps",
            tag = "PR_ATTEMPT",
            isCompleted = true
        )
        
        assertThat(annotatedSet.notes).isNotNull()
        assertThat(annotatedSet.notes).contains("heavy")
        assertThat(annotatedSet.tag).isEqualTo("PR_ATTEMPT")
        assertThat(annotatedSet.actualRpe).isEqualTo(9f) // High RPE for PR attempt
    }
}