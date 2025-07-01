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

## Common Commands

- Build: `./gradlew assembleDebug`
- Lint: `./gradlew lint`
- Install: `./gradlew installDebug`

## Database Strategy

- Using `fallbackToDestructiveMigration()` during development
- Modify existing entities directly - no V2 classes or versioning
- Progressive overload data stored in JSON fields for flexibility

## Current Focus: Progressive Overload System

**Phase 1** ✅: Foundation with StrongLifts 5x5 (Exercise validation, weight calculation logging, basic progression)

**Phase 2** 🎯: Deload logic and failure tracking for StrongLifts

**Phase 3**: Freestyle workout intelligence using programme data

**Phase 4**: Advanced programme support (5/3/1, nSuns, etc.)

**Key Principle**: Perfect one programme completely before expanding to others.