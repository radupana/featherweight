package com.github.radupana.featherweight.data.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            FeatherweightDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrate1To2_addsNewColumns() {
        // Create database with version 1
        helper.createDatabase(testDb, 1).apply {
            // Insert test data
            execSQL(
                """
                INSERT INTO set_logs (id, userId, exerciseLogId, setOrder, actualReps, actualWeight, isCompleted)
                VALUES ('set1', 'user1', 'exLog1', 1, 5, 100.0, 1)
                """.trimIndent(),
            )

            execSQL(
                """
                INSERT INTO personal_records (id, userId, exerciseId, weight, reps, recordDate,
                                             improvementPercentage, recordType, volume)
                VALUES ('pr1', 'user1', 'ex1', 100.0, 5, '2023-01-01T00:00:00', 10.0, 'WEIGHT', 500.0)
                """.trimIndent(),
            )

            close()
        }

        // Run migration
        val db =
            helper.runMigrationsAndValidate(
                testDb,
                2,
                true,
                MIGRATION_1_2,
            )

        // Verify set_logs columns exist
        val setLogCursor =
            db.query(
                """
                SELECT triggeredUsageIncrement, previous1RMEstimate
                FROM set_logs
                WHERE id = 'set1'
                """.trimIndent(),
            )

        assertThat(setLogCursor.moveToFirst()).isTrue()
        assertThat(setLogCursor.getColumnIndex("triggeredUsageIncrement")).isAtLeast(0)
        assertThat(setLogCursor.getColumnIndex("previous1RMEstimate")).isAtLeast(0)

        // Verify default values for new columns
        assertThat(setLogCursor.getInt(0)).isEqualTo(0) // triggeredUsageIncrement defaults to 0
        assertThat(setLogCursor.isNull(1)).isTrue() // previous1RMEstimate is nullable

        setLogCursor.close()

        // Verify personal_records column exists
        val prCursor =
            db.query(
                """
                SELECT sourceSetId
                FROM personal_records
                WHERE id = 'pr1'
                """.trimIndent(),
            )

        assertThat(prCursor.moveToFirst()).isTrue()
        assertThat(prCursor.getColumnIndex("sourceSetId")).isAtLeast(0)
        assertThat(prCursor.isNull(0)).isTrue() // sourceSetId is nullable

        prCursor.close()

        db.close()
    }

    @Test
    fun migrate1To2_preservesExistingData() {
        // Create database with version 1
        helper.createDatabase(testDb, 1).apply {
            // Insert test data in set_logs
            execSQL(
                """
                INSERT INTO set_logs (id, userId, exerciseLogId, setOrder, targetReps, targetWeight,
                                     targetRpe, actualReps, actualWeight, actualRpe, tag, notes,
                                     isCompleted, completedAt)
                VALUES ('set1', 'user1', 'exLog1', 1, 5, 100.0, 8.0, 5, 105.0, 7.5, 'PR',
                       'Great set!', 1, '2023-01-01T10:00:00')
                """.trimIndent(),
            )

            // Insert test data in personal_records
            execSQL(
                """
                INSERT INTO personal_records (id, userId, exerciseId, weight, reps, rpe, recordDate,
                                             previousWeight, previousReps, previousDate,
                                             improvementPercentage, recordType, volume, estimated1RM,
                                             notes, workoutId)
                VALUES ('pr1', 'user1', 'ex1', 105.0, 5, 7.5, '2023-01-01T10:00:00',
                       100.0, 5, '2022-12-01T10:00:00', 5.0, 'WEIGHT', 525.0, 120.0,
                       'New PR!', 'workout1')
                """.trimIndent(),
            )

            close()
        }

        // Run migration
        val db =
            helper.runMigrationsAndValidate(
                testDb,
                2,
                true,
                MIGRATION_1_2,
            )

        // Verify set_logs data preserved
        val setLogCursor =
            db.query(
                """
                SELECT id, userId, exerciseLogId, setOrder, targetReps, targetWeight, targetRpe,
                       actualReps, actualWeight, actualRpe, tag, notes, isCompleted, completedAt
                FROM set_logs
                WHERE id = 'set1'
                """.trimIndent(),
            )

        assertThat(setLogCursor.moveToFirst()).isTrue()
        assertThat(setLogCursor.getString(0)).isEqualTo("set1")
        assertThat(setLogCursor.getString(1)).isEqualTo("user1")
        assertThat(setLogCursor.getString(2)).isEqualTo("exLog1")
        assertThat(setLogCursor.getInt(3)).isEqualTo(1)
        assertThat(setLogCursor.getInt(4)).isEqualTo(5)
        assertThat(setLogCursor.getFloat(5)).isEqualTo(100.0f)
        assertThat(setLogCursor.getFloat(6)).isEqualTo(8.0f)
        assertThat(setLogCursor.getInt(7)).isEqualTo(5)
        assertThat(setLogCursor.getFloat(8)).isEqualTo(105.0f)
        assertThat(setLogCursor.getFloat(9)).isEqualTo(7.5f)
        assertThat(setLogCursor.getString(10)).isEqualTo("PR")
        assertThat(setLogCursor.getString(11)).isEqualTo("Great set!")
        assertThat(setLogCursor.getInt(12)).isEqualTo(1)
        assertThat(setLogCursor.getString(13)).isEqualTo("2023-01-01T10:00:00")

        setLogCursor.close()

        // Verify personal_records data preserved
        val prCursor =
            db.query(
                """
                SELECT id, userId, exerciseId, weight, reps, rpe, recordDate, previousWeight,
                       previousReps, previousDate, improvementPercentage, recordType, volume,
                       estimated1RM, notes, workoutId
                FROM personal_records
                WHERE id = 'pr1'
                """.trimIndent(),
            )

        assertThat(prCursor.moveToFirst()).isTrue()
        assertThat(prCursor.getString(0)).isEqualTo("pr1")
        assertThat(prCursor.getString(1)).isEqualTo("user1")
        assertThat(prCursor.getString(2)).isEqualTo("ex1")
        assertThat(prCursor.getFloat(3)).isEqualTo(105.0f)
        assertThat(prCursor.getInt(4)).isEqualTo(5)
        assertThat(prCursor.getFloat(5)).isEqualTo(7.5f)
        assertThat(prCursor.getString(6)).isEqualTo("2023-01-01T10:00:00")
        assertThat(prCursor.getFloat(7)).isEqualTo(100.0f)
        assertThat(prCursor.getInt(8)).isEqualTo(5)
        assertThat(prCursor.getString(9)).isEqualTo("2022-12-01T10:00:00")
        assertThat(prCursor.getFloat(10)).isEqualTo(5.0f)
        assertThat(prCursor.getString(11)).isEqualTo("WEIGHT")
        assertThat(prCursor.getFloat(12)).isEqualTo(525.0f)
        assertThat(prCursor.getFloat(13)).isEqualTo(120.0f)
        assertThat(prCursor.getString(14)).isEqualTo("New PR!")
        assertThat(prCursor.getString(15)).isEqualTo("workout1")

        prCursor.close()

        db.close()
    }

    @Test
    fun migrate1To2_createsIndex() {
        // Create database with version 1
        helper.createDatabase(testDb, 1).apply {
            close()
        }

        // Run migration
        val db =
            helper.runMigrationsAndValidate(
                testDb,
                2,
                true,
                MIGRATION_1_2,
            )

        // Query sqlite_master to verify index exists
        val cursor =
            db.query(
                """
                SELECT name
                FROM sqlite_master
                WHERE type = 'index'
                AND name = 'index_personal_records_sourceSetId'
                """.trimIndent(),
            )

        assertThat(cursor.moveToFirst()).isTrue()
        assertThat(cursor.getString(0)).isEqualTo("index_personal_records_sourceSetId")

        cursor.close()

        db.close()
    }

    @Test
    fun migrate1To2_allowsInsertingNewDataWithNewColumns() {
        // Create database with version 1
        helper.createDatabase(testDb, 1).apply {
            close()
        }

        // Run migration
        val db =
            helper.runMigrationsAndValidate(
                testDb,
                2,
                true,
                MIGRATION_1_2,
            )

        // Insert data with new columns
        val setLogValues =
            ContentValues().apply {
                put("id", "set2")
                put("userId", "user2")
                put("exerciseLogId", "exLog2")
                put("setOrder", 2)
                put("actualReps", 8)
                put("actualWeight", 80.0f)
                put("isCompleted", 1)
                put("triggeredUsageIncrement", 1)
                put("previous1RMEstimate", 95.5f)
            }

        val setLogRowId = db.insert("set_logs", SQLiteDatabase.CONFLICT_FAIL, setLogValues)
        assertThat(setLogRowId).isGreaterThan(0)

        // Verify the inserted data
        val setLogCursor =
            db.query(
                """
                SELECT triggeredUsageIncrement, previous1RMEstimate
                FROM set_logs
                WHERE id = 'set2'
                """.trimIndent(),
            )

        assertThat(setLogCursor.moveToFirst()).isTrue()
        assertThat(setLogCursor.getInt(0)).isEqualTo(1)
        assertThat(setLogCursor.getFloat(1)).isEqualTo(95.5f)

        setLogCursor.close()

        // Insert PR with sourceSetId
        val prValues =
            ContentValues().apply {
                put("id", "pr2")
                put("userId", "user2")
                put("exerciseId", "ex2")
                put("weight", 80.0f)
                put("reps", 8)
                put("recordDate", "2023-02-01T10:00:00")
                put("improvementPercentage", 8.0f)
                put("recordType", "WEIGHT")
                put("volume", 640.0f)
                put("sourceSetId", "set2")
            }

        val prRowId = db.insert("personal_records", SQLiteDatabase.CONFLICT_FAIL, prValues)
        assertThat(prRowId).isGreaterThan(0)

        // Verify the inserted data
        val prCursor =
            db.query(
                """
                SELECT sourceSetId
                FROM personal_records
                WHERE id = 'pr2'
                """.trimIndent(),
            )

        assertThat(prCursor.moveToFirst()).isTrue()
        assertThat(prCursor.getString(0)).isEqualTo("set2")

        prCursor.close()

        db.close()
    }

    @Test
    fun migrate1To2_verifyColumnTypes() {
        // Create database with version 1
        helper.createDatabase(testDb, 1).apply {
            close()
        }

        // Run migration
        val db =
            helper.runMigrationsAndValidate(
                testDb,
                2,
                true,
                MIGRATION_1_2,
            )

        // Verify set_logs column types using PRAGMA table_info
        val setLogCursor = db.query("PRAGMA table_info(set_logs)")

        val columns = mutableMapOf<String, String>()
        while (setLogCursor.moveToNext()) {
            val name = setLogCursor.getString(1) // name column
            val type = setLogCursor.getString(2) // type column
            val notNull = setLogCursor.getInt(3) // notnull column
            columns[name] = "$type|$notNull"
        }

        setLogCursor.close()

        // Verify triggeredUsageIncrement is INTEGER NOT NULL with default 0
        assertThat(columns["triggeredUsageIncrement"]).isEqualTo("INTEGER|1")

        // Verify previous1RMEstimate is REAL (nullable)
        assertThat(columns["previous1RMEstimate"]).isEqualTo("REAL|0")

        // Verify personal_records column types
        val prCursor = db.query("PRAGMA table_info(personal_records)")

        val prColumns = mutableMapOf<String, String>()
        while (prCursor.moveToNext()) {
            val name = prCursor.getString(1)
            val type = prCursor.getString(2)
            val notNull = prCursor.getInt(3)
            prColumns[name] = "$type|$notNull"
        }

        prCursor.close()

        // Verify sourceSetId is TEXT (nullable)
        assertThat(prColumns["sourceSetId"]).isEqualTo("TEXT|0")

        db.close()
    }

    @Test
    fun migrate1To2_handlesMultipleRecords() {
        // Create database with version 1 and insert multiple records
        helper.createDatabase(testDb, 1).apply {
            for (i in 1..10) {
                execSQL(
                    """
                    INSERT INTO set_logs (id, userId, exerciseLogId, setOrder, actualReps, actualWeight, isCompleted)
                    VALUES ('set$i', 'user1', 'exLog$i', $i, ${5 + i}, ${100.0 + i}, 1)
                    """.trimIndent(),
                )

                execSQL(
                    """
                    INSERT INTO personal_records (id, userId, exerciseId, weight, reps, recordDate,
                                                 improvementPercentage, recordType, volume)
                    VALUES ('pr$i', 'user1', 'ex$i', ${100.0 + i}, ${5 + i}, '2023-01-0${i}T00:00:00',
                           ${10.0 + i}, 'WEIGHT', ${(100.0 + i) * (5 + i)})
                    """.trimIndent(),
                )
            }

            close()
        }

        // Run migration
        val db =
            helper.runMigrationsAndValidate(
                testDb,
                2,
                true,
                MIGRATION_1_2,
            )

        // Verify all records migrated correctly
        val setLogCursor =
            db.query(
                """
                SELECT COUNT(*) FROM set_logs
                """.trimIndent(),
            )

        assertThat(setLogCursor.moveToFirst()).isTrue()
        assertThat(setLogCursor.getInt(0)).isEqualTo(10)

        setLogCursor.close()

        val prCursor =
            db.query(
                """
                SELECT COUNT(*) FROM personal_records
                """.trimIndent(),
            )

        assertThat(prCursor.moveToFirst()).isTrue()
        assertThat(prCursor.getInt(0)).isEqualTo(10)

        prCursor.close()

        // Verify new columns exist for all records
        val verifySetLogCursor =
            db.query(
                """
                SELECT id, triggeredUsageIncrement, previous1RMEstimate
                FROM set_logs
                ORDER BY id
                """.trimIndent(),
            )

        var count = 0
        while (verifySetLogCursor.moveToNext()) {
            count++
            assertThat(verifySetLogCursor.getInt(1)).isEqualTo(0) // default value
            assertThat(verifySetLogCursor.isNull(2)).isTrue() // nullable
        }

        assertThat(count).isEqualTo(10)
        verifySetLogCursor.close()

        db.close()
    }

    @Test
    fun migrate1To2_idempotency_indexCreationIsSafe() {
        // Create database with version 1
        helper.createDatabase(testDb, 1).apply {
            close()
        }

        // Run migration
        val db =
            helper.runMigrationsAndValidate(
                testDb,
                2,
                true,
                MIGRATION_1_2,
            )

        // Attempt to create the index again - should not fail due to IF NOT EXISTS
        // This simulates partial migration recovery
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_personal_records_sourceSetId ON personal_records(sourceSetId)",
        )

        // Verify index still exists and only one copy
        val cursor =
            db.query(
                """
                SELECT COUNT(*) FROM sqlite_master
                WHERE type = 'index'
                AND name = 'index_personal_records_sourceSetId'
                """.trimIndent(),
            )

        assertThat(cursor.moveToFirst()).isTrue()
        assertThat(cursor.getInt(0)).isEqualTo(1)

        cursor.close()
        db.close()
    }

    @Test
    fun migrate1To2_handlesLargeDataset() {
        // Create database with version 1 and insert large dataset
        // Using 1000 records to test migration performance without being too slow
        helper.createDatabase(testDb, 1).apply {
            // Batch insert for performance
            beginTransaction()
            try {
                for (i in 1..1000) {
                    execSQL(
                        """
                        INSERT INTO set_logs (id, userId, exerciseLogId, setOrder, actualReps, actualWeight, isCompleted)
                        VALUES ('set$i', 'user1', 'exLog${i % 100}', ${i % 10}, ${5 + (i % 5)}, ${100.0 + (i % 50)}, 1)
                        """.trimIndent(),
                    )
                }
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }

            close()
        }

        // Run migration - should complete without timeout
        val db =
            helper.runMigrationsAndValidate(
                testDb,
                2,
                true,
                MIGRATION_1_2,
            )

        // Verify all records migrated
        val cursor =
            db.query(
                """
                SELECT COUNT(*) FROM set_logs
                WHERE triggeredUsageIncrement = 0
                """.trimIndent(),
            )

        assertThat(cursor.moveToFirst()).isTrue()
        assertThat(cursor.getInt(0)).isEqualTo(1000)

        cursor.close()
        db.close()
    }

    @Test
    fun migrate1To2_handlesNullableColumnsCorrectly() {
        // Test that existing records with NULL values in optional columns migrate correctly
        helper.createDatabase(testDb, 1).apply {
            // Insert record with all nullable columns as NULL
            execSQL(
                """
                INSERT INTO set_logs (id, userId, exerciseLogId, setOrder, actualReps, actualWeight, isCompleted,
                                     targetReps, targetWeight, targetRpe, actualRpe, tag, notes, completedAt)
                VALUES ('set1', 'user1', 'exLog1', 1, 5, 100.0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
                """.trimIndent(),
            )

            // Insert record with values in all columns
            execSQL(
                """
                INSERT INTO set_logs (id, userId, exerciseLogId, setOrder, actualReps, actualWeight, isCompleted,
                                     targetReps, targetWeight, targetRpe, actualRpe, tag, notes, completedAt)
                VALUES ('set2', 'user1', 'exLog1', 2, 5, 100.0, 1, 5, 100.0, 8.0, 7.5, 'PR', 'Notes', '2023-01-01T10:00:00')
                """.trimIndent(),
            )

            close()
        }

        // Run migration
        val db =
            helper.runMigrationsAndValidate(
                testDb,
                2,
                true,
                MIGRATION_1_2,
            )

        // Verify both records exist with new columns
        val cursor =
            db.query(
                """
                SELECT id, triggeredUsageIncrement, previous1RMEstimate
                FROM set_logs
                ORDER BY id
                """.trimIndent(),
            )

        assertThat(cursor.moveToFirst()).isTrue()
        assertThat(cursor.getString(0)).isEqualTo("set1")
        assertThat(cursor.getInt(1)).isEqualTo(0)
        assertThat(cursor.isNull(2)).isTrue()

        assertThat(cursor.moveToNext()).isTrue()
        assertThat(cursor.getString(0)).isEqualTo("set2")
        assertThat(cursor.getInt(1)).isEqualTo(0)
        assertThat(cursor.isNull(2)).isTrue()

        cursor.close()
        db.close()
    }

    @Test
    fun migrate1To2_emptyTablesHandledCorrectly() {
        // Create database with version 1 with empty tables
        helper.createDatabase(testDb, 1).apply {
            // Don't insert any data
            close()
        }

        // Run migration - should succeed even with empty tables
        val db =
            helper.runMigrationsAndValidate(
                testDb,
                2,
                true,
                MIGRATION_1_2,
            )

        // Verify tables exist and have correct schema
        val setLogCursor = db.query("PRAGMA table_info(set_logs)")
        val columns = mutableListOf<String>()
        while (setLogCursor.moveToNext()) {
            columns.add(setLogCursor.getString(1))
        }
        setLogCursor.close()

        assertThat(columns).contains("triggeredUsageIncrement")
        assertThat(columns).contains("previous1RMEstimate")

        val prCursor = db.query("PRAGMA table_info(personal_records)")
        val prColumns = mutableListOf<String>()
        while (prCursor.moveToNext()) {
            prColumns.add(prCursor.getString(1))
        }
        prCursor.close()

        assertThat(prColumns).contains("sourceSetId")

        db.close()
    }
}
