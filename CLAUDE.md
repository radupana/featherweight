## System prompts

Apply critical thinking and give me your objective assessment of the viability of ideas. Play the role of a skeptical team member who's job it is to play devil's advocate. Provide honest, balanced feedback without excessive praise or flattery.

## Project Background

Building a weightlifting Super App that combines the best features from apps like Boostcamp, Juggernaut.ai, KeyLifts, and Hevy. Focus on iterative development to create an amazing user experience.

## Technical Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3) 
- **Design System**: Athletic Elegance with glassmorphism (light theme only)
- **Database**: Room with SQLite v13 (destructive migration)
- **Architecture**: MVVM with Repository layer
- **Navigation**: Single-Activity with enum-based routing
- **Async**: Coroutines and Flow with Dispatchers.IO
- **Animations**: Spring-based with haptic feedback

## Current Implementation Status

### Core Features ✅

- **Exercise Database**: 112 exercises with full metadata - ALL using singular naming convention
- **Programme System**: Popular templates (531, StrongLifts, Upper/Lower, nSuns) with 1RM setup
- **Workout Tracking**: Real-time set completion, smart weight input, programme integration
- **Analytics**: Interactive charts (tappable volume bars), Quick Stats, Big 4 focus
- **History**: Paginated with long-tap delete, edit mode for completed workouts
- **Smart Features**: Auto-populate weights, swipe-to-delete sets, programme progress tracking

### Programme System Details

- **Templates**: Starting Strength, StrongLifts 5x5, Upper/Lower Split, Wendler's 531, nSuns 531
- **Setup Flow**: 1RM input for main lifts with robust decimal handling
- **Progress Tracking**: Visual progress cards, completed/total workouts, percentage display
- **Exercise Validation**: All programme exercises auto-created if missing from database

### Recent Improvements

- ✅ Fixed navigation to return to correct screen (HOME/HISTORY)
- ✅ Added long-tap delete for workouts across all screens
- ✅ Fixed 1RM input UX using TextFieldValue with proper cursor control
- ✅ Replaced all placeholder exercises with real names
- ✅ Standardized ALL exercises to singular form (Pull-up, not Pull-ups)
- ✅ Fixed programme progress race condition with proper refresh on navigation
- ✅ Ensured programme workouts only use persisted exercises

## Key Files & Architecture

**Data Layer:**
- `FeatherweightDatabase.kt` - Room v13 with destructive migration
- `FeatherweightRepository.kt` - Central data access, programme workout creation
- `ExerciseSeeder.kt` - 112 exercises (all singular names)
- `ProgrammeSeeder.kt` - Programme templates with real exercises

**Programme Components:**
- `ProgrammesScreen.kt` - Template browser with filters
- `ActiveProgrammeScreen.kt` - Active programme management with delete
- `ProgrammeSetupDialog.kt` - 1RM input with proper weight validation
- `ProgrammeViewModel.kt` - Programme state management with refreshData()

**Workout Components:**
- `WorkoutScreen.kt` - Main interface with programme context
- `SetEditingModal.kt` - Full-screen modal with swipe-to-delete
- `ExerciseCard.kt` - Summary view with progress metrics

## Important Implementation Details

### Exercise Naming Convention
- ALL exercises use singular form: "Barbell Curl" not "Barbell Curls"
- Database enforces this through v13 migration
- Historical workouts wiped and re-seeded with correct names

### Programme Workouts
- Sequential day numbering (1,2,3,4) regardless of week days
- Automatic exercise creation if not in database
- Intelligent categorization based on exercise name
- Programme progress refreshes on screen navigation

### Input Handling
- Weight inputs use TextFieldValue with TextRange for cursor control
- Auto-select on focus for easy replacement
- Validation: reps ≤ 999, weight ≤ 9999.99, RPE ≤ 10.0

## Common Commands

- Lint: `./gradlew lint`
- Build: `./gradlew assembleDebug`
- Install: `./gradlew installDebug`

## Next Priority Issues

1. **Analytics Text Overflow**: 
   - "Latest PR" card needs to show exercise name clearly
   - "Training Frequency" shows "4.0 days/" - needs full "days/week"

2. **Programme Enhancement**:
   - Exercise substitution UI

## Future Milestones

### AI Programme Import
- LLM integration for converting AI text to structured programmes
- Exercise name fuzzy matching
- User review before activation
- 5 imports/month quota

### Social Features
- Workout sharing
- Community challenges
- Leaderboards

### Advanced Analytics
- ML-powered insights
- Plateau detection
- Form video analysis

## Development Guidelines

- No comments unless explaining complex logic
- Follow Material Design 3 consistently
- Test edge cases (empty states, long text, errors)
- Atomic commits with descriptive messages
- Update CLAUDE.md for architectural changes