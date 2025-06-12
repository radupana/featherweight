## System prompts

Do not flatter and say that everything is a great idea. Be critical and objective and provide
alternatives when you think that you have better ways of achieving our goals.

Please step back and consider whether anything I say is truly a good idea or whether you’re just
agreeing with me. Apply critical thinking and give me your objective assessment of the viability of
the idea. Why won’t this work? What are my blind spots? Play the role of a skeptical team member
who’s job it is to play devil's advocate and not give me an easy ride”

Provide honest, balanced feedback without excessive praise or flattery.

Make sure to run ktlint before every build.

## Project background

We are building a weightlifting Super App, that combines the best that other best-in-class apps have
to offer. Think of things like Boostcamp, Juggernaut.ai, KeyLifts, Hevy.

Our goal is to iteratively build an amazing app.

Some features are:

- The ability to track workouts, in terms of exercises, sets, reps, RPE, time/duration, etc.
- Workout history tracker
- Profile creation and management
- Social aspect (think the Duolingo of weightlifting)
- AI assistance, with things like auto-regulation, depending on how hard an exercise was
- Wide range of exercises, potentially dynamically sourced and maintained, with video/image
  explanations of how to perform them, as well as brief documentation of what muscles they target
- Analytics/trends across the board
- Ability to either start an empty workout, or use a template
- Quality of life improvements like pre-populating weights with previously used once
- Body weight + measurements tracking

We are starting with Android and I am using Android Studio with Kotlin Compose for the codebase,
which is already available in your context.

## Technical Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3) with custom premium theme system
- **Design System**: Athletic Elegance with glassmorphism, gradients, and micro-interactions (light
  theme only)
- **Database**: Room with SQLite (currently using `fallbackToDestructiveMigration()`)
- **Architecture Pattern**: MVVM with Repository layer
- **Navigation**: Single-Activity architecture with enum-based navigation
- **Async**: Coroutines and Flow with Dispatchers.IO
- **Animations**: Spring-based animations with haptic feedback
- **Dependency Injection**: Manual (considering Hilt for future)

## Current Implementation Status

### Data Layer

- **Core entities**: Exercise, Workout, ExerciseLog, SetLog with proper relationships
- **Junction tables**: ExerciseMuscleGroup, ExerciseEquipment, ExerciseMovementPattern for
  many-to-many relationships
- **Type converters**: LocalDateTime, List<String>, List<Int> for complex data storage
- **Comprehensive DAOs**: Complex queries for analytics, filtering, and aggregations
- **Type safety**: Extensive use of enums (MuscleGroup, Equipment, MovementPattern,
  ExerciseDifficulty, ExerciseType, ExerciseCategory)
- **Exercise database**: 104 high-quality exercises across all categories (from basic bodyweight to
  advanced barbell movements)

### Features Implemented

- **Splash screen**: Clean branding with app logo
- **Home screen**: Recent workout history, quick start options, template selection
- **Exercise selector**: Search, filter by category/equipment, 104 exercises with detailed metadata
- **Workout tracking**: Real-time set completion, RPE tracking, weight/reps input with validation
- **Smart set editing**: Inline editing with text selection, keyboard navigation between fields
- **History view**: Expandable workout details, edit mode for completed workouts
- **Advanced analytics**: Comprehensive analytics screen with interactive charts, volume trends,
  strength progression tracking, and performance insights
- **In-progress workout detection**: Auto-resume interrupted workouts
- **Custom exercise creation**: User-defined exercises with full metadata support
- **Edit mode system**: Temporary editing of completed workouts with save/discard options

### UI/UX Patterns & Recent Improvements

- **Material Design 3**: Custom blue theme (#2196F3) with proper color semantics
- **State hoisting**: ViewModels as single source of truth
- **Smart defaults**: Pre-populated weights from previous sets
- **Empty states**: Helpful prompts and guided actions
- **Lazy loading**: Efficient recomposition and data loading
- **Responsive layouts**: Adaptive to screen sizes and orientations
- **Keyboard handling**: Proper IME padding and scroll behavior
- **Text input optimization**: Auto-select text on focus, proper field sizing
- **Bulk actions**: "Complete All Sets" functionality with validation
- **Navigation flow**: Back navigation tracks previous screen (HOME/HISTORY)
- **Modal editing**: Full-screen SetEditingModal for cramped UI issues
- **Deletion UX**: Swipe-to-delete for sets, long-tap for exercises/workouts
- **Summary cards**: Exercise cards show progress metrics instead of individual sets
- **Real-time saving**: Set data persists on every keystroke for instant completion

## Code Conventions

- **Naming**: PascalCase for classes/composables, camelCase for functions/variables
- **Compose**:
    - State hoisting pattern with ViewModels
    - Remember for local state, LaunchedEffect for side effects
    - Modifier as last parameter, key() for stable list items
    - imePadding() for keyboard handling
- **Database**:
    - Type-safe queries with suspend functions
    - Proper foreign keys with CASCADE deletes
    - Junction tables for many-to-many relationships
    - Enum storage with converters for type safety
- **ViewModels**:
    - StateFlow for UI state management
    - Suspend functions for data operations
    - Data classes for screen states
    - Clear separation of concerns
- **Error Handling**: Try-catch blocks around database operations with logging
- **Null Safety**: Explicit handling with safe calls and proper default values
- **Text Input**: TextFieldValue with TextRange for proper selection control

## Important Files & Architecture

### Core Data Files

- `FeatherweightDatabase.kt`: Room database configuration, currently v1 with destructive migration
- `FeatherweightRepository.kt`: Central data access layer with complex queries
- `ExerciseEnums.kt`: Comprehensive enums (MuscleGroup, Equipment, MovementPattern, etc.)
- `ExerciseSeeder.kt`: 104 exercises across all categories with metadata
- `Exercise.kt`: Core exercise entity with junction table relationships

### UI Components

- `SetRow.kt`: Inline editing component with text selection and keyboard navigation ✅
- `SetEditingModal.kt`: Full-screen modal for set editing with swipe-to-delete ✅
- `ExerciseCard.kt`: Summary display with progress metrics, fully clickable with long-tap delete
- `WorkoutScreen.kt`: Main workout interface with edit mode system and improved navigation
- `ExerciseSelectorScreen.kt`: Search and filter interface for exercise selection
- `HistoryScreen.kt`: Workout history with expandable details and long-tap delete
- `AnalyticsScreen.kt`: Comprehensive analytics with interactive charts, quick stats, and
  performance insights
- `SimpleChart.kt`: Custom chart components with tap detection and visual feedback

### ViewModels

- `WorkoutViewModel.kt`: Workout state management, set operations, edit mode logic
- `ExerciseSelectorViewModel.kt`: Exercise search, filtering, and selection
- `HistoryViewModel.kt`: Workout history with intelligent caching (5-minute cache validity)
- `AnalyticsViewModel.kt`: Analytics data processing with cache-then-update strategy for instant UI
  updates

### Premium Theme & Styling

- `Theme.kt`: Premium Athletic Elegance theme (light theme only for consistency)
- `ModernComponents.kt`: Custom glassmorphic components with animations and haptic feedback
- **Color Palette**: Deep athletic blue to purple gradients, energetic success greens
- **Typography**: Enhanced hierarchy with proper spacing and weights
- **Effects**: Glassmorphism cards, gradient progress indicators, breathing animations
- **Interactions**: Spring animations, haptic feedback, micro-interactions
- **Focus Management**: Smart text selection with non-interfering backend updates

## Common Commands

- Lint: `./gradlew lint`
- Type checking: Built into Kotlin compiler
- Unit tests: `./gradlew test`
- Instrumented tests: `./gradlew connectedAndroidTest`
- Build debug: `./gradlew assembleDebug`
- Clean build: `./gradlew clean build`
- Install on device: `./gradlew installDebug`

## Exercise System Details

### Exercise Database Structure

- **104 Total Exercises**: Comprehensive coverage from beginner to expert level
- **Categories**: Chest, Back, Shoulders, Arms, Legs, Core, Cardio, Full Body
- **Equipment Types**: Bodyweight, Barbell, Dumbbell, Cable Machine, Machines, Accessories
- **Difficulty Levels**: Beginner → Novice → Intermediate → Advanced → Expert
- **Metadata**: Primary/secondary muscles, movement patterns, equipment requirements
- **Validation Logic**: Sets require reps > 0 AND weight > 0 to be marked complete

### Key Exercise Examples by Category

- **Compound Movements**: Back Squat, Deadlift, Bench Press, Pull-ups, Overhead Press
- **Bodyweight Basics**: Push-ups, Planks, Burpees, Mountain Climbers
- **Machine Variations**: Leg Press, Lat Pulldown, Cable Rows, Chest Press
- **Isolation Work**: Bicep Curls, Lateral Raises, Calf Raises, Face Pulls

## UI/UX Implementation Details

### Workout Flow

1. **Start Workout**: Empty workout or resume in-progress
2. **Add Exercises**: Search/filter from 104 options + custom creation
3. **Track Sets**: Inline editing with auto-select text, proper keyboard navigation
4. **Complete Sets**: Individual or bulk "Complete All Sets" with validation
5. **Save Workout**: Auto-save with optional naming and completion

### Edit Mode System

- **Read-only Protection**: Completed workouts are locked by default
- **Temporary Editing**: Edit mode allows modifications with save/discard options
- **Visual Indicators**: Clear "EDITING" badges and color changes
- **State Management**: Proper handling of concurrent edits and validation

### Text Input Optimizations

- **Auto-selection**: Clicking any field selects all existing text
- **Field Sizing**: Proper width for digits (Reps: 80dp, Weight: 100dp, RPE: 80dp)
- **Keyboard Handling**: IME padding, adjustResize, smooth scrolling during input
- **Validation**: Real-time input filtering (reps ≤ 999, weight ≤ 9999.99, RPE ≤ 10.0)

## Documentation & Resources

- `CLAUDE.md`: This file - complete codebase overview and development guidelines
- Recent git commits show UI improvements and exercise database expansion

## Current Issues & Immediate Priorities

**✅ FIXED Issues:**

1. **Red Set Backgrounds**: Fixed by using opaque green color (0xFFE8F5E9) instead of
   semi-transparent, and only showing SwipeToDismissBox red background during active swipe gesture
2. **Missing Add Set Button**: Fixed by restructuring SetEditingModal to always show LazyColumn
   regardless of whether sets exist
3. **Text Input Nightmare**: Fixed weight input appending digits (1→1.0→21.0→231.0) by preventing
   LaunchedEffect interference during user input
4. **Dark Theme Issues**: Disabled problematic dark theme, app now uses consistent premium light
   theme only

**URGENT - Today's Session Results:**

- ✅ Fixed navigation to return to correct screen (HOME/HISTORY) instead of always WORKOUT_HUB
- ✅ Added long-tap delete for workouts in HistoryScreen and WorkoutHubScreen
- ✅ Implemented swipe-to-delete for sets in SetEditingModal
- ✅ Fixed Copy Last button logic (only appears when last set has reps AND weight > 0)
- ✅ Moved Add Set/Copy Last buttons to appear right below last set in LazyColumn
- ✅ Added auto-scroll to newly added sets
- ✅ Removed redundant section headers ("Ready to train?", "Workouts", "Workout History")
- ✅ Fixed volume display from lbs to kg across all screens
- ✅ Made exercise cards and in-progress workout cards fully clickable
- ✅ Changed "Complete Workout" button to "Complete" for better fit
- ✅ **Fixed**: Red backgrounds on completed sets (was SwipeToDismissBox red showing through
  transparent green)
- ✅ **Fixed**: Add Set button now appears for new exercises

**COMPLETED - Premium Design Overhaul ✅**

1. **Modern Theme System**: Implemented dual light/dark themes with Athletic Elegance design
   philosophy
2. **Glassmorphism Effects**: Added semi-transparent cards with backdrop blur and modern depth
3. **Smooth Animations**: Spring animations, haptic feedback, breathing effects for active elements
4. **Enhanced Typography**: Improved hierarchy with better spacing, weights, and letter spacing
5. **Micro-interactions**: Touch feedback, animated progress indicators, modern status displays
6. **Visual Improvements**: Gradient progress bars, modern metric cards, improved empty states

**COMPLETED - Analytics Enhancement ✅**

1. **Interactive Volume Charts**: Tappable volume bars with exact kg display and visual feedback
2. **Enhanced Quick Stats**: Exercise name in Recent PR, average training days/week, "Strength Gain"
   clarity
3. **Big 4 Focus**: Limited analytics to main lifts (Squat, Deadlift, Bench Press, Overhead Press)
4. **Performance Insights**: 5 dynamic insights including volume progression, consistency tracking,
   and RPE guidance
5. **Smart Caching**: Cache-then-update strategy for instant UI responses with background data
   refresh
6. **Tooltips**: Calculation explanations for all Quick Stats metrics

**URGENT - Next Session Priority Issues:**

1. **Custom Exercise Enhancement**: Allow users to provide full exercise metadata (muscle groups,
   equipment, etc.) when creating custom exercises, with optional fields
2. **Duplicate Exercise Prevention**: Prevent adding custom exercises with duplicate names
3. **Analytics Quick Stats Text Overflow**:
    - Latest PR card should show both "Latest PR" label AND exercise name clearly
    - Training Frequency card shows "4.0 days/" with text cut off, need full "days/week" display
4. **Volume Filter Chip**: Still shows "V" instead of "Volume" on Strength Progression section -
   text doesn't fit in chip
5. **Volume Bar Alignment**: Volume trend bars are misaligned with date labels, causing tap
   detection issues (need to tap next to bar instead of on it)

**NEXT MAJOR MILESTONE - Workout Templates:**
After fixing above issues, implement pre-defined workout templates with popular strength programs:

- **Wendler's 531 variations** (Original, BBB, FSL, etc.)
- **StrongLifts 5x5**
- **nSuns 531**
- **Starting Strength**
- **GZCLP**
- Templates should include: exercises, sets, reps, weight progression, RPE guidance
- Custom template creation for advanced users

**Current High Priority:**

1. **Performance**: Large dataset optimizations, pagination for exercise list
2. **Achievement System**: Gamification with streaks, badges, and celebrations3
3. **Analytics Polish**: Add export functionality, time range filters, and exercise comparison
   features

**Medium Priority:**

1. **Social Features**: Workout sharing, leaderboards, community challenges
2. **AI Integration**: Smart weight suggestions, auto-regulation based on RPE
3. **Exercise Media**: Video demonstrations, form tips, muscle activation guides
4. **Export/Import**: Backup functionality, data portability

**Long-term:**

1. **Cross-platform**: iOS implementation with shared business logic
2. **Wearable Integration**: Apple Watch, Wear OS support
3. **Advanced Analytics**: ML-powered insights, plateau detection
4. **Nutrition Integration**: Meal tracking, macro calculations

## 🤖 AI Import Feature (Next Major Priority)

### 📋 Feature Overview

**AI-to-App Workflow Automation** - Users can paste AI-generated workout programs (from ChatGPT,
Claude, etc.) and automatically convert them into structured, trackable programs within
Featherweight. Eliminates manual transcription of AI workout programs.

### 🎯 Key Implementation Steps

#### 1. Introduce Program Concept

- **Programs** = Collection of related workouts with shared goals/metadata (e.g., "12-week
  Upper/Lower Split")
- Extends current single-workout tracking to multi-workout program tracking
- Programs contain: name, description, duration, difficulty, goals, progression scheme
- Individual workouts belong to programs and follow program structure

#### 2. Create JSON Schema for Workout Programs

- Design flexible schema handling input from vague ("4-day PPL") to specific ("Week 1: Bench 3x8
  @80%")
- Schema must capture: program metadata, individual workouts, exercises with
  sets/reps/RPE/progression
- Handle ambiguity with sensible defaults while preserving user-specified details
- Support various rep schemes (fixed, ranges, RPE-based), weight prescriptions (%, fixed,
  bodyweight)

#### 3. LLM Integration Service

- HTTP integration with OpenAI API (GPT-4o mini recommended for cost efficiency)
- System prompt includes JSON schema + examples of input→output transformations
- User provides text input → LLM returns structured JSON → App parses JSON
- Include rate limiting (5 imports per user per month) and quota management
- Handle malformed responses and API errors gracefully

#### 4. Program Creation Pipeline

- Parse LLM JSON response into app data structures
- Match AI exercise names to existing exercise database (handle variations/aliases)
- Create new exercises for unrecognized names, suggest alternatives
- Generate Program + ProgramWorkout + Exercise entities in database
- Validate program structure and provide user review interface before saving

#### 5. UI Integration Points

- Add "Import" option to Templates/Home screen
- Create import screen with text input area and processing feedback
- Add program review screen for user to verify/edit before saving. Allow user to edit program after
  generation as well.
- Modify workout flow to support program-based workouts vs standalone workouts
- Add program management screens (browse, activate, view progress)

### 🎯 Success Metrics

- Users can go from "Claude, create me a workout program" to trackable templates in under 30 seconds
- Eliminates manual workout program transcription
- Positions Featherweight as first AI-native fitness tracking app
- Creates viral sharing potential ("Check out this AI import feature")

### ⚠️ Current Dependencies

- Existing workout tracking system must remain fully functional
- Template system needs to be expanded to support program templates
- Exercise database needs robust matching/alias system
- User quota and rate limiting system for API costs

## Development Best Practices

### Before Making Changes

1. Read existing code to understand patterns and conventions
2. Check CLAUDE.md (this file) for architectural decisions
3. Run `./gradlew lint` to ensure code quality
4. Test on device with `./gradlew installDebug`

### Code Quality Standards

- No comments unless explaining complex business logic
- Follow Material Design 3 guidelines consistently
- Use proper state hoisting and ViewModel patterns
- Implement proper keyboard handling for all text inputs
- Validate all user inputs with clear error messaging
- Test edge cases (empty states, long text, network issues)

### Git Workflow

- Keep commits focused and atomic
- Test builds before committing
- Update CLAUDE.md when making architectural changes
- Use descriptive commit messages explaining the "why"

## Recently Fixed Issues

### Red Background Problem ✅ SOLVED

**Root Cause:** SwipeToDismissBox's red delete background was showing through the semi-transparent
green background (15% alpha) on completed sets.

**Solution:**

1. Changed completed set background from `Color(0xFF4CAF50).copy(alpha = 0.15f)` to opaque
   `Color(0xFFE8F5E9)`
2. Modified SwipeToDismissBox to only show red background when actively swiping:
   `if (dismissState.targetValue != SwipeToDismissBoxValue.Settled)`

**Files Modified:**

- `SetEditingModal.kt` - Opaque green background + conditional swipe background
- `SetRow.kt` - Opaque green background for consistency

### Missing Add Set Button ✅ SOLVED

**Root Cause:** LazyColumn was wrapped inside `if (sets.isNotEmpty())`, preventing it from rendering
when no sets existed.

**Solution:** Restructured SetEditingModal to always show LazyColumn, moving the conditional logic
to only wrap the header and divider.

**Files Modified:**

- `SetEditingModal.kt` - Moved `if (sets.isNotEmpty())` to only wrap header section, LazyColumn
  always renders

### Analytics Screen Enhancement ✅ COMPLETED

**Implementation:** Comprehensive analytics overhaul with focus on user experience and actionable
insights.

**Features Added:**

1. **Interactive Volume Charts**: Tappable bars show exact kg values with visual highlighting
2. **Enhanced Quick Stats**: Exercise names in PRs, average training days/week calculation, "
   Strength Gain" clarity
3. **Performance Insights**: 5 dynamic insights covering strength trends, volume progression,
   consistency, PRs, and RPE guidance
4. **Smart Caching**: Cache-then-update strategy for instant UI responses with background refresh
5. **Big 4 Focus**: Analytics limited to main compound lifts for meaningful progression tracking

**Files Modified:**

- `AnalyticsScreen.kt` - Complete UI overhaul with interactive elements and enhanced insights
- `AnalyticsViewModel.kt` - Added cache system, Big 4 filtering, and average training days
  calculation
- `SimpleChart.kt` - Added tap detection and visual feedback for volume bars
- `FeatherweightRepository.kt` - Added getAverageTrainingDaysPerWeek() method

### Workout Templates - Next Major Feature

Ready to implement popular strength training programs as selectable templates:

- Template data structure design
- Exercise selection and progression logic
- Weight calculation based on user maxes
- RPE guidance integration
- Popular programs: 531, StrongLifts, nSuns, Starting Strength, GZCLP
