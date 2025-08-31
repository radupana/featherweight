package com.github.radupana.featherweight.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutTemplateTest {
    // Tests for TimeAvailable enum
    @Test
    fun timeAvailable_hasCorrectValues() {
        val values = TimeAvailable.entries.toTypedArray()
        assertThat(values).hasLength(3)
        assertThat(values).asList().containsExactly(
            TimeAvailable.QUICK,
            TimeAvailable.STANDARD,
            TimeAvailable.EXTENDED,
        )
    }

    @Test
    fun timeAvailable_valueOf_returnsCorrectEnum() {
        assertThat(TimeAvailable.valueOf("QUICK")).isEqualTo(TimeAvailable.QUICK)
        assertThat(TimeAvailable.valueOf("STANDARD")).isEqualTo(TimeAvailable.STANDARD)
        assertThat(TimeAvailable.valueOf("EXTENDED")).isEqualTo(TimeAvailable.EXTENDED)
    }

    @Test(expected = IllegalArgumentException::class)
    fun timeAvailable_valueOf_throwsForInvalidValue() {
        TimeAvailable.valueOf("INVALID")
    }

    // Tests for TrainingGoal enum
    @Test
    fun trainingGoal_hasCorrectValues() {
        val values = TrainingGoal.values()
        assertThat(values).hasLength(3)
        assertThat(values).asList().containsExactly(
            TrainingGoal.STRENGTH,
            TrainingGoal.HYPERTROPHY,
            TrainingGoal.ENDURANCE,
        )
    }

    @Test
    fun trainingGoal_valueOf_returnsCorrectEnum() {
        assertThat(TrainingGoal.valueOf("STRENGTH")).isEqualTo(TrainingGoal.STRENGTH)
        assertThat(TrainingGoal.valueOf("HYPERTROPHY")).isEqualTo(TrainingGoal.HYPERTROPHY)
        assertThat(TrainingGoal.valueOf("ENDURANCE")).isEqualTo(TrainingGoal.ENDURANCE)
    }

    // Tests for IntensityLevel enum
    @Test
    fun intensityLevel_hasCorrectValues() {
        val values = IntensityLevel.values()
        assertThat(values).hasLength(3)
        assertThat(values).asList().containsExactly(
            IntensityLevel.CONSERVATIVE,
            IntensityLevel.MODERATE,
            IntensityLevel.AGGRESSIVE,
        )
    }

    // Tests for SkillLevel enum
    @Test
    fun skillLevel_hasCorrectValues() {
        val values = SkillLevel.values()
        assertThat(values).hasLength(3)
        assertThat(values).asList().containsExactly(
            SkillLevel.BEGINNER,
            SkillLevel.INTERMEDIATE,
            SkillLevel.ADVANCED,
        )
    }

    @Test
    fun skillLevel_ordinal_increasesWithExperience() {
        assertThat(SkillLevel.BEGINNER.ordinal).isLessThan(SkillLevel.INTERMEDIATE.ordinal)
        assertThat(SkillLevel.INTERMEDIATE.ordinal).isLessThan(SkillLevel.ADVANCED.ordinal)
    }

    // Tests for WorkoutTemplateConfig data class
    @Test
    fun workoutTemplateConfig_creation() {
        val config =
            WorkoutTemplateConfig(
                timeAvailable = TimeAvailable.STANDARD,
                goal = TrainingGoal.STRENGTH,
                intensity = IntensityLevel.MODERATE,
            )

        assertThat(config.timeAvailable).isEqualTo(TimeAvailable.STANDARD)
        assertThat(config.goal).isEqualTo(TrainingGoal.STRENGTH)
        assertThat(config.intensity).isEqualTo(IntensityLevel.MODERATE)
    }

    @Test
    fun workoutTemplateConfig_equality() {
        val config1 =
            WorkoutTemplateConfig(
                timeAvailable = TimeAvailable.QUICK,
                goal = TrainingGoal.HYPERTROPHY,
                intensity = IntensityLevel.CONSERVATIVE,
            )

        val config2 =
            WorkoutTemplateConfig(
                timeAvailable = TimeAvailable.QUICK,
                goal = TrainingGoal.HYPERTROPHY,
                intensity = IntensityLevel.CONSERVATIVE,
            )

        val config3 =
            WorkoutTemplateConfig(
                timeAvailable = TimeAvailable.EXTENDED,
                goal = TrainingGoal.HYPERTROPHY,
                intensity = IntensityLevel.CONSERVATIVE,
            )

        assertThat(config1).isEqualTo(config2)
        assertThat(config1).isNotEqualTo(config3)
    }

    @Test
    fun workoutTemplateConfig_copy() {
        val original =
            WorkoutTemplateConfig(
                timeAvailable = TimeAvailable.STANDARD,
                goal = TrainingGoal.STRENGTH,
                intensity = IntensityLevel.MODERATE,
            )

        val modified = original.copy(intensity = IntensityLevel.AGGRESSIVE)

        assertThat(modified.timeAvailable).isEqualTo(original.timeAvailable)
        assertThat(modified.goal).isEqualTo(original.goal)
        assertThat(modified.intensity).isEqualTo(IntensityLevel.AGGRESSIVE)
        assertThat(modified.intensity).isNotEqualTo(original.intensity)
    }

    // Tests for ExerciseSlot data class
    @Test
    fun exerciseSlot_creation() {
        val slot =
            ExerciseSlot(
                required = true,
                exerciseOptions = listOf("Barbell Squat", "Front Squat", "Goblet Squat"),
            )

        assertThat(slot.required).isTrue()
        assertThat(slot.exerciseOptions).hasSize(3)
        assertThat(slot.exerciseOptions[0]).isEqualTo("Barbell Squat")
    }

    @Test
    fun exerciseSlot_canBeOptional() {
        val slot =
            ExerciseSlot(
                required = false,
                exerciseOptions = listOf("Leg Curls", "Romanian Deadlift"),
            )

        assertThat(slot.required).isFalse()
        assertThat(slot.exerciseOptions).hasSize(2)
    }

    @Test
    fun exerciseSlot_canHaveEmptyOptions() {
        val slot =
            ExerciseSlot(
                required = false,
                exerciseOptions = emptyList(),
            )

        assertThat(slot.exerciseOptions).isEmpty()
    }

    // Tests for WorkoutTemplate data class
    @Test
    fun workoutTemplate_creation() {
        val template =
            WorkoutTemplate(
                name = "Upper Body Strength",
                muscleGroups = listOf("Chest", "Back", "Shoulders"),
                exerciseSlots =
                    listOf(
                        ExerciseSlot(true, listOf("Bench Press", "Dumbbell Press")),
                        ExerciseSlot(true, listOf("Pull-up", "Lat Pulldown")),
                        ExerciseSlot(false, listOf("Shoulder Press", "Military Press")),
                    ),
            )

        assertThat(template.name).isEqualTo("Upper Body Strength")
        assertThat(template.muscleGroups).hasSize(3)
        assertThat(template.exerciseSlots).hasSize(3)
        assertThat(template.exerciseSlots[0].required).isTrue()
        assertThat(template.exerciseSlots[2].required).isFalse()
    }

    @Test
    fun workoutTemplate_equality() {
        val slot1 = ExerciseSlot(true, listOf("Squat"))
        val slot2 = ExerciseSlot(false, listOf("Deadlift"))

        val template1 =
            WorkoutTemplate(
                name = "Leg Day",
                muscleGroups = listOf("Quads", "Hamstrings"),
                exerciseSlots = listOf(slot1, slot2),
            )

        val template2 =
            WorkoutTemplate(
                name = "Leg Day",
                muscleGroups = listOf("Quads", "Hamstrings"),
                exerciseSlots = listOf(slot1, slot2),
            )

        val template3 =
            WorkoutTemplate(
                name = "Push Day",
                muscleGroups = listOf("Chest", "Shoulders"),
                exerciseSlots = listOf(slot1),
            )

        assertThat(template1).isEqualTo(template2)
        assertThat(template1).isNotEqualTo(template3)
    }

    @Test
    fun workoutTemplate_canHaveNoExerciseSlots() {
        val template =
            WorkoutTemplate(
                name = "Rest Day",
                muscleGroups = emptyList(),
                exerciseSlots = emptyList(),
            )

        assertThat(template.exerciseSlots).isEmpty()
        assertThat(template.muscleGroups).isEmpty()
    }

    // Tests for WorkoutTemplateGenerationConfig data class
    @Test
    fun workoutTemplateGenerationConfig_creation() {
        val config =
            WorkoutTemplateGenerationConfig(
                time = TimeAvailable.EXTENDED,
                goal = TrainingGoal.ENDURANCE,
                skillLevel = SkillLevel.ADVANCED,
            )

        assertThat(config.time).isEqualTo(TimeAvailable.EXTENDED)
        assertThat(config.goal).isEqualTo(TrainingGoal.ENDURANCE)
        assertThat(config.skillLevel).isEqualTo(SkillLevel.ADVANCED)
    }

    @Test
    fun workoutTemplateGenerationConfig_differentCombinations() {
        val beginnerQuickStrength =
            WorkoutTemplateGenerationConfig(
                time = TimeAvailable.QUICK,
                goal = TrainingGoal.STRENGTH,
                skillLevel = SkillLevel.BEGINNER,
            )

        val advancedExtendedHypertrophy =
            WorkoutTemplateGenerationConfig(
                time = TimeAvailable.EXTENDED,
                goal = TrainingGoal.HYPERTROPHY,
                skillLevel = SkillLevel.ADVANCED,
            )

        assertThat(beginnerQuickStrength).isNotEqualTo(advancedExtendedHypertrophy)

        // Different configs should be distinct
        assertThat(beginnerQuickStrength.hashCode()).isNotEqualTo(advancedExtendedHypertrophy.hashCode())
    }

    @Test
    fun workoutTemplateGenerationConfig_copy() {
        val original =
            WorkoutTemplateGenerationConfig(
                time = TimeAvailable.STANDARD,
                goal = TrainingGoal.STRENGTH,
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        val modified = original.copy(skillLevel = SkillLevel.ADVANCED)

        assertThat(modified.time).isEqualTo(original.time)
        assertThat(modified.goal).isEqualTo(original.goal)
        assertThat(modified.skillLevel).isEqualTo(SkillLevel.ADVANCED)
        assertThat(modified).isNotEqualTo(original)
    }

    // Integration tests
    @Test
    fun workoutTemplate_realWorldExample() {
        // Create a realistic workout template
        val pushDayTemplate =
            WorkoutTemplate(
                name = "Push Day (Chest, Shoulders, Triceps)",
                muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
                exerciseSlots =
                    listOf(
                        // Main compound movement - required
                        ExerciseSlot(
                            required = true,
                            exerciseOptions = listOf("Barbell Bench Press", "Dumbbell Bench Press", "Incline Barbell Press"),
                        ),
                        // Secondary compound - required
                        ExerciseSlot(
                            required = true,
                            exerciseOptions = listOf("Overhead Press", "Dumbbell Shoulder Press", "Arnold Press"),
                        ),
                        // Chest isolation - optional
                        ExerciseSlot(
                            required = false,
                            exerciseOptions = listOf("Cable Fly", "Dumbbell Fly", "Pec Deck"),
                        ),
                        // Shoulder isolation - optional
                        ExerciseSlot(
                            required = false,
                            exerciseOptions = listOf("Lateral Raise", "Face Pull", "Rear Delt Fly"),
                        ),
                        // Tricep work - optional
                        ExerciseSlot(
                            required = false,
                            exerciseOptions = listOf("Tricep Pushdown", "Overhead Extension", "Close Grip Bench"),
                        ),
                    ),
            )

        assertThat(pushDayTemplate.name).contains("Push Day")
        assertThat(pushDayTemplate.muscleGroups).containsExactly("Chest", "Shoulders", "Triceps")
        assertThat(pushDayTemplate.exerciseSlots).hasSize(5)

        // Count required vs optional
        val requiredCount = pushDayTemplate.exerciseSlots.count { it.required }
        val optionalCount = pushDayTemplate.exerciseSlots.count { !it.required }

        assertThat(requiredCount).isEqualTo(2)
        assertThat(optionalCount).isEqualTo(3)
    }

    @Test
    fun workoutTemplateConfig_allCombinations() {
        // Test that all enum combinations work
        for (time in TimeAvailable.entries) {
            for (goal in TrainingGoal.entries) {
                for (intensity in IntensityLevel.entries) {
                    val config = WorkoutTemplateConfig(time, goal, intensity)
                    assertThat(config).isNotNull()
                    assertThat(config.timeAvailable).isEqualTo(time)
                    assertThat(config.goal).isEqualTo(goal)
                    assertThat(config.intensity).isEqualTo(intensity)
                }
            }
        }
    }
}
