package com.github.radupana.featherweight.ui.screens

import com.github.radupana.featherweight.data.WorkoutMode
import com.github.radupana.featherweight.data.WorkoutStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutScreenTest {
    @Test
    fun `WorkoutMode enum values are correctly defined`() {
        // Test that WorkoutMode enum has expected values
        val modes = WorkoutMode.values()

        assertThat(modes).asList().contains(WorkoutMode.ACTIVE)
        assertThat(modes).asList().contains(WorkoutMode.TEMPLATE_EDIT)

        // Verify enum ordinals for serialization
        assertThat(WorkoutMode.ACTIVE.ordinal).isEqualTo(0)
        assertThat(WorkoutMode.TEMPLATE_EDIT.ordinal).isEqualTo(1)
    }

    @Test
    fun `WorkoutStatus enum values are correctly defined`() {
        // Test that WorkoutStatus enum has expected values
        val statuses = WorkoutStatus.values()

        assertThat(statuses).asList().contains(WorkoutStatus.NOT_STARTED)
        assertThat(statuses).asList().contains(WorkoutStatus.IN_PROGRESS)
        assertThat(statuses).asList().contains(WorkoutStatus.COMPLETED)
        assertThat(statuses).asList().contains(WorkoutStatus.TEMPLATE)
        assertThat(statuses.size).isEqualTo(4)
    }

    @Test
    fun `WorkoutMode and WorkoutStatus are distinct enums`() {
        // Verify that WorkoutMode and WorkoutStatus are different types
        val mode = WorkoutMode.ACTIVE
        val status = WorkoutStatus.IN_PROGRESS

        // They are different types
        assertThat(mode::class.java).isNotEqualTo(status::class.java)

        // Mode names
        assertThat(WorkoutMode.values().map { it.name })
            .containsExactly("ACTIVE", "TEMPLATE_EDIT")
    }

    @Test
    fun `Template edit mode should not be confused with in progress status`() {
        val editMode = WorkoutMode.TEMPLATE_EDIT

        assertThat(editMode).isNotEqualTo(WorkoutMode.ACTIVE)
        assertThat(editMode.name).doesNotContain("ACTIVE")

        assertThat(WorkoutStatus.entries.map { it.name })
            .containsExactly("NOT_STARTED", "IN_PROGRESS", "COMPLETED", "TEMPLATE")
    }
}
