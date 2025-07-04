# Development Commands

## Build Commands
- `./gradlew assembleDebug` - Build debug APK
- `./gradlew lint` - Run linting
- `./gradlew installDebug` - Install debug APK on device

## Testing Commands
- `./gradlew test` - Run unit tests
- `./gradlew connectedAndroidTest` - Run instrumented tests

## Code Quality
- `./gradlew ktlintCheck` - Check code style
- `./gradlew ktlintFormat` - Auto-format code

## Database
- Uses destructive migrations during development
- Database schema changes nuke and rebuild automatically
- No backward compatibility concerns in development

## System Commands (Darwin)
- `git` - Version control
- `find` - Search files
- `grep` - Search content
- `ls` - List directory contents
- Standard Unix commands available