package com.github.radupana.featherweight.viewmodel

import com.github.radupana.featherweight.data.WorkoutStatus
import com.github.radupana.featherweight.data.WorkoutMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutViewModelTest {
    
    @Test
    fun `WorkoutState has correct default values`() {
        // Testing WorkoutState data class directly since WorkoutViewModel 
        // creates repository internally and can't be easily mocked
        val state = WorkoutState()
        
        assertThat(state.isActive).isFalse()
        assertThat(state.status).isEqualTo(WorkoutStatus.NOT_STARTED)
        assertThat(state.mode).isEqualTo(WorkoutMode.ACTIVE)
        assertThat(state.workoutId).isNull()
        assertThat(state.startTime).isNull()
        assertThat(state.workoutName).isNull()
        assertThat(state.isReadOnly).isFalse()
        assertThat(state.isInEditMode).isFalse()
        assertThat(state.originalWorkoutData).isNull()
        assertThat(state.isProgrammeWorkout).isFalse()
        assertThat(state.programmeId).isNull()
        assertThat(state.programmeName).isNull()
        assertThat(state.weekNumber).isNull()
        assertThat(state.dayNumber).isNull()
        assertThat(state.programmeWorkoutName).isNull()
    }
    
    @Test
    fun `WorkoutState copy function works correctly`() {
        val original = WorkoutState(
            isActive = true,
            status = WorkoutStatus.IN_PROGRESS,
            workoutId = 123L
        )
        
        val modified = original.copy(
            isActive = false,
            status = WorkoutStatus.COMPLETED
        )
        
        assertThat(modified.isActive).isFalse()
        assertThat(modified.status).isEqualTo(WorkoutStatus.COMPLETED)
        assertThat(modified.workoutId).isEqualTo(123L) // unchanged
    }
}