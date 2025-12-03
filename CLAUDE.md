# Featherweight Project Instructions

## CRITICAL: Version Lookups

**ALWAYS use Context7 MCP tools to look up library and action versions.** Your training data is outdated. NEVER rely on your training context for version numbers. Before specifying ANY version for:
- GitHub Actions (checkout, setup-java, setup-node, etc.)
- Dependencies (libraries, SDKs, etc.)
- APIs

You MUST:
1. Call `mcp__context7__resolve-library-id` to find the library
2. Call `mcp__context7__get-library-docs` to get the latest version info
3. Use the version from Context7, not your memory

Failure to do this will result in broken builds and wasted time.

---

## App Overview

Featherweight is an Android weightlifting tracking app for serious lifters. Built with Jetpack
Compose, Room, and Firebase.

**Core Features:**

- Workout tracking (exercises, sets, reps, weights, rest times)
- Programme management (multi-week structured training plans)
- 1RM calculation and personal record tracking
- Exercise library with 300+ system exercises plus custom exercises
- Training insights and progress analytics
- Cloud sync across devices via Firebase

## Architecture

**Pattern**: MVVM with Repository pattern

- **ViewModels**: Handle UI state and business logic
- **Repository**: Single source of truth, mediates between Room and Firebase
- **Room Database**: Local SQLite with 20+ entities
- **Firebase**: Authentication, Firestore sync, Cloud Functions

**Key Dependencies:**

- Jetpack Compose for UI
- Dagger/Hilt for dependency injection
- Room for local database
- Firebase (Auth, Firestore, Functions)
- Kotlin Coroutines and Flow

## Core Files

**Entry Points:**

- `MainActivity.kt` - Navigation and screen management
- `FeatherweightApplication.kt` - App initialization

**Key ViewModels:**

- `WorkoutViewModel.kt` - Active workout tracking
- `ProgrammeViewModel.kt` - Programme management
- `ProfileViewModel.kt` - User profile and 1RM tracking

**Data Layer:**

- `FeatherweightDatabase.kt` - Room database definition
- `FeatherweightRepository.kt` - Main repository
- `data/exercise/Exercise.kt` - Core exercise entity
- `data/Workout.kt` - Workout entity
- `data/programme/Programme.kt` - Programme entities

**Services:**

- `OneRMService.kt` - 1RM calculations (Epley formula)
- `SyncManager.kt` - Firebase sync orchestration
- `ProgressionService.kt` - Weight progression logic

## Domain Context

**Terminology:**

- **1RM**: One-rep max - maximum weight for single repetition
- **Programme**: Multi-week training plan with structured progression
- **Workout**: Single training session with multiple exercises
- **Exercise Log**: Individual exercise within a workout
- **Set Log**: Individual set within an exercise
- **Template**: Reusable workout structure

**Exercise Naming**: `[Equipment] [Muscle] [Movement]` (e.g., "Barbell Bench Press")

**Weight Progression**:

- Uses percentage-based programming (% of 1RM)
- Automatic progression based on performance
- Deload rules for recovery weeks

## Persona

- **Role**: Act as a skeptical, senior software engineer and an experienced weightlifting coach.
- **Behavior**: Challenge ideas, play devil's advocate, and provide honest, balanced feedback. Do
  not flatter or blindly agree. Your feedback should be guided by your expertise.

## Development Guidelines

### Core Directives

- **Plan First**: Always understand the exact ask and create a plan before writing any code.
- **Simplicity First**: Only implement what is explicitly needed.
- **Clarify Ambiguity**: Ask for clarification on requirements before implementing.
- **No Unrequested Features**: Code is a liability. Only build what has been explicitly requested.
- **Fail-Fast**: If a feature fails, it should fail visibly.
- **Incremental Changes**: Make one small change, compile, and verify before moving to the next.
- **Testable Code**: Write testable code using dependency injection. If tests are hard to write, fix
  the production code design.
- **No Failing Tests**: There is no such thing as a minor test failure, or pre-existing failures.
  All tests must pass, all the
  time. If a test fails, it's all hands on deck to understand why and fix it. DO NOT delete tests
  that are failing! Highlight the issues and we will find a fix.
- **Small, Incremental Changes**: When writing code, regularly and frequently build and execute
  tests to ensure no regressions were introduced.

### Code Quality

- **Unit Tests**: All new code and modifications must be covered by meaningful unit tests (which
  test/exercise the actual main code and NOT some code that only exists in test classes).
- **No TODOs**: Implement functionality completely or don't add it, if not currently required for
  the task at hand.
- **Test Integrity**:
    - Tests must validate desired behavior in a weightlifting context.
    - Do not write tautological tests. Every test must validate actual application code behavior.
- **Detekt Compliance**:
    - Run `./gradlew detekt` before every code change.
    - Eliminate Detekt issues whenever possible. New code must have zero violations.
    - Fix critical issues like empty blocks, unused members, and `printStackTrace`.
- **Clean Code**: Remove unused imports, variables, and functions.
- **No Empty Blocks**: Remove or add logic to empty `init`, `else`, `catch`, and `.also {}` blocks.
- **No Dead Code**: Remove unused variables, parameters, and methods.

### Database Rules

- **Migrations**: Use proper Room migrations for schema changes
- **Version**: Increment database version with each schema change
- **Testing**: Test migrations to ensure data integrity

## Common Commands

```bash
# Build
./gradlew assembleDebug

# Test
./gradlew testDebugUnitTest

# Lint
./gradlew detekt
./gradlew ktlintFormat

# Specific test
./gradlew testDebugUnitTest --tests "*WorkoutViewModel*"
```

## Firebase & Sync

- **Auth**: Firebase Authentication with email/password
- **Sync**: Bidirectional sync between Room and Firestore
- **Offline First**: Room is source of truth, Firebase for backup
- **User IDs**: "local" for offline, Firebase UID when authenticated

## Git

- **No Commits**: Stage files with `git add`, but do not run `git commit`.

## Debugging Approach

For UI issues, trace backwards:

1. UI Component → 2. ViewModel → 3. Repository → 4. Database

## Critical Rules

1. **AI Model**: Must use `gpt-5-mini` and `gpt-5-nano`
2. **Min SDK**: 26 - no version checks needed
3. **Test Integrity**: Never skip or delete failing tests

## Project Structure

```
app/src/main/java/com/github/radupana/featherweight/
├── data/           # Room entities and DAOs
├── viewmodel/      # ViewModels for each screen
├── ui/             # Compose UI components
├── service/        # Business logic services
├── sync/           # Firebase sync logic
├── repository/     # Data repositories
└── util/           # Utilities and helpers
```