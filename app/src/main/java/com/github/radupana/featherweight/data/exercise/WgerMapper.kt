package com.github.radupana.featherweight.data.exercise

object WgerMapper {
    
    fun mapWgerCategoryToExerciseCategory(wgerCategoryId: Int, wgerCategoryName: String): ExerciseCategory {
        return when (wgerCategoryId) {
            8 -> ExerciseCategory.SHOULDERS  // Shoulders
            9 -> ExerciseCategory.ARMS       // Arms
            10 -> ExerciseCategory.LEGS      // Legs
            11 -> ExerciseCategory.CORE      // Abs
            12 -> ExerciseCategory.BACK      // Back
            13 -> ExerciseCategory.CHEST     // Chest
            14 -> ExerciseCategory.CARDIO    // Calves (closest match)
            15 -> ExerciseCategory.CARDIO    // Cardio
            else -> {
                when {
                    wgerCategoryName.contains("shoulder", ignoreCase = true) -> ExerciseCategory.SHOULDERS
                    wgerCategoryName.contains("arm", ignoreCase = true) || wgerCategoryName.contains("bicep", ignoreCase = true) || wgerCategoryName.contains("tricep", ignoreCase = true) -> ExerciseCategory.ARMS
                    wgerCategoryName.contains("leg", ignoreCase = true) || wgerCategoryName.contains("quad", ignoreCase = true) || wgerCategoryName.contains("hamstring", ignoreCase = true) -> ExerciseCategory.LEGS
                    wgerCategoryName.contains("ab", ignoreCase = true) || wgerCategoryName.contains("core", ignoreCase = true) -> ExerciseCategory.CORE
                    wgerCategoryName.contains("back", ignoreCase = true) || wgerCategoryName.contains("lat", ignoreCase = true) -> ExerciseCategory.BACK
                    wgerCategoryName.contains("chest", ignoreCase = true) || wgerCategoryName.contains("pec", ignoreCase = true) -> ExerciseCategory.CHEST
                    wgerCategoryName.contains("cardio", ignoreCase = true) -> ExerciseCategory.CARDIO
                    else -> ExerciseCategory.FULL_BODY
                }
            }
        }
    }
    
    fun mapWgerMuscleToMuscleGroup(wgerMuscleId: Int, wgerMuscleName: String): MuscleGroup? {
        return when (wgerMuscleId) {
            1 -> MuscleGroup.BICEPS          // Biceps brachii
            2 -> MuscleGroup.FRONT_DELTS     // Anterior deltoid
            3 -> MuscleGroup.FRONT_DELTS     // Serratus anterior (closest match)
            4 -> MuscleGroup.CHEST           // Pectoralis major
            5 -> MuscleGroup.TRICEPS         // Triceps brachii
            6 -> MuscleGroup.SIDE_DELTS      // Lateral deltoid
            7 -> MuscleGroup.REAR_DELTS      // Posterior deltoid
            8 -> MuscleGroup.SIDE_DELTS      // Deltoid (general)
            9 -> MuscleGroup.UPPER_BACK      // Rhomboid major
            10 -> MuscleGroup.TRAPS          // Trapezius
            11 -> MuscleGroup.UPPER_BACK     // Serratus anterior
            12 -> MuscleGroup.UPPER_BACK     // Levator scapulae
            13 -> MuscleGroup.CHEST          // Pectoralis minor
            14 -> MuscleGroup.LATS           // Latissimus dorsi
            15 -> MuscleGroup.LATS           // Teres major
            16 -> MuscleGroup.ABS            // Rectus abdominis
            17 -> MuscleGroup.OBLIQUES       // Obliquus externus abdominis
            18 -> MuscleGroup.OBLIQUES       // Obliquus internus abdominis
            19 -> MuscleGroup.ABS            // Transversus abdominis
            20 -> MuscleGroup.GLUTES         // Gluteus maximus
            21 -> MuscleGroup.GLUTES         // Gluteus medius
            22 -> MuscleGroup.GLUTES         // Gluteus minimus
            23 -> MuscleGroup.QUADS          // Quadriceps femoris
            24 -> MuscleGroup.HAMSTRINGS     // Biceps femoris
            25 -> MuscleGroup.HAMSTRINGS     // Semitendinosus
            26 -> MuscleGroup.HAMSTRINGS     // Semimembranosus
            27 -> MuscleGroup.CALVES         // Gastrocnemius
            28 -> MuscleGroup.CALVES         // Soleus
            29 -> MuscleGroup.FOREARMS       // Brachioradialis
            30 -> MuscleGroup.FOREARMS       // Flexor carpi radialis
            31 -> MuscleGroup.FOREARMS       // Flexor carpi ulnaris
            32 -> MuscleGroup.FOREARMS       // Extensor carpi radialis longus
            33 -> MuscleGroup.FOREARMS       // Extensor carpi radialis brevis
            34 -> MuscleGroup.FOREARMS       // Extensor carpi ulnaris
            35 -> MuscleGroup.FOREARMS       // Palmaris longus
            else -> {
                when {
                    wgerMuscleName.contains("bicep", ignoreCase = true) -> MuscleGroup.BICEPS
                    wgerMuscleName.contains("tricep", ignoreCase = true) -> MuscleGroup.TRICEPS
                    wgerMuscleName.contains("deltoid", ignoreCase = true) && wgerMuscleName.contains("anterior", ignoreCase = true) -> MuscleGroup.FRONT_DELTS
                    wgerMuscleName.contains("deltoid", ignoreCase = true) && wgerMuscleName.contains("lateral", ignoreCase = true) -> MuscleGroup.SIDE_DELTS
                    wgerMuscleName.contains("deltoid", ignoreCase = true) && wgerMuscleName.contains("posterior", ignoreCase = true) -> MuscleGroup.REAR_DELTS
                    wgerMuscleName.contains("deltoid", ignoreCase = true) -> MuscleGroup.SIDE_DELTS
                    wgerMuscleName.contains("pectoralis", ignoreCase = true) || wgerMuscleName.contains("chest", ignoreCase = true) -> MuscleGroup.CHEST
                    wgerMuscleName.contains("latissimus", ignoreCase = true) || wgerMuscleName.contains("lat", ignoreCase = true) -> MuscleGroup.LATS
                    wgerMuscleName.contains("trapezius", ignoreCase = true) || wgerMuscleName.contains("trap", ignoreCase = true) -> MuscleGroup.TRAPS
                    wgerMuscleName.contains("rhomboid", ignoreCase = true) -> MuscleGroup.RHOMBOIDS
                    wgerMuscleName.contains("rectus abdominis", ignoreCase = true) || wgerMuscleName.contains("abs", ignoreCase = true) -> MuscleGroup.ABS
                    wgerMuscleName.contains("oblique", ignoreCase = true) -> MuscleGroup.OBLIQUES
                    wgerMuscleName.contains("quadriceps", ignoreCase = true) || wgerMuscleName.contains("quad", ignoreCase = true) -> MuscleGroup.QUADS
                    wgerMuscleName.contains("hamstring", ignoreCase = true) || wgerMuscleName.contains("biceps femoris", ignoreCase = true) -> MuscleGroup.HAMSTRINGS
                    wgerMuscleName.contains("gluteus", ignoreCase = true) || wgerMuscleName.contains("glute", ignoreCase = true) -> MuscleGroup.GLUTES
                    wgerMuscleName.contains("gastrocnemius", ignoreCase = true) || wgerMuscleName.contains("soleus", ignoreCase = true) || wgerMuscleName.contains("calf", ignoreCase = true) -> MuscleGroup.CALVES
                    wgerMuscleName.contains("forearm", ignoreCase = true) || wgerMuscleName.contains("brachioradialis", ignoreCase = true) -> MuscleGroup.FOREARMS
                    else -> null
                }
            }
        }
    }
    
    fun mapWgerEquipmentToEquipment(wgerEquipmentId: Int, wgerEquipmentName: String): Equipment? {
        return when {
            wgerEquipmentName.contains("barbell", ignoreCase = true) -> Equipment.BARBELL
            wgerEquipmentName.contains("dumbbell", ignoreCase = true) -> Equipment.DUMBBELL
            wgerEquipmentName.contains("kettlebell", ignoreCase = true) -> Equipment.KETTLEBELL
            wgerEquipmentName.contains("cable", ignoreCase = true) -> Equipment.CABLE_MACHINE
            wgerEquipmentName.contains("machine", ignoreCase = true) -> Equipment.MACHINE
            wgerEquipmentName.contains("pull", ignoreCase = true) && wgerEquipmentName.contains("up", ignoreCase = true) -> Equipment.PULL_UP_BAR
            wgerEquipmentName.contains("bodyweight", ignoreCase = true) || wgerEquipmentName.contains("body weight", ignoreCase = true) -> Equipment.BODYWEIGHT
            wgerEquipmentName.contains("none", ignoreCase = true) || wgerEquipmentName.isEmpty() -> Equipment.BODYWEIGHT
            wgerEquipmentName.contains("bench", ignoreCase = true) -> Equipment.MACHINE
            wgerEquipmentName.contains("smith", ignoreCase = true) -> Equipment.SMITH_MACHINE
            wgerEquipmentName.contains("leg press", ignoreCase = true) -> Equipment.LEG_PRESS
            wgerEquipmentName.contains("resistance", ignoreCase = true) && wgerEquipmentName.contains("band", ignoreCase = true) -> Equipment.RESISTANCE_BAND
            wgerEquipmentName.contains("medicine", ignoreCase = true) && wgerEquipmentName.contains("ball", ignoreCase = true) -> Equipment.MEDICINE_BALL
            else -> null
        }
    }
    
    fun generateAliasesFromWgerName(wgerName: String): List<String> {
        val aliases = mutableListOf<String>()
        
        // Add the original name
        aliases.add(wgerName)
        
        // Add variations with/without articles
        val nameWithoutArticles = wgerName
            .replace("\\b(the|a|an)\\b".toRegex(RegexOption.IGNORE_CASE), "")
            .trim()
            .replace("\\s+".toRegex(), " ")
        
        if (nameWithoutArticles != wgerName) {
            aliases.add(nameWithoutArticles)
        }
        
        // Add variations with equipment in different positions
        val equipmentVariations = listOf(
            "barbell" to "BB",
            "dumbbell" to "DB", 
            "kettlebell" to "KB",
            "cable" to "",
            "machine" to ""
        )
        
        equipmentVariations.forEach { (full, short) ->
            if (wgerName.contains(full, ignoreCase = true)) {
                if (short.isNotEmpty()) {
                    aliases.add(wgerName.replace(full, short, ignoreCase = true))
                }
                // Version without equipment
                val withoutEquipment = wgerName.replace(full, "", ignoreCase = true)
                    .trim()
                    .replace("\\s+".toRegex(), " ")
                if (withoutEquipment.isNotBlank()) {
                    aliases.add(withoutEquipment)
                }
            }
        }
        
        // Add common exercise name variations
        val nameVariations = mapOf(
            "press" to "presses",
            "curl" to "curls", 
            "raise" to "raises",
            "row" to "rows",
            "extension" to "extensions",
            "pull" to "pulls",
            "push" to "pushes"
        )
        
        nameVariations.forEach { (singular, plural) ->
            if (wgerName.contains(singular, ignoreCase = true) && !wgerName.contains(plural, ignoreCase = true)) {
                aliases.add(wgerName.replace(singular, plural, ignoreCase = true))
            }
            if (wgerName.contains(plural, ignoreCase = true) && !wgerName.contains(singular, ignoreCase = true)) {
                aliases.add(wgerName.replace(plural, singular, ignoreCase = true))
            }
        }
        
        return aliases.distinct()
    }
}