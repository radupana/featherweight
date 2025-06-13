## System prompts

Do not flatter and say that everything is a great idea. Be critical and objective and provide
alternatives when you think that you have better ways of achieving our goals.

Please step back and consider whether anything I say is truly a good idea or whether you're just
agreeing with me. Apply critical thinking and give me your objective assessment of the viability of
the idea. Why won't this work? What are my blind spots? Play the role of a skeptical team member
who's job it is to play devil's advocate and not give me an easy ride"

Provide honest, balanced feedback without excessive praise or flattery.

Make sure to run ktlint before every build.

## Project Background

Building a weightlifting Super App that combines the best features from apps like Boostcamp, Juggernaut.ai, KeyLifts, and Hevy. Focus on iterative development to create an amazing user experience.

Key features: workout tracking (sets/reps/RPE), history, analytics, AI assistance, comprehensive exercise database, templates, and quality-of-life improvements.

## Technical Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3) 
- **Design System**: Athletic Elegance with glassmorphism (light theme only)
- **Database**: Room with SQLite (destructive migration for development)
- **Architecture**: MVVM with Repository layer
- **Navigation**: Single-Activity with enum-based routing
- **Async**: Coroutines and Flow with Dispatchers.IO
- **Animations**: Spring-based with haptic feedback

## Current Implementation Status

### Complete Features ✅

- **Exercise Database**: 104 exercises with full metadata (muscles, equipment, difficulty)
- **Workout Tracking**: Real-time set completion, RPE tracking, smart input validation
- **History System**: Paginated workout history with edit mode and long-tap deletion
- **Analytics**: Interactive charts, volume trends, strength progression, performance insights
- **Custom Exercises**: Full metadata creation with duplicate prevention
- **Smart Features**: Auto-populate weights, copy last set, in-progress workout detection
- **Premium UI**: Athletic Elegance theme with glassmorphism and micro-interactions

### Key Files & Architecture

**Data Layer:**
- `FeatherweightDatabase.kt` - Room configuration
- `FeatherweightRepository.kt` - Central data access
- `ExerciseSeeder.kt` - 104 exercise definitions
- Junction tables for many-to-many relationships

**UI Components:**
- `WorkoutScreen.kt` - Main workout tracking interface
- `AnalyticsScreen.kt` - Interactive charts and insights
- `HistoryScreen.kt` - Workout history with pagination
- `ExerciseSelectorScreen.kt` - Exercise search and filtering
- `SetEditingModal.kt` - Set editing with swipe-to-delete

**ViewModels:**
- `WorkoutViewModel.kt` - Workout state and set operations
- `AnalyticsViewModel.kt` - Analytics with smart caching
- `HistoryViewModel.kt` - History with 5-minute cache validity

## Code Conventions

- **Naming**: PascalCase for classes/composables, camelCase for functions/variables
- **State Management**: StateFlow in ViewModels, state hoisting pattern
- **Database**: Type-safe suspend functions, proper foreign keys, enum converters
- **UI**: Modifier as last parameter, proper keyboard handling, auto-select text inputs
- **Error Handling**: Try-catch with logging, graceful degradation

## Common Commands

- Lint: `./gradlew lint`
- Build debug: `./gradlew assembleDebug`
- Install: `./gradlew installDebug`
- Clean build: `./gradlew clean build`

## 🎯 Next Major Milestone: Programme System

### Programme Concept
Introduce structured multi-workout programs (e.g., "12-week Upper/Lower Split") that extend beyond single workout tracking:

- **Programme Entity**: Collection of related workouts with shared metadata
- **Programme Templates**: Pre-built popular programs (531, StrongLifts, nSuns, GZCLP)
- **Progression Schemes**: Built-in weight/rep progression logic
- **Duration Support**: 4-week, 8-week, 12-week programs

### Implementation Plan

1. **Data Model Extensions**
   - Add Programme, ProgrammeWorkout entities
   - Link existing Workout entity to programmes
   - Support standalone workouts and programme-based workouts

2. **Popular Programme Templates**
   - Wendler's 531 variations (Original, BBB, FSL)
   - StrongLifts 5x5
   - nSuns 531  
   - Starting Strength
   - GZCLP

3. **UI Integration**
   - Programme browser/selector screen
   - Active programme tracking
   - Progress visualization across programme duration
   - Programme completion celebrations

## 🤖 AI Programme Import System

### Vision
Transform AI-generated workout programs into structured, trackable programmes within Featherweight. Eliminate manual transcription of AI workout recommendations.

### Core Workflow
1. **User Input**: Paste AI-generated programme text (from ChatGPT, Claude, etc.)
2. **LLM Processing**: Convert unstructured text to structured JSON
3. **Programme Creation**: Parse JSON into app entities with exercise matching
4. **User Review**: Allow editing before saving
5. **Active Tracking**: Follow programme with progression tracking

### Technical Implementation

**LLM Integration:**
- OpenAI API integration (GPT-4o mini for cost efficiency)
- Flexible JSON schema handling vague → specific input
- Exercise name matching with existing 104-exercise database
- Fallback exercise creation for unrecognized names

**Programme Schema:**
- Programme metadata (name, duration, difficulty, goals)
- Workout structure (exercises, sets, reps, RPE, progression)
- Support percentage-based, fixed weight, and bodyweight prescriptions
- Handle rep ranges, AMRAP sets, and progression schemes

**UI Flow:**
- "Import Programme" option on Templates/Home screen
- Text input with processing feedback
- Programme review/edit screen before activation
- Programme management dashboard

### Success Metrics
- 30-second workflow from "AI recommendation" to "trackable programme"
- Position as first AI-native fitness tracking app
- Eliminate manual programme transcription
- Create viral sharing potential

### Implementation Dependencies
- Existing workout system remains fully functional
- Exercise database needs robust matching/alias system
- User quota management for API costs (5 imports/month)
- Error handling for malformed AI responses

## Development Best Practices

### Code Quality
- Follow Material Design 3 guidelines
- Proper state hoisting and ViewModel patterns
- Comprehensive input validation
- Test edge cases and error states
- No comments unless explaining complex logic

### Git Workflow
- Atomic, focused commits
- Test builds before committing
- Update CLAUDE.md for architectural changes
- Descriptive commit messages explaining "why"

---

*This document reflects the current state of the codebase and immediate development priorities.*