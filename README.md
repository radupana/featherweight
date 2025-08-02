# Featherweight

Featherweight is a sophisticated weightlifting "Super App" for Android, designed for intermediate and advanced lifters. It focuses on providing a rich, data-driven experience for users who are already familiar with weight training principles.

**Target Audience**: Experienced lifters who do not require beginner guidance or exercise tutorials.

**Core Philosophy**: The app prioritizes a high-quality, robust experience. It follows a "fail-fast" approach, avoiding degraded functionality in favor of clear error states if a core system fails.

## Technical Architecture

The application is a native Android app built with a modern, MVVM-based architecture.

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material Design 3.
- **Asynchronous Operations**: Kotlin Coroutines and Flow.
- **Database**: Room for local persistence.
- **API Communication**: Retrofit and OkHttp for network requests.
- **Serialization**: `kotlinx.serialization` for JSON parsing.

## Development

To build the project, use the following command:

```bash
./gradlew assembleDebug
```