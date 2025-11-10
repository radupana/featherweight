package com.github.radupana.featherweight.util

import com.github.radupana.featherweight.data.programme.ProgrammeDifficulty
import com.github.radupana.featherweight.data.programme.ProgrammeType

object ProgrammeTypeMapper {
    fun mapProgrammeType(typeString: String): ProgrammeType {
        val normalized = typeString.trim().uppercase()
        return when {
            normalized == "STRENGTH" -> ProgrammeType.STRENGTH
            normalized == "POWERLIFTING" -> ProgrammeType.POWERLIFTING
            normalized == "BODYBUILDING" -> ProgrammeType.BODYBUILDING
            normalized == "HYPERTROPHY" -> ProgrammeType.BODYBUILDING
            normalized == "GENERAL_FITNESS" || normalized == "GENERAL" || normalized == "FITNESS" -> ProgrammeType.GENERAL_FITNESS
            normalized == "OLYMPIC_LIFTING" || normalized == "OLYMPIC" || normalized == "WEIGHTLIFTING" -> ProgrammeType.OLYMPIC_LIFTING
            normalized == "HYBRID" || normalized == "POWERBUILDING" -> ProgrammeType.HYBRID
            else -> {
                CloudLogger.warn("ProgrammeTypeMapper", "Unknown programme type: $typeString, defaulting to GENERAL_FITNESS")
                ProgrammeType.GENERAL_FITNESS
            }
        }
    }

    fun mapProgrammeDifficulty(difficultyString: String): ProgrammeDifficulty {
        val normalized = difficultyString.trim().uppercase()
        return when {
            normalized == "BEGINNER" -> ProgrammeDifficulty.BEGINNER
            normalized == "NOVICE" -> ProgrammeDifficulty.NOVICE
            normalized == "INTERMEDIATE" -> ProgrammeDifficulty.INTERMEDIATE
            normalized == "ADVANCED" -> ProgrammeDifficulty.ADVANCED
            normalized == "EXPERT" -> ProgrammeDifficulty.EXPERT
            else -> {
                CloudLogger.warn("ProgrammeTypeMapper", "Unknown difficulty: $difficultyString, defaulting to INTERMEDIATE")
                ProgrammeDifficulty.INTERMEDIATE
            }
        }
    }
}
