# Featherweight Project Knowledge Base
*Last Updated: 2025-06-03*

## 🎯 Project Vision
**Super App for Weightlifting** - Combining best features from Hevy, Boostcamp, Juggernaut.ai, KeyLifts
- Goal: Iteratively build amazing weightlifting tracking app
- Platform: Android (Kotlin + Compose)
- Approach: Start with core features, expand systematically

## 🏗️ Current Technical Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM pattern
- **Database**: Room (SQLite) with fallback destructive migration
- **Navigation**: Compose Navigation (currently basic)
- **Dependency Injection**: None yet (consider Hilt later)

## 📊 Current Data Model (Room Database v3)
```kotlin
// Core entities with relationships
Workout (id, date, notes)
  ↓ 1:many
ExerciseLog (id, workoutId, exerciseName, exerciseOrder, supersetGroup?, notes?)
  ↓ 1:many  
SetLog (id, exerciseLogId, setOrder, reps, weight, rpe?, tag?, notes?, isCompleted, completedAt?)

// Key features in data model:
- Foreign key cascades (delete workout = delete all exercises/sets)
- Set completion tracking with timestamps
- RPE (Rate of Perceived Exertion) support
- Flexible notes/tags system
- Superset grouping capability
```

## 📱 Current UI State & Features
### Implemented Screens:
1. **HomeScreen** - Welcome screen with action buttons
   - "Start Freestyle Workout" (primary CTA)
   - "Start From Template" (placeholder dialog)
   - Clean Material3 design

2. **WorkoutScreen** - Active workout tracking
   - Add exercises to current workout
   - Add/edit/delete sets per exercise
   - Set completion checkboxes with visual feedback
   - Progress indicator (completed sets / total sets)
   - Swipe-to-delete functionality
   - Edit set dialog (reps, weight, RPE)
   - Copy last set functionality
   - Expandable exercise cards

### Technical Implementation Details:
- **WorkoutViewModel**: Manages workout state with StateFlow
- **Repository Pattern**: Clean data access layer
- **Compose Best Practices**: Proper state management, recomposition
- **Material3 Theming**: Custom color scheme, typography
- **Error Handling**: Basic validation, user-friendly dialogs

## 🗺️ Agreed Navigation Architecture
**DECISION MADE**: Bottom Navigation (5 tabs) - Train-centric approach
```
🏠 Home     - Dashboard, quick actions, recent stats
🏋️ Train    - Start workouts (CENTER/PRIMARY ACTION)  
📊 History  - Past workouts, calendar view
📈 Analytics - Progress charts, PRs, trends
👤 Profile  - Settings, achievements, social
```

**Key Principle**: Train tab is the hero - everything else supports workout experience

## 🎯 Next Immediate Priorities
1. **Navigation Refactor**: Implement 5-tab bottom navigation
2. **Home Screen Enhancement**: Transform into proper dashboard
3. **History Screen**: Basic workout history list
4. **Analytics Foundation**: Simple progress tracking
5. **Profile Basics**: Settings and user info

## 🚀 Feature Roadmap (Prioritized)
### Phase 1 - Core Experience ⭐ 
- [x] Basic workout tracking (DONE)
- [x] Set completion (DONE)
- [ ] 5-tab navigation structure
- [ ] Dashboard home screen
- [ ] Workout history view
- [ ] Basic user profile

### Phase 2 - Enhanced Tracking
- [ ] Workout templates system
- [ ] Exercise library with instructions
- [ ] Rest timer between sets
- [ ] Workout duration tracking
- [ ] Basic progress charts

### Phase 3 - Smart Features
- [ ] Auto-populate previous weights
- [ ] RPE-based recommendations
- [ ] Bodyweight/measurements tracking
- [ ] PR (Personal Record) detection
- [ ] Workout streak tracking

### Phase 4 - Advanced Features
- [ ] Social features (achievements, sharing)
- [ ] AI assistance/auto-regulation
- [ ] Program/periodization support
- [ ] Advanced analytics & trends
- [ ] Export/import functionality

## 🎨 Design Philosophy & Patterns
- **Material3 Design Language**: Modern, accessible, consistent
- **Fitness-focused Color Palette**: Blue primary, green for success/completion
- **Information Hierarchy**: Bold headers, clear CTAs, scannable content
- **Thumb-friendly**: Bottom navigation, large touch targets
- **Progressive Disclosure**: Expandable cards, clean initial states
- **Instant Feedback**: Loading states, success animations, error handling

## 💾 Key Code Patterns & Conventions
```kotlin
// ViewModel pattern for state management
class WorkoutViewModel : AndroidViewModel {
    private val _state = MutableStateFlow(...)
    val state: StateFlow<...> = _state
}

// Repository pattern for data access
class FeatherweightRepository {
    suspend fun insertWorkout(workout: Workout): Long
}

// Compose UI patterns
@Composable
fun ExerciseCard(
    exercise: ExerciseLog,
    expanded: Boolean,
    onExpand: () -> Unit,
    // ... other callbacks
) { ... }
```

## 🔧 Known Technical Debt
- Navigation: Currently basic screen switching in MainActivity
- Database: Uses destructive migration (fine for development)
- Testing: No unit/integration tests yet
- Error Handling: Basic, needs improvement for edge cases
- Performance: No optimization done yet (fine for current scope)

## 📝 Recent Decisions & Context
- **2025-06-03**: Decided on bottom navigation over drawer/FAB-only
- **Current Focus**: Get foundation solid before adding advanced features
- **Development Approach**: Iterative, validate core features first
- **User Experience**: Prioritize speed and simplicity for core workout flow

## 🔄 Context Bootstrapping Instructions
When starting new conversation:
1. Reference this knowledge base file
2. Current codebase is in Android Studio project (Kotlin + Compose)
3. We're in "foundation building" phase - focusing on navigation & core screens
4. Next immediate task: Implement 5-tab bottom navigation structure
5. Always consider: "Does this make starting/tracking a workout faster?"

## 📂 Project Structure Overview
```
app/src/main/java/com/github/radupana/featherweight/
├── data/              # Room entities, DAOs, database
├── viewmodel/         # ViewModels and Repository
├── ui/               # Compose screens and components
│   ├── theme/        # Material3 theming
│   ├── HomeScreen.kt
│   ├── WorkoutScreen.kt
│   └── ChooseTemplateDialog.kt
└── MainActivity.kt   # Navigation controller
```

---
*This file should be referenced at the start of each new conversation to maintain project continuity and prevent context loss.*
