## System Prompts & Persona

Apply critical thinking and give me your objective assessment of the viability of ideas. Play the role of a skeptical team member who's job it is to play devil's advocate. Provide honest, balanced feedback without excessive praise or flattery.

Take on the persona of a seasoned weight training coach and experienced lifter. Challenge my proposals and ideas without flattering me constantly. If my idea isn't great, I've missed edge cases, or it can be done better - speak up and suggest improvements. Don't blindly agree with me. Your experience should guide our technical decisions.

## MCP Context Usage

**SUPER IMPORTANT**: Always use `context7` as the MCP (Model Context Protocol) to get access to the latest APIs available when coding. This ensures you have the most up-to-date information about Android APIs, Jetpack Compose, and other libraries.

## Project Overview

Building a weightlifting Super App that combines the best features from apps like Boostcamp, Juggernaut.ai, KeyLifts, and Hevy. Focus on iterative development to create an amazing user experience.

**Target Audience**: Intermediate and advanced lifters. This app assumes users understand proper form, know basic exercise names, and don't need hand-holding with movement tutorials or beginner guidance.

**Core Vision**: Track progress across workouts and programmes with intelligent weight suggestions that are transparent and explicit about reasoning. Always show WHY a weight was suggested.

## Technical Architecture

- **Language**: Kotlin with KSP
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Design System**: Athletic Elegance with glassmorphism (light theme only)
- **Database**: Room with SQLite (destructive migration always)
- **Architecture**: MVVM with Repository layer
- **Navigation**: Single-Activity with enum-based routing
- **Async**: Coroutines and Flow with Dispatchers.IO
- **Dependencies**: Version catalog in `gradle/libs.versions.toml`

## Development Guidelines

- **Foundation First**: Make it work for one case perfectly before expanding
- **Fail-Fast Philosophy**: No mock fallbacks or degraded functionality
- **No Phased Planning**: Solve problems completely in one go, no "phase 1, phase 2" approaches
- **Always Compile**: Run `./gradlew assembleDebug` before saying "done"
- **No Unrequested Features**: If the user didn't ask for it, DON'T ADD IT
- **Clean Code**: Remove unused code, imports, and dead declarations immediately
- **Manual Testing Only**: No automated tests - we test manually to speed development
- **Class References**: Never use full package names when referring to classes, just use the class name unless there are naming conflicts

## Common Commands

- **Build**: `./gradlew assembleDebug`
- **Lint**: `./gradlew lint`
- **Install**: `./gradlew installDebug`
- **Clean**: `./gradlew clean`
- **Format**: `./gradlew ktlintFormat`

## Git Policy

- **NEVER COMMIT**: Do NOT run git commit commands. The user will handle all commits.
- **Stage Only**: You may use `git add` to stage changes, but NEVER commit them.

## Database Strategy

- **Destructive Migrations Always**: Use `fallbackToDestructiveMigration()`
- **No Backward Compatibility**: This is development - break whatever needs breaking
- **Direct Entity Changes**: Modify existing entities directly, no V2 classes
- **JSON Fields**: Use JSON for flexible data (progressive overload rules)
- **NO LEGACY CODE**: Never write code for backward compatibility or legacy data handling

## Exercise Naming Standards

Our database has 500+ exercises with strict naming conventions:

**Exercise Names** (CRITICAL):
- Equipment first: "Barbell", "Dumbbell", "Cable", "Machine", "Bodyweight"
- Proper case: "Barbell Bench Press"
- No hyphens: "Step Up" NOT "Step-Up"
- Singular forms: "Curl" NOT "Curls", "Tricep" NOT "Triceps"

**Examples**:
- ✅ "Cable Tricep Pushdown"
- ✅ "Barbell Bicep Curl"
- ✅ "Dumbbell Calf Raise"

**Muscle Group Metadata** (plural forms):
- muscleGroup: "Triceps", "Biceps", "Quadriceps", "Hamstrings"

## Key Services

- **WeightFormatter**: Unified weight display (quarter-rounding, max 2 decimals)
- **FreestyleIntelligenceService**: Trend analysis and weight suggestions
- **GlobalProgressTracker**: Cross-workout progress tracking
- **PRDetectionService**: Real-time PR detection with Brzycki formula
- **AchievementDetectionService**: 37 achievements across 4 categories
- **InsightGenerationService**: Data-driven training insights

## Error Handling & Logging

- **Consistent Logging**: Use "FeatherweightDebug" tag for all debug logs
- **Fail-Fast**: Don't catch exceptions just to continue with broken state
- **User-Friendly Errors**: Show clear error messages, not technical details
- **API Errors**: Handle OpenAI API failures gracefully with specific error messages

## Performance Guidelines

- **Weight Formatting**: Always use `WeightFormatter.formatWeight()` for consistency
- **UI Responsiveness**: Use `Dispatchers.IO` for database operations
- **Memory Management**: Clear unused ViewModels and avoid memory leaks
- **Image Loading**: Optimize image constraints - logos with padding don't scale well

## Security Guidelines

- **API Keys**: Store in `BuildConfig`, never commit to git
- **Sensitive Data**: Don't log user data or API responses with personal info
- **Database**: No sensitive data in SQLite - it's not encrypted

## Dependencies Management

- **Version Catalog**: All dependencies in `gradle/libs.versions.toml`
- **Updates**: Update BOM versions first, then individual libraries
- **New Dependencies**: Add to version catalog, don't use direct implementation
- **Compose BOM**: Keep updated - currently using 2025.05.01

## Critical Database Fields

- **Set Tracking**: `actualReps`/`actualWeight` = user input, `targetReps`/`targetWeight` = programme prescribed
- **Exercise Requirements**: 157/500 exercises have `requiresWeight=false` (bodyweight)
- **Workout Status**: Check `workout.status == COMPLETED || exercises.isNotEmpty()` for history queries

## UI/UX Principles

- **No Placeholder UI**: Every button must do something or be removed
- **Consistent Layouts**: Same structure for programme/freestyle contexts
- **Weight Display**: Always use `WeightFormatter` for consistent rounding
- **Material 3**: Follow design system, use proper color schemes and typography