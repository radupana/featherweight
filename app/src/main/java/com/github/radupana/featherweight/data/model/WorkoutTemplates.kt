package com.github.radupana.featherweight.data.model

object WorkoutTemplates {
    private val PUSH =
        WorkoutTemplate(
            name = "Push",
            muscleGroups = listOf("Chest", "Shoulders", "Triceps"),
            exerciseSlots =
                listOf(
                    // Required Slot 1: Barbell/Dumbbell Bench Press
                    ExerciseSlot(
                        required = true,
                        exerciseOptions =
                            listOf(
                                "Barbell Bench Press",
                                "Dumbbell Bench Press",
                            ),
                    ),
                    // Required Slot 2: Barbell/Dumbbell Shoulder Press
                    ExerciseSlot(
                        required = true,
                        exerciseOptions =
                            listOf(
                                "Barbell Shoulder Press",
                                "Dumbbell Shoulder Press",
                            ),
                    ),
                    // Optional Slot 3: Incline Press
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Incline Barbell Press",
                                "Incline Dumbbell Press",
                            ),
                    ),
                    // Optional Slot 4: Dips or Close Grip Bench
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Dip",
                                "Close Grip Bench Press",
                            ),
                    ),
                    // Optional Slot 5: Lateral Raises
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Cable Lateral Raise",
                                "Dumbbell Lateral Raise",
                            ),
                    ),
                    // Optional Slot 6: Tricep Isolation
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Cable Tricep Pushdown",
                                "Overhead Tricep Extension",
                            ),
                    ),
                    // Optional Slot 7: Chest Isolation
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Cable Fly",
                                "Dumbbell Fly",
                            ),
                    ),
                    // Optional Slot 8: Face Pull
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Face Pull",
                            ),
                    ),
                ),
        )

    private val PULL =
        WorkoutTemplate(
            name = "Pull",
            muscleGroups = listOf("Back", "Biceps"),
            exerciseSlots =
                listOf(
                    // Required Slot 1: Barbell Row or Cable Row
                    ExerciseSlot(
                        required = true,
                        exerciseOptions =
                            listOf(
                                "Barbell Row",
                                "Cable Row",
                            ),
                    ),
                    // Required Slot 2: Pull Up or Lat Pulldown
                    ExerciseSlot(
                        required = true,
                        exerciseOptions =
                            listOf(
                                "Pull Up",
                                "Lat Pulldown",
                            ),
                    ),
                    // Optional Slot 3: Dumbbell Row or T-Bar Row
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Dumbbell Row",
                                "T Bar Row",
                            ),
                    ),
                    // Optional Slot 4: Face Pull or Rear Delt Fly
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Cable Face Pull",
                                "Rear Delt Fly",
                            ),
                    ),
                    // Optional Slot 5: Barbell or Dumbbell Bicep Curl
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Barbell Bicep Curl",
                                "Dumbbell Bicep Curl",
                            ),
                    ),
                    // Optional Slot 6: Cable or Dumbbell Hammer Curl
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Cable Hammer Curl",
                                "Dumbbell Hammer Curl",
                            ),
                    ),
                    // Optional Slot 7: Cable or Dumbbell Shrug
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Cable Shrug",
                                "Dumbbell Shrug",
                            ),
                    ),
                    // Optional Slot 8: Cable Bicep Curl
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Cable Bicep Curl",
                            ),
                    ),
                ),
        )

    private val LEGS =
        WorkoutTemplate(
            name = "Legs",
            muscleGroups = listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"),
            exerciseSlots =
                listOf(
                    // Required Slot 1: Barbell Squat or Leg Press
                    ExerciseSlot(
                        required = true,
                        exerciseOptions =
                            listOf(
                                "Barbell Squat",
                                "Leg Press",
                            ),
                    ),
                    // Required Slot 2: Romanian Deadlift or Lying Leg Curl
                    ExerciseSlot(
                        required = true,
                        exerciseOptions =
                            listOf(
                                "Romanian Deadlift",
                                "Lying Leg Curl",
                            ),
                    ),
                    // Optional Slot 3: Walking Lunge or Bulgarian Split Squat
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Walking Lunge",
                                "Bulgarian Split Squat",
                            ),
                    ),
                    // Optional Slot 4: Leg Extension
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Leg Extension",
                            ),
                    ),
                    // Optional Slot 5: Standing or Seated Calf Raise
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Standing Calf Raise",
                                "Seated Calf Raise",
                            ),
                    ),
                    // Optional Slot 6: Leg Curl
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Leg Curl",
                            ),
                    ),
                    // Optional Slot 7: Goblet Squat
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Goblet Squat",
                            ),
                    ),
                    // Optional Slot 8: Cable Pull Through
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Cable Pull Through",
                            ),
                    ),
                ),
        )

    private val UPPER =
        WorkoutTemplate(
            name = "Upper",
            muscleGroups = listOf("Chest", "Back", "Shoulders", "Arms"),
            exerciseSlots =
                listOf(
                    // Required Slot 1: Barbell Bench Press
                    ExerciseSlot(
                        required = true,
                        exerciseOptions =
                            listOf(
                                "Barbell Bench Press",
                            ),
                    ),
                    // Required Slot 2: Barbell Row
                    ExerciseSlot(
                        required = true,
                        exerciseOptions =
                            listOf(
                                "Barbell Row",
                            ),
                    ),
                    // Required Slot 3: Barbell Shoulder Press
                    ExerciseSlot(
                        required = true,
                        exerciseOptions =
                            listOf(
                                "Barbell Shoulder Press",
                            ),
                    ),
                    // Optional Slot 4: Pull Up or Lat Pulldown
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Pull Up",
                                "Lat Pulldown",
                            ),
                    ),
                    // Optional Slot 5: Dips
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Dip",
                            ),
                    ),
                    // Optional Slot 6: Barbell Bicep Curl
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Barbell Bicep Curl",
                            ),
                    ),
                    // Optional Slot 7: Cable Tricep Pushdown
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Cable Tricep Pushdown",
                            ),
                    ),
                    // Optional Slot 8: Cable Lateral Raise
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Cable Lateral Raise",
                            ),
                    ),
                ),
        )

    private val LOWER =
        WorkoutTemplate(
            name = "Lower",
            muscleGroups = listOf("Quadriceps", "Hamstrings", "Glutes", "Calves"),
            exerciseSlots =
                listOf(
                    // Required Slot 1: Barbell Squat
                    ExerciseSlot(
                        required = true,
                        exerciseOptions =
                            listOf(
                                "Barbell Squat",
                            ),
                    ),
                    // Required Slot 2: Romanian Deadlift
                    ExerciseSlot(
                        required = true,
                        exerciseOptions =
                            listOf(
                                "Romanian Deadlift",
                            ),
                    ),
                    // Required Slot 3: Walking Lunge
                    ExerciseSlot(
                        required = true,
                        exerciseOptions =
                            listOf(
                                "Walking Lunge",
                            ),
                    ),
                    // Optional Slot 4: Leg Press
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Leg Press",
                            ),
                    ),
                    // Optional Slot 5: Lying Leg Curl
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Lying Leg Curl",
                            ),
                    ),
                    // Optional Slot 6: Leg Extension
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Leg Extension",
                            ),
                    ),
                    // Optional Slot 7: Standing Calf Raise
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Standing Calf Raise",
                            ),
                    ),
                    // Optional Slot 8: Bulgarian Split Squat
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Bulgarian Split Squat",
                            ),
                    ),
                ),
        )

    private val FULL_BODY =
        WorkoutTemplate(
            name = "Full Body",
            muscleGroups = listOf("Full Body"),
            exerciseSlots =
                listOf(
                    // Required Slot 1: Barbell Squat or Goblet Squat
                    ExerciseSlot(
                        required = true,
                        exerciseOptions =
                            listOf(
                                "Barbell Squat",
                                "Goblet Squat",
                            ),
                    ),
                    // Required Slot 2: Barbell Bench Press or Dumbbell Bench Press
                    ExerciseSlot(
                        required = true,
                        exerciseOptions =
                            listOf(
                                "Barbell Bench Press",
                                "Dumbbell Bench Press",
                            ),
                    ),
                    // Required Slot 3: Barbell Row or Cable Row
                    ExerciseSlot(
                        required = true,
                        exerciseOptions =
                            listOf(
                                "Barbell Row",
                                "Cable Row",
                            ),
                    ),
                    // Optional Slot 4: Barbell Shoulder Press
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Barbell Shoulder Press",
                            ),
                    ),
                    // Optional Slot 5: Romanian Deadlift
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Romanian Deadlift",
                            ),
                    ),
                    // Optional Slot 6: Pull Up or Lat Pulldown
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Pull Up",
                                "Lat Pulldown",
                            ),
                    ),
                    // Optional Slot 7: Dips
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Dip",
                            ),
                    ),
                    // Optional Slot 8: Walking Lunge
                    ExerciseSlot(
                        required = false,
                        exerciseOptions =
                            listOf(
                                "Walking Lunge",
                            ),
                    ),
                ),
        )

    fun getTemplate(name: String): WorkoutTemplate? =
        when (name.uppercase()) {
            "PUSH" -> PUSH
            "PULL" -> PULL
            "LEGS" -> LEGS
            "UPPER" -> UPPER
            "LOWER" -> LOWER
            "FULL BODY" -> FULL_BODY
            else -> null
        }
}
