package com.github.radupana.featherweight.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 1 to version 2.
 *
 * Changes:
 * - SetLog: Added triggeredUsageIncrement (Boolean) and previous1RMEstimate (Float?)
 *   for undo set completion tracking
 * - PersonalRecord: Added sourceSetId (String?) to link PRs to the set that triggered them,
 *   plus an index on sourceSetId for efficient queries
 */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add new columns to set_logs table
            db.execSQL(
                "ALTER TABLE set_logs ADD COLUMN triggeredUsageIncrement INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE set_logs ADD COLUMN previous1RMEstimate REAL",
            )

            // Add new column to personal_records table
            db.execSQL(
                "ALTER TABLE personal_records ADD COLUMN sourceSetId TEXT",
            )

            // Add index on sourceSetId for efficient PR lookups by set
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_personal_records_sourceSetId ON personal_records(sourceSetId)",
            )
        }
    }
