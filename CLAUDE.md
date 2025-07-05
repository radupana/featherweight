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

## Latest Session: Set Input UI Overhaul (2025-07-01)

Fixed critical UI/UX issues with set input fields that were causing major usability problems:

**Problems Solved:**
- **Text Reversal Bug**: Typing "123" resulted in "321" due to cursor position loss
- **Auto-Decimal Insertion**: Keyboard was inserting unwanted decimals ("1.02" instead of "123")
- **Placeholder Misalignment**: Placeholder text was left-aligned while input was center-aligned
- **Missing Text Selection**: Select-all on focus was completely broken
- **Input Length Limits**: No enforcement of max character limits per field type

**Technical Fixes:**
- **Fixed Cursor Position**: Changed `CleanSetLayout` to store `TextFieldValue` instead of `String`
- **Removed Recomposition Triggers**: Fixed `remember` keys that caused input resets on every keystroke
- **Implemented Hard Input Limits**: Weight (7 chars max), Reps (2 chars), RPE (2 chars with >10 clamping)
- **Created CenteredInputField**: Universal component with proper validation and center alignment
- **Fixed Keyboard Type**: Used `KeyboardType.Number` for all fields to prevent auto-decimals

**UI Improvements:**
- **Elegant Swipe-to-Delete**: Dynamic red stripe that grows with swipe distance
- **Proper Text Selection**: Select-all works correctly on field focus
- **Consistent Placeholders**: All placeholders center-aligned and disappear on focus

**Key Files Modified:**
- `CenteredInputField.kt` - New universal input component with validation
- `SetEditingModal.kt` - Fixed state management and swipe behavior
- Deleted `SelectAllOnFocusTextField.kt` - Replaced with working solution

**Critical Lesson**: Always store `TextFieldValue` for cursor preservation, never convert to/from `String` in input flows.

## Critical Bug Fixes & Architecture Lessons (2025-07-01 Continued)

### 1. **SetLog Field Mismatch Bug**
**Problem**: Checkbox wouldn't become clickable even with valid data entered
**Root Cause**: UI was updating `actualReps`/`actualWeight` fields, but validation was checking legacy `reps`/`weight` fields
**Fix**: Updated `canMarkSetComplete` to check actual fields: `set.actualReps > 0 && set.actualWeight > 0`
**Lesson**: Always verify which fields the UI is actually updating vs what validation is checking

### 2. **Exercise Weight Requirements**
**Problem**: "Complete All" functionality didn't respect that some exercises (pull-ups) don't require weight
**Solution**: Use existing `exercise.requiresWeight` flag in validation logic
**Database**: 157/500 exercises correctly marked as `requiresWeight=false` (all bodyweight exercises)

### 3. **History Not Showing Completed Workouts**
**Problem**: Completed workouts weren't appearing in history
**Fix**: Modified history query to include completed workouts even without exercises
**Code**: Changed filter from `exercises.isNotEmpty()` to `workout.status == COMPLETED || exercises.isNotEmpty()`

### 4. **Previous Performance Data**
**Problem**: "Previous: -" shown on every set row, never loading actual data
**Fix**: 
- Call `loadExerciseHistoryForName()` when opening set modal
- Filter to only completed workouts/sets
- Display in dedicated card instead of per-row

### 5. **UI Information Architecture Overhaul**

**Removed Confusing Features:**
- Weight suggestions (lightbulb) - unclear what it represented
- Hide/Show Previous toggle - unnecessary complexity
- Per-row previous display - redundant

**New Clean Architecture:**
```
[Exercise Name Header]
[Previous Performance Card] ← Shows last workout: "3 × 5×100kg @8"
[Rest Timer Bar]
[Set Input Table]
  | Target | Weight | Reps | RPE | ✓ |
  | 5×80   |   [ ]  | [ ]  | [ ] | □ |  ← Programme workout
  |        |   [ ]  | [ ]  | [ ] | □ |  ← Freestyle workout
```

**Benefits:**
- Clear separation of intent (Target) vs execution (actual values)
- Consistent UI for both programme and freestyle workouts
- No placeholder text - headers are sufficient
- Target column shows it's read-only with subtle background

### 6. **Missing "Complete All" Button**
**Problem**: Feature was accidentally removed during UI refactoring
**Fix**: Re-added icon button in header, only shows when uncompleted sets have reps
**Implementation**: Check `sets.any { !it.isCompleted && it.actualReps > 0 }`

## Key Development Principles

### Database Field Usage
- **ALWAYS** check which fields the UI is updating
- `actualReps`/`actualWeight` = what user did
- `targetReps`/`targetWeight` = what programme prescribed
- `reps`/`weight` = legacy fields, being phased out

### UI/UX Guidelines
- **Remove before adding**: Question every UI element's purpose
- **Consistent layouts**: Same structure for all contexts (programme/freestyle)
- **Clear visual hierarchy**: Read-only data looks different from editable
- **No redundant information**: Show data once in the right place

### Validation Logic
- **Respect exercise requirements**: Not all exercises need weight
- **Cache validation results**: Async validation in `_setCompletionValidation`
- **Check actual values**: Validate what the UI actually updates

### Common Pitfalls to Avoid
1. Don't add features without clear user benefit
2. Don't show the same information in multiple places
3. Don't make UI elements that aren't self-explanatory
4. Always test with both weighted and bodyweight exercises
5. Verify database saves are actually happening (check logs)

## Phase 3.2: PR Detection & Celebration System Implementation (2025-07-04)

Successfully implemented the complete PR detection and celebration system with real-time feedback during workouts.

### Key Components Built:

**PR Detection Infrastructure:**
- **PersonalRecord entity** with support for multiple PR types (Weight, Reps, Volume, 1RM)
- **PRDetectionService** using Brzycki formula for accurate 1RM calculations
- **Real-time PR detection** integrated into `markSetCompleted` workflow
- **Comprehensive logging** for debugging PR detection logic

**Celebration & UI:**
- **PRCelebrationDialog** with confetti animations and haptic feedback
- **Streamlined UI** showing only 1RM PRs (filtered from 4 types)
- **Single exit flow** with "Continue Workout" and "Share" buttons
- **Removed duplicate exit options** (X button, outside-tap dismiss)

**1RM Profile Integration:**
- **Baseline-aware confidence calculation** for 1RM profile updates
- **Context-driven logic** that considers stored 1RM for credibility assessment
- **Reduced RPE dependency** - no RPE required when baseline exists
- **Intelligent thresholds** based on improvement percentage vs stored max

### Critical Bug Fixes:

**Timing Issues:**
- **PR detection timing**: Moved from before to after database save
- **Weight comparison bug**: Removed `estimated1RM > weight` condition that broke 1-rep PRs
- **Confidence calculation**: Fixed over-reliance on RPE, added baseline context

**Data Issues:**
- **Database cleanup**: Only save 1RM PRs to prevent "10 PRs everywhere" problem
- **History PR loading**: Fixed missing `prCount` field in HistoryViewModel refresh
- **Corrupted PR data**: Added startup cleanup for PersonalRecord table

**UI/UX Issues:**
- **Set input timing**: Fixed cursor position loss in weight input fields
- **Dialog integration**: Properly connected PRCelebrationDialog to WorkoutScreen
- **State management**: Fixed PR state clearing and celebration triggers

### Architecture Patterns:

**Dual Detection Systems:**
- **PRDetectionService**: Handles celebration PRs (immediate feedback)
- **GlobalProgressTracker**: Handles profile 1RM updates (workout completion)
- **Clear separation**: Celebrations vs profile updates serve different purposes

**Confidence-Based Logic:**
```kotlin
// New baseline-aware confidence calculation
val baseConfidence = when {
    reps == 1 -> 0.9f // Singles are very reliable
    reps in 2..3 -> 0.85f // Low reps are quite reliable
    reps in 4..6 -> 0.75f // Medium reps are decent
    else -> 0.6f // Higher reps less reliable
}

// Boost confidence when improving over known baseline
val finalConfidence = if (currentStoredMax != null && improvement >= 0.05f) {
    Math.min(0.95f, baseConfidence + 0.2f) // 5%+ improvement = high confidence
} else baseConfidence
```

**Key Technical Insights:**
- **Context matters more than RPE**: A 105kg lift vs 100kg stored max is obviously credible
- **Timing is critical**: PR detection must happen after database persistence
- **User feedback separation**: Immediate celebrations vs profile management decisions
- **Confidence should be additive**: Base confidence + context bonus + RPE bonus

## Phase 3.3: Achievements System Implementation (2025-07-04)

Successfully implemented the complete achievements system with 37 achievements across 4 categories and real-time detection.

### Key Components Built:

**Achievement Infrastructure:**
- **Achievement Definitions** - 37 achievements across Strength, Consistency, Volume, and Progress categories
- **UserAchievement entity** with context data storage and timestamp tracking
- **AchievementDetectionService** with intelligent condition checking and duplicate prevention
- **Real-time achievement detection** integrated into workout completion workflow

**Awards UI System:**
- **Complete Awards tab** in Analytics screen with progress overview
- **Recent Unlocks carousel** highlighting latest achievements
- **Category filtering** with achievement counts per category
- **Separated sections** for unlocked ("Your Achievements") vs locked ("Available to Unlock")
- **Achievement cards** with unlock state visualization

**Data Integration:**
- **AchievementSummary** with complete progress tracking and category breakdown
- **AnalyticsViewModel** integration for real-time achievement data loading
- **Repository pattern** with proper service delegation
- **Database queries** optimized for achievement state management

### Achievement Categories:

**Strength Milestones:** 9 achievements (100kg Squat, 2x Bodyweight Deadlift, 80kg Bench, etc.)
**Consistency Streaks:** 7 achievements (7-day, 30-day, 90-day workout streaks)
**Volume Records:** 11 achievements (10-Ton Club, Century Sets, High Volume Training)
**Progress Medals:** 10 achievements (25% Strength Gain, Plateau Breaker, Linear Progression)

### Technical Implementation:

**Real-time Detection:**
```kotlin
// Runs after every workout completion
val newAchievements = repository.checkForNewAchievements(userId, workoutId)
if (newAchievements.isNotEmpty()) {
    // Update UI state for celebration
    _workoutState.value = state.copy(
        pendingAchievements = newAchievements,
        shouldShowAchievementCelebration = true
    )
}
```

**Efficient Data Loading:**
- Achievement summary with all unlocked IDs and recent unlocks
- Category-based progress tracking with counts
- Lazy loading of achievement data in Analytics tab
- Cache-friendly data structure for responsive UI

### Architecture Benefits:
- **Motivational feedback**: Immediate achievement detection provides workout completion rewards
- **Progress visualization**: Clear category breakdown shows advancement areas
- **Extensible system**: Easy to add new achievements without code changes
- **Clean separation**: Detection logic separate from UI rendering logic

## Phase 3.4: Insights Engine Implementation (2025-07-04)

Completed data-driven insights system providing intelligent training analysis without misleading "AI" claims.

### Key Components:
- **ProgressInsight entity** with priority system and autoGenerate primary key
- **InsightGenerationService** with 6 insight categories (Progress, Plateaus, Consistency, Volume, RPE, Recovery)
- **Complete Insights tab** in Analytics with filtering and read/unread state
- **Actionable insights** with specific recommendations (deload, reduce volume, etc.)

### Critical Fixes Applied:
- **Primary key constraint**: Added `autoGenerate = true` to prevent duplicate ID errors
- **History refresh bug**: Fixed stale data by adding auto-refresh on navigation
- **Database version**: Updated to v34 to reflect schema changes

### Technical Lessons:
- **No assumptions**: Don't invent problems (empty workouts) when user provides clear repro steps
- **Simple solutions first**: History not updating was just missing refresh, not complex data issue
- **Database versioning**: Always increment version when changing entity schemas

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