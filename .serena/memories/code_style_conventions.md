# Code Style and Conventions

## Kotlin Style
- Uses ktlint for automatic formatting
- Standard Kotlin conventions
- Coroutines with proper scope management
- Flow for reactive data streams

## Architecture Patterns
- MVVM with Repository pattern
- Single Activity with Compose navigation
- ViewModels handle UI state and business logic
- Repository layer for data access
- Room database with DAOs

## Database Conventions
- Destructive migrations always during development
- Entity classes with proper annotations
- DAOs with suspend functions
- Use of `@Transaction` for complex queries

## UI Conventions
- Jetpack Compose with Material Design 3
- Athletic Elegance design system
- Glassmorphism effects
- Light theme only
- Consistent component patterns

## Naming Conventions
- Exercise names: `[Equipment] [Target/Muscle] [Movement]`
- File names: PascalCase for classes, camelCase for functions
- Database fields: snake_case in SQL, camelCase in Kotlin
- UI state classes end with `State` or `UiState`

## Error Handling
- Fail-fast philosophy
- No mock fallbacks or degraded functionality
- Comprehensive logging for debugging
- Proper exception handling in coroutines