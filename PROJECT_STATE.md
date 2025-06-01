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

- Modularized Compose navigation (root MainActivity manages screen state)
- Welcome/Home screen with:
  - Centered Featherweight logo/app name
  - Friendly welcome message
  - “Start Freestyle Workout” and “Start From Template” buttons
  - Dummy “choose template” dialog (scaffold for future templates)
- “Freestyle Workout” screen stub (navigated only after user action)
- Room database with fallbackToDestructiveMigration for dev velocity
- All Compose UI is now separated into their own files in a ui package

---

## In Progress / North Star Vision

- Tick/check off sets as “done” (per-set completion in UI)
- Start rest timer after set completion, visually show rest state/timer
- Templates:
  - Template data model
  - “Start from template” flow (pre-fills workout structure)
- Progress indicators (e.g. % complete, motivational cues)
- Sleek/animated UI theme, Material3 look and feel
- Insights: trends, PR tracking, bodyweight and nutrition integration

---

## Next Steps

- [x] Add isCompleted and completedAt to SetLog
- [x] Modularize UI (HomeScreen, WorkoutScreen, TemplateDialog)
- [x] Add a proper onboarding/welcome flow (user chooses to start a workout)
- [ ] Update WorkoutScreen: add UI for logging sets, marking as “done,” visual progress
- [ ] Add rest timer logic and display (starts on set completion)
- [ ] Implement template data model and template-driven workout UI
- [ ] Add basic theme polish (animations, icons, cleaner color palette)
- [ ] Document known issues and TODOs (UI polish, navigation, error handling, etc.)

---

