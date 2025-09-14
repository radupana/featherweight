package com.github.radupana.featherweight.data.migration

import android.database.Cursor
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from database version 3 to 4.
 * Adds userId fields to support authenticated and unauthenticated modes.
 */
class Migration3to4 : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        fun columnExists(
            tableName: String,
            columnName: String,
        ): Boolean {
            val cursor: Cursor = db.query("PRAGMA table_info($tableName)")
            var exists = false
            while (cursor.moveToNext()) {
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex >= 0 && cursor.getString(nameIndex) == columnName) {
                    exists = true
                    break
                }
            }
            cursor.close()
            return exists
        }

        fun addColumnIfNotExists(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            columnType: String = "TEXT",
        ) {
            if (!columnExists(tableName, columnName)) {
                db.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $columnType")
            }
        }

        addColumnIfNotExists(db, "Workout", "userId")
        addColumnIfNotExists(db, "ExerciseLog", "userId")
        addColumnIfNotExists(db, "SetLog", "userId")

        addColumnIfNotExists(db, "programmes", "userId")
        addColumnIfNotExists(db, "programme_weeks", "userId")
        addColumnIfNotExists(db, "programme_workouts", "userId")
        addColumnIfNotExists(db, "exercise_substitutions", "userId")
        addColumnIfNotExists(db, "programme_progress", "userId")

        addColumnIfNotExists(db, "user_exercise_maxes", "userId")
        addColumnIfNotExists(db, "one_rm_history", "userId")
        addColumnIfNotExists(db, "PersonalRecord", "userId")
        addColumnIfNotExists(db, "exercise_swap_history", "userId")
        addColumnIfNotExists(db, "exercise_performance_tracking", "userId")
        addColumnIfNotExists(db, "global_exercise_progress", "userId")

        addColumnIfNotExists(db, "training_analysis", "userId")
        addColumnIfNotExists(db, "parse_requests", "userId")

        addColumnIfNotExists(db, "exercise_cores", "createdByUserId")
        addColumnIfNotExists(db, "exercise_variations", "createdByUserId")

        createIndices(db)
    }

    private fun createIndices(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_Workout_userId ON Workout(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_ExerciseLog_userId ON ExerciseLog(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_SetLog_userId ON SetLog(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_programmes_userId ON programmes(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_variations_createdByUserId ON exercise_variations(createdByUserId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_cores_createdByUserId ON exercise_cores(createdByUserId)")
    }
}
