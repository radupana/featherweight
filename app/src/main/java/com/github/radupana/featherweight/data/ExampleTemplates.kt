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
        ),

        // Additional comprehensive templates
        ExampleTemplate(
            title = "Minimal Equipment Maximizer",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 2,
            duration = SessionDuration.STANDARD,
            exampleText = "I can only train twice per week due to work and family commitments. I have a basic home setup with a barbell, plates, and a rack. I want maximum strength gains with minimal time investment. Focus on the big three lifts with intelligent programming. I'm intermediate level and need an efficient full-body approach.",
            tags = listOf("Minimalist", "Full Body", "Time Constrained", "High Frequency")
        ),

        ExampleTemplate(
            title = "Women's Strength & Curves",
            goal = ProgrammeGoal.BUILD_MUSCLE,
            frequency = 4,
            duration = SessionDuration.EXTENDED,
            exampleText = "I want to build lean muscle, especially in my glutes and legs, while also building upper body strength. I can train 4 days per week for 60-90 minutes. I'm not afraid of heavy weights and want to deadlift and squat. I prefer programmes with good glute activation and want to see definition in my arms and shoulders too.",
            tags = listOf("Women", "Glutes", "Curves", "Strong Not Skinny")
        ),

        ExampleTemplate(
            title = "Powerbuilding Hybrid",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 5,
            duration = SessionDuration.EXTENDED,
            exampleText = "I want the best of both worlds - strength and size. I compete in powerlifting but also want to look good. 5 days per week training with heavy compounds followed by hypertrophy work. Current maxes: Squat 365 lbs, Bench 275 lbs, Deadlift 435 lbs. I want to add muscle while still progressing my big three lifts.",
            tags = listOf("Powerbuilding", "Strength + Size", "Competition", "Hybrid")
        ),

        ExampleTemplate(
            title = "Strongman Foundation",
            goal = ProgrammeGoal.ATHLETIC_PERFORMANCE,
            frequency = 4,
            duration = SessionDuration.LONG,
            exampleText = "I want to get into strongman training. I have access to strongman implements like atlas stones, yoke, farmers walks, and log press. 4 days per week for 90+ minutes. I'm experienced with powerlifting but new to strongman events. I want to build work capacity, grip strength, and event-specific skills while maintaining my base strength.",
            tags = listOf("Strongman", "Implements", "Work Capacity", "Functional")
        ),

        ExampleTemplate(
            title = "Bench Press Specialist",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 4,
            duration = SessionDuration.STANDARD,
            exampleText = "I want to dramatically improve my bench press. Currently benching 185 lbs and want to hit 225 lbs in 12 weeks. I can train 4 days per week focusing heavily on bench and supporting movements. I want to address my weak points: off the chest strength and lockout power. Include close grip bench, paused bench, and tricep work.",
            tags = listOf("Bench Specialist", "Weak Point", "Accessory Heavy", "Frequency")
        ),

        ExampleTemplate(
            title = "Squat Depth Mastery",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 3,
            duration = SessionDuration.EXTENDED,
            exampleText = "My squat is lagging behind my other lifts. Currently squatting 275 lbs but struggling with depth and hip mobility. I want a programme that improves my squat technique, depth, and strength. Include mobility work, pause squats, and different squat variations. 3 days per week with extra time for mobility and technique work.",
            tags = listOf("Squat Focus", "Mobility", "Technique", "Depth")
        ),

        ExampleTemplate(
            title = "Bodybuilding Contest Prep",
            goal = ProgrammeGoal.LOSE_FAT,
            frequency = 6,
            duration = SessionDuration.EXTENDED,
            exampleText = "I'm 16 weeks out from my first bodybuilding competition. I need to lean down while maintaining as much muscle as possible. 6 days per week training with high volume and moderate intensity. Include both compound and isolation movements. I also do cardio separately but need the lifting programme to support fat loss while preserving muscle.",
            tags = listOf("Contest Prep", "High Volume", "Muscle Preservation", "6-Day Split")
        ),

        ExampleTemplate(
            title = "Calisthenics Strength Builder",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 4,
            duration = SessionDuration.STANDARD,
            exampleText = "I want to master bodyweight movements and build impressive relative strength. Goals include muscle-ups, handstand push-ups, pistol squats, and eventually one-arm pull-ups. I train at a park with pull-up bars and parallel bars. 4 days per week focusing on skill work and strength progressions. Mix of static holds and dynamic movements.",
            tags = listOf("Calisthenics", "Bodyweight", "Skill Work", "Relative Strength")
        ),

        ExampleTemplate(
            title = "Kettlebell Warrior",
            goal = ProgrammeGoal.ATHLETIC_PERFORMANCE,
            frequency = 3,
            duration = SessionDuration.STANDARD,
            exampleText = "I want to build strength and conditioning using primarily kettlebells. I have various weights from 16kg to 48kg. Focus on swings, Turkish get-ups, clean and press, and snatches. 3 days per week emphasizing power, endurance, and functional movement patterns. I want to be strong and have great cardio too.",
            tags = listOf("Kettlebells", "Conditioning", "Power", "Functional")
        ),

        ExampleTemplate(
            title = "CrossFit Competitor",
            goal = ProgrammeGoal.ATHLETIC_PERFORMANCE,
            frequency = 5,
            duration = SessionDuration.LONG,
            exampleText = "I compete in CrossFit and need to improve my strength base for the competition season. 5 days per week focusing on Olympic lifts, powerlifting movements, and gymnastics strength. My weaknesses are overhead pressing and muscle-ups. I need a programme that builds strength while maintaining the conditioning and skills I get from regular CrossFit classes.",
            tags = listOf("CrossFit", "Olympic Lifts", "Gymnastics", "Competition")
        ),

        ExampleTemplate(
            title = "Off-Season Mass Builder",
            goal = ProgrammeGoal.BUILD_MUSCLE,
            frequency = 5,
            duration = SessionDuration.EXTENDED,
            exampleText = "It's my off-season and I want to pack on serious muscle mass. I'm not worried about being super lean right now - just want to get bigger and stronger. 5 days per week with high volume, compound movements, and plenty of accessory work. Focus on arms, shoulders, and back. I can eat in a surplus and recover well.",
            tags = listOf("Mass Building", "Off-Season", "High Volume", "Surplus")
        ),

        ExampleTemplate(
            title = "Teenage Athlete Development",
            goal = ProgrammeGoal.ATHLETIC_PERFORMANCE,
            frequency = 3,
            duration = SessionDuration.STANDARD,
            exampleText = "I'm 16 years old and play football. I want to get stronger and more athletic for next season. 3 days per week focusing on fundamental movement patterns, speed, and strength. Emphasize good form and injury prevention. Include unilateral work, core stability, and explosive movements. I'm new to serious weight training.",
            tags = listOf("Youth", "Athletic Development", "Football", "Movement Quality")
        ),

        ExampleTemplate(
            title = "Deadlift Domination",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 4,
            duration = SessionDuration.EXTENDED,
            exampleText = "The deadlift is my passion and I want to maximize it. Currently pulling 415 lbs and want to hit 500 lbs. 4 days per week with multiple deadlift variations, posterior chain work, and grip strength. Include conventional, sumo, deficit, and block pulls. Focus on addressing my weak point which is breaking the bar off the floor.",
            tags = listOf("Deadlift Specialist", "Posterior Chain", "Variation", "Weak Point")
        ),

        ExampleTemplate(
            title = "Functional Fitness Over 40",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 4,
            duration = SessionDuration.STANDARD,
            exampleText = "I'm 42 years old and want to stay strong and functional for life. I don't need to lift the heaviest weights but want to maintain muscle, bone density, and movement quality. 4 days per week with a mix of strength training and mobility work. Focus on anti-aging, joint health, and movements that transfer to real life.",
            tags = listOf("Over 40", "Functional", "Longevity", "Anti-Aging")
        ),

        ExampleTemplate(
            title = "Marathon Runner's Strength",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 2,
            duration = SessionDuration.QUICK,
            exampleText = "I'm training for a marathon but want to add strength training to prevent injuries and improve performance. Only 2 days per week for 30-45 minutes due to high running volume. Focus on single-leg strength, core stability, and posterior chain. No leg fatigue that would interfere with running training.",
            tags = listOf("Runner", "Injury Prevention", "Single Leg", "Minimal Volume")
        ),

        ExampleTemplate(
            title = "Gym Anxiety Beginner",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 3,
            duration = SessionDuration.QUICK,
            exampleText = "I'm new to the gym and feel intimidated by free weights. I want to build confidence and strength starting with machines and dumbbells before progressing to barbells. 3 days per week for 30-45 minutes. Focus on basic movement patterns and building a foundation. I want clear progression and beginner-friendly exercises.",
            tags = listOf("Beginner", "Gym Anxiety", "Machines", "Confidence Building")
        ),

        ExampleTemplate(
            title = "Military/LEO Tactical",
            goal = ProgrammeGoal.ATHLETIC_PERFORMANCE,
            frequency = 4,
            duration = SessionDuration.EXTENDED,
            exampleText = "I'm in law enforcement and need functional strength for my job. 4 days per week focusing on movements that translate to tactical situations: carrying, climbing, sprinting, and fighting. Include weighted vest work, uneven carries, and explosive movements. I need to be strong, fast, and have great endurance.",
            tags = listOf("Tactical", "Law Enforcement", "Functional", "Occupational")
        ),

        ExampleTemplate(
            title = "Garage Gym Gladiator",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 4,
            duration = SessionDuration.STANDARD,
            exampleText = "I train in my garage with a power rack, barbell, plates, and adjustable dumbbells. 4 days per week focusing on compound movements and minimal equipment needs. I want maximum results with basic equipment. Include rack pulls, pin squats, and floor press since I don't have a full bench setup.",
            tags = listOf("Garage Gym", "Limited Equipment", "Creativity", "Minimalist")
        ),

        ExampleTemplate(
            title = "Plateau Breaker Protocol",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 4,
            duration = SessionDuration.EXTENDED,
            exampleText = "I've been stuck at the same weights for 6 months and need to break through plateaus. Currently intermediate level with decent strength but no recent progress. 4 days per week with periodization, new exercises, and different rep ranges. Need to shock my system and restart progress on all main lifts.",
            tags = listOf("Plateau", "Intermediate", "Periodization", "Progress")
        ),

        ExampleTemplate(
            title = "Student Schedule Strength",
            goal = ProgrammeGoal.BUILD_STRENGTH,
            frequency = 3,
            duration = SessionDuration.STANDARD,
            exampleText = "I'm a college student with an unpredictable schedule. I can usually manage 3 workouts per week but the days vary. I want a flexible programme that doesn't require specific days between sessions. University gym access with good equipment. Focus on strength gains that work around my class schedule and social life.",
            tags = listOf("Student", "Flexible Schedule", "University", "Adaptable")
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