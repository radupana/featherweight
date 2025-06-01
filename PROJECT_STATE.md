# Featherweight App — Project State

_Last updated: 2025-06-01_

---

## Data Model

- Workout
  - id: Long (PK)
  - date: LocalDateTime
  - notes: String?

- ExerciseLog
  - id: Long (PK)
  - workoutId: Long (FK)
  - exerciseName: String
  - exerciseOrder: Int
  - supersetGroup: Int? (nullable)
  - notes: String?

- SetLog
  - id: Long (PK)
  - exerciseLogId: Long (FK)
  - setOrder: Int
  - reps: Int
  - weight: Float
  - rpe: Float? (nullable)
  - tag: String? (nullable; e.g. "warmup", "drop")
  - notes: String? (nullable)
  - isCompleted: Boolean (added v3)
  - completedAt: String? (added v3; ISO8601 datetime)

---

## Key Features Implemented

- Modularized Compose navigation (MainActivity manages screen state)
- Welcome/Home screen:
  - Centered Featherweight logo/app name
  - Welcome message
  - “Start Freestyle Workout” and “Start From Template” buttons
  - Dummy “choose template” dialog (placeholder for templates)
- “Freestyle Workout” screen (user navigates only after choosing action)
- Add exercises and sets to a workout, with per-set completion checkbox
- **Swipe-to-delete sets** with UI feedback
- Linear progress indicator based on completed sets
- Room database with `fallbackToDestructiveMigration` for fast iteration
- All Compose UI modularized in `ui` package

---

## In Progress / North Star Vision

- Per-set completion (checkboxes + green highlight for done sets)
- Start rest timer after set completion, visually show timer
- Templates:
  - Template data model & structure
  - “Start from template” flow (prefills workout)
- Progress indicators (e.g. % complete, motivational cues)
- Sleek/animated Material3 theme and improved UX
- Insights: trends, PR tracking, bodyweight and nutrition integration

---

## Next Steps

- [x] Add `isCompleted` and `completedAt` to SetLog
- [x] Modularize UI (HomeScreen, WorkoutScreen, TemplateDialog)
- [x] Add onboarding/welcome flow (start workout via user action)
- [x] Add swipe-to-delete for sets in WorkoutScreen
- [ ] Allow adding/removing sets and editing set data
- [ ] Add rest timer logic and display (starts after set completion)
- [ ] Implement template data model and template-driven workout UI
- [ ] Add basic theme polish (animations, icons, color palette)
- [ ] Document known issues and TODOs (UI polish, navigation, error handling, etc.)

---

