package com.github.radupana.featherweight.data.exercise

import com.github.radupana.featherweight.data.ParseStatus
import com.github.radupana.featherweight.data.profile.OneRMType
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class ExerciseTypeConvertersTest {
    private lateinit var converters: ExerciseTypeConverters

    @Before
    fun setUp() {
        converters = ExerciseTypeConverters()
    }

    @Test
    fun `fromExerciseCategory converts enum to string`() {
        assertThat(converters.fromExerciseCategory(ExerciseCategory.CHEST)).isEqualTo("CHEST")
        assertThat(converters.fromExerciseCategory(ExerciseCategory.BACK)).isEqualTo("BACK")
        assertThat(converters.fromExerciseCategory(ExerciseCategory.SHOULDERS)).isEqualTo("SHOULDERS")
        assertThat(converters.fromExerciseCategory(ExerciseCategory.ARMS)).isEqualTo("ARMS")
        assertThat(converters.fromExerciseCategory(ExerciseCategory.LEGS)).isEqualTo("LEGS")
        assertThat(converters.fromExerciseCategory(ExerciseCategory.CORE)).isEqualTo("CORE")
        assertThat(converters.fromExerciseCategory(ExerciseCategory.CARDIO)).isEqualTo("CARDIO")
        assertThat(converters.fromExerciseCategory(ExerciseCategory.FULL_BODY)).isEqualTo("FULL_BODY")
    }

    @Test
    fun `toExerciseCategory converts string to enum`() {
        assertThat(converters.toExerciseCategory("CHEST")).isEqualTo(ExerciseCategory.CHEST)
        assertThat(converters.toExerciseCategory("BACK")).isEqualTo(ExerciseCategory.BACK)
        assertThat(converters.toExerciseCategory("SHOULDERS")).isEqualTo(ExerciseCategory.SHOULDERS)
        assertThat(converters.toExerciseCategory("ARMS")).isEqualTo(ExerciseCategory.ARMS)
        assertThat(converters.toExerciseCategory("LEGS")).isEqualTo(ExerciseCategory.LEGS)
        assertThat(converters.toExerciseCategory("CORE")).isEqualTo(ExerciseCategory.CORE)
        assertThat(converters.toExerciseCategory("CARDIO")).isEqualTo(ExerciseCategory.CARDIO)
        assertThat(converters.toExerciseCategory("FULL_BODY")).isEqualTo(ExerciseCategory.FULL_BODY)
    }

    @Test
    fun `fromExerciseDifficulty converts enum to string`() {
        assertThat(converters.fromExerciseDifficulty(ExerciseDifficulty.BEGINNER)).isEqualTo("BEGINNER")
        assertThat(converters.fromExerciseDifficulty(ExerciseDifficulty.INTERMEDIATE)).isEqualTo("INTERMEDIATE")
        assertThat(converters.fromExerciseDifficulty(ExerciseDifficulty.ADVANCED)).isEqualTo("ADVANCED")
    }

    @Test
    fun `toExerciseDifficulty converts string to enum`() {
        assertThat(converters.toExerciseDifficulty("BEGINNER")).isEqualTo(ExerciseDifficulty.BEGINNER)
        assertThat(converters.toExerciseDifficulty("INTERMEDIATE")).isEqualTo(ExerciseDifficulty.INTERMEDIATE)
        assertThat(converters.toExerciseDifficulty("ADVANCED")).isEqualTo(ExerciseDifficulty.ADVANCED)
    }

    @Test
    fun `fromEquipment converts enum to string`() {
        assertThat(converters.fromEquipment(Equipment.BARBELL)).isEqualTo("BARBELL")
        assertThat(converters.fromEquipment(Equipment.DUMBBELL)).isEqualTo("DUMBBELL")
        assertThat(converters.fromEquipment(Equipment.CABLE)).isEqualTo("CABLE")
        assertThat(converters.fromEquipment(Equipment.MACHINE)).isEqualTo("MACHINE")
        assertThat(converters.fromEquipment(Equipment.BODYWEIGHT)).isEqualTo("BODYWEIGHT")
        assertThat(converters.fromEquipment(Equipment.KETTLEBELL)).isEqualTo("KETTLEBELL")
        assertThat(converters.fromEquipment(Equipment.BAND)).isEqualTo("BAND")
        assertThat(converters.fromEquipment(Equipment.SMITH_MACHINE)).isEqualTo("SMITH_MACHINE")
        assertThat(converters.fromEquipment(Equipment.SWISS_BAR)).isEqualTo("SWISS_BAR")
        assertThat(converters.fromEquipment(Equipment.TRAP_BAR)).isEqualTo("TRAP_BAR")
        assertThat(converters.fromEquipment(Equipment.SANDBAG)).isEqualTo("SANDBAG")
    }

    @Test
    fun `toEquipment converts string to enum`() {
        assertThat(converters.toEquipment("BARBELL")).isEqualTo(Equipment.BARBELL)
        assertThat(converters.toEquipment("DUMBBELL")).isEqualTo(Equipment.DUMBBELL)
        assertThat(converters.toEquipment("CABLE")).isEqualTo(Equipment.CABLE)
        assertThat(converters.toEquipment("MACHINE")).isEqualTo(Equipment.MACHINE)
        assertThat(converters.toEquipment("BODYWEIGHT")).isEqualTo(Equipment.BODYWEIGHT)
        assertThat(converters.toEquipment("KETTLEBELL")).isEqualTo(Equipment.KETTLEBELL)
        assertThat(converters.toEquipment("BAND")).isEqualTo(Equipment.BAND)
        assertThat(converters.toEquipment("SMITH_MACHINE")).isEqualTo(Equipment.SMITH_MACHINE)
        assertThat(converters.toEquipment("SWISS_BAR")).isEqualTo(Equipment.SWISS_BAR)
        assertThat(converters.toEquipment("TRAP_BAR")).isEqualTo(Equipment.TRAP_BAR)
        assertThat(converters.toEquipment("SANDBAG")).isEqualTo(Equipment.SANDBAG)
    }

    @Test
    fun `fromMovementPattern converts enum to string`() {
        assertThat(converters.fromMovementPattern(MovementPattern.PUSH)).isEqualTo("PUSH")
        assertThat(converters.fromMovementPattern(MovementPattern.PULL)).isEqualTo("PULL")
        assertThat(converters.fromMovementPattern(MovementPattern.SQUAT)).isEqualTo("SQUAT")
        assertThat(converters.fromMovementPattern(MovementPattern.HINGE)).isEqualTo("HINGE")
        assertThat(converters.fromMovementPattern(MovementPattern.LUNGE)).isEqualTo("LUNGE")
        assertThat(converters.fromMovementPattern(MovementPattern.CARRY)).isEqualTo("CARRY")
        assertThat(converters.fromMovementPattern(MovementPattern.ROTATION)).isEqualTo("ROTATION")
        assertThat(converters.fromMovementPattern(MovementPattern.VERTICAL_PUSH)).isEqualTo("VERTICAL_PUSH")
        assertThat(converters.fromMovementPattern(MovementPattern.EXTENSION)).isEqualTo("EXTENSION")
        assertThat(converters.fromMovementPattern(MovementPattern.HORIZONTAL_PULL)).isEqualTo("HORIZONTAL_PULL")
        assertThat(converters.fromMovementPattern(MovementPattern.PLANK)).isEqualTo("PLANK")
        assertThat(converters.fromMovementPattern(MovementPattern.ANTI_ROTATION)).isEqualTo("ANTI_ROTATION")
        assertThat(converters.fromMovementPattern(MovementPattern.ISOMETRIC)).isEqualTo("ISOMETRIC")
        assertThat(converters.fromMovementPattern(MovementPattern.JUMP)).isEqualTo("JUMP")
        assertThat(converters.fromMovementPattern(MovementPattern.CONDITIONING)).isEqualTo("CONDITIONING")
    }

    @Test
    fun `toMovementPattern converts string to enum`() {
        assertThat(converters.toMovementPattern("PUSH")).isEqualTo(MovementPattern.PUSH)
        assertThat(converters.toMovementPattern("PULL")).isEqualTo(MovementPattern.PULL)
        assertThat(converters.toMovementPattern("SQUAT")).isEqualTo(MovementPattern.SQUAT)
        assertThat(converters.toMovementPattern("HINGE")).isEqualTo(MovementPattern.HINGE)
        assertThat(converters.toMovementPattern("LUNGE")).isEqualTo(MovementPattern.LUNGE)
        assertThat(converters.toMovementPattern("CARRY")).isEqualTo(MovementPattern.CARRY)
        assertThat(converters.toMovementPattern("ROTATION")).isEqualTo(MovementPattern.ROTATION)
        assertThat(converters.toMovementPattern("VERTICAL_PUSH")).isEqualTo(MovementPattern.VERTICAL_PUSH)
        assertThat(converters.toMovementPattern("EXTENSION")).isEqualTo(MovementPattern.EXTENSION)
        assertThat(converters.toMovementPattern("HORIZONTAL_PULL")).isEqualTo(MovementPattern.HORIZONTAL_PULL)
        assertThat(converters.toMovementPattern("PLANK")).isEqualTo(MovementPattern.PLANK)
        assertThat(converters.toMovementPattern("ANTI_ROTATION")).isEqualTo(MovementPattern.ANTI_ROTATION)
        assertThat(converters.toMovementPattern("ISOMETRIC")).isEqualTo(MovementPattern.ISOMETRIC)
        assertThat(converters.toMovementPattern("JUMP")).isEqualTo(MovementPattern.JUMP)
        assertThat(converters.toMovementPattern("CONDITIONING")).isEqualTo(MovementPattern.CONDITIONING)
    }

    @Test
    fun `fromInstructionType converts enum to string`() {
        assertThat(converters.fromInstructionType(InstructionType.EXECUTION)).isEqualTo("EXECUTION")
    }

    @Test
    fun `toInstructionType converts string to enum`() {
        assertThat(converters.toInstructionType("EXECUTION")).isEqualTo(InstructionType.EXECUTION)
    }

    @Test
    fun `fromOneRMType converts enum to string`() {
        assertThat(converters.fromOneRMType(OneRMType.MANUALLY_ENTERED)).isEqualTo("MANUALLY_ENTERED")
        assertThat(converters.fromOneRMType(OneRMType.AUTOMATICALLY_CALCULATED)).isEqualTo("AUTOMATICALLY_CALCULATED")
    }

    @Test
    fun `toOneRMType converts string to enum`() {
        assertThat(converters.toOneRMType("MANUALLY_ENTERED")).isEqualTo(OneRMType.MANUALLY_ENTERED)
        assertThat(converters.toOneRMType("AUTOMATICALLY_CALCULATED")).isEqualTo(OneRMType.AUTOMATICALLY_CALCULATED)
    }

    @Test
    fun `fromParseStatus converts enum to string`() {
        assertThat(converters.fromParseStatus(ParseStatus.PROCESSING)).isEqualTo("PROCESSING")
        assertThat(converters.fromParseStatus(ParseStatus.COMPLETED)).isEqualTo("COMPLETED")
        assertThat(converters.fromParseStatus(ParseStatus.FAILED)).isEqualTo("FAILED")
        assertThat(converters.fromParseStatus(ParseStatus.IMPORTED)).isEqualTo("IMPORTED")
    }

    @Test
    fun `toParseStatus converts string to enum`() {
        assertThat(converters.toParseStatus("PROCESSING")).isEqualTo(ParseStatus.PROCESSING)
        assertThat(converters.toParseStatus("COMPLETED")).isEqualTo(ParseStatus.COMPLETED)
        assertThat(converters.toParseStatus("FAILED")).isEqualTo(ParseStatus.FAILED)
        assertThat(converters.toParseStatus("IMPORTED")).isEqualTo(ParseStatus.IMPORTED)
    }

    @Test
    fun `round trip conversion for ExerciseCategory`() {
        ExerciseCategory.values().forEach { category ->
            val string = converters.fromExerciseCategory(category)
            val result = converters.toExerciseCategory(string)
            assertThat(result).isEqualTo(category)
        }
    }

    @Test
    fun `round trip conversion for ExerciseDifficulty`() {
        ExerciseDifficulty.values().forEach { difficulty ->
            val string = converters.fromExerciseDifficulty(difficulty)
            val result = converters.toExerciseDifficulty(string)
            assertThat(result).isEqualTo(difficulty)
        }
    }

    @Test
    fun `round trip conversion for Equipment`() {
        Equipment.values().forEach { equipment ->
            val string = converters.fromEquipment(equipment)
            val result = converters.toEquipment(string)
            assertThat(result).isEqualTo(equipment)
        }
    }

    @Test
    fun `round trip conversion for MovementPattern`() {
        MovementPattern.values().forEach { pattern ->
            val string = converters.fromMovementPattern(pattern)
            val result = converters.toMovementPattern(string)
            assertThat(result).isEqualTo(pattern)
        }
    }

    @Test
    fun `round trip conversion for InstructionType`() {
        InstructionType.values().forEach { type ->
            val string = converters.fromInstructionType(type)
            val result = converters.toInstructionType(string)
            assertThat(result).isEqualTo(type)
        }
    }

    @Test
    fun `round trip conversion for OneRMType`() {
        OneRMType.values().forEach { type ->
            val string = converters.fromOneRMType(type)
            val result = converters.toOneRMType(string)
            assertThat(result).isEqualTo(type)
        }
    }

    @Test
    fun `round trip conversion for ParseStatus`() {
        ParseStatus.values().forEach { status ->
            val string = converters.fromParseStatus(status)
            val result = converters.toParseStatus(string)
            assertThat(result).isEqualTo(status)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toExerciseCategory throws on invalid string`() {
        converters.toExerciseCategory("INVALID_CATEGORY")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toEquipment throws on invalid string`() {
        converters.toEquipment("INVALID_EQUIPMENT")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toMovementPattern throws on invalid string`() {
        converters.toMovementPattern("INVALID_PATTERN")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toOneRMType throws on invalid string`() {
        converters.toOneRMType("INVALID_TYPE")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toParseStatus throws on invalid string`() {
        converters.toParseStatus("INVALID_STATUS")
    }
}
