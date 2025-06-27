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

### AI Programme Generation (In Development)

**Overview**: Enable users to create custom programmes through natural language input, powered by LLM technology.

#### Phase 1: Core Infrastructure ✅ COMPLETED
1. **Basic Input Screen** ✅
   - New navigation route: PROGRAMME_GENERATOR
   - Simple text input with 500 character limit
   - "Generate Programme" button with loading state
   - Basic error handling UI
   - Character counter with validation

2. **LLM Service Setup** ✅
   - Created `AIProgrammeService.kt` with OpenAI API structure
   - Complete data models for programme generation
   - Rate limiting framework (5 requests/user/day)
   - JSON schema parsing for programme structure
   - Mock responses for testing (real API integration ready)

3. **Exercise Matching System** ✅
   - Created `ExerciseNameMatcher.kt` with fuzzy matching
   - Comprehensive exercise alias table (60+ common aliases)
   - Multi-algorithm confidence scoring (Levenshtein + word matching)
   - Top-N match selection with reasoning

4. **Navigation Integration** ✅
   - AI Generation button on HomeScreen (prominent placement)
   - AI Generation button on ProgrammesScreen (contextual)
   - Proper navigation flow: Home/Programmes → Generator → Back
   - Tertiary color scheme for visual distinction

#### Phase 2: Guided Input UI ✅ COMPLETED
1. **Smart Input Analysis** ✅
   - Created `InputAnalyzer.kt` with 150+ keyword pattern matching
   - Real-time analysis for experience level, equipment, injuries, schedule
   - Completeness scoring with sophisticated pattern recognition
   - Contextual feedback and suggestions

2. **Progressive Disclosure UI** ✅
   - Goal selection chips (Strength, Muscle, Endurance, General Fitness)
   - Frequency selection (2-7 days/week) with animated reveal
   - Duration selection (30min-2hr) with smart defaults
   - Quick-add contextual chips based on selections

3. **Example Templates System** ✅
   - Created `ExampleTemplates.kt` with 8 comprehensive scenarios
   - Smart template filtering based on user selections (ANY criteria match)
   - Template scoring system prioritizing better matches
   - One-click template loading for instant completion

4. **Dynamic Placeholder System** ✅
   - Created `PlaceholderGenerator.kt` with context-aware prompts
   - Placeholders update based on goal/frequency/duration selections
   - Personalized examples and suggestions

#### Phase 3: Programme Preview & Validation ✅ COMPLETED
1. **Data Models & Architecture** ✅
   - Created `ProgrammePreviewModels.kt` with comprehensive preview structures
   - Exercise matching confidence scoring and alternatives system
   - Validation framework with warnings/errors and severity levels
   - Edit action system for programme modifications

2. **Validation Engine** ✅
   - Created `ProgrammeValidator.kt` with volume guidelines and safety checks
   - Muscle balance validation and movement pattern analysis
   - Programme scoring system (0.0-1.0) with detailed feedback
   - Category-based validation (Volume, Balance, Progression, Safety, etc.)

3. **Preview UI Components** ✅
   - Created `ProgrammePreviewScreen.kt` with loading/success/error states
   - Created `ProgrammePreviewComponents.kt` for programme overview and actions
   - Created `WorkoutPreviewComponents.kt` for detailed workout and exercise views
   - Exercise resolution UI with confidence indicators and alternatives

4. **Integration & Navigation** ✅
   - New navigation route: PROGRAMME_PREVIEW
   - Connected generator → preview navigation flow
   - Exercise matching with ExerciseWithDetails compatibility
   - Fixed compilation errors and type conflicts

**Current Issue**: Generate button creates 2-second loading simulation but doesn't create actual programme data, so preview screen shows infinite loading.

**Test Checkpoint**: ❌ Incomplete - Navigation works but no programme data generated
- Generator UI fully functional with guided input
- Preview screen architecture complete but no data flow
- Need to create mock programme data or connect to AI service

#### Phase 4: Remaining Implementation Tasks 📋 IN PROGRESS

**Next Priority Tasks:**
1. **Exercise Resolution System** 🔄 NEXT
   - Create mock programme data generation for testing
   - Connect generator → preview data flow
   - Implement exercise matching and confidence scoring in UI
   - Test exercise resolution and alternative selection

2. **Edit Capabilities** 📋 PENDING
   - Wire up exercise editing forms to backend logic
   - Implement real-time validation during edits
   - Add bulk editing options (adjust all sets/reps)
   - Quick swap exercise functionality

3. **Regeneration Options** 📋 PENDING
   - Implement regeneration mode selection UI
   - Create regeneration prompt variants for AI service
   - Preserve user edits during regeneration
   - Version history for programme iterations

4. **Activation Flow** 📋 PENDING
   - Convert preview to active programme structure
   - Handle 1RM integration for strength programmes
   - Programme scheduling and start date selection
   - Database integration with existing programme system

5. **End-to-End Testing** 📋 PENDING
   - Complete programme generation → preview → activation flow
   - Exercise resolution edge cases
   - Programme validation scenarios
   - Error handling and recovery

6. **Enhanced UI/UX** ✅
   - Progressive animations with smooth slide-in/fade effects
   - Scrollable LazyColumn interface accommodating all content
   - Material Design 3 consistent styling with proper color schemes
   - Generation limit tracking (5 per day) with "X left today" indicator
   - Collapsible template browser with expand/collapse animations

**Test Checkpoint**: ✅ Smooth flow from quick options → text input → generation
- Progressive disclosure works smoothly
- Text analysis detects information accurately
- Completeness score updates in real-time
- Templates filter and load correctly
- Quick-add chips populate text appropriately
- All animations and interactions polished

#### Phase 3: Programme Preview & Validation (Week 3)
1. **Preview Screen**
   - Full programme display before activation
   - Exercise name resolution UI (show fuzzy matches for confirmation)
   - Basic programme stats (volume, frequency, exercise count)
   - "Activate" or "Regenerate" options

2. **Validation Engine**
   - Volume sanity checks (sets/week per muscle group)
   - Progressive overload validation
   - Rest day distribution check
   - Exercise availability verification

3. **Edit Capabilities**
   - Swap exercises with alternatives
   - Adjust sets/reps/progression
   - Add/remove training days
   - Save as draft for later

**Test Checkpoint**: Generate, preview, edit, and activate a programme

#### Phase 4: Iterative Refinement (Week 4)
1. **Conversation UI**
   - Chat-style interface for clarifications
   - LLM can ask for missing information
   - Previous messages visible for context
   - "Start Over" option

2. **Advanced System Prompt**
   - Include full Exercise database schema
   - Programme progression patterns
   - Volume/intensity guidelines
   - Safety constraints

3. **User Profile Integration**
   - Auto-populate known 1RMs
   - Consider workout history
   - Respect equipment preferences
   - Account for stated limitations

**Test Checkpoint**: Multi-turn conversation producing refined programme

#### Phase 5: Polish & Scale (Week 5)
1. **Cost Management**
   - User quota system (free tier: 5/month)
   - Token usage optimization
   - Caching for similar requests
   - Premium tier planning

2. **Programme Quality**
   - A/B testing framework
   - User feedback collection
   - Programme success metrics
   - Continuous prompt improvement

3. **Advanced Features**
   - Voice input integration
   - Programme sharing marketplace
   - Template library from successful generations
   - Export to other formats

**Test Checkpoint**: Production-ready with monitoring and analytics

### Technical Architecture for AI Programme Generation

**New Files Created**:
- `AIProgrammeService.kt` - OpenAI API structure and mock responses ✅
- `ExerciseNameMatcher.kt` - Fuzzy matching with 60+ exercise aliases ✅
- `ProgrammeGeneratorScreen.kt` - Main guided input UI ✅
- `ProgrammeGeneratorViewModel.kt` - State management with real-time analysis ✅
- `GuidedInputModels.kt` - Data models for goals, duration, chips, templates ✅
- `InputAnalyzer.kt` - Text analysis with 150+ keyword patterns ✅
- `PlaceholderGenerator.kt` - Dynamic placeholder based on selections ✅
- `ExampleTemplates.kt` - 8 comprehensive programme templates ✅
- `ProgrammeGeneratorComponents.kt` - Reusable UI components ✅

**Database Changes (Future)**:
- Add `generated_by_ai` flag to Programme table
- Create `programme_generation_log` table for analytics
- Add `exercise_aliases` table for name matching

**Key Technical Implementation**:
- Mock API responses for Phase 1-2, real OpenAI integration ready for Phase 3+ ✅
- Text analysis using regex patterns and keyword matching ✅
- Progressive disclosure UI with smooth animations ✅
- Real-time completeness scoring and validation feedback ✅
- Template-based example system with intelligent filtering ✅
- Rate limiting framework (5 generations/day) ✅

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