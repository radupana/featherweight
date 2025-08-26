package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutStatusTest {
    
    @Test
    fun `WorkoutStatus has correct values`() {
        val statuses = WorkoutStatus.values()
        
        assertThat(statuses).hasLength(3)
        assertThat(statuses).asList().containsExactly(
            WorkoutStatus.NOT_STARTED,
            WorkoutStatus.IN_PROGRESS,
            WorkoutStatus.COMPLETED
        )
    }
    
    @Test
    fun `WorkoutStatus valueOf returns correct enum`() {
        assertThat(WorkoutStatus.valueOf("NOT_STARTED")).isEqualTo(WorkoutStatus.NOT_STARTED)
        assertThat(WorkoutStatus.valueOf("IN_PROGRESS")).isEqualTo(WorkoutStatus.IN_PROGRESS)
        assertThat(WorkoutStatus.valueOf("COMPLETED")).isEqualTo(WorkoutStatus.COMPLETED)
    }
    
    @Test
    fun `WorkoutStatus can be compared`() {
        val status1 = WorkoutStatus.NOT_STARTED
        val status2 = WorkoutStatus.NOT_STARTED
        val status3 = WorkoutStatus.IN_PROGRESS
        
        assertThat(status1).isEqualTo(status2)
        assertThat(status1).isNotEqualTo(status3)
    }
    
    @Test
    fun `WorkoutStatus ordinal values are consistent`() {
        assertThat(WorkoutStatus.NOT_STARTED.ordinal).isEqualTo(0)
        assertThat(WorkoutStatus.IN_PROGRESS.ordinal).isEqualTo(1)
        assertThat(WorkoutStatus.COMPLETED.ordinal).isEqualTo(2)
    }
}