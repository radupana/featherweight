# Featherweight App — Project State

_Last updated: [2025-06-01]_

---

## **Data Model**

- **Workout**
    - id: Long (PK)
    - date: LocalDateTime
    - notes: String?

- **ExerciseLog**
    - id: Long (PK)
    - workoutId: Long (FK)
    - exerciseName: String
    - exerciseOrder: Int
    - supersetGroup: Int? (nullable)
    - notes: String?

- **SetLog**
    - id: Long (PK)
    - exerciseLogId: Long (FK)
    - setOrder: Int
    - reps: Int
    - weight: Float
    - rpe: Float? (nullable)
    - tag: String? (nullable; e.g. "warmup", "drop")
    - notes: String? (nullable)
    - isCompleted: Boolean (new, v3)
    - completedAt: String? (new, v3; ISO8601 datetime)

---

## **Key Features Implemented**

- Create new workout sessions (workout = a gym session)
- For each workout: add multiple exercises (ExerciseLogs)
- For each exercise: add multiple sets (SetLogs)
- View workout history, exercise detail, set detail
- UI supports dynamic creation of exercises and sets
- Room database with fallbackToDestructiveMigration for dev

---

## **In Progress / North Star Vision**

- Per-set “completed” checkboxes
- Timer starts on set completion (rest timer)
- Template-driven workouts (user starts from a planned routine; pre-fills exercises, sets, reps, weights, RPEs)
- Progress tracking (show % complete for a workout, motivational visuals)
- Smart UI/UX (quick entry, animations, sleek theme)
- Insights: Trends, PR tracking, bodyweight integration, nutrition reminders

---

## **Next Steps**

- [x] Add `isCompleted` and `completedAt` fields to SetLog
- [ ] Update UI to support ticking sets as “done” and display progress
- [ ] Start timer when a set is completed; visually show rest state
- [ ] Template data model + “Start from template” UI
- [ ] Polish: Animation, iconography, slick theme

---

