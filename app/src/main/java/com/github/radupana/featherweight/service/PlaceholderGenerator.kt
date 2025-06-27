package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.ProgrammeGoal
import com.github.radupana.featherweight.data.SessionDuration

class PlaceholderGenerator {
    
    fun generatePlaceholder(
        goal: ProgrammeGoal?,
        frequency: Int?,
        duration: SessionDuration?
    ): String {
        return when {
            goal == null && frequency == null && duration == null -> getDefaultPlaceholder()
            goal != null && frequency != null && duration != null -> getFullPlaceholder(goal, frequency, duration)
            goal != null && frequency != null -> getGoalAndFrequencyPlaceholder(goal, frequency)
            goal != null -> getGoalOnlyPlaceholder(goal)
            frequency != null -> getFrequencyOnlyPlaceholder(frequency)
            else -> getPartialPlaceholder(goal, frequency, duration)
        }
    }
    
    private fun getDefaultPlaceholder(): String {
        return "Describe your ideal training programme. Include your experience level, goals, training frequency, and any preferences or limitations you have..."
    }
    
    private fun getGoalOnlyPlaceholder(goal: ProgrammeGoal): String {
        return when (goal) {
            ProgrammeGoal.BUILD_STRENGTH -> "I want to build strength. Tell us about your current lifts, training experience, how many days you can train per week, and any specific movements you want to focus on..."
            
            ProgrammeGoal.BUILD_MUSCLE -> "I want to build muscle and size. Share your training experience, current physique goals, how often you can train, and any body parts you want to emphasize..."
            
            ProgrammeGoal.LOSE_FAT -> "I want to lose fat while maintaining muscle. Describe your current fitness level, how many days you can train, preferred training style, and any equipment limitations..."
            
            ProgrammeGoal.ATHLETIC_PERFORMANCE -> "I want to improve athletic performance. Tell us about your sport, current fitness level, training frequency, and specific performance goals (speed, power, endurance)..."
            
            ProgrammeGoal.CUSTOM -> "Describe your custom training goals. Include your experience level, specific objectives, training frequency, and any unique requirements or preferences..."
        }
    }
    
    private fun getFrequencyOnlyPlaceholder(frequency: Int): String {
        return "Training $frequency ${if (frequency == 1) "day" else "days"} per week. Tell us about your goals, experience level, session duration preferences, and any specific movements or training styles you prefer..."
    }
    
    private fun getGoalAndFrequencyPlaceholder(goal: ProgrammeGoal, frequency: Int): String {
        val goalText = when (goal) {
            ProgrammeGoal.BUILD_STRENGTH -> "build strength"
            ProgrammeGoal.BUILD_MUSCLE -> "build muscle"
            ProgrammeGoal.LOSE_FAT -> "lose fat"
            ProgrammeGoal.ATHLETIC_PERFORMANCE -> "improve athletic performance"
            ProgrammeGoal.CUSTOM -> "achieve your custom goals"
        }
        
        val daysText = "$frequency ${if (frequency == 1) "day" else "days"} per week"
        
        return when (goal) {
            ProgrammeGoal.BUILD_STRENGTH -> "I want to $goalText training $daysText. Share your current maxes (squat, bench, deadlift), training experience, session duration, and any specific strength goals..."
            
            ProgrammeGoal.BUILD_MUSCLE -> "I want to $goalText training $daysText. Tell us about your training experience, current physique, preferred session length, and any muscle groups you want to prioritize..."
            
            ProgrammeGoal.LOSE_FAT -> "I want to $goalText training $daysText. Describe your current fitness level, preferred session duration, training style (circuits, traditional sets), and equipment access..."
            
            ProgrammeGoal.ATHLETIC_PERFORMANCE -> "I want to $goalText training $daysText. Share your sport, current performance level, session duration, and specific athletic qualities to develop..."
            
            ProgrammeGoal.CUSTOM -> "Training $daysText to $goalText. Provide details about your experience, specific objectives, preferred session length, and any unique requirements..."
        }
    }
    
    private fun getFullPlaceholder(goal: ProgrammeGoal, frequency: Int, duration: SessionDuration): String {
        val goalText = when (goal) {
            ProgrammeGoal.BUILD_STRENGTH -> "build strength"
            ProgrammeGoal.BUILD_MUSCLE -> "build muscle"
            ProgrammeGoal.LOSE_FAT -> "lose fat"
            ProgrammeGoal.ATHLETIC_PERFORMANCE -> "improve athletic performance"
            ProgrammeGoal.CUSTOM -> "achieve your goals"
        }
        
        val daysText = "$frequency ${if (frequency == 1) "day" else "days"} per week"
        val durationText = duration.minutesRange
        
        return when (goal) {
            ProgrammeGoal.BUILD_STRENGTH -> "I want to $goalText training $daysText for $durationText per session. Add your current lifts (squat, bench, deadlift), training experience (beginner/intermediate/advanced), equipment access, and any injuries or movement limitations..."
            
            ProgrammeGoal.BUILD_MUSCLE -> "I want to $goalText training $daysText for $durationText per session. Include your training background, current physique stats, preferred training style, equipment available, and any muscle groups you want to emphasize..."
            
            ProgrammeGoal.LOSE_FAT -> "I want to $goalText training $daysText for $durationText per session. Share your current fitness level, preferred training intensity, equipment access, and any dietary or schedule constraints..."
            
            ProgrammeGoal.ATHLETIC_PERFORMANCE -> "I want to $goalText training $daysText for $durationText per session. Describe your sport, current performance level, specific athletic goals, equipment access, and training history..."
            
            ProgrammeGoal.CUSTOM -> "Training $daysText for $durationText per session to $goalText. Provide comprehensive details about your experience, specific objectives, equipment, constraints, and preferences..."
        }
    }
    
    private fun getPartialPlaceholder(goal: ProgrammeGoal?, frequency: Int?, duration: SessionDuration?): String {
        val parts = mutableListOf<String>()
        
        goal?.let { 
            when (it) {
                ProgrammeGoal.BUILD_STRENGTH -> parts.add("focusing on strength")
                ProgrammeGoal.BUILD_MUSCLE -> parts.add("focusing on muscle building")
                ProgrammeGoal.LOSE_FAT -> parts.add("focusing on fat loss")
                ProgrammeGoal.ATHLETIC_PERFORMANCE -> parts.add("focusing on athletic performance")
                ProgrammeGoal.CUSTOM -> parts.add("with custom goals")
            }
        }
        
        frequency?.let { 
            parts.add("training $it ${if (it == 1) "day" else "days"} per week")
        }
        
        duration?.let {
            parts.add("for ${it.minutesRange} per session")
        }
        
        val context = if (parts.isNotEmpty()) {
            "Training " + parts.joinToString(", ") + ". "
        } else {
            ""
        }
        
        return "${context}Tell us about your experience level, current fitness stats, equipment access, and any specific preferences or limitations..."
    }
}