## System prompts
Please step back and consider whether anything I say is truly a good idea or whether you’re just agreeing with me. Apply critical thinking and give me your objective assessment of the viability of the idea. Why won’t this work? What are my blind spots? Play the role of a skeptical team member who’s job it is to play devil's advocate and not give me an easy ride”

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
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Database**: Room with SQLite (currently using `fallbackToDestructiveMigration()`)
- **Architecture Pattern**: MVVM with Repository layer
- **Navigation**: Single-Activity architecture with enum-based navigation
- **Async**: Coroutines and Flow with Dispatchers.IO
- **Dependency Injection**: Manual (considering Hilt for future)

## Current Implementation Status

### Data Layer
- **Core entities**: Exercise, Workout, ExerciseLog, SetLog with proper relationships
- **Junction tables**: ExerciseMuscleGroup, ExerciseEquipment, ExerciseMovementPattern for many-to-many relationships
- **Type converters**: LocalDateTime, List<String>, List<Int> for complex data storage
- **Comprehensive DAOs**: Complex queries for analytics, filtering, and aggregations
- **Type safety**: Extensive use of enums (MuscleGroup, Equipment, MovementPattern, ExerciseDifficulty, ExerciseType, ExerciseCategory)
- **Exercise database**: 104 high-quality exercises across all categories (from basic bodyweight to advanced barbell movements)

### Features Implemented
- **Splash screen**: Clean branding with app logo
- **Home screen**: Recent workout history, quick start options, template selection
- **Exercise selector**: Search, filter by category/equipment, 104 exercises with detailed metadata
- **Workout tracking**: Real-time set completion, RPE tracking, weight/reps input with validation
- **Smart set editing**: Inline editing with text selection, keyboard navigation between fields
- **History view**: Expandable workout details, edit mode for completed workouts
- **Basic analytics**: Volume calculations, estimated 1RM, set completion tracking
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

## Key Development Guidelines

1. **Database Migrations**: Always increment version and add migration when changing schema
2. **Exercise Management**: ExerciseSeeder.kt contains 104 curated exercises with full metadata
3. **Testing**: Focus on repository and ViewModel testing, build verification before commits
4. **Performance**: Use lazy loading, efficient recomposition, and proper key management
5. **State Management**: Single source of truth in ViewModels with proper StateFlow usage
6. **UI Consistency**: Follow Material Design 3 guidelines, consistent spacing and typography
7. **Keyboard UX**: Proper IME handling with adjustResize and imePadding()

## Important Files & Architecture

### Core Data Files
- `FeatherweightDatabase.kt`: Room database configuration, currently v1 with destructive migration
- `FeatherweightRepository.kt`: Central data access layer with complex queries
- `ExerciseEnums.kt`: Comprehensive enums (MuscleGroup, Equipment, MovementPattern, etc.)
- `ExerciseSeeder.kt`: 104 exercises across all categories with metadata
- `Exercise.kt`: Core exercise entity with junction table relationships

### UI Components  
- `SetRow.kt`: Inline editing component with text selection and keyboard navigation (⚠️ red background issue)
- `SetEditingModal.kt`: Full-screen modal for set editing with swipe-to-delete (⚠️ red background + missing button issues)
- `ExerciseCard.kt`: Summary display with progress metrics, fully clickable with long-tap delete
- `WorkoutScreen.kt`: Main workout interface with edit mode system and improved navigation
- `ExerciseSelectorScreen.kt`: Search and filter interface for exercise selection
- `HistoryScreen.kt`: Workout history with expandable details and long-tap delete

### ViewModels
- `WorkoutViewModel.kt`: Workout state management, set operations, edit mode logic
- `ExerciseSelectorViewModel.kt`: Exercise search, filtering, and selection
- `HistoryViewModel.kt`: Workout history and analytics

### Theme & Styling
- `Theme.kt`: Material Design 3 implementation with custom blue theme
- Consistent use of MaterialTheme.colorScheme and typography
- Proper elevation and surface treatments

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

- `EXERCISES.md`: Comprehensive exercise system specification with competitive analysis
- `CLAUDE.md`: This file - complete codebase overview and development guidelines
- Recent git commits show UI improvements and exercise database expansion

## Current Issues & Immediate Priorities

**CRITICAL - Fix Tomorrow:**
1. **Red Set Backgrounds**: Completed sets still show red background despite fixes in both SetEditingModal.kt and SetRow.kt (changed tertiary theme to Color(0xFF4CAF50) but still appears red)
2. **Missing Add Set Button**: Button doesn't appear for new exercises with no sets, despite restructuring LazyColumn to always render

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
- ⚠️ **Still broken**: Red backgrounds on completed sets (need to find root cause)
- ⚠️ **Still broken**: Add Set button missing for new exercises

**High Priority After Bug Fixes:**
1. **Database Migrations**: Implement proper schema versioning (remove fallbackToDestructiveMigration)
2. **Performance**: Large dataset optimizations, pagination for exercise list  
3. **Workout Templates**: Pre-built programs and custom template creation
4. **Analytics Enhancement**: Progress visualization, trend analysis, volume tracking

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

## Known Issues & Investigation Notes

### Red Background Problem
**Files Checked & Modified:**
- `SetEditingModal.kt:413-417` - Changed to `Color(0xFF4CAF50).copy(alpha = 0.15f)`
- `SetRow.kt:41-46` - Changed to `Color(0xFF4CAF50).copy(alpha = 0.15f)`  
- Both files had `MaterialTheme.colorScheme.tertiary` which was red

**Still Red - Potential Causes:**
- Another component overriding the background color
- Theme-level color definition forcing red
- Cached build/compile issue
- Different component being used than expected
- State not properly triggering recomposition

**Investigation Steps for Tomorrow:**
1. Search entire codebase for any remaining `tertiary` color usage on completed sets
2. Check if there's a Theme.kt override forcing tertiary to be red
3. Verify which exact component is rendering (SetRow vs ExpandedSetRow)
4. Add debug logging to track color values at runtime
5. Clean build with `./gradlew clean assembleDebug`

### Missing Add Set Button Problem  
**Root Cause Found:** Two separate empty states - main UI shows text, LazyColumn is conditionally hidden

**Files Modified:**
- `SetEditingModal.kt` - Restructured conditional logic to always show LazyColumn
- Added button for `sets.isEmpty()` case inside LazyColumn items

**Still Missing - Potential Causes:**
- LazyColumn still not rendering when empty
- Button positioned outside visible area  
- Conditional logic error in restructuring
- Build cache not reflecting changes

**Investigation Steps for Tomorrow:**
1. Add debug logging to verify LazyColumn render and item count
2. Check if empty sets list triggers different code path
3. Verify LazyColumn contentPadding and item visibility
4. Test with manual empty list to isolate issue
5. Simplify button logic to always show regardless of conditions
