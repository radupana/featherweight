package com.github.radupana.featherweight.viewmodel

import android.util.Log
import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.github.radupana.featherweight.util.ProgrammeTypeMapper
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test

class ImportProgrammeViewModelTest {
    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `mapProgrammeType maps STRENGTH correctly`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("STRENGTH")
        assertThat(result).isEqualTo(ProgrammeType.STRENGTH)
    }

    @Test
    fun `mapProgrammeType maps strength in lowercase correctly`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("strength")
        assertThat(result).isEqualTo(ProgrammeType.STRENGTH)
    }

    @Test
    fun `mapProgrammeType maps POWERLIFTING correctly`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("POWERLIFTING")
        assertThat(result).isEqualTo(ProgrammeType.POWERLIFTING)
    }

    @Test
    fun `mapProgrammeType maps BODYBUILDING correctly`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("BODYBUILDING")
        assertThat(result).isEqualTo(ProgrammeType.BODYBUILDING)
    }

    @Test
    fun `mapProgrammeType maps HYPERTROPHY to BODYBUILDING`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("HYPERTROPHY")
        assertThat(result).isEqualTo(ProgrammeType.BODYBUILDING)
    }

    @Test
    fun `mapProgrammeType maps hypertrophy in lowercase to BODYBUILDING`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("hypertrophy")
        assertThat(result).isEqualTo(ProgrammeType.BODYBUILDING)
    }

    @Test
    fun `mapProgrammeType maps GENERAL_FITNESS correctly`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("GENERAL_FITNESS")
        assertThat(result).isEqualTo(ProgrammeType.GENERAL_FITNESS)
    }

    @Test
    fun `mapProgrammeType maps GENERAL to GENERAL_FITNESS`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("GENERAL")
        assertThat(result).isEqualTo(ProgrammeType.GENERAL_FITNESS)
    }

    @Test
    fun `mapProgrammeType maps FITNESS to GENERAL_FITNESS`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("FITNESS")
        assertThat(result).isEqualTo(ProgrammeType.GENERAL_FITNESS)
    }

    @Test
    fun `mapProgrammeType maps OLYMPIC_LIFTING correctly`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("OLYMPIC_LIFTING")
        assertThat(result).isEqualTo(ProgrammeType.OLYMPIC_LIFTING)
    }

    @Test
    fun `mapProgrammeType maps OLYMPIC to OLYMPIC_LIFTING`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("OLYMPIC")
        assertThat(result).isEqualTo(ProgrammeType.OLYMPIC_LIFTING)
    }

    @Test
    fun `mapProgrammeType maps WEIGHTLIFTING to OLYMPIC_LIFTING`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("WEIGHTLIFTING")
        assertThat(result).isEqualTo(ProgrammeType.OLYMPIC_LIFTING)
    }

    @Test
    fun `mapProgrammeType maps HYBRID correctly`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("HYBRID")
        assertThat(result).isEqualTo(ProgrammeType.HYBRID)
    }

    @Test
    fun `mapProgrammeType maps POWERBUILDING to HYBRID`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("POWERBUILDING")
        assertThat(result).isEqualTo(ProgrammeType.HYBRID)
    }

    @Test
    fun `mapProgrammeType handles whitespace`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("  STRENGTH  ")
        assertThat(result).isEqualTo(ProgrammeType.STRENGTH)
    }

    @Test
    fun `mapProgrammeType defaults unknown types to GENERAL_FITNESS`() {
        val result = ProgrammeTypeMapper.mapProgrammeType("UNKNOWN_TYPE")
        assertThat(result).isEqualTo(ProgrammeType.GENERAL_FITNESS)
    }

    @Test
    fun `mapProgrammeDifficulty maps BEGINNER correctly`() {
        val result = ProgrammeTypeMapper.mapProgrammeDifficulty("BEGINNER")
        assertThat(result).isEqualTo(ProgrammeDifficulty.BEGINNER)
    }

    @Test
    fun `mapProgrammeDifficulty maps beginner in lowercase correctly`() {
        val result = ProgrammeTypeMapper.mapProgrammeDifficulty("beginner")
        assertThat(result).isEqualTo(ProgrammeDifficulty.BEGINNER)
    }

    @Test
    fun `mapProgrammeDifficulty maps NOVICE correctly`() {
        val result = ProgrammeTypeMapper.mapProgrammeDifficulty("NOVICE")
        assertThat(result).isEqualTo(ProgrammeDifficulty.NOVICE)
    }

    @Test
    fun `mapProgrammeDifficulty maps INTERMEDIATE correctly`() {
        val result = ProgrammeTypeMapper.mapProgrammeDifficulty("INTERMEDIATE")
        assertThat(result).isEqualTo(ProgrammeDifficulty.INTERMEDIATE)
    }

    @Test
    fun `mapProgrammeDifficulty maps ADVANCED correctly`() {
        val result = ProgrammeTypeMapper.mapProgrammeDifficulty("ADVANCED")
        assertThat(result).isEqualTo(ProgrammeDifficulty.ADVANCED)
    }

    @Test
    fun `mapProgrammeDifficulty maps EXPERT correctly`() {
        val result = ProgrammeTypeMapper.mapProgrammeDifficulty("EXPERT")
        assertThat(result).isEqualTo(ProgrammeDifficulty.EXPERT)
    }

    @Test
    fun `mapProgrammeDifficulty handles whitespace`() {
        val result = ProgrammeTypeMapper.mapProgrammeDifficulty("  INTERMEDIATE  ")
        assertThat(result).isEqualTo(ProgrammeDifficulty.INTERMEDIATE)
    }

    @Test
    fun `mapProgrammeDifficulty defaults unknown difficulties to INTERMEDIATE`() {
        val result = ProgrammeTypeMapper.mapProgrammeDifficulty("UNKNOWN")
        assertThat(result).isEqualTo(ProgrammeDifficulty.INTERMEDIATE)
    }
}
