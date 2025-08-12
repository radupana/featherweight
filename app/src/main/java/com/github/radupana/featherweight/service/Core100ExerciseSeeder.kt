package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.exercise.*

/**
 * Seeds the database with the Core 100 exercises in normalized format.
 * These exercises represent the foundation of all training programs.
 * 
 * This seeder works with the simplified schema where:
 * - ExerciseCore is just a grouping mechanism
 * - ExerciseVariation is the central entity
 * - Muscles are stored in the VariationMuscle join table
 * - Instructions are stored in VariationInstruction
 * - Aliases are stored in VariationAlias
 */
class Core100ExerciseSeeder(
    private val database: FeatherweightDatabase
) {
    
    suspend fun seedCore100Exercises() {
        // Check if already seeded
        val existingCount = database.exerciseCoreDao().getExerciseCoreCount()
        if (existingCount >= 50) {
            return // Already seeded
        }
        
        seedSquatVariations()
        seedDeadliftVariations()
        seedBenchPressVariations()
        seedOverheadPressVariations()
        seedRowVariations()
        seedPullUpVariations()
        seedDipVariations()
        seedCurlVariations()
        seedTricepVariations()
        seedShoulderVariations()
        seedCoreExercises()
        seedLegAccessories()
    }
    
    private suspend fun seedSquatVariations() {
        // Create Squat core exercise
        val squatCore = ExerciseCore(
            name = "Squat",
            category = ExerciseCategory.LEGS,
            movementPattern = MovementPattern.SQUAT,
            isCompound = true
        )
        val squatCoreId = database.exerciseCoreDao().insertExerciseCore(squatCore)
        
        // Create variations with simplified schema
        val variations = listOf(
            Triple("Barbell Back Squat", Equipment.BARBELL, ExerciseDifficulty.INTERMEDIATE),
            Triple("Barbell Front Squat", Equipment.BARBELL, ExerciseDifficulty.ADVANCED),
            Triple("Goblet Squat", Equipment.DUMBBELL, ExerciseDifficulty.BEGINNER),
            Triple("Bulgarian Split Squat", Equipment.DUMBBELL, ExerciseDifficulty.INTERMEDIATE),
            Triple("Box Squat", Equipment.BARBELL, ExerciseDifficulty.INTERMEDIATE),
            Triple("Hack Squat", Equipment.MACHINE, ExerciseDifficulty.BEGINNER),
            Triple("Leg Press", Equipment.MACHINE, ExerciseDifficulty.BEGINNER)
        )
        
        variations.forEach { (name, equipment, difficulty) ->
            val variation = ExerciseVariation(
                coreExerciseId = squatCoreId,
                name = name,
                equipment = equipment,
                difficulty = difficulty,
                requiresWeight = equipment != Equipment.BODYWEIGHT,
                recommendedRepRange = when(difficulty) {
                    ExerciseDifficulty.BEGINNER -> "10-15"
                    ExerciseDifficulty.INTERMEDIATE -> "6-10"
                    ExerciseDifficulty.ADVANCED -> "3-8"
                    else -> "8-12"
                },
                isCustom = false
            )
            val variationId = database.exerciseVariationDao().insertExerciseVariation(variation)
            
            // Add muscle mappings
            val primaryMuscles = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES)
            val secondaryMuscles = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CORE)
            
            primaryMuscles.forEach { muscle ->
                database.variationMuscleDao().insertVariationMuscle(
                    VariationMuscle(
                        variationId = variationId,
                        muscle = muscle,
                        isPrimary = true,
                        emphasisModifier = 1.0f
                    )
                )
            }
            
            secondaryMuscles.forEach { muscle ->
                database.variationMuscleDao().insertVariationMuscle(
                    VariationMuscle(
                        variationId = variationId,
                        muscle = muscle,
                        isPrimary = false,
                        emphasisModifier = 0.5f
                    )
                )
            }
            
            // Add basic instructions
            val setupInstruction = when(name) {
                "Barbell Back Squat" -> "Position bar on upper traps. Stand with feet shoulder-width apart, toes slightly out."
                "Barbell Front Squat" -> "Rest bar on front delts. Keep elbows high, upper arms parallel to floor."
                "Goblet Squat" -> "Hold dumbbell vertically at chest level with both hands."
                "Bulgarian Split Squat" -> "Place rear foot on bench. Front foot positioned for vertical shin at bottom."
                "Box Squat" -> "Set box at parallel or slightly below. Sit back fully, pause, then drive up."
                else -> "Set up in starting position with proper foot placement."
            }
            
            database.variationInstructionDao().insertInstruction(
                VariationInstruction(
                    variationId = variationId,
                    instructionType = InstructionType.SETUP,
                    content = setupInstruction,
                    orderIndex = 0,
                    languageCode = "en"
                )
            )
            
            database.variationInstructionDao().insertInstruction(
                VariationInstruction(
                    variationId = variationId,
                    instructionType = InstructionType.EXECUTION,
                    content = "Descend by pushing hips back and bending knees. Keep chest up and maintain neutral spine. Drive through heels to return to start.",
                    orderIndex = 1,
                    languageCode = "en"
                )
            )
            
            database.variationInstructionDao().insertInstruction(
                VariationInstruction(
                    variationId = variationId,
                    instructionType = InstructionType.SAFETY,
                    content = "Knees track over toes. Maintain tight core. Full depth (hip crease below knee) if mobility allows.",
                    orderIndex = 2,
                    languageCode = "en"
                )
            )
        }
    }
    
    private suspend fun seedDeadliftVariations() {
        val deadliftCore = ExerciseCore(
            name = "Deadlift",
            category = ExerciseCategory.BACK,
            movementPattern = MovementPattern.HINGE,
            isCompound = true
        )
        val deadliftCoreId = database.exerciseCoreDao().insertExerciseCore(deadliftCore)
        
        val variations = listOf(
            Triple("Barbell Deadlift", Equipment.BARBELL, ExerciseDifficulty.INTERMEDIATE),
            Triple("Romanian Deadlift", Equipment.BARBELL, ExerciseDifficulty.INTERMEDIATE),
            Triple("Sumo Deadlift", Equipment.BARBELL, ExerciseDifficulty.ADVANCED),
            Triple("Trap Bar Deadlift", Equipment.TRAP_BAR, ExerciseDifficulty.BEGINNER),
            Triple("Dumbbell Romanian Deadlift", Equipment.DUMBBELL, ExerciseDifficulty.BEGINNER),
            Triple("Good Morning", Equipment.BARBELL, ExerciseDifficulty.INTERMEDIATE)
        )
        
        variations.forEach { (name, equipment, difficulty) ->
            val variation = ExerciseVariation(
                coreExerciseId = deadliftCoreId,
                name = name,
                equipment = equipment,
                difficulty = difficulty,
                requiresWeight = true,
                recommendedRepRange = when(name) {
                    "Barbell Deadlift", "Sumo Deadlift" -> "1-5"
                    "Trap Bar Deadlift" -> "3-8"
                    else -> "8-12"
                },
                isCustom = false
            )
            val variationId = database.exerciseVariationDao().insertExerciseVariation(variation)
            
            // Add muscle mappings
            val primaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.LOWER_BACK)
            val secondaryMuscles = listOf(MuscleGroup.UPPER_BACK, MuscleGroup.TRAPS, MuscleGroup.CORE)
            
            primaryMuscles.forEach { muscle ->
                database.variationMuscleDao().insertVariationMuscle(
                    VariationMuscle(variationId, muscle, true, 1.0f)
                )
            }
            
            secondaryMuscles.forEach { muscle ->
                database.variationMuscleDao().insertVariationMuscle(
                    VariationMuscle(variationId, muscle, false, 0.5f)
                )
            }
        }
    }
    
    private suspend fun seedBenchPressVariations() {
        val benchCore = ExerciseCore(
            name = "Bench Press",
            category = ExerciseCategory.CHEST,
            movementPattern = MovementPattern.PUSH,
            isCompound = true
        )
        val benchCoreId = database.exerciseCoreDao().insertExerciseCore(benchCore)
        
        val variations = listOf(
            Triple("Barbell Bench Press", Equipment.BARBELL, ExerciseDifficulty.INTERMEDIATE),
            Triple("Incline Barbell Press", Equipment.BARBELL, ExerciseDifficulty.INTERMEDIATE),
            Triple("Dumbbell Bench Press", Equipment.DUMBBELL, ExerciseDifficulty.BEGINNER),
            Triple("Incline Dumbbell Press", Equipment.DUMBBELL, ExerciseDifficulty.BEGINNER),
            Triple("Close-Grip Bench Press", Equipment.BARBELL, ExerciseDifficulty.INTERMEDIATE),
            Triple("Decline Bench Press", Equipment.BARBELL, ExerciseDifficulty.INTERMEDIATE)
        )
        
        variations.forEach { (name, equipment, difficulty) ->
            val variation = ExerciseVariation(
                coreExerciseId = benchCoreId,
                name = name,
                equipment = equipment,
                difficulty = difficulty,
                requiresWeight = true,
                recommendedRepRange = when(name) {
                    "Barbell Bench Press" -> "3-8"
                    "Close-Grip Bench Press" -> "8-12"
                    else -> "6-12"
                },
                isCustom = false
            )
            val variationId = database.exerciseVariationDao().insertExerciseVariation(variation)
            
            // Add muscle mappings
            val primaryMuscles = when(name) {
                "Close-Grip Bench Press" -> listOf(MuscleGroup.TRICEPS, MuscleGroup.CHEST)
                else -> listOf(MuscleGroup.CHEST)
            }
            val secondaryMuscles = listOf(MuscleGroup.FRONT_DELTS, MuscleGroup.TRICEPS)
            
            primaryMuscles.forEach { muscle ->
                database.variationMuscleDao().insertVariationMuscle(
                    VariationMuscle(variationId, muscle, true, 1.0f)
                )
            }
            
            secondaryMuscles.forEach { muscle ->
                if (!primaryMuscles.contains(muscle)) {
                    database.variationMuscleDao().insertVariationMuscle(
                        VariationMuscle(variationId, muscle, false, 0.5f)
                    )
                }
            }
        }
    }
    
    private suspend fun seedOverheadPressVariations() {
        val ohpCore = ExerciseCore(
            name = "Overhead Press",
            category = ExerciseCategory.SHOULDERS,
            movementPattern = MovementPattern.PUSH,
            isCompound = true
        )
        val ohpCoreId = database.exerciseCoreDao().insertExerciseCore(ohpCore)
        
        val variations = listOf(
            Triple("Barbell Overhead Press", Equipment.BARBELL, ExerciseDifficulty.INTERMEDIATE),
            Triple("Dumbbell Shoulder Press", Equipment.DUMBBELL, ExerciseDifficulty.BEGINNER),
            Triple("Arnold Press", Equipment.DUMBBELL, ExerciseDifficulty.INTERMEDIATE),
            Triple("Push Press", Equipment.BARBELL, ExerciseDifficulty.ADVANCED),
            Triple("Behind Neck Press", Equipment.BARBELL, ExerciseDifficulty.ADVANCED)
        )
        
        variations.forEach { (name, equipment, difficulty) ->
            val variation = ExerciseVariation(
                coreExerciseId = ohpCoreId,
                name = name,
                equipment = equipment,
                difficulty = difficulty,
                requiresWeight = true,
                recommendedRepRange = when(name) {
                    "Push Press" -> "3-6"
                    "Barbell Overhead Press" -> "3-8"
                    else -> "8-12"
                },
                isCustom = false
            )
            val variationId = database.exerciseVariationDao().insertExerciseVariation(variation)
            
            // Add muscle mappings
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.FRONT_DELTS, true, 1.0f)
            )
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.SIDE_DELTS, false, 0.7f)
            )
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.TRICEPS, false, 0.5f)
            )
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.CORE, false, 0.3f)
            )
        }
    }
    
    private suspend fun seedRowVariations() {
        val rowCore = ExerciseCore(
            name = "Row",
            category = ExerciseCategory.BACK,
            movementPattern = MovementPattern.PULL,
            isCompound = true
        )
        val rowCoreId = database.exerciseCoreDao().insertExerciseCore(rowCore)
        
        val variations = listOf(
            Triple("Barbell Row", Equipment.BARBELL, ExerciseDifficulty.INTERMEDIATE),
            Triple("Pendlay Row", Equipment.BARBELL, ExerciseDifficulty.ADVANCED),
            Triple("Dumbbell Row", Equipment.DUMBBELL, ExerciseDifficulty.BEGINNER),
            Triple("Cable Row", Equipment.CABLE, ExerciseDifficulty.BEGINNER),
            Triple("T-Bar Row", Equipment.BARBELL, ExerciseDifficulty.INTERMEDIATE),
            Triple("Chest Supported Row", Equipment.DUMBBELL, ExerciseDifficulty.BEGINNER)
        )
        
        variations.forEach { (name, equipment, difficulty) ->
            val variation = ExerciseVariation(
                coreExerciseId = rowCoreId,
                name = name,
                equipment = equipment,
                difficulty = difficulty,
                requiresWeight = true,
                recommendedRepRange = when(name) {
                    "Pendlay Row" -> "5-8"
                    "Barbell Row" -> "6-10"
                    else -> "8-15"
                },
                isCustom = false
            )
            val variationId = database.exerciseVariationDao().insertExerciseVariation(variation)
            
            // Add muscle mappings
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.LATS, true, 1.0f)
            )
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.RHOMBOIDS, true, 1.0f)
            )
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.MIDDLE_BACK, true, 0.8f)
            )
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.BICEPS, false, 0.5f)
            )
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.REAR_DELTS, false, 0.5f)
            )
        }
    }
    
    private suspend fun seedPullUpVariations() {
        val pullUpCore = ExerciseCore(
            name = "Pull-up",
            category = ExerciseCategory.BACK,
            movementPattern = MovementPattern.PULL,
            isCompound = true
        )
        val pullUpCoreId = database.exerciseCoreDao().insertExerciseCore(pullUpCore)
        
        val variations = listOf(
            Triple("Pull-up", Equipment.PULL_UP_BAR, ExerciseDifficulty.INTERMEDIATE),
            Triple("Chin-up", Equipment.PULL_UP_BAR, ExerciseDifficulty.INTERMEDIATE),
            Triple("Lat Pulldown", Equipment.CABLE, ExerciseDifficulty.BEGINNER),
            Triple("Assisted Pull-up", Equipment.MACHINE, ExerciseDifficulty.BEGINNER),
            Triple("Weighted Pull-up", Equipment.PULL_UP_BAR, ExerciseDifficulty.ADVANCED)
        )
        
        variations.forEach { (name, equipment, difficulty) ->
            val variation = ExerciseVariation(
                coreExerciseId = pullUpCoreId,
                name = name,
                equipment = equipment,
                difficulty = difficulty,
                requiresWeight = name == "Weighted Pull-up",
                recommendedRepRange = when(name) {
                    "Weighted Pull-up" -> "3-8"
                    "Pull-up", "Chin-up" -> "5-12"
                    else -> "8-15"
                },
                isCustom = false
            )
            val variationId = database.exerciseVariationDao().insertExerciseVariation(variation)
            
            // Add muscle mappings
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.LATS, true, 1.0f)
            )
            
            val bicepsEmphasis = if (name == "Chin-up") 1.0f else 0.5f
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.BICEPS, name == "Chin-up", bicepsEmphasis)
            )
            
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.UPPER_BACK, false, 0.7f)
            )
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.REAR_DELTS, false, 0.5f)
            )
        }
    }
    
    private suspend fun seedDipVariations() {
        val dipCore = ExerciseCore(
            name = "Dip",
            category = ExerciseCategory.CHEST,
            movementPattern = MovementPattern.PUSH,
            isCompound = true
        )
        val dipCoreId = database.exerciseCoreDao().insertExerciseCore(dipCore)
        
        val variations = listOf(
            Triple("Parallel Bar Dip", Equipment.BODYWEIGHT, ExerciseDifficulty.INTERMEDIATE),
            Triple("Weighted Dip", Equipment.BODYWEIGHT, ExerciseDifficulty.ADVANCED),
            Triple("Bench Dip", Equipment.BODYWEIGHT, ExerciseDifficulty.BEGINNER),
            Triple("Assisted Dip", Equipment.MACHINE, ExerciseDifficulty.BEGINNER)
        )
        
        variations.forEach { (name, equipment, difficulty) ->
            val variation = ExerciseVariation(
                coreExerciseId = dipCoreId,
                name = name,
                equipment = equipment,
                difficulty = difficulty,
                requiresWeight = name == "Weighted Dip",
                recommendedRepRange = when(name) {
                    "Weighted Dip" -> "4-8"
                    "Bench Dip" -> "10-20"
                    else -> "6-12"
                },
                isCustom = false
            )
            val variationId = database.exerciseVariationDao().insertExerciseVariation(variation)
            
            // Add muscle mappings
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.CHEST, true, 1.0f)
            )
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.TRICEPS, true, 1.0f)
            )
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.FRONT_DELTS, false, 0.5f)
            )
        }
    }
    
    private suspend fun seedCurlVariations() {
        val curlCore = ExerciseCore(
            name = "Curl",
            category = ExerciseCategory.ARMS,
            movementPattern = MovementPattern.PULL,
            isCompound = false
        )
        val curlCoreId = database.exerciseCoreDao().insertExerciseCore(curlCore)
        
        val variations = listOf(
            Triple("Barbell Curl", Equipment.BARBELL, ExerciseDifficulty.BEGINNER),
            Triple("Dumbbell Curl", Equipment.DUMBBELL, ExerciseDifficulty.BEGINNER),
            Triple("Hammer Curl", Equipment.DUMBBELL, ExerciseDifficulty.BEGINNER),
            Triple("Preacher Curl", Equipment.BARBELL, ExerciseDifficulty.INTERMEDIATE),
            Triple("Cable Curl", Equipment.CABLE, ExerciseDifficulty.BEGINNER),
            Triple("Incline Dumbbell Curl", Equipment.DUMBBELL, ExerciseDifficulty.INTERMEDIATE)
        )
        
        variations.forEach { (name, equipment, difficulty) ->
            val variation = ExerciseVariation(
                coreExerciseId = curlCoreId,
                name = name,
                equipment = equipment,
                difficulty = difficulty,
                requiresWeight = true,
                recommendedRepRange = "8-15",
                isCustom = false
            )
            val variationId = database.exerciseVariationDao().insertExerciseVariation(variation)
            
            // Add muscle mappings
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.BICEPS, true, 1.0f)
            )
            
            if (name == "Hammer Curl") {
                database.variationMuscleDao().insertVariationMuscle(
                    VariationMuscle(variationId, MuscleGroup.FOREARMS, false, 0.7f)
                )
            } else {
                database.variationMuscleDao().insertVariationMuscle(
                    VariationMuscle(variationId, MuscleGroup.FOREARMS, false, 0.3f)
                )
            }
        }
    }
    
    private suspend fun seedTricepVariations() {
        val extensionCore = ExerciseCore(
            name = "Tricep Extension",
            category = ExerciseCategory.ARMS,
            movementPattern = MovementPattern.PUSH,
            isCompound = false
        )
        val extensionCoreId = database.exerciseCoreDao().insertExerciseCore(extensionCore)
        
        val variations = listOf(
            Triple("Overhead Tricep Extension", Equipment.DUMBBELL, ExerciseDifficulty.BEGINNER),
            Triple("Cable Tricep Pushdown", Equipment.CABLE, ExerciseDifficulty.BEGINNER),
            Triple("Lying Tricep Extension", Equipment.BARBELL, ExerciseDifficulty.INTERMEDIATE),
            Triple("Diamond Push-up", Equipment.BODYWEIGHT, ExerciseDifficulty.INTERMEDIATE),
            Triple("Cable Overhead Extension", Equipment.CABLE, ExerciseDifficulty.BEGINNER)
        )
        
        variations.forEach { (name, equipment, difficulty) ->
            val variation = ExerciseVariation(
                coreExerciseId = extensionCoreId,
                name = name,
                equipment = equipment,
                difficulty = difficulty,
                requiresWeight = equipment != Equipment.BODYWEIGHT,
                recommendedRepRange = when(name) {
                    "Diamond Push-up" -> "8-15"
                    else -> "10-20"
                },
                isCustom = false
            )
            val variationId = database.exerciseVariationDao().insertExerciseVariation(variation)
            
            // Add muscle mappings
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.TRICEPS, true, 1.0f)
            )
        }
    }
    
    private suspend fun seedShoulderVariations() {
        val raiseCore = ExerciseCore(
            name = "Raise",
            category = ExerciseCategory.SHOULDERS,
            movementPattern = MovementPattern.PUSH,
            isCompound = false
        )
        val raiseCoreId = database.exerciseCoreDao().insertExerciseCore(raiseCore)
        
        val variations = listOf(
            Triple("Dumbbell Lateral Raise", Equipment.DUMBBELL, ExerciseDifficulty.BEGINNER),
            Triple("Cable Lateral Raise", Equipment.CABLE, ExerciseDifficulty.BEGINNER),
            Triple("Front Raise", Equipment.DUMBBELL, ExerciseDifficulty.BEGINNER),
            Triple("Rear Delt Fly", Equipment.DUMBBELL, ExerciseDifficulty.BEGINNER),
            Triple("Face Pull", Equipment.CABLE, ExerciseDifficulty.BEGINNER),
            Triple("Upright Row", Equipment.BARBELL, ExerciseDifficulty.INTERMEDIATE)
        )
        
        variations.forEach { (name, equipment, difficulty) ->
            val variation = ExerciseVariation(
                coreExerciseId = raiseCoreId,
                name = name,
                equipment = equipment,
                difficulty = difficulty,
                requiresWeight = true,
                recommendedRepRange = when(name) {
                    "Upright Row" -> "10-15"
                    else -> "12-20"
                },
                isCustom = false
            )
            val variationId = database.exerciseVariationDao().insertExerciseVariation(variation)
            
            // Add muscle mappings based on the exercise
            when(name) {
                "Dumbbell Lateral Raise", "Cable Lateral Raise" -> {
                    database.variationMuscleDao().insertVariationMuscle(
                        VariationMuscle(variationId, MuscleGroup.SIDE_DELTS, true, 1.0f)
                    )
                }
                "Front Raise" -> {
                    database.variationMuscleDao().insertVariationMuscle(
                        VariationMuscle(variationId, MuscleGroup.FRONT_DELTS, true, 1.0f)
                    )
                }
                "Rear Delt Fly", "Face Pull" -> {
                    database.variationMuscleDao().insertVariationMuscle(
                        VariationMuscle(variationId, MuscleGroup.REAR_DELTS, true, 1.0f)
                    )
                    if (name == "Face Pull") {
                        database.variationMuscleDao().insertVariationMuscle(
                            VariationMuscle(variationId, MuscleGroup.RHOMBOIDS, false, 0.7f)
                        )
                    }
                }
                "Upright Row" -> {
                    database.variationMuscleDao().insertVariationMuscle(
                        VariationMuscle(variationId, MuscleGroup.SIDE_DELTS, true, 1.0f)
                    )
                    database.variationMuscleDao().insertVariationMuscle(
                        VariationMuscle(variationId, MuscleGroup.TRAPS, false, 0.7f)
                    )
                }
            }
        }
    }
    
    private suspend fun seedCoreExercises() {
        // Plank variations
        val plankCore = ExerciseCore(
            name = "Plank",
            category = ExerciseCategory.CORE,
            movementPattern = MovementPattern.PLANK,
            isCompound = false
        )
        val plankCoreId = database.exerciseCoreDao().insertExerciseCore(plankCore)
        
        val plankVariations = listOf(
            Triple("Plank", Equipment.BODYWEIGHT, ExerciseDifficulty.BEGINNER),
            Triple("Side Plank", Equipment.BODYWEIGHT, ExerciseDifficulty.INTERMEDIATE),
            Triple("Weighted Plank", Equipment.BODYWEIGHT, ExerciseDifficulty.ADVANCED)
        )
        
        plankVariations.forEach { (name, equipment, difficulty) ->
            val variation = ExerciseVariation(
                coreExerciseId = plankCoreId,
                name = name,
                equipment = equipment,
                difficulty = difficulty,
                requiresWeight = name == "Weighted Plank",
                recommendedRepRange = when(difficulty) {
                    ExerciseDifficulty.BEGINNER -> "30-60s"
                    ExerciseDifficulty.INTERMEDIATE -> "20-45s"
                    else -> "20-40s"
                },
                isCustom = false
            )
            val variationId = database.exerciseVariationDao().insertExerciseVariation(variation)
            
            // Add muscle mappings
            if (name == "Side Plank") {
                database.variationMuscleDao().insertVariationMuscle(
                    VariationMuscle(variationId, MuscleGroup.OBLIQUES, true, 1.0f)
                )
                database.variationMuscleDao().insertVariationMuscle(
                    VariationMuscle(variationId, MuscleGroup.CORE, false, 0.7f)
                )
            } else {
                database.variationMuscleDao().insertVariationMuscle(
                    VariationMuscle(variationId, MuscleGroup.CORE, true, 1.0f)
                )
                database.variationMuscleDao().insertVariationMuscle(
                    VariationMuscle(variationId, MuscleGroup.OBLIQUES, false, 0.5f)
                )
            }
        }
        
        // Crunch variations
        val crunchCore = ExerciseCore(
            name = "Crunch",
            category = ExerciseCategory.CORE,
            movementPattern = MovementPattern.ROTATE,
            isCompound = false
        )
        val crunchCoreId = database.exerciseCoreDao().insertExerciseCore(crunchCore)
        
        val crunchVariations = listOf(
            Triple("Crunch", Equipment.BODYWEIGHT, ExerciseDifficulty.BEGINNER),
            Triple("Cable Crunch", Equipment.CABLE, ExerciseDifficulty.INTERMEDIATE),
            Triple("Hanging Leg Raise", Equipment.PULL_UP_BAR, ExerciseDifficulty.ADVANCED),
            Triple("Ab Wheel Rollout", Equipment.BODYWEIGHT, ExerciseDifficulty.EXPERT)
        )
        
        crunchVariations.forEach { (name, equipment, difficulty) ->
            val variation = ExerciseVariation(
                coreExerciseId = crunchCoreId,
                name = name,
                equipment = equipment,
                difficulty = difficulty,
                requiresWeight = false,
                recommendedRepRange = when(name) {
                    "Crunch" -> "15-30"
                    "Cable Crunch" -> "12-20"
                    "Hanging Leg Raise" -> "8-15"
                    "Ab Wheel Rollout" -> "8-12"
                    else -> "10-20"
                },
                isCustom = false
            )
            val variationId = database.exerciseVariationDao().insertExerciseVariation(variation)
            
            // Add muscle mappings
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.CORE, true, 1.0f)
            )
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.OBLIQUES, false, 0.5f)
            )
        }
    }
    
    private suspend fun seedLegAccessories() {
        // Lunge variations
        val lungeCore = ExerciseCore(
            name = "Lunge",
            category = ExerciseCategory.LEGS,
            movementPattern = MovementPattern.LUNGE,
            isCompound = true
        )
        val lungeCoreId = database.exerciseCoreDao().insertExerciseCore(lungeCore)
        
        val lungeVariations = listOf(
            Triple("Walking Lunge", Equipment.DUMBBELL, ExerciseDifficulty.INTERMEDIATE),
            Triple("Reverse Lunge", Equipment.DUMBBELL, ExerciseDifficulty.BEGINNER),
            Triple("Step Up", Equipment.DUMBBELL, ExerciseDifficulty.BEGINNER)
        )
        
        lungeVariations.forEach { (name, equipment, difficulty) ->
            val variation = ExerciseVariation(
                coreExerciseId = lungeCoreId,
                name = name,
                equipment = equipment,
                difficulty = difficulty,
                requiresWeight = true,
                recommendedRepRange = "10-15",
                isCustom = false
            )
            val variationId = database.exerciseVariationDao().insertExerciseVariation(variation)
            
            // Add muscle mappings
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.QUADS, true, 1.0f)
            )
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.GLUTES, true, 1.0f)
            )
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.HAMSTRINGS, false, 0.5f)
            )
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.CORE, false, 0.3f)
            )
        }
        
        // Calf Raise
        val calfRaiseCore = ExerciseCore(
            name = "Calf Raise",
            category = ExerciseCategory.LEGS,
            movementPattern = MovementPattern.PUSH,
            isCompound = false
        )
        val calfRaiseCoreId = database.exerciseCoreDao().insertExerciseCore(calfRaiseCore)
        
        val calfVariations = listOf(
            Pair("Standing Calf Raise", Equipment.MACHINE),
            Pair("Seated Calf Raise", Equipment.MACHINE)
        )
        
        calfVariations.forEach { (name, equipment) ->
            val variation = ExerciseVariation(
                coreExerciseId = calfRaiseCoreId,
                name = name,
                equipment = equipment,
                difficulty = ExerciseDifficulty.BEGINNER,
                requiresWeight = true,
                recommendedRepRange = if (name == "Standing Calf Raise") "12-20" else "15-25",
                isCustom = false
            )
            val variationId = database.exerciseVariationDao().insertExerciseVariation(variation)
            
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.CALVES, true, 1.0f)
            )
        }
        
        // Leg Curl
        val legCurlCore = ExerciseCore(
            name = "Leg Curl",
            category = ExerciseCategory.LEGS,
            movementPattern = MovementPattern.PULL,
            isCompound = false
        )
        val legCurlCoreId = database.exerciseCoreDao().insertExerciseCore(legCurlCore)
        
        val legCurlVariations = listOf(
            Triple("Lying Leg Curl", Equipment.MACHINE, ExerciseDifficulty.BEGINNER),
            Triple("Seated Leg Curl", Equipment.MACHINE, ExerciseDifficulty.BEGINNER),
            Triple("Nordic Curl", Equipment.BODYWEIGHT, ExerciseDifficulty.EXPERT)
        )
        
        legCurlVariations.forEach { (name, equipment, difficulty) ->
            val variation = ExerciseVariation(
                coreExerciseId = legCurlCoreId,
                name = name,
                equipment = equipment,
                difficulty = difficulty,
                requiresWeight = equipment == Equipment.MACHINE,
                recommendedRepRange = if (name == "Nordic Curl") "5-10" else "10-15",
                isCustom = false
            )
            val variationId = database.exerciseVariationDao().insertExerciseVariation(variation)
            
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.HAMSTRINGS, true, 1.0f)
            )
            database.variationMuscleDao().insertVariationMuscle(
                VariationMuscle(variationId, MuscleGroup.CALVES, false, 0.3f)
            )
        }
        
        // Leg Extension
        val legExtensionCore = ExerciseCore(
            name = "Leg Extension",
            category = ExerciseCategory.LEGS,
            movementPattern = MovementPattern.PUSH,
            isCompound = false
        )
        val legExtensionCoreId = database.exerciseCoreDao().insertExerciseCore(legExtensionCore)
        
        val legExtensionVariation = ExerciseVariation(
            coreExerciseId = legExtensionCoreId,
            name = "Leg Extension",
            equipment = Equipment.MACHINE,
            difficulty = ExerciseDifficulty.BEGINNER,
            requiresWeight = true,
            recommendedRepRange = "12-20",
            isCustom = false
        )
        val variationId = database.exerciseVariationDao().insertExerciseVariation(legExtensionVariation)
        
        database.variationMuscleDao().insertVariationMuscle(
            VariationMuscle(variationId, MuscleGroup.QUADS, true, 1.0f)
        )
    }
}