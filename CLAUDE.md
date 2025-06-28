## System prompts

Apply critical thinking and give me your objective assessment of the viability of ideas. Play the role of a skeptical team member who's job it is to play devil's advocate. Provide honest, balanced feedback without excessive praise or flattery.

## Project Overview

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
- **Exercise Database**: 112 exercises with full metadata (all singular naming)
- **Programme System**: Popular templates (531, StrongLifts, etc.) with 1RM setup
- **Workout Tracking**: Real-time set completion, smart weight input, programme integration
- **Analytics**: Interactive charts, Quick Stats, Big 4 focus
- **History**: Paginated with long-tap delete, edit mode for completed workouts
- **Rest Timer**: Smart auto-start based on exercise type with cross-screen persistence
- **AI Programme Generation**: Complete production system with dual-mode interface, template library, and bulletproof exercise validation

### Key Implementation Details

#### Exercise Naming Convention
- ALL exercises use singular form: "Barbell Curl" not "Barbell Curls"
- Usage tracking with frequency-based sorting in exercise selection
- **Exercise Database Integrity**: Strict 1-to-1 validation ensures LLM responses only contain supported exercises

#### Programme System
- Sequential day numbering (1,2,3,4) regardless of week structure
- Enhanced progress tracking with comprehensive debugging
- Programme progress refreshes on screen navigation
- **Fixed Critical Bug**: Programmes no longer incorrectly marked inactive mid-programme

#### Rest Timer
- Smart categorization: Compound (4min), Accessory (2min), Isolation (90s), Cardio (60s)
- Intensity adjustments: Heavy (+30%), Light (-20%)
- Timer persists across screens via hoisted ViewModel

#### AI Programme Generation
- **Real OpenAI Integration**: Uses gpt-4o-mini for personalized programmes
- **Secure API Management**: BuildConfig + local.properties for API keys
- **Smart Fallback**: Seamlessly uses mock responses on API failures
- **Quota System**: 5 generations per day with tracking (disabled for testing)
- **Exercise Database Validation**: Strict validation with automatic retry logic
- **Dual-Mode Interface**: Simplified (guided) and Advanced (freeform) generation modes
- **Comprehensive Template Library**: 28 well-researched templates covering diverse scenarios
- **Streamlined Regeneration**: 4 intuitive options (More/Less Volume, More/Less Intensity)

## Key Files & Architecture

### Data Layer
- `FeatherweightDatabase.kt` - Room database with destructive migration
- `FeatherweightRepository.kt` - Central data access layer
- `ExerciseSeeder.kt` - 112 exercises initialization
- `ProgrammeSeeder.kt` - Programme templates

### Core Screens
- `HomeScreen.kt` - Main hub with programme/freestyle sections
- `WorkoutScreen.kt` - Main workout interface with drag-and-drop
- `ProgrammesScreen.kt` - Template browser
- `AnalyticsScreen.kt` - Charts and statistics
- `HistoryScreen.kt` - Paginated workout history

### Key Components
- `SetEditingModal.kt` - Full-screen set editor with swipe-to-delete
- `RestTimerPill.kt` - Smart rest timer UI
- `ExerciseSelectorScreen.kt` - Exercise browser with usage stats

### AI Programme Features
- `AIProgrammeService.kt` - Real OpenAI integration with gpt-4o-mini
- `OpenAIModels.kt` - Complete OpenAI API data models
- `OpenAIApi.kt` - Retrofit interface for API calls
- `AIProgrammeQuotaManager.kt` - Daily quota tracking (5/day limit)
- `ProgrammePreviewScreen.kt` - Programme preview and editing
- `ProgrammeGeneratorScreen.kt` - Dual-mode generation interface
- `ExerciseNameMatcher.kt` - Fuzzy matching with aliases
- `ProgrammeValidator.kt` - Validation with scoring
- `ExampleTemplates.kt` - 28 comprehensive programme templates

## Common Commands

- Lint: `./gradlew lint`
- Build: `./gradlew assembleDebug`
- Install: `./gradlew installDebug`

## Current Known Issues

1. **UI Polish**: Analytics cards have text overflow, programme preview cards need improvement
2. **Missing Features**: No exercise substitution in programmes, no user profile/settings, no exercise media

## Next Priority Features

### Immediate (Phase 1) ✅ COMPLETED
1. **AI Programme Complete Overhaul** ✅
   - Integrated real OpenAI API with gpt-4o-mini
   - Implemented dual-mode generation interface
   - Added 28 comprehensive programme templates
   - Bulletproof exercise database validation
   - Enhanced programme progress tracking
   - Streamlined regeneration options

### Short-term (Phase 2)
1. **Analytics Improvements**
   - Fix text overflow issues
   - Add volume analytics
   - Exercise-specific progress tracking

2. **Programme Enhancements**
   - Exercise substitution in active programmes
   - Programme workout preview before starting
   - Custom programme builder (enhance existing AI system)

3. **AI System Refinements**
   - Programme validation score tooltip
   - Advanced mode templates and examples
   - Exercise preference learning
   - Programme difficulty auto-adjustment

### Medium-term (Phase 3)
1. **User Profile & Settings**
   - User stats and preferences
   - Units preference (kg/lbs)
   - Rest timer preferences
   - AI generation history and favorites

2. **Exercise Database Expansion**
   - Muscle group data
   - Form videos and guides
   - Equipment alternatives
   - Exercise demonstration integration

3. **Advanced AI Features**
   - Workout auto-progression based on performance
   - Injury prevention recommendations
   - Plateau detection and programme adjustments
   - Integration with wearable devices for recovery metrics

## Development Guidelines

- No comments unless explaining complex logic
- Follow Material Design 3 consistently
- Test edge cases (empty states, long text, errors)
- Use ViewModel factory pattern for ViewModels requiring dependencies
- Prefer composition over inheritance in UI components

## Technical Gotchas

### State Management
- LazyColumn with keys: Always access current state from ViewModel in drag operations
- Exercise order must be updated in both UI state and database
- Use `copy()` for immutable updates with correct exerciseOrder values

### Performance
- Heavy logging in recomposing components impacts performance
- Use `derivedStateOf` for expensive calculations
- Structure state to minimize recompositions

### Database Strategy
- Using `fallbackToDestructiveMigration()` during development
- All data re-seeded on schema changes
- Production will need proper migrations

## Development Workflow

- Don't commit to Git. I do that myself.
- Always build the code base before saying that you've completed a task.

## Recent Focus Areas

### Latest Session: Complete AI Programme System Overhaul (2025-06-28)
Comprehensive enhancement of the AI programme generation system based on extensive UAT:

**Critical Bug Fixes:**
- **Programme Progress Tracking**: Fixed programmes being incorrectly marked inactive after first week
- **Post-Activation Navigation**: Changed to navigate to Home screen instead of Programmes screen
- **Exercise Database Validation**: Implemented strict 1-to-1 relationship enforcement with automatic retry logic

**Major Feature Implementations:**
- **Dual-Mode Generation**: Added Simplified (guided) and Advanced (freeform) generation modes
- **Template Library Expansion**: Increased from 8 to 28 comprehensive, well-researched templates
- **Regeneration Simplification**: Reduced from 6 complex options to 4 intuitive ones
- **UI/UX Overhaul**: Unified styling, repositioned Browse Templates, enhanced Quick Add chips

**Technical Enhancements:**
- `ProgrammeGeneratorViewModel.kt`: Added exercise validation with retry logic
- `ProgrammeGeneratorScreen.kt`: Implemented dual-mode interface with dynamic UI
- `ExampleTemplates.kt`: Added 20 new templates covering diverse scenarios
- `MainActivity.kt`: Fixed post-activation navigation flow
- `FeatherweightRepository.kt`: Enhanced programme progress debugging

**Result**: AI programme generation is now production-ready with bulletproof exercise validation, comprehensive template library, and significantly improved user experience.

### Previous Session: Critical AI Programme Fixes (2025-01-28)
Fixed critical issues with AI programme generation and activation:
- **Exercise Resolution Removed**: Completely removed exercise resolution UI - all exercises are now accepted as-is
- **Programme Visibility**: Fixed issue where activated programmes didn't appear immediately by adding refresh on ProgrammesScreen load
- **Programme Completion Status**: Fixed "Programme Complete" bug for AI-generated programmes

### Previous Session: Real AI Integration (2025-01-27)
Successfully integrated real OpenAI API for AI Programme Generation:
- **API Integration**: Full OpenAI integration with gpt-4o-mini model
- **Secure Key Management**: BuildConfig + local.properties approach implemented
- **Production Ready**: System now makes real API calls when configured