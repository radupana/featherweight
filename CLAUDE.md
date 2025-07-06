## IMPORTANT: Always Read Serena Instructions First

Before starting ANY work on code tasks, ALWAYS call the `mcp__serena__initial_instructions` tool first. This provides critical context about:
- Available semantic coding tools
- How to efficiently read and edit code
- Project-specific memory files
- Current operating modes

You MUST do this at the start of every conversation involving code work.

## System prompts

Apply critical thinking and give me your objective assessment of the viability of ideas. Play the
role of a skeptical team member who's job it is to play devil's advocate. Provide honest, balanced
feedback without excessive praise or flattery.

Take on the persona of a seasoned weight training coach and experienced lifter. Challenge my
proposals and ideas without flattering me constantly. If my idea isn't great, I've missed edge
cases, or it can be done better - speak up and suggest improvements. Don't blindly agree with me.
Your experience should guide our technical decisions.

## Project Overview

Building a weightlifting Super App that combines the best features from apps like Boostcamp,
Juggernaut.ai, KeyLifts, and Hevy. Focus on iterative development to create an amazing user
experience.

**Target Audience**: Intermediate and advanced lifters. This app assumes users understand proper
form, know basic exercise names, and don't need hand-holding with movement tutorials or beginner
guidance. We won't include exercise videos, detailed form guides, or basic lifting education
features.

**Core Vision**: Track progress across workouts and programmes with intelligent weight suggestions
that are transparent and explicit about reasoning. Always show WHY a weight was suggested.

## Technical Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Design System**: Athletic Elegance with glassmorphism (light theme only)
- **Database**: Room with SQLite (destructive migration during development)
- **Architecture**: MVVM with Repository layer
- **Navigation**: Single-Activity with enum-based routing
- **Async**: Coroutines and Flow with Dispatchers.IO

## Documentation Strategy

**ALWAYS** use Context7 for the latest documentation and APIs for:
- Kotlin language features and standard library
- Jetpack Compose components and Material Design 3
- Android SDK and framework APIs
- Room database and SQLite
- Coroutines and Flow
- Any third-party libraries in our tech stack

Context7 provides up-to-date, accurate documentation that supersedes any knowledge cutoff limitations. When implementing features or debugging issues, always:
1. Call `mcp__context7__resolve-library-id` to find the correct library ID
2. Use `mcp__context7__get-library-docs` to fetch current documentation
3. Implement according to the latest APIs and best practices from Context7

## Current Implementation Status

### Core Features ✅

- **Exercise Database**: 500 curated exercises with consistent naming (`[Equipment] [Target/Muscle] [Movement]`)
- **Programme System**: Templates with 1RM setup and progressive overload (StrongLifts 5x5 working)
- **Workout Tracking**: Real-time set completion, smart weight input, exercise swapping
- **AI Programme Generation**: Real OpenAI integration with dual-mode interface and template library
- **Analytics & History**: Interactive charts, workout history with edit mode
- **Rest Timer**: Smart auto-start based on exercise type with cross-screen persistence

### Progressive Overload System

**Current State**: Foundation-first approach focusing on StrongLifts 5x5 as test case

**What's Working:**
- Exercise validation system preventing invalid names
- Weight calculation with comprehensive logging showing reasoning
- Last workout performance tracking and progression rules
- 1RM-based starting weights for programmes

**What's Missing:**
- Deload logic after consecutive failures
- Freestyle workout weight suggestions
- Cross-programme intelligence

## Key Files & Architecture

### Data Layer
- `FeatherweightDatabase.kt` - Room database
- `FeatherweightRepository.kt` - Central data access layer  
- `exercises.json` - 500 exercises in JSON asset file
- `WeightCalculationService.kt` - Progressive overload logic

### Core Screens
- `HomeScreen.kt` - Main hub with programme/freestyle sections
- `WorkoutScreen.kt` - Main workout interface with unified progress card
- `ProgrammesScreen.kt` - Template browser
- `AnalyticsScreen.kt` - Charts and statistics

### AI Programme Features
- `AIProgrammeService.kt` - Real OpenAI integration with gpt-4o-mini
- `ExerciseValidator.kt` - Validates exercise names against database
- `ExampleTemplates.kt` - 28 comprehensive programme templates

## Development Guidelines

- **Foundation First**: Make it work for one case perfectly before expanding
- **Fail-Fast Philosophy**: No mock fallbacks or degraded functionality
- **Clean Code Maintenance**: Remove unused code, imports, and dead declarations
- **No Half-Ass Implementations**: Complete features properly across ALL impacted components
- **Transparent Logic**: Every weight calculation must be explainable to users
- **No Phased Planning**: Provide minimal planning and code to fix the problem at hand completely. Never give "phase 1, phase 2" or "now and later" approaches. Solve it properly in one go.

## Common Commands

- Build: `./gradlew assembleDebug`
- Lint: `./gradlew lint`
- Install: `./gradlew installDebug`

## Database Strategy

- **DESTRUCTIVE MIGRATIONS ALWAYS** - We use `fallbackToDestructiveMigration()` 
- **NO BACKWARD COMPATIBILITY** - This is development. We nuke and rebuild as needed
- **NO PRODUCTION CONCERNS** - Break whatever needs breaking. Database gets wiped on every schema change
- Modify existing entities directly - no V2 classes or versioning
- Progressive overload data stored in JSON fields for flexibility

## Current Focus: Progressive Overload System

**Phase 1** ✅: Foundation with StrongLifts 5x5 (Exercise validation, weight calculation logging, basic progression)

**Phase 2** 🎯: Deload logic and failure tracking for StrongLifts

**Phase 3**: Freestyle workout intelligence using programme data

**Phase 4**: Advanced programme support (5/3/1, nSuns, etc.)

**Key Principle**: Perfect one programme completely before expanding to others.

## Latest Session: Home Screen UI Revamp (2025-07-05)

**Major Change**: Replaced traditional card-based HomeScreen with innovative circular wheel navigation

**Key Components:**
- **CircularWheelMenu.kt** - Custom wheel component with 6 colored segments
- **Static Design** - Removed spinning feature after user feedback (too complicated)
- **Profile at Center** - User is literally at the center of the app
- **Direct Navigation** - Each segment navigates to main app sections

**Critical Lessons Learned:**
- **Image Padding Issues**: Banner images with excessive whitespace don't scale well in constrained spaces
- **TopAppBar Constraints**: Limited vertical space makes logo placement challenging
- **Simple Text Works**: Sometimes text is better than struggling with image scaling
- **User Feedback First**: Removed spinning when user said it was too complicated

**Technical Implementation:**
- Canvas for visual wheel drawing
- Overlay Boxes for click detection (Canvas can't handle clicks)
- Trigonometric calculations for proper icon/text positioning
- Proper angle calculations: `(index * segmentAngle) - 90f + (segmentAngle / 2f)`

## Critical Architecture Lessons

### Database Field Usage
- **ALWAYS** verify which fields the UI is updating vs validation checking
- `actualReps`/`actualWeight` = what user did (UI updates these)
- `targetReps`/`targetWeight` = what programme prescribed
- `reps`/`weight` = legacy fields, being phased out
- 157/500 exercises have `requiresWeight=false` (all bodyweight exercises)

### Set Input UI Architecture
```
[Exercise Name Header]
[Previous Performance Card] ← Shows last workout: "3 × 5×100kg @8"
[Rest Timer Bar]
[Set Input Table]
  | Target | Weight | Reps | RPE | ✓ |
  | 5×80   |   [ ]  | [ ]  | [ ] | □ |  ← Programme workout
  |        |   [ ]  | [ ]  | [ ] | □ |  ← Freestyle workout
```

### Key Validation Patterns
- Check `sets.any { !it.isCompleted && it.actualReps > 0 }` for "Complete All" visibility
- Always store `TextFieldValue` for cursor preservation in inputs
- Cache async validation results in `_setCompletionValidation`
- History query must include: `workout.status == COMPLETED || exercises.isNotEmpty()`

### UI/UX Principles
- **Remove before adding**: Question every UI element's purpose
- **Consistent layouts**: Same structure for all contexts (programme/freestyle)
- **Clear visual hierarchy**: Read-only data looks different from editable
- **No redundant information**: Show data once in the right place
- **Image constraints**: Logos with padding don't scale well in TopAppBar

### Common Pitfalls to Avoid
1. Don't add features without clear user benefit
2. Don't show the same information in multiple places
3. Don't make UI elements that aren't self-explanatory
4. Always test with both weighted and bodyweight exercises
5. Verify database saves are actually happening (check logs)
6. Canvas elements can't handle clicks - use overlay components

## Recent Feature Implementations

### PR Detection & Celebration System (Phase 3.2)
- **PRDetectionService** with Brzycki formula for 1RM calculations
- **PRCelebrationDialog** with confetti animations and haptic feedback
- Only save 1RM PRs to prevent database pollution
- PR detection must happen AFTER database save (timing critical)
- Baseline-aware confidence: 5%+ improvement over stored max = high confidence
- Dual systems: PRDetectionService (celebrations) vs GlobalProgressTracker (profile updates)

### Achievements System (Phase 3.3)
- 37 achievements across 4 categories (Strength, Consistency, Volume, Progress)
- **AchievementDetectionService** runs after workout completion
- Awards tab in Analytics with Recent Unlocks carousel
- Achievement state tracked in UserAchievement entity with context data

### Insights Engine (Phase 3.4)
- **InsightGenerationService** with 6 categories (Progress, Plateaus, Consistency, etc.)
- ProgressInsight entity with `autoGenerate = true` for primary key
- Insights tab with filtering and read/unread state
- Always increment database version when changing entity schemas

## Next Milestone: Exercise-Specific Progress Tracking

### Vision
Transform the app into a comprehensive progress tracking powerhouse with individual exercise analytics that rival dedicated strength training apps.

### Core Features to Build:
1. **Exercise Detail Screens** - Deep analytics for each lift with multiple chart types
2. **Intelligent Stall Detection** - Proactive plateau identification with context-aware suggestions  
3. **Visual Excellence** - Interactive charts for weight progression, volume trends, RPE patterns
4. **Smart Recommendations** - Exercise variations, deload timing, technique adjustments
5. **Predictive Projections** - "At this rate, you'll hit 140kg by..." motivational insights

### Integration Requirements:
- Seamless navigation from Analytics → Exercise → Detailed Progress
- Real-time progress context during workouts
- Historical comparison overlays (this month vs last)
- Exercise-specific filtering in History screen

### Success Criteria:
- Users check individual exercise progress weekly
- Stall detection prevents plateaus before they happen
- Visual charts become primary method for tracking progress
- Recommendations lead to measurable performance improvements

This next phase will establish Featherweight as the premier app for serious lifters who demand intelligent, data-driven training insights!