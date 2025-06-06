## System prompts
Please step back and consider whether anything I say is truly a good idea or whether you’re just agreeing with me. Apply critical thinking and give me your objective assessment of the viability of the idea. Why won’t this work? What are my blind spots? Play the role of a skeptical team member who’s job it is to play devil's advocate and not give me an easy ride”

Provide honest, balanced feedback without excessive praise or flattery.

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
- Core entities: Exercise, Workout, ExerciseLog, SetLog
- Junction tables for many-to-many relationships
- Type converters for complex data types (List, LocalDateTime)
- Comprehensive DAOs with complex queries for analytics
- Type safety through extensive use of enums

### Features Implemented
- Splash screen with branding
- Home screen with workout templates and history
- Exercise selector with search/filter (500+ exercises)
- Workout tracking with real-time updates
- Smart set editing for completed workouts
- History view with expandable workout details
- Basic analytics (volume, estimated 1RM)
- In-progress workout detection and resume
- Custom exercise creation

### UI/UX Patterns
- Material Design 3 with custom blue theme (#2196F3)
- State hoisting with ViewModels
- Smart defaults from previous performance
- Empty states with helpful prompts
- Lazy loading for performance
- Responsive layouts with Compose

## Code Conventions

- **Naming**: PascalCase for classes/composables, camelCase for functions/variables
- **Compose**: 
  - State hoisting pattern
  - Remember for local state
  - LaunchedEffect for side effects
  - Modifier as last parameter
- **Database**: 
  - Type-safe queries
  - Proper foreign keys with CASCADE
  - Wrapped in coroutines
- **ViewModels**: 
  - StateFlow for UI state
  - Suspend functions for data operations
  - Data classes for screen states
- **Error Handling**: Try-catch blocks around database operations
- **Null Safety**: Explicit handling with safe calls

## Key Development Guidelines

1. **Database Migrations**: Always increment version and add migration when changing schema
2. **Exercise Seeding**: ExerciseSeeder.kt contains initial exercise data
3. **Testing**: Focus on repository and ViewModel testing
4. **Performance**: Use lazy loading and efficient recomposition
5. **State Management**: Single source of truth in ViewModels

## Important Files
- `FeatherweightDatabase.kt`: Database configuration and migrations
- `FeatherweightRepository.kt`: Central data access layer  
- `ExerciseEnums.kt`: All exercise-related enums
- `Theme.kt`: Material Design 3 theming and colors
- `ExerciseSeeder.kt`: Initial exercise data

## Common Commands
- Lint: `./gradlew lint`
- Type checking: Built into Kotlin compiler
- Unit tests: `./gradlew test`
- Instrumented tests: `./gradlew connectedAndroidTest`
- Build debug: `./gradlew assembleDebug`
- Clean build: `./gradlew clean build`
- Install on device: `./gradlew installDebug`

## Documentation Structure
- `EXERCISES.md`: Comprehensive exercise system specification with competitive analysis
- Feature-specific .md files track vision and implementation plans for each subdomain

## Next Development Priorities

Based on the current state, focus areas include:
1. Implementing proper database migrations (remove fallbackToDestructiveMigration)
2. Adding more analytics and progress visualization
3. Implementing workout templates and programs
4. Adding social features
5. Integrating AI-powered suggestions
6. Performance optimizations for large datasets

Hints: 
1. Avoid comments unless absolutely required to explain a strange situation
