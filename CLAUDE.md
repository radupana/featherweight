## System prompts

Apply critical thinking and give me your objective assessment of the viability of ideas. Play the role of a skeptical team member who's job it is to play devil's advocate. Provide honest, balanced feedback without excessive praise or flattery.

## Project Background

Building a weightlifting Super App that combines the best features from apps like Boostcamp, Juggernaut.ai, KeyLifts, and Hevy. Focus on iterative development to create an amazing user experience.

### Latest Session (Rest Timer Complete Implementation)
Completed full rest timer system with smart features and user-friendly enhancements:
- Fixed timer positioning issues (moved below workout buttons)
- Added smart exercise categorization and intensity-based suggestions
- Implemented haptic feedback on timer completion
- Fixed "All" button to trigger timer
- Simplified UI by removing expand/collapse - all controls now always visible
- Fixed Copy Last button for bodyweight exercises (now only checks reps)
- Added cross-screen timer persistence
- Implemented pause/resume functionality with visual feedback
- Time adjustments (+/-) now work while paused
- Push notifications show only on completion (no spam)
- Timer auto-dismisses 2 seconds after completion
- Fixed notification vibration conflicts with haptic feedback

### UI/UX Improvements Completed
Major home screen and navigation improvements for better user experience:
- **Home Screen Organization**: Separated programme workouts from freestyle workouts into distinct sections
- **Consolidated Programme Display**: Eliminated redundant programme workout cards - all info now in single Active Programme card
- **Direct Navigation**: Continue/Start buttons go directly to workout, skipping redundant Active Programme screen step
- **Removed Confusing Notifications**: No more delayed activation/deactivation messages on wrong screens
- **Clear Visual Hierarchy**: Programme workouts under PROGRAMMES section, freestyle under FREESTYLE WORKOUTS
- **Smart Workout Display**: Shows in-progress workout details or "Start Next Workout" button as appropriate

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

**In-Progress Workouts & Programme Integration:**
- ✅ Enhanced workout tracking to include programme metadata
- ✅ Fixed multiple in-progress workouts display on Home screen
- ✅ Visual distinction between programme and freestyle workouts
- ✅ ActiveProgrammeScreen recognizes existing workouts
- ✅ Programme workouts link back to ActiveProgrammeScreen
- ✅ Completed sets tracking for in-progress workouts
- ✅ Programme 1RM auto-population from user profile

**Exercise & Workout Management:**
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
- ✅ Fixed Copy Last button for bodyweight exercises (now checks reps only)

**Rest Timer System (Fully Implemented):**
- ✅ Smart timer with exercise categorization (Compound: 4min, Accessory: 2min, Isolation: 90s, Cardio: 60s)
- ✅ Intensity-based adjustments (Heavy: +30%, Light: -20%)
- ✅ Auto-start on set completion with smart suggestions
- ✅ Timer pill positioned below workout buttons (no UI overlap)
- ✅ Compact timer in SetEditingModal with +15s/-15s controls
- ✅ Haptic feedback on timer completion (distinctive pattern)
- ✅ Cross-screen persistence (ViewModel hoisted to MainActivity)
- ✅ Fixed "All" button to trigger timer
- ✅ Simplified UI - removed expand/collapse, all controls always visible

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
- `RestTimer.kt` - Domain class with countdown logic, progress tracking, and pause/resume support
- `RestTimerViewModel.kt` - Timer state management with coroutines, haptic feedback, smart suggestions, notifications
- `RestTimerPill.kt` - Main timer UI with glassmorphism (simplified - all controls visible including pause/resume)
- `RestTimeCalculator.kt` - Exercise categorization and intensity-based rest calculations
- `CompactRestTimer` (in RestTimerPill.kt) - Compact version for SetEditingModal
- `NotificationManager.kt` - Handles push notifications for timer updates and completion

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

### Rest Timer System (Complete)
- **Smart Auto-start**: Intelligent timer based on exercise type and intensity
- **UI Design**: 
  - Main pill: Positioned below workout buttons, all controls visible (Pause/Play, +30s/-30s/Skip)
  - Compact pill: In SetEditingModal with Pause/Play, +15s/-15s/Skip controls
  - Glassmorphism aesthetic with smooth animations
  - Auto-dismisses 2 seconds after completion
- **Exercise Intelligence**:
  - Categorizes exercises by name patterns (Compound/Accessory/Isolation/Cardio)
  - Adjusts rest based on intensity (reps < 5 = Heavy +30%, reps > 12 = Light -20%)
  - Shows suggestion reason (e.g., "Compound • Heavy")
- **State Management**: 
  - RestTimerViewModel hoisted to MainActivity for cross-screen persistence
  - Coroutines for countdown with proper lifecycle management
  - Pause/resume functionality with state preservation
  - Time adjustments work in any state (running or paused)
- **User Feedback**:
  - Haptic feedback on completion (pattern: 100ms on, 50ms off, repeated 3x)
  - Push notification on completion only (no spam)
  - Visual feedback: red text when finished, blue when paused
  - "(Paused)" text indicator when timer is paused
- **Integration Points**:
  - Auto-starts from set completion in SetEditingModal
  - Manual start from "All" button in exercise header
  - Timer visible in both WorkoutScreen and SetEditingModal
  - Notification permission requested on app start (Android 13+)

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

## Current Known Issues

1. **UI Polish Needed**:
   - Analytics cards have text overflow issues (exercise names truncated, "days/" instead of "days/week")
   - Programme screen could use better workout preview cards
   - History screen pagination could be smoother
   
2. **Missing Features**:
   - No exercise substitution in programmes
   - No user profile management (settings, preferences)
   - No exercise media (videos, images, form guides)
   - No export/backup functionality

## Next Priority Features

1. **Analytics Improvements** (Quick Win):
   - Fix text overflow in PR and frequency cards
   - Add more detailed volume analytics
   - Exercise-specific progress tracking

2. **Programme Enhancements**:
   - Exercise substitution with smart alternatives
   - Programme workout preview before starting
   - Custom programme builder
   - Progress photos integration
   
3. **Profile & Settings**:
   - User profile with stats and achievements
   - Rest timer preferences (auto-start, default times)
   - Units preference (kg/lbs)
   - Theme customization (when we add dark mode)
   
4. **Exercise Database Expansion**:
   - Primary/secondary muscle group data
   - Form videos and technique guides
   - Exercise difficulty ratings
   - Equipment alternatives

## Rest Timer Implementation Status

### ✅ Completed (All Phases)
- **Core Architecture**: Timer domain logic with countdown and progress tracking
- **State Management**: RestTimerViewModel with coroutines, smart suggestions, and haptic feedback
- **UI Components**: 
  - Main RestTimerPill with glassmorphism design (simplified - all controls always visible)
  - CompactRestTimer for modal contexts with simplified exercise type display
  - Removed expand/collapse complexity - all controls shown in single row
- **Smart Auto-start**: Intelligent timer suggestions based on exercise categorization and set intensity
- **Exercise Intelligence**: 
  - Compound movements: 4 minutes base (Squat, Deadlift, Bench Press)
  - Accessory exercises: 2 minutes base (Rows, Overhead Press)
  - Isolation work: 90 seconds base (Curls, Extensions)
  - Cardio movements: 60 seconds base (Burpees, Sprints)
- **Intensity Adjustments**: Heavy sets (+30%), Light sets (-20%) based on rep ranges
- **Visual Feedback**: Shows exercise type reasoning (e.g., "Compound • Heavy", "Isolation")
- **Manual Controls**: 
  - Main timer: Pause/Resume, +30s/-30s/Skip buttons always visible
  - Compact timer: Pause/Resume, +15s/-15s/Skip buttons always visible
  - Pause state shown with color change and "(Paused)" text
- **Optimal Positioning**: Timer appears below action buttons in WorkoutScreen, compact in SetEditingModal
- **Cross-screen Integration**: Timer visible with appropriate UI in both WorkoutScreen and SetEditingModal
- **Animation**: Smooth expand/shrink animations that respect content flow
- **Haptic Feedback**: Distinctive vibration pattern on timer completion
- **Timer Persistence**: ViewModel hoisted to MainActivity for cross-screen continuity
- **Bug Fixes**: "All" button now properly triggers smart timer, Copy Last button appears for bodyweight exercises
- **Pause/Resume**: Timer can be paused and resumed with visual feedback, time adjustments work while paused
- **Push Notifications**: Shows single notification only when timer completes (Android 13+ permission handled)
- **Auto-dismiss**: Timer automatically disappears 2 seconds after completion

### Rest Timer Known Limitations
- No exercise-specific rest preferences (always uses smart suggestions)
- No timer presets for quick manual selection
- No sound/audio cues (only haptic and visual feedback)
- Timer resets when navigating away from workout screens
- No rest history tracking or analytics

### 📋 Future Enhancements

#### High Priority (Remaining)
- **Exercise-Specific Settings**: Remember user's preferred rest time per exercise
- **Timer Presets**: Quick access buttons for common rest periods (30s, 60s, 90s, 2min, 3min)

#### Medium Priority
- **Circular Progress Visual**: Replace linear progress with circular timer (more glanceable)
- **Audio Options**: Countdown beeps in final 5 seconds, completion sound
- **Rest Analytics**: Track average rest times per exercise, show trends
- **Different Timer Modes**: Separate timers for warm-up vs working sets
- **Timer Position Options**: Bottom bar vs floating pill preference

#### Low Priority
- **Timer Templates**: Pre-configured for Strength/Hypertrophy/Endurance
- **Set-Specific Rest**: Different rest times based on set number or RPE
- **Auto-Progression**: Suggest rest adjustments based on performance patterns
- **Motivational Features**: Quotes, form reminders during rest
- **Smart Integrations**: Wearables for HR recovery, smart home devices
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

## Key Technical Decisions & Patterns

### UI/UX Philosophy
- **Simplicity First**: Removed expand/collapse complexity in favor of always-visible controls
- **Context Awareness**: Different timer controls for different contexts (±30s main, ±15s compact)
- **Smart Defaults**: Intelligent rest suggestions reduce user decision fatigue
- **Visual Feedback**: Haptic feedback for important actions, smooth animations
- **Information Hierarchy**: Most important info (timer, reps, weight) always prominent

### State Management Patterns
- **ViewModel Hoisting**: App-level ViewModels (RestTimer) for cross-screen state
- **Repository Pattern**: Single source of truth for all data operations
- **Flow & StateFlow**: Reactive UI updates with proper lifecycle handling
- **Optimistic UI**: Update UI immediately, sync database asynchronously

### Performance Optimizations
- **Lazy Loading**: Pagination in History screen
- **Key-based LazyColumn**: Proper item identity for smooth animations
- **Destructive Migration**: Fast development iteration, production will need proper migrations
- **Selective Recomposition**: Careful state structuring to minimize recompositions

## Development Guidelines

- No comments unless explaining complex logic
- Follow Material Design 3 consistently
- Test edge cases (empty states, long text, errors)
- Atomic commits with descriptive messages
- Update CLAUDE.md for architectural changes
- Use ViewModel factory pattern for ViewModels requiring context/dependencies
- Prefer composition over inheritance in UI components

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

- Don't commit to Git. I do that myself.
- Always build the code base before saying that you've completed a task.