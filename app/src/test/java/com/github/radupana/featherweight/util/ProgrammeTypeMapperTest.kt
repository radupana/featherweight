package com.github.radupana.featherweight.util

import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProgrammeTypeMapperTest {
    // Programme Type Mapping Tests

    @Test
    fun `mapProgrammeType maps STRENGTH correctly`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("STRENGTH")).isEqualTo(ProgrammeType.STRENGTH)
    }

    @Test
    fun `mapProgrammeType maps POWERLIFTING correctly`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("POWERLIFTING")).isEqualTo(ProgrammeType.POWERLIFTING)
    }

    @Test
    fun `mapProgrammeType maps BODYBUILDING correctly`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("BODYBUILDING")).isEqualTo(ProgrammeType.BODYBUILDING)
    }

    @Test
    fun `mapProgrammeType maps HYPERTROPHY to BODYBUILDING`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("HYPERTROPHY")).isEqualTo(ProgrammeType.BODYBUILDING)
    }

    @Test
    fun `mapProgrammeType maps GENERAL_FITNESS correctly`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("GENERAL_FITNESS")).isEqualTo(ProgrammeType.GENERAL_FITNESS)
    }

    @Test
    fun `mapProgrammeType maps GENERAL to GENERAL_FITNESS`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("GENERAL")).isEqualTo(ProgrammeType.GENERAL_FITNESS)
    }

    @Test
    fun `mapProgrammeType maps FITNESS to GENERAL_FITNESS`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("FITNESS")).isEqualTo(ProgrammeType.GENERAL_FITNESS)
    }

    @Test
    fun `mapProgrammeType maps OLYMPIC_LIFTING correctly`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("OLYMPIC_LIFTING")).isEqualTo(ProgrammeType.OLYMPIC_LIFTING)
    }

    @Test
    fun `mapProgrammeType maps OLYMPIC to OLYMPIC_LIFTING`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("OLYMPIC")).isEqualTo(ProgrammeType.OLYMPIC_LIFTING)
    }

    @Test
    fun `mapProgrammeType maps WEIGHTLIFTING to OLYMPIC_LIFTING`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("WEIGHTLIFTING")).isEqualTo(ProgrammeType.OLYMPIC_LIFTING)
    }

    @Test
    fun `mapProgrammeType maps HYBRID correctly`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("HYBRID")).isEqualTo(ProgrammeType.HYBRID)
    }

    @Test
    fun `mapProgrammeType maps POWERBUILDING to HYBRID`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("POWERBUILDING")).isEqualTo(ProgrammeType.HYBRID)
    }

    @Test
    fun `mapProgrammeType is case insensitive - lowercase`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("strength")).isEqualTo(ProgrammeType.STRENGTH)
    }

    @Test
    fun `mapProgrammeType is case insensitive - mixed case`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("Powerlifting")).isEqualTo(ProgrammeType.POWERLIFTING)
    }

    @Test
    fun `mapProgrammeType trims whitespace`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("  STRENGTH  ")).isEqualTo(ProgrammeType.STRENGTH)
    }

    @Test
    fun `mapProgrammeType returns GENERAL_FITNESS for unknown type`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("UNKNOWN_TYPE")).isEqualTo(ProgrammeType.GENERAL_FITNESS)
    }

    @Test
    fun `mapProgrammeType returns GENERAL_FITNESS for empty string`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeType("")).isEqualTo(ProgrammeType.GENERAL_FITNESS)
    }

    // Programme Difficulty Mapping Tests

    @Test
    fun `mapProgrammeDifficulty maps BEGINNER correctly`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeDifficulty("BEGINNER")).isEqualTo(ProgrammeDifficulty.BEGINNER)
    }

    @Test
    fun `mapProgrammeDifficulty maps NOVICE correctly`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeDifficulty("NOVICE")).isEqualTo(ProgrammeDifficulty.NOVICE)
    }

    @Test
    fun `mapProgrammeDifficulty maps INTERMEDIATE correctly`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeDifficulty("INTERMEDIATE")).isEqualTo(ProgrammeDifficulty.INTERMEDIATE)
    }

    @Test
    fun `mapProgrammeDifficulty maps ADVANCED correctly`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeDifficulty("ADVANCED")).isEqualTo(ProgrammeDifficulty.ADVANCED)
    }

    @Test
    fun `mapProgrammeDifficulty maps EXPERT correctly`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeDifficulty("EXPERT")).isEqualTo(ProgrammeDifficulty.EXPERT)
    }

    @Test
    fun `mapProgrammeDifficulty is case insensitive - lowercase`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeDifficulty("beginner")).isEqualTo(ProgrammeDifficulty.BEGINNER)
    }

    @Test
    fun `mapProgrammeDifficulty is case insensitive - mixed case`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeDifficulty("Advanced")).isEqualTo(ProgrammeDifficulty.ADVANCED)
    }

    @Test
    fun `mapProgrammeDifficulty trims whitespace`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeDifficulty("  INTERMEDIATE  ")).isEqualTo(ProgrammeDifficulty.INTERMEDIATE)
    }

    @Test
    fun `mapProgrammeDifficulty returns INTERMEDIATE for unknown difficulty`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeDifficulty("UNKNOWN_DIFFICULTY")).isEqualTo(ProgrammeDifficulty.INTERMEDIATE)
    }

    @Test
    fun `mapProgrammeDifficulty returns INTERMEDIATE for empty string`() {
        assertThat(ProgrammeTypeMapper.mapProgrammeDifficulty("")).isEqualTo(ProgrammeDifficulty.INTERMEDIATE)
    }

    // Enum Coverage Tests

    @Test
    fun `ProgrammeType enum has all expected values`() {
        val types = ProgrammeType.entries.map { it.name }

        assertThat(types).containsExactly(
            "STRENGTH",
            "POWERLIFTING",
            "BODYBUILDING",
            "GENERAL_FITNESS",
            "OLYMPIC_LIFTING",
            "HYBRID",
        )
    }

    @Test
    fun `ProgrammeDifficulty enum has all expected values`() {
        val difficulties = ProgrammeDifficulty.entries.map { it.name }

        assertThat(difficulties).containsExactly(
            "BEGINNER",
            "NOVICE",
            "INTERMEDIATE",
            "ADVANCED",
            "EXPERT",
        )
    }
}
