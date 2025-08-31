package com.github.radupana.featherweight.ui.screens

import com.github.radupana.featherweight.data.WorkoutMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutScreenTest {
    
    @Test
    fun `shouldShowWorkoutTimer returns false when in template edit mode`() {
        val mode = WorkoutMode.TEMPLATE_EDIT
        val completedSets = 10
        
        val shouldShow = shouldShowWorkoutTimer(mode, completedSets)
        
        assertThat(shouldShow).isFalse()
    }
    
    @Test
    fun `shouldShowWorkoutTimer returns false when no sets completed in active mode`() {
        val mode = WorkoutMode.ACTIVE
        val completedSets = 0
        
        val shouldShow = shouldShowWorkoutTimer(mode, completedSets)
        
        assertThat(shouldShow).isFalse()
    }
    
    @Test
    fun `shouldShowWorkoutTimer returns true when sets completed in active mode`() {
        val mode = WorkoutMode.ACTIVE
        val completedSets = 1
        
        val shouldShow = shouldShowWorkoutTimer(mode, completedSets)
        
        assertThat(shouldShow).isTrue()
    }
    
    @Test
    fun `shouldShowWorkoutTimer returns true with multiple completed sets in active mode`() {
        val mode = WorkoutMode.ACTIVE
        val completedSets = 5
        
        val shouldShow = shouldShowWorkoutTimer(mode, completedSets)
        
        assertThat(shouldShow).isTrue()
    }
    
    companion object {
        fun shouldShowWorkoutTimer(mode: WorkoutMode, completedSets: Int): Boolean {
            return mode != WorkoutMode.TEMPLATE_EDIT && completedSets > 0
        }
    }
}