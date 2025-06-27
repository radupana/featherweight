## System prompts

Apply critical thinking and give me your objective assessment of the viability of ideas. Play the role of a skeptical team member who's job it is to play devil's advocate. Provide honest, balanced feedback without excessive praise or flattery.

## Project Background

Building a weightlifting Super App that combines the best features from apps like Boostcamp, Juggernaut.ai, KeyLifts, and Hevy. Focus on iterative development to create an amazing user experience.

### Latest Session (AI Programme Generation UX Fixes & Validation System Overhaul)
Fixed critical UX issues in the AI Programme Generation system with a complete validation system redesign:

**✅ Core AI Programme Generation System:**
- **Phase 1**: Basic AI service framework with OpenAI integration structure (mock responses)
- **Phase 2**: Programme generator screen with input forms and validation
- **Phase 3**: Complete programme preview system with exercise resolution and editing
- **Phase 4**: Full activation flow with proper navigation timing

**✅ Advanced Features Implemented:**
- **Mock Programme Generator**: 5 different programme types (Strength, Muscle Building, Fat Loss, Athletic Performance, General Fitness)
- **Exercise Resolution**: Fuzzy matching system with confidence scoring and alternative suggestions
- **Programme Validation**: Comprehensive validation engine with automated issue detection
- **Bulk Editing**: Programme-wide adjustments (volume, beginner mode, progressive overload)
- **Regeneration Options**: 6 different regeneration modes (full regenerate, keep structure, alternative approach, fix validation, more variety, simpler version)
- **Professional UI/UX**: Complete preview system with week selection, workout cards, and action buttons

**✅ Technical Architecture:**
- **Data Models**: Complete type-safe models for AI responses, programme previews, and validation
- **State Management**: Cross-screen state sharing with GeneratedProgrammeHolder
- **Exercise Matching**: Intelligent exercise name matching with fuzzy logic
- **Validation Engine**: Volume guidelines, safety checks, and automated issue resolution
- **UI Components**: Modular preview components with professional Material Design 3 styling

**✅ User Experience Enhancements:**
- **Smart Navigation**: Proper activation flow timing to prevent "Programme Complete" confusion
- **Real-time Validation**: Live feedback on programme quality and safety
- **Exercise Alternatives**: One-click exercise swapping with confidence indicators
- **Progressive Disclosure**: Expandable sections for bulk editing and regeneration options
- **Professional Polish**: Consistent styling, proper loading states, and error handling

**🔧 Critical UX Fixes (Current Session):**

**1. Fixed Broken Fix Button System:**
- **Problem**: Fix buttons appeared for exercise resolution issues that require human judgment, but clicking them did nothing
- **Root Cause**: Exercise matching requires manual user selection, cannot be auto-fixed by system
- **Solution**: Complete validation system redesign:
  - Added `isAutoFixable: Boolean` property to `ValidationError`
  - Exercise resolution errors marked as `isAutoFixable = false` 
  - Fix buttons only show for truly auto-fixable issues (volume, balance, etc.)
  - Top-level Fix button only appears when auto-fixable issues exist

**2. Fixed Validation Score Update Bug:**
- **Problem**: Validation score stayed at 0% even after users resolved exercise matching issues
- **Root Cause**: Score calculation was binary (0% if any errors exist)
- **Solution**: Implemented proportional penalty system:
  - Score = `(baseScore - unresolvedExercises * 0.2f).coerceAtLeast(0.0f)`
  - Score updates in real-time as users resolve exercises
  - More nuanced feedback instead of always showing 0%

**3. Improved User Guidance System:**
- **Problem**: Confusing Fix buttons and unclear messaging about what users need to do
- **Solution**: Clear, actionable guidance:
  - Error messages: "Scroll down and click on exercises highlighted in red to resolve them"
  - Help text: "💡 Scroll down to find exercises highlighted in red and click them to select correct matches"
  - Visual distinction between auto-fixable and manual-fix-required issues
  - No more misleading Fix buttons for human-judgment tasks

**4. Smart Validation Logic:**
- **Auto-fixable Issues**: Volume problems, balance issues, progression gaps (show Fix buttons)
- **Manual Resolution Required**: Exercise matching, exercise selection (show guidance only)
- **Real-time Updates**: Validation re-runs immediately after user actions
- **Proportional Scoring**: Gradual score improvement as issues are resolved

**🔄 Technical Implementation Details:**
- **ValidationError Model**: Added `isAutoFixable: Boolean` property for UI logic
- **Score Calculation**: Changed from binary to proportional: `(baseScore - unresolvedCount * 0.2f).coerceAtLeast(0.0f)`
- **UI Components**: Enhanced ValidationResultCard with smart Fix button visibility
- **State Management**: Real-time validation updates after exercise resolution
- **User Guidance**: Context-sensitive help messages for different issue types

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

**AI Programme Generation:**
- `AIProgrammeService.kt` - Core service with OpenAI integration framework (currently mock responses)
- `MockProgrammeGenerator.kt` - Comprehensive mock data generator with 5 programme types and realistic workout structures
- `ProgrammePreviewModels.kt` - Complete data models for programme preview, validation, and editing
- `ProgrammePreviewViewModel.kt` - Business logic for programme processing, validation, editing, and activation
- `ProgrammePreviewScreen.kt` - Main preview UI with proper activation flow timing
- `ProgrammePreviewComponents.kt` - Modular UI components (header, overview, validation, actions, bulk editing)
- `WorkoutPreviewComponents.kt` - Workout and exercise preview cards with resolution system
- `ExerciseNameMatcher.kt` - Fuzzy matching service with confidence scoring
- `ProgrammeValidator.kt` - Validation engine with volume guidelines and safety checks
- `GeneratedProgrammeHolder.kt` - Cross-screen state management singleton

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

### AI Programme Generation System (Complete)
- **Mock Data Architecture**: MockProgrammeGenerator creates realistic programmes based on goal, frequency, and duration
- **Exercise Resolution**: 
  - Fuzzy matching with confidence scoring (0.0-1.0)
  - Alternative exercise suggestions with reasoning
  - One-click exercise swapping with automatic re-validation
- **Programme Validation**: 
  - Volume guidelines (sets per muscle group per week)
  - Safety checks (rest periods, RPE ranges, rep ranges)
  - Automated issue detection with fix suggestions
- **State Management**: 
  - GeneratedProgrammeHolder for cross-screen data passing
  - Real-time validation updates on any programme modification
  - Proper activation flow with success callbacks
- **User Experience**: 
  - Progressive disclosure (expandable sections for advanced features)
  - Professional Material Design 3 styling with glassmorphism elements
  - Smart navigation timing to prevent UI confusion
- **Regeneration System**: 
  - 6 different regeneration modes with unique algorithms
  - Maintains user preferences while varying programme structure
  - 2.5-second simulation with realistic loading states
- **Activation Flow**: 
  - Fixed timing issue where "Programme Complete" message appeared instead of navigation
  - Proper validation before activation (exercise resolution, validation errors)
  - Success callback ensures navigation happens after activation completes

## Common Commands

- Lint: `./gradlew lint`
- Build: `./gradlew assembleDebug`
- Install: `./gradlew installDebug`

## Current Known Issues

1. **AI Programme Generation UX Issues**:
   - **Validation Score Clarity**: The percentage score (e.g., "19%") next to "Programme Validation Passed" is confusing - users don't understand what it represents
   - **Solution Needed**: Add tooltip or explanatory text to clarify this is an overall programme quality score
   - **Priority**: Medium (affects user understanding but doesn't break functionality)

2. **UI Polish Needed**:
   - Analytics cards have text overflow issues (exercise names truncated, "days/" instead of "days/week")
   - Programme screen could use better workout preview cards
   - History screen pagination could be smoother
   
3. **Missing Features**:
   - No exercise substitution in programmes
   - No user profile management (settings, preferences)
   - No exercise media (videos, images, form guides)
   - No export/backup functionality

## Next Priority Features

### Phase 1: AI Programme Generation Polish (Immediate)
1. **Validation Score Clarity** (Quick Fix):
   - Add tooltip or info icon next to validation percentage
   - Explain: "Programme Quality Score - higher is better"
   - Consider visual improvements (color coding, better labeling)

2. **Real AI Integration** (Major Feature):
   - Replace mock AIProgrammeService with actual LLM integration
   - Implement OpenAI API calls with proper prompt engineering
   - Add quota management and usage tracking
   - Handle API errors gracefully with fallback options

### Phase 2: Programme System Enhancements
1. **Analytics Improvements** (Quick Win):
   - Fix text overflow in PR and frequency cards
   - Add more detailed volume analytics
   - Exercise-specific progress tracking

2. **Programme Enhancements**:
   - Exercise substitution with smart alternatives
   - Programme workout preview before starting
   - Custom programme builder
   - Progress photos integration

### Phase 3: User Experience & Polish
1. **Profile & Settings**:
   - User profile with stats and achievements
   - Rest timer preferences (auto-start, default times)
   - Units preference (kg/lbs)
   - Theme customization (dark mode implementation)
   
2. **Exercise Database Expansion**:
   - Primary/secondary muscle group data
   - Form videos and technique guides
   - Exercise difficulty ratings
   - Equipment alternatives

## Long-term Vision: AI Programme Import
- LLM integration for converting AI text to structured programmes
- Exercise name fuzzy matching
- User review before activation
- 5 imports/month quota

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

#### Phase 4: Complete Implementation ✅ COMPLETED

**All Core Features Implemented:**

1. **Exercise Resolution System** ✅ COMPLETED
   - ✅ Created comprehensive mock programme data generator with 5 programme types
   - ✅ Connected generator → preview data flow with GeneratedProgrammeHolder
   - ✅ Implemented exercise matching with confidence scoring (✓ ! ? ✗ indicators)
   - ✅ Exercise resolution UI with alternatives and manual override options
   - ✅ Cross-screen state management and automatic programme loading

2. **Advanced Edit Capabilities** ✅ COMPLETED
   - ✅ Programme name editing with inline text field
   - ✅ Individual exercise parameter editing (sets, reps, RPE, rest)
   - ✅ Bulk editing system with Quick Adjustments card:
     - Reduce/Increase Volume (±20%)
     - Beginner Mode (simplify complexity)
     - Add Progressive Overload (weekly intensity increases)
   - ✅ Real-time validation during edits
   - ✅ Collapsible UI for optional advanced features

3. **Regeneration System** ✅ COMPLETED
   - ✅ 6 regeneration modes with smart programme variants:
     - Full Regenerate: Completely new programme
     - Keep Structure: Same workouts, different exercises
     - Alternative Approach: Different programme style
     - Fix Validation Errors: Auto-correct issues
     - More Variety: Add extra exercises
     - Simpler Version: Beginner-friendly reduction
   - ✅ Exercise substitution mapping for realistic alternatives
   - ✅ Loading states and error handling for all regeneration paths

4. **Programme Activation Flow** ✅ COMPLETED
   - ✅ Pre-activation validation (exercise resolution + error checks)
   - ✅ Simulated activation process with user feedback
   - ✅ Success states with visual confirmation
   - ✅ Framework for future database integration
   - ✅ Error prevention for unresolved exercises or validation issues

5. **End-to-End Testing** ✅ COMPLETED
   - ✅ Complete flow: Generator → Preview → Edit → Regenerate → Activate
   - ✅ Exercise resolution workflows tested
   - ✅ Bulk editing and regeneration scenarios validated
   - ✅ Build verification successful with all features integrated

**Current State**: ✅ **AI Programme Generation MVP COMPLETE**

The entire AI Programme Generation feature is now fully functional with:
- **Smart input analysis** with 150+ keyword patterns
- **Realistic programme generation** based on user goals and preferences  
- **Professional preview interface** with validation and editing capabilities
- **Exercise resolution system** with confidence scoring and alternatives
- **Advanced editing tools** for fine-tuning programmes
- **Multiple regeneration options** for programme variants
- **Activation workflow** ready for database integration

**Test Checkpoint**: ✅ **FULLY FUNCTIONAL**
- Generate button creates realistic programme data with proper navigation
- Preview screen shows complete programme with interactive features
- All editing, regeneration, and activation flows working
- Professional UI/UX with proper error handling and loading states

## Next Phase: Production Integration & Advanced Features

### Phase 5: Production-Ready Implementation 🎯

**High Priority (Essential for Release):**
1. **Real LLM Integration** 
   - Replace MockProgrammeGenerator with actual OpenAI API calls
   - Implement proper prompt engineering for programme generation
   - Add response parsing and error handling for AI service
   - Rate limiting and cost management (5 generations/user/day)

2. **Database Integration**
   - Create ProgrammeTemplate table for AI-generated programmes
   - Implement full activation flow with programme persistence
   - Handle 1RM setup integration for strength programmes
   - Version control for programme iterations and user modifications

3. **Exercise Database Enhancement**
   - Improve exercise matching confidence with better fuzzy logic
   - Add fallback exercise suggestions for unmatched names
   - Implement exercise substitution recommendations
   - Enhanced exercise metadata for better programme validation

**Medium Priority (Enhanced Experience):**
4. **Advanced Validation & Safety**
   - Implement comprehensive programme validation rules
   - Muscle balance analysis and corrective suggestions
   - Volume load management and overtraining prevention
   - Progressive overload validation and guidance

5. **User Experience Polish**
   - Programme preview PDF export functionality
   - Share programme functionality (export/import JSON)
   - Programme comparison tools (before/after regeneration)
   - User rating and feedback system for generated programmes

6. **Personalization Engine**
   - Learning from user preferences and modifications
   - Historical programme performance analysis
   - Adaptive programme generation based on past success
   - Injury history and limitation considerations

### Phase 6: Advanced AI Features 🚀

**Cutting-Edge Capabilities:**
1. **Audio-to-Text Integration**
   - Voice input for programme generation ("I want to build strength for powerlifting")
   - Real-time speech analysis and intent detection
   - Hands-free programme modification commands

2. **Smart Programme Evolution**
   - Automatic programme progression based on workout performance
   - AI-driven exercise substitutions based on equipment availability
   - Adaptive volume adjustments based on recovery metrics
   - Integration with wearables for personalized programming

3. **Community & Social Features**
   - AI programme sharing and community ratings
   - Collaborative programme refinement
   - Coach integration for programme review and approval
   - Programme marketplace with AI-generated templates

### Grand Vision: The Future of Training Programming 🌟

**Ultimate Goal**: Create the most intelligent, adaptive, and user-friendly programme generation system in the fitness industry.

**Key Differentiators:**
- **Conversational AI**: Natural language programme creation and modification
- **Continuous Learning**: AI that improves programmes based on user outcomes
- **Professional Integration**: Tools for coaches and trainers to leverage AI assistance
- **Scientific Backing**: Evidence-based programming with research integration
- **Universal Accessibility**: From beginner-friendly to elite athlete programming

**Success Metrics:**
- 95%+ user satisfaction with generated programmes
- 80%+ programme completion rates
- 90%+ exercise matching accuracy
- Sub-10 second programme generation times
- Integration with top 10 fitness tracking platforms

**Technical Evolution:**
- Multi-modal AI (text, voice, image analysis for form feedback)
- Real-time programme adaptation during workouts
- Predictive analytics for injury prevention
- Integration with gym equipment and smart devices
- AR/VR programme visualization and guidance

The AI Programme Generation feature now serves as the foundation for revolutionizing how people create, customize, and execute their training programmes. This MVP demonstrates the core capabilities while providing a clear roadmap for building the future of intelligent fitness programming.
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

## Session Summary: AI Programme Generation UX Overhaul

**Date**: 2025-01-27
**Focus**: Fixed critical UX issues in AI Programme Generation system

### ✅ Problems Solved Today:

1. **Broken Fix Buttons**: Removed misleading Fix buttons for exercise resolution (requires human judgment)
2. **Validation Score Bug**: Fixed score staying at 0% - now updates proportionally as issues are resolved  
3. **Confusing UI**: Replaced broken buttons with clear, actionable user guidance
4. **Smart Validation**: Distinguish auto-fixable vs manual-fix-required issues

### 🔧 Technical Changes Made:

- **ValidationError Model**: Added `isAutoFixable: Boolean` property
- **Score Calculation**: Proportional penalties instead of binary 0%/100%
- **UI Logic**: Fix buttons only show for truly auto-fixable issues
- **User Guidance**: Context-sensitive help messages and visual indicators

### 📋 Identified for Next Session:

- **Validation Score Clarity**: Add tooltip/explanation for percentage score next to validation status
- **Real AI Integration**: Replace mock service with actual LLM integration

### 🎯 Current State:
AI Programme Generation system now has proper UX with working validation, appropriate Fix buttons, and clear user guidance. Ready for real AI integration and minor polish improvements.