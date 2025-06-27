## System prompts

Apply critical thinking and give me your objective assessment of the viability of ideas. Play the role of a skeptical team member who's job it is to play devil's advocate. Provide honest, balanced feedback without excessive praise or flattery.

## Project Background

Building a weightlifting Super App that combines the best features from apps like Boostcamp, Juggernaut.ai, KeyLifts, and Hevy. Focus on iterative development to create an amazing user experience.

## Technical Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3) 
- **Design System**: Athletic Elegance with glassmorphism (light theme only)
- **Database**: Room with SQLite v17 (destructive migration)
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
- ✅ Implemented drag-and-drop reordering for workout exercises
- ✅ Added compact exercise cards with drag handles for better space utilization  
- ✅ Fixed LazyColumn key-based item identity issues affecting drag state
- ✅ Added exercise frequency tracking with usage count in database
- ✅ Implemented frequency-based sorting in exercise selection (most used first)
- ✅ Added discrete usage count display (e.g., "5×") on exercise selection cards
- ✅ Implemented core rest timer functionality with auto-start on set completion
- ✅ Added floating timer pill UI with glassmorphism design and expandable controls

## Key Files & Architecture

**Data Layer:**
- `FeatherweightDatabase.kt` - Room v17 with destructive migration
- `FeatherweightRepository.kt` - Central data access, programme workout creation, usage tracking
- `ExerciseSeeder.kt` - 112 exercises (all singular names)
- `ProgrammeSeeder.kt` - Programme templates with real exercises
- `ExerciseDao.kt` - Exercise queries with frequency-based sorting (usageCount DESC, name ASC)

**Programme Components:**
- `ProgrammesScreen.kt` - Template browser with filters
- `ActiveProgrammeScreen.kt` - Active programme management with delete
- `ProgrammeSetupDialog.kt` - 1RM input with proper weight validation
- `ProgrammeViewModel.kt` - Programme state management with refreshData()

**Workout Components:**
- `WorkoutScreen.kt` - Main interface with programme context, drag-and-drop reordering
- `SetEditingModal.kt` - Full-screen modal with swipe-to-delete
- `CompactExerciseCard.kt` - Compact card with drag handle, progress metrics
- `DragHandleReorderableUtils.kt` - Drag handle component with visual feedback

**Exercise Selection:**
- `ExerciseSelectorScreen.kt` - Exercise browser with frequency-based sorting and usage indicators
- `ExerciseCard.kt` - Shows usage count badge when > 0, maintains visual hierarchy

**Rest Timer:**
- `RestTimer.kt` - Domain class with countdown logic and progress tracking
- `RestTimerViewModel.kt` - Timer state management with coroutines, add/subtract time controls
- `RestTimerPill.kt` - Floating glassmorphism UI with expandable controls (+30s, -30s, Skip)

## Important Implementation Details

### Exercise Naming Convention
- ALL exercises use singular form: "Barbell Curl" not "Barbell Curls"
- Database enforces this through v17 migration
- Historical workouts wiped and re-seeded with correct names

### Exercise Usage Tracking
- `usageCount` field automatically incremented when exercise is logged
- Exercise selection sorted by frequency (usageCount DESC, name ASC)
- Usage counts calculated from both real workouts and seeded test data
- Discrete badge display shows "×" format only when count > 0

### Rest Timer System
- **Auto-start**: 90s timer triggers when completing any set in WorkoutScreen
- **UI**: Floating pill at top center with glassmorphism design
- **Controls**: Tap to expand → +30s, -30s, Skip buttons with exercise name display
- **State**: Managed by RestTimerViewModel with coroutines and Flow for countdown
- **Integration**: Currently works in WorkoutScreen, auto-starts from SetEditingModal callbacks

### Programme Workouts
- Sequential day numbering (1,2,3,4) regardless of week days
- Automatic exercise creation if not in database
- Intelligent categorization based on exercise name
- Programme progress refreshes on screen navigation

### Input Handling
- Weight inputs use TextFieldValue with TextRange for cursor control
- Auto-select on focus for easy replacement
- Validation: reps ≤ 999, weight ≤ 9999.99, RPE ≤ 10.0

### Drag-and-Drop Implementation
- Long-press drag handles to initiate reordering
- Smooth animations using tween with FastOutSlowInEasing
- Real-time position updates from ViewModel state (critical for LazyColumn with keys)
- 60% hysteresis threshold prevents jittery movement
- Haptic feedback on drag start
- Visual feedback: elevation, opacity changes, and color highlights

## Common Commands

- Lint: `./gradlew lint`
- Build: `./gradlew assembleDebug`
- Install: `./gradlew installDebug`

## Next Priority Issues

1. **Rest Timer Enhanced Features** (Phase 3):
   - **High**: Add user preferences for auto-start toggle and default rest periods
   - **Medium**: Enhanced feedback with haptic pulse and sound when timer completes
   - **Medium**: Background persistence using WorkManager for cross-screen timer continuity
   - **Low**: Exercise database integration for more precise categorization

2. **Analytics Text Overflow**: 
   - "Latest PR" card needs to show exercise name clearly
   - "Training Frequency" shows "4.0 days/" - needs full "days/week"

3. **Programme Enhancement**:
   - Exercise substitution UI
   
4. **Profile Screen**:
   - Test user selection implementation
   - User stats and settings management

## Rest Timer Implementation Status

### ✅ Completed (Phase 1, 2 & Smart Features)
- **Core Architecture**: Timer domain logic with countdown and progress tracking
- **State Management**: RestTimerViewModel with coroutines and smart suggestion integration
- **UI Components**: Main RestTimerPill with glassmorphism design and expandable controls
- **Compact Component**: CompactRestTimer for modal contexts with simplified exercise type display
- **Smart Auto-start**: Intelligent timer suggestions based on exercise categorization and set intensity
- **Exercise Intelligence**: 
  - Compound movements: 4 minutes base (Squat, Deadlift, Bench Press)
  - Accessory exercises: 2 minutes base (Rows, Overhead Press)
  - Isolation work: 90 seconds base (Curls, Extensions)
  - Cardio movements: 60 seconds base (Burpees, Sprints)
- **Intensity Adjustments**: Heavy sets (+30%), Light sets (-20%) based on rep ranges
- **Visual Feedback**: Shows exercise type reasoning (e.g., "Compound • Heavy", "Isolation")
- **Manual Controls**: +30s, -30s, Skip Rest buttons with haptic feedback
- **Optimal Positioning**: Timer appears below action buttons in WorkoutScreen, compact in SetEditingModal
- **Transparency**: Expanded pill fully opaque, collapsed semi-transparent for glassmorphism
- **Cross-screen Integration**: Timer visible with appropriate UI in both WorkoutScreen and SetEditingModal
- **Animation**: Smooth expand/shrink animations that respect content flow

### 📋 Phase 3 Priorities (Enhanced Features)
- **User Preferences**: Settings for auto-start toggle, default rest periods per exercise type
- **Enhanced Feedback**: Haptic pulse and optional sound when timer completes
- **Background Persistence**: Continue timer when navigating between screens using WorkManager
- **Exercise Database Integration**: Use actual Exercise entities for more precise categorization

### 📋 Phase 4 Enhancements (Advanced Features)
- **Analytics Integration**: Track average rest times per exercise for insights
- **Adaptive Learning**: Suggest rest times based on user's historical patterns
- **Workout Context**: Shorter rests for volume work, longer for strength phases
- **1RM Integration**: Use actual 1RM data for precise intensity-based rest calculations

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

## Known Technical Gotchas

### LazyColumn with Keys
- When using `key` parameter, item composables maintain identity across recompositions
- Drag handlers can capture stale state references - always access current state from ViewModel
- Use `items()` instead of `itemsIndexed()` when indices need dynamic calculation

### State Management in Drag Operations
- Exercise order must be updated both in UI state and database
- Use `copy()` to ensure immutable updates with correct exerciseOrder values
- Database updates should be async to maintain smooth UI animations

### Compose Recomposition
- Heavy logging in frequently recomposing components impacts performance
- Use `derivedStateOf` for expensive calculations based on changing state
- Remember to clean up drag state completely on drag end to prevent inconsistencies

### Database Development Strategy
- Using `fallbackToDestructiveMigration()` during development for simplicity
- All data is re-seeded on schema changes to avoid migration complexity
- Usage counts are recalculated after seeding to ensure accurate frequency data

## Development Workflow

- don't commit to Git. I do that myself.