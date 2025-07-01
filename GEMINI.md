
# Gemini Project Context: Featherweight

This document provides a summary of the Featherweight project's context for Gemini, based on the existing `CLAUDE.md` file and an analysis of the codebase.

## 1. Project Overview

Featherweight is a sophisticated weightlifting "Super App" for Android, designed for intermediate and advanced lifters. It focuses on providing a rich, data-driven experience for users who are already familiar with weight training principles.

- **Target Audience**: Experienced lifters who do not require beginner guidance or exercise tutorials.
- **Core Philosophy**: The app prioritizes a high-quality, robust experience. It follows a "fail-fast" approach, avoiding degraded functionality in favor of clear error states if a core system fails.

## 2. Technical Architecture

The application is a native Android app built with a modern, MVVM-based architecture.

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material Design 3.
- **Asynchronous Operations**: Kotlin Coroutines and Flow.
- **Database**: Room for local persistence.
- **API Communication**: Retrofit and OkHttp for network requests.
- **Serialization**: `kotlinx.serialization` for JSON parsing.

### Key Dependencies:

- **AndroidX**: Core KTX, Lifecycle, Activity Compose, Room, ViewModel Compose, Core Splashscreen.
- **Compose**: Compose BOM, UI, Graphics, Tooling, Material3, Material Icons.
- **Networking**: Retrofit, OkHttp (with logging interceptor), and `retrofit-kotlinx-serialization-converter`.
- **Serialization**: `kotlinx-serialization-json`.
- **Linting**: Ktlint is configured.

## 3. Codebase Structure & Conventions

The project follows a standard Android project structure with a multi-package architecture.

- **Packages**: `ai`, `data`, `domain`, `repository`, `service`, `ui`, `utils`, `viewmodel`.
- **Naming Conventions**: Follows standard Kotlin and Android conventions. `CLAUDE.md` specifies a detailed and consistent naming convention for exercises.
- **Import Style**: Non-fully qualified class names are preferred.
- **Code Comments**: Comments are used sparingly, primarily to explain complex logic.

## 4. Key Architectural Components

- **`FeatherweightDatabase.kt`**: The Room database definition. It uses destructive migrations, which is suitable for the current development phase.
- **`FeatherweightRepository.kt`**: **(NEEDS REFACTORING)** A very large repository class that currently acts as a "God object," managing all data operations and business logic. This is the primary candidate for refactoring.
- **`AIProgrammeService.kt`**: Integrates with the OpenAI API (`gpt-4.1-mini`) for AI-powered program generation.
- **`WorkoutScreen.kt`**: A key UI component that demonstrates the use of Jetpack Compose, ViewModels, and StateFlow for building the user interface.
- **`exercises.json`**: A static JSON file in `app/src/main/assets` that contains a curated list of 500 exercises.

## 5. Development Workflow & Commands

- **Lint**: `./gradlew lint`
- **Build**: `./gradlew assembleDebug`
- **Install**: `./gradlew installDebug`

## 6. High-Priority Areas for Improvement

Based on the initial code review, the following areas should be prioritized for improvement:

1.  **Repository Refactoring**: The `FeatherweightRepository` must be broken down into smaller, more focused repositories to improve modularity, maintainability, and testability.
2.  **Introduction of a UseCase Layer**: A `domain` layer with UseCases should be introduced to encapsulate business logic, further separating concerns.
3.  **Dependency Injection**: The project would benefit from a dependency injection framework like Hilt to manage dependencies and improve testability.
4.  **UI Layer Refactoring**: The UI layer can be improved by decoupling ViewModels, using a more structured approach for dialog management, and extracting hardcoded strings into resources.
5.  **Consistent Error Handling**: A more consistent and robust error handling strategy should be implemented throughout the application.
