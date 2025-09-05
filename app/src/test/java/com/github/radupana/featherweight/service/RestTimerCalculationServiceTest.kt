package com.github.radupana.featherweight.service

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class RestTimerCalculationServiceTest {
    private lateinit var service: RestTimerCalculationService

    @Before
    fun setUp() {
        service = RestTimerCalculationService()
    }

    @Test
    fun `RPE of 5 or below returns 60 seconds`() {
        assertThat(service.calculateRestDuration(rpe = 1f, exerciseRestDuration = null)).isEqualTo(60)
        assertThat(service.calculateRestDuration(rpe = 3f, exerciseRestDuration = null)).isEqualTo(60)
        assertThat(service.calculateRestDuration(rpe = 5f, exerciseRestDuration = null)).isEqualTo(60)
    }

    @Test
    fun `RPE between 5 and 7 returns 90 seconds`() {
        assertThat(service.calculateRestDuration(rpe = 5.5f, exerciseRestDuration = null)).isEqualTo(90)
        assertThat(service.calculateRestDuration(rpe = 6f, exerciseRestDuration = null)).isEqualTo(90)
        assertThat(service.calculateRestDuration(rpe = 7f, exerciseRestDuration = null)).isEqualTo(90)
    }

    @Test
    fun `RPE between 7 and 9 returns 120 seconds`() {
        assertThat(service.calculateRestDuration(rpe = 7.5f, exerciseRestDuration = null)).isEqualTo(120)
        assertThat(service.calculateRestDuration(rpe = 8f, exerciseRestDuration = null)).isEqualTo(120)
        assertThat(service.calculateRestDuration(rpe = 9f, exerciseRestDuration = null)).isEqualTo(120)
    }

    @Test
    fun `RPE of 10 returns 180 seconds`() {
        assertThat(service.calculateRestDuration(rpe = 10f, exerciseRestDuration = null)).isEqualTo(180)
    }

    @Test
    fun `RPE above 10 also returns 180 seconds`() {
        assertThat(service.calculateRestDuration(rpe = 11f, exerciseRestDuration = null)).isEqualTo(180)
        assertThat(service.calculateRestDuration(rpe = 15f, exerciseRestDuration = null)).isEqualTo(180)
    }

    @Test
    fun `when RPE is provided it takes priority over exercise rest duration`() {
        assertThat(service.calculateRestDuration(rpe = 8f, exerciseRestDuration = 180)).isEqualTo(120)
        assertThat(service.calculateRestDuration(rpe = 5f, exerciseRestDuration = 120)).isEqualTo(60)
    }

    @Test
    fun `when no RPE is provided uses exercise rest duration`() {
        assertThat(service.calculateRestDuration(rpe = null, exerciseRestDuration = 180)).isEqualTo(180)
        assertThat(service.calculateRestDuration(rpe = null, exerciseRestDuration = 120)).isEqualTo(120)
        assertThat(service.calculateRestDuration(rpe = null, exerciseRestDuration = 60)).isEqualTo(60)
    }

    @Test
    fun `when neither RPE nor exercise rest duration is provided returns default`() {
        assertThat(service.calculateRestDuration(rpe = null, exerciseRestDuration = null)).isEqualTo(90)
    }

    @Test
    fun `edge cases for RPE boundaries`() {
        assertThat(service.calculateRestDuration(rpe = 5.001f, exerciseRestDuration = null)).isEqualTo(90)
        assertThat(service.calculateRestDuration(rpe = 7.001f, exerciseRestDuration = null)).isEqualTo(120)
        assertThat(service.calculateRestDuration(rpe = 9.001f, exerciseRestDuration = null)).isEqualTo(180)
    }

    @Test
    fun `negative RPE values still calculate correctly`() {
        assertThat(service.calculateRestDuration(rpe = -1f, exerciseRestDuration = null)).isEqualTo(60)
        assertThat(service.calculateRestDuration(rpe = 0f, exerciseRestDuration = null)).isEqualTo(60)
    }
}
