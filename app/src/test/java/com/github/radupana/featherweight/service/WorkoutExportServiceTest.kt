package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.export.ExportOptions
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for WorkoutExportService.
 *
 * Note: The WorkoutExportService uses android.util.JsonWriter which requires Android framework.
 * These tests verify the ExportOptions data class and service configuration without
 * actually running the export logic.
 *
 * Full export functionality tests require Android instrumentation tests.
 */
class WorkoutExportServiceTest {
    @Test
    fun `ExportOptions has correct default values`() {
        val options = ExportOptions()

        assertThat(options.includeBodyweight).isTrue()
        assertThat(options.includeOneRepMaxes).isTrue()
        assertThat(options.includeNotes).isTrue()
        assertThat(options.includeProfile).isTrue()
    }

    @Test
    fun `ExportOptions can be customized`() {
        val options =
            ExportOptions(
                includeBodyweight = true,
                includeOneRepMaxes = true,
                includeNotes = false,
                includeProfile = true,
            )

        assertThat(options.includeBodyweight).isTrue()
        assertThat(options.includeOneRepMaxes).isTrue()
        assertThat(options.includeNotes).isFalse()
        assertThat(options.includeProfile).isTrue()
    }

    @Test
    fun `ExportOptions copy works correctly`() {
        val original = ExportOptions(includeNotes = true)
        val modified = original.copy(includeNotes = false)

        assertThat(original.includeNotes).isTrue()
        assertThat(modified.includeNotes).isFalse()
    }
}
