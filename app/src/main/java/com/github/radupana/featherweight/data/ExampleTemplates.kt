package com.github.radupana.featherweight.data

object ExampleTemplates {
    
    val templates = listOf(
        ExampleTemplate(
            title = "Beginner Strength Foundation",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 3,
            duration = SessionDuration.STANDARD,
            exampleText = "I'm new to lifting and want to build a strength foundation. I can train 3 days per week for about 45-60 minutes. I have access to a full gym with barbells, dumbbells, and machines. I want to focus on the main compound movements like squat, bench press, and deadlift. No current injuries or limitations.",
            tags = listOf("Beginner", "Strength", "Linear Progression", "Compound Movements")
        ),
        
        ExampleTemplate(
            title = "Intermediate Muscle Building",
            goal = ProgrammeGoal.BUILD_MUSCLE,
            frequency = 4,
            duration = SessionDuration.EXTENDED,
            exampleText = "I've been training consistently for 2 years and want to focus on building muscle mass. I can train 4 days per week for 60-90 minutes per session. My current lifts are around: Squat 225 lbs, Bench 185 lbs, Deadlift 275 lbs. I have access to a commercial gym and prefer upper/lower or push/pull/legs splits. I want to emphasize chest and back development.",
            tags = listOf("Intermediate", "Hypertrophy", "Upper/Lower", "PPL")
        ),
        
        ExampleTemplate(
            title = "Advanced Powerlifting Prep",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 5,
            duration = SessionDuration.LONG,
            exampleText = "I'm preparing for a powerlifting meet in 12 weeks. I've been powerlifting for 4 years with current maxes: Squat 405 lbs, Bench 315 lbs, Deadlift 495 lbs. I can train 5 days per week for 90+ minutes. I need a peaking programme that will help me maximize my competition lifts while managing fatigue. I have a fully equipped powerlifting gym.",
            tags = listOf("Advanced", "Competition Prep", "Peaking", "Powerlifting")
        ),
        
        ExampleTemplate(
            title = "Home Gym Fat Loss",
            goal = ProgrammeGoal.LOSE_FAT,
            frequency = 4,
            duration = SessionDuration.STANDARD,
            exampleText = "I want to lose fat while maintaining muscle. I train at home with dumbbells, a pull-up bar, and resistance bands. I can work out 4 times per week for 45-60 minutes. I'm intermediate level with about 18 months of training experience. I prefer circuit-style training and supersets to keep my heart rate up.",
            tags = listOf("Fat Loss", "Home Gym", "Circuit Training", "Dumbbells")
        ),
        
        ExampleTemplate(
            title = "Athletic Performance",
            goal = ProgrammeGoal.ATHLETIC_PERFORMANCE,
            frequency = 4,
            duration = SessionDuration.EXTENDED,
            exampleText = "I play basketball competitively and want to improve my vertical jump, speed, and overall athleticism. I can train 4 days per week for 60-90 minutes, focusing on explosive movements and functional strength. I have access to a full gym plus a track for sprint work. I want to emphasize lower body power and core stability.",
            tags = listOf("Athletic", "Explosive", "Basketball", "Plyometrics")
        ),
        
        ExampleTemplate(
            title = "Busy Professional Routine",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 3,
            duration = SessionDuration.QUICK,
            exampleText = "I'm a busy professional who can only train 3 days per week for 30-45 minutes maximum. I want an efficient strength programme that hits all the major movements. I have access to a corporate gym with basic equipment. I prefer compound movements and minimal accessory work to maximize time efficiency.",
            tags = listOf("Time Efficient", "Busy Schedule", "Corporate Gym", "Compound Focus")
        ),
        
        ExampleTemplate(
            title = "Post-Injury Comeback",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 3,
            duration = SessionDuration.STANDARD,
            exampleText = "I'm coming back from a lower back injury and cleared by my physical therapist to return to lifting. I want to rebuild my strength safely with proper progression. I need to avoid heavy deadlifts initially but can do other movements. My goal is to get back to my previous strength levels over 6 months. I have 3 years of lifting experience before the injury.",
            tags = listOf("Injury Recovery", "Conservative Progression", "Modified Movements")
        ),
        
        ExampleTemplate(
            title = "Senior Fitness Focus",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 3,
            duration = SessionDuration.STANDARD,
            exampleText = "I'm 55 years old and want to maintain and build functional strength for daily activities and long-term health. I can train 3 days per week for about 45-60 minutes. I prefer moderate weights with good form over heavy lifting. I want to focus on balance, mobility, and preventing muscle loss. I have some minor joint stiffness but no major limitations.",
            tags = listOf("Senior", "Functional", "Joint Health", "Longevity")
        )
    )
    
    fun getTemplatesForGoal(goal: ProgrammeGoal): List<ExampleTemplate> {
        return templates.filter { it.goal == goal }
    }
    
    fun getTemplatesForFrequency(frequency: Int): List<ExampleTemplate> {
        return templates.filter { it.frequency == frequency }
    }
    
    fun getFilteredTemplates(
        goal: ProgrammeGoal? = null,
        frequency: Int? = null,
        duration: SessionDuration? = null
    ): List<ExampleTemplate> {
        return templates.filter { template ->
            (goal == null || template.goal == goal) &&
            (frequency == null || template.frequency == frequency) &&
            (duration == null || template.duration == duration)
        }
    }
}