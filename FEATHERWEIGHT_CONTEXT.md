# Featherweight Development Context

_Last updated: 2025-06-01_

## Current Status
- **Current phase**: Phase 1 - Foundation Polish (UI fixes and basic UX improvements)
- **Recently completed**:
  - Fixed janky swipe-to-delete (replaced with proper Material3 delete buttons + confirmation)
  - Enhanced WorkoutScreen with proper set editing, Material3 UI patterns
  - Added set completion tracking with green visual feedback
  - Implemented "Copy Last Set" functionality
- **Next priorities**: Smart data entry improvements (previous set suggestions, auto-fill weight, RPE buttons)

## Vision & Competitive Position
**North Star**: "Duolingo for Weightlifting" - Social fitness app with intelligent AI coaching that democratizes smart programming.

**Key Market Gaps We're Targeting**:
1. **True AI-Powered Social Fitness**: No app combines Hevy's social features (4M users) with JuggernautAI's intelligence ($35/month)
2. **Perfect UI/UX with Advanced Features**: Advanced apps have complex UIs, simple apps lack features
3. **Intelligent Autoregulation for Everyone**: JuggernautAI is expensive/complex, others don't have real AI

## 6-Phase Roadmap

### **Phase 1: Foundation Polish** (Weeks 1-2) - *IN PROGRESS*
- ✅ Set editing with proper data entry
- ✅ Modern Material3 UI
- ✅ Progress tracking
- 🎯 **Next**: Smart data entry, rest timer, exercise search/autocomplete

### **Phase 2: Social Foundation** (Weeks 3-4)
- Friend system (add via username/QR)
- Workout feed ("Sarah just PR'd her deadlift! 🔥")
- High-five button (like Duolingo hearts)
- Basic social motivation (streaks, PR celebrations)

### **Phase 3: Intelligent Assistance** (Weeks 5-8)
- Exercise library with videos
- Smart progressive overload suggestions
- RPE-based autoregulation
- Weak point analysis

### **Phase 4: Templates & Programs** (Weeks 9-12)
- Program library (5/3/1, Starting Strength, etc.)
- Auto-progression (no manual calculation)
- Community programs

### **Phase 5: Advanced AI Coaching** (Weeks 13-20)
- Individual response pattern learning
- Real-time workout adaptations
- Health integration (sleep, HRV)

### **Phase 6: Super-App Features** (Weeks 21+)
- Nutrition integration
- Body composition tracking
- Advanced analytics
- Ecosystem integration

## Architecture & Technical Decisions

### **Tech Stack**:
- **UI**: Jetpack Compose + Material3 design system
- **Database**: Room + SQLite with fallbackToDestructiveMigration
- **Architecture**: MVVM with Repository pattern
- **Language**: Kotlin
- **Min SDK**: 26, Target SDK: 35
- **Navigation**: Simple state management in MainActivity

### **Key Dependencies**:
```kotlin
// Core
implementation("androidx.compose.material3:material3")
implementation("androidx.room:room-runtime:2.7.1")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")

// Planned additions
implementation("androidx.health.connect:connect-client:1.0.0") // Health data
implementation("com.google.mlkit:vision:16.0.0") // Computer vision
implementation("com.squareup.retrofit2:retrofit:2.9.0") // API calls
```

## Data Model (Current - v3)

### **Core Entities**:
```kotlin
// Workout session
data class Workout(
    val id: Long,
    val date: LocalDateTime,
    val notes: String?
)

// Exercise within a workout
data class ExerciseLog(
    val id: Long,
    val workoutId: Long,
    val exerciseName: String,
    val exerciseOrder: Int,
    val supersetGroup: Int?,
    val notes: String?
)

// Individual set
data class SetLog(
    val id: Long,
    val exerciseLogId: Long,
    val setOrder: Int,
    val reps: Int,
    val weight: Float,
    val rpe: Float?,
    val tag: String?, // "warmup", "drop", etc.
    val notes: String?,
    val isCompleted: Boolean,
    val completedAt: String? // ISO8601
)
```

## Working Features (Tested & Functional)
- ✅ **Workout creation**: Auto-creates new workout on app start
- ✅ **Exercise management**: Add exercises to workout with proper ordering
- ✅ **Set tracking**: Add sets, mark complete with visual feedback
- ✅ **Set editing**: Tap-to-edit reps, weight, RPE with proper validation
- ✅ **Set deletion**: Material3 delete button + confirmation dialog
- ✅ **Progress tracking**: Visual progress bar based on completed sets
- ✅ **Copy last set**: Quick duplication of previous set data
- ✅ **Material3 UI**: Modern, clean interface with proper color scheme
- ✅ **Database persistence**: All data saved to Room database

## Known Issues & Limitations
- **Sets created with 0/0 values**: Need smart defaults from previous sessions
- **No rest timer**: Should auto-start after set completion
- **Manual exercise entry**: Need autocomplete/search
- **No exercise library**: Users must type exercise names
- **Single workout only**: No workout history or multiple workouts
- **No social features**: Completely single-user at this point
- **No templates**: Only freestyle workouts supported

## Immediate Next Steps (Week 1)

### **Priority 1: Smart Data Entry**
```kotlin
// Add to SetLogDao
suspend fun getLastSetForExercise(exerciseName: String): SetLog?

// Add to ViewModel
fun suggestSetValues(exerciseLogId: Long): SetSuggestion
```
- Show "Last time: 3x8 @ 185lbs" when adding sets
- Auto-fill weight from previous session as starting point
- Quick RPE selection (1-10 buttons instead of text input)

### **Priority 2: Rest Timer**
- Auto-start timer after set completion
- Customizable rest periods by exercise type
- Visual countdown with notifications

### **Priority 3: Exercise UX**
- Exercise search/autocomplete with common exercises
- Exercise library with basic muscle group categorization

## Code Patterns & Conventions

### **File Organization**:
```
app/src/main/java/com/github/radupana/featherweight/
├── data/           # Room entities, DAOs, database
├── ui/             # Compose screens and components  
├── viewmodel/      # ViewModels and repository
└── MainActivity.kt # Simple navigation state management
```

### **UI Patterns**:
- **Cards for major components**: Exercise cards, progress cards
- **Material3 elevation**: 4-8dp for cards, 0dp for surfaces
- **Color scheme**: Primary blue (#4A90E2), secondary cyan (#50E3C2)
- **Typography**: Proper hierarchy with font weights
- **Touch targets**: Minimum 32dp for interactive elements

### **Data Flow**:
1. UI triggers ViewModel function
2. ViewModel calls Repository method
3. Repository executes database operation
4. StateFlow updates trigger UI recomposition

## Competitive Intelligence Summary

### **Key Competitors**:
- **Boostcamp**: 500K users, program library strength, Reddit favorite
- **Hevy**: 4M users, excellent social features, 4.9/5 rating
- **JuggernautAI**: Premium AI coaching ($35/month), true autoregulation
- **KeyLifts**: 5/3/1 specialization, percentage calculations
- **Strong**: Simple tracking, popular but basic

### **Our Competitive Advantages**:
1. **Social Intelligence**: Friends' data improves your programming
2. **Progressive Disclosure UX**: Simple for beginners, powerful for experts
3. **Ethical AI**: Transparent, user-controlled, privacy-first
4. **Modern Design**: Material3 with fitness-specific patterns

## Monetization Strategy
- **Free**: Basic tracking + limited social
- **Premium ($4.99/month)**: All programs + advanced analytics + unlimited social
- **Pro ($9.99/month)**: AI coaching + nutrition + body composition
- **Coach ($19.99/month)**: Everything + priority support + beta features

## Context Usage Instructions

### **For New AI Assistants**:
1. Read this entire document first
2. Review current codebase state in repository
3. Check "Immediate Next Steps" for current priorities
4. Follow established code patterns and UI conventions
5. Update this file after major changes or decisions

### **When to Update This File**:
- After completing features listed in "Next Steps"
- When making architectural decisions
- Before starting new phases
- When encountering significant bugs or design changes
- At the end of each coding session

### **Emergency Context Rebuild**:
If you're a new AI assistant and lost context:
1. The app is a weightlifting tracker built in Kotlin + Compose
2. We're in Phase 1 of a 6-phase plan to build "Duolingo for Weightlifting"
3. Priority is smart data entry improvements (see "Immediate Next Steps")
4. Follow Material3 design patterns established in current UI
5. All new features should integrate with existing Room database schema

---

**Remember**: This is not just a tracking app - we're building the ultimate strength training super-app that combines the best of social motivation, intelligent coaching, and beautiful UX. Every feature should move us closer to that vision.