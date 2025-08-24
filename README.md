# Featherweight

Featherweight is a sophisticated weightlifting "Super App" for Android, designed for intermediate and advanced lifters. It provides a comprehensive, data-driven experience for experienced weightlifters who are familiar with training principles and want detailed tracking, analysis, and program management.

## Core Features

### 🏋️ Workout Management
- **Freestyle Workouts**: Create and log custom workouts with intelligent exercise selection
- **Programme Support**: Import, create, and follow structured training programmes with week-by-week progression
- **Template System**: Pre-built workout templates for common training styles
- **Smart Weight Suggestions**: AI-powered weight recommendations based on your history and 1RM calculations
- **Exercise Swapping**: Intelligent exercise substitution with muscle group and movement pattern matching
- **Real-time Workout Timer**: Track workout duration with automatic set timing
- **Rest Timer**: Configurable rest periods between sets

### 📊 Advanced Analytics & Insights
- **Personal Record Tracking**: Automatic PR detection and celebration
- **Exercise Performance Analysis**: Detailed progress tracking with visual charts
- **Training Analysis**: AI-powered insights on training patterns, volume, intensity, and recovery
- **Volume Tracking**: Monitor training volume across exercises and time periods
- **Intensity Zone Analysis**: Track training intensity distribution
- **Progressive Overload Monitoring**: Ensure consistent progression over time

### 📈 Progress Tracking
- **1RM Calculator**: Multiple calculation methods (Epley, Brzycki, etc.) with historical tracking
- **Exercise History**: Comprehensive log of all sets, reps, and weights
- **Visual Progress Charts**: Line charts, bar charts, and trend analysis
- **Strength Progress**: Track improvements in major lifts and accessory exercises
- **Calendar View**: Visual workout history with completion streaks

### 🤖 AI Integration
- **Programme Parser**: Parse text-based training programmes using OpenAI GPT models
- **Intelligent Suggestions**: Exercise recommendations based on training history
- **Weight Calculation**: Automatic weight suggestions based on percentage work and RPE
- **Exercise Mapping**: AI-powered exercise name matching and correlation

### 🗃️ Data Management
- **Exercise Database**: 500+ exercises with detailed categorization, muscle groups, and equipment types
- **Exercise Correlation**: Smart exercise relationships for progression and alternatives
- **Import/Export**: JSON-based workout and program data export
- **Backup Support**: Full database backup and restore functionality

## Target Audience

**Primary Users**: Intermediate to advanced lifters who:
- Understand basic training principles (sets, reps, RPE, periodization)
- Want detailed tracking without hand-holding
- Follow structured programmes or create their own workouts
- Value data-driven training decisions
- Don't need exercise tutorials or beginner guidance

**Core Philosophy**: The app prioritizes quality and functionality over simplicity. It follows a "fail-fast" approach, providing clear error states rather than degraded functionality when systems fail.

## Technical Architecture

### Core Technologies
- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel) with Repository pattern
- **Database**: Room (SQLite) with normalized schema
- **Asynchronous Operations**: Kotlin Coroutines and Flow
- **Dependency Injection**: Manual DI (no framework)
- **Navigation**: Compose-based state management
- **Serialization**: kotlinx.serialization for JSON
- **HTTP Client**: Retrofit + OkHttp for API calls
- **AI Integration**: OpenAI API for text parsing and analysis

### Database Schema
The app uses a normalized Room database with the following key entities:

**Core Workout Entities**:
- `Workout`: Main workout sessions with metadata
- `ExerciseLog`: Individual exercises within workouts
- `SetLog`: Individual sets with reps, weight, RPE, and completion status

**Exercise System**:
- `ExerciseCore`: Base exercise groupings (e.g., "Squat")
- `ExerciseVariation`: Specific variations (e.g., "Barbell Back Squat")
- `VariationAlias`: Alternative names for exercises
- `VariationMuscle`: Muscle group mappings
- `VariationInstruction`: Setup and execution instructions

**Programme System**:
- `Programme`: Multi-week training programs
- `ProgrammeWeek`: Individual weeks within programmes
- `ProgrammeWorkout`: Structured workouts within weeks
- `ProgrammeProgress`: User progress tracking

**Analytics & Tracking**:
- `PersonalRecord`: PR tracking with automatic detection
- `OneRMHistory`: 1RM calculations and history
- `TrainingAnalysis`: AI-generated training insights
- `ExerciseCorrelation`: Exercise relationships and alternatives

### Key Services
- **ProgrammeTextParser**: AI-powered programme parsing using OpenAI
- **OneRMService**: Multiple 1RM calculation formulas
- **ProgressAnalyticsService**: Training pattern analysis
- **IntelligentSuggestionEngine**: Smart exercise and weight recommendations
- **PRDetectionService**: Automatic personal record detection
- **WorkoutExportService**: Data export in multiple formats

## Project Structure

```
app/src/main/java/com/github/radupana/featherweight/
├── data/                    # Data entities and models
│   ├── exercise/           # Exercise-related entities
│   ├── programme/          # Programme system entities
│   └── profile/           # User profile and 1RM data
├── dao/                    # Room database access objects
├── repository/             # Data layer with business logic
├── service/               # Business logic services
├── viewmodel/             # UI state management
├── ui/
│   ├── screens/           # Main app screens
│   ├── components/        # Reusable UI components
│   ├── dialogs/          # Modal dialogs
│   └── theme/            # Material Design theme
├── utils/                 # Utility classes
└── worker/               # Background tasks
```

## Key Features Deep Dive

### Programme Import System
Users can import training programmes by:
1. Pasting text-based programmes (e.g., from PDFs, websites)
2. AI parsing converts unstructured text to structured workouts
3. Exercise mapping matches programme exercise names to database
4. Template editing allows customization before importing
5. Automatic weight calculation based on user's 1RMs

### Smart Exercise Selection
- Searchable exercise database with 500+ exercises
- Category filtering (Legs, Push, Pull, etc.)
- Equipment-based filtering
- Recent exercise prioritization
- Alternative exercise suggestions based on muscle groups

### Workout Flow
1. **Start**: Choose freestyle, programme workout, or template
2. **Exercise Selection**: Add exercises with smart suggestions
3. **Set Logging**: Log sets with weight, reps, RPE, and notes
4. **Real-time Timer**: Track workout duration and rest periods
5. **Completion**: Automatic PR detection and workout summary
6. **Analysis**: Generate insights and update progress tracking

### Analytics Dashboard
- **Recent Highlights**: Latest PRs, workout streak, weekly summary
- **Exercise Progress**: Individual exercise performance over time
- **Training Patterns**: Volume, intensity, and frequency analysis
- **AI Insights**: Personalized training recommendations
- **Calendar View**: Visual workout history and completion tracking

## Development

### Build Requirements
- Android Studio (latest stable)
- Kotlin 1.9+
- Android SDK 34+
- Minimum SDK 26 (Android 8.0)

### Setup
1. Clone the repository
2. Create `local.properties` file with your OpenAI API key:
   ```
   OPENAI_API_KEY=your_api_key_here
   ```
3. Build and run:
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

### Code Quality
The project maintains high code quality standards:
- **Detekt**: Static analysis with zero-tolerance policy for new issues
- **KtLint**: Kotlin code formatting
- **Custom Rules**: Enforced via `detekt-config.yml`
- **No TODOs**: All functionality must be complete
- **Clean Code**: No unused imports, variables, or dead code

### Testing
- Unit tests for business logic
- UI tests using Compose testing framework
- Integration tests for database operations
- Cucumber BDD tests for user flows

### Key Commands
```bash
# Build
./gradlew assembleDebug

# Code quality
./gradlew detekt          # Static analysis
./gradlew ktlintFormat    # Format code
./gradlew lint           # Android lint

# Testing
./gradlew test           # Unit tests
./gradlew connectedAndroidTest  # UI tests

# Install
./gradlew installDebug
```

## Architecture Decisions

### Why No Dependency Injection Framework?
The app uses manual DI to:
- Reduce build complexity and compile time
- Maintain explicit dependencies
- Avoid framework-specific abstractions
- Keep the codebase simple and readable

### Why Room over Other Databases?
- Type-safe SQL queries at compile time
- Excellent integration with Kotlin Coroutines
- Migration support for schema evolution
- Performance optimizations for complex queries

### Why Manual Navigation vs NavComponent?
- Direct control over navigation state
- Simplified data passing between screens
- No XML configuration required
- Better integration with Compose state management

### AI Integration Strategy
- OpenAI API for complex text parsing (programme import)
- Local algorithms for real-time suggestions
- Hybrid approach: AI for intelligence, local processing for speed
- Graceful degradation when AI services are unavailable

## Future Roadmap

### Planned Features
- **Social Features**: Share workouts and programmes with friends
- **Advanced Analytics**: Machine learning for injury prevention
- **Wearable Integration**: Apple Watch and Android Wear support
- **Video Integration**: Exercise form videos and analysis
- **Nutrition Tracking**: Basic macro tracking integration
- **Cloud Sync**: Multi-device synchronization

### Technical Improvements
- **Modularization**: Split into feature modules
- **Compose Multiplatform**: iOS version
- **Performance**: Database query optimization
- **Offline-First**: Better offline capabilities

## Contributing

This is a personal project, but feedback and suggestions are welcome through GitHub issues.

## License

See LICENSE file for details.