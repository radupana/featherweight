package com.github.radupana.featherweight.data.migration

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

/**
 * Unit test for database migration from version 3 to version 4.
 *
 * This migration adds userId fields to support cloud sync while maintaining
 * backward compatibility with existing data.
 *
 * Note: This is a unit test that mocks the database. For full integration testing
 * of Room migrations, androidTest would be required with MigrationTestHelper.
 */
class Migration3to4Test {
    private lateinit var migration: Migration3to4
    private lateinit var database: SupportSQLiteDatabase
    private lateinit var cursor: Cursor

    @Before
    fun setup() {
        migration = Migration3to4()
        database = mockk(relaxed = true)
        cursor = mockk(relaxed = true)
    }

    @Test
    fun `migration should add userId column to Workout table when it doesn't exist`() {
        // Given - userId column doesn't exist
        setupColumnDoesNotExist("Workout", "userId")

        // When
        migration.migrate(database)

        // Then - should alter table to add userId (without DEFAULT NULL as per actual migration)
        verify { database.execSQL("ALTER TABLE Workout ADD COLUMN userId TEXT") }
    }

    @Test
    fun `migration should not add userId column to Workout table when it already exists`() {
        // Given - userId column already exists
        setupColumnExists("Workout", "userId")

        // When
        migration.migrate(database)

        // Then - should not alter table
        verify(exactly = 0) { database.execSQL("ALTER TABLE Workout ADD COLUMN userId TEXT") }
    }

    @Test
    fun `migration should add userId to workout-related tables`() {
        // Given - no userId columns exist
        setupAllColumnsDoNotExist()

        // When
        migration.migrate(database)

        // Then - should add userId to workout tables with correct table names
        verify { database.execSQL("ALTER TABLE Workout ADD COLUMN userId TEXT") }
        verify { database.execSQL("ALTER TABLE ExerciseLog ADD COLUMN userId TEXT") }
        verify { database.execSQL("ALTER TABLE SetLog ADD COLUMN userId TEXT") }
    }

    @Test
    fun `migration should add userId to programme-related tables`() {
        // Given - no userId columns exist
        setupAllColumnsDoNotExist()

        // When
        migration.migrate(database)

        // Then - should add userId to programme tables (using snake_case as per actual migration)
        verify { database.execSQL("ALTER TABLE programmes ADD COLUMN userId TEXT") }
        verify { database.execSQL("ALTER TABLE programme_weeks ADD COLUMN userId TEXT") }
        verify { database.execSQL("ALTER TABLE programme_workouts ADD COLUMN userId TEXT") }
        verify { database.execSQL("ALTER TABLE programme_progress ADD COLUMN userId TEXT") }
        verify { database.execSQL("ALTER TABLE exercise_substitutions ADD COLUMN userId TEXT") }
    }

    @Test
    fun `migration should add userId to personal data tables`() {
        // Given - no userId columns exist
        setupAllColumnsDoNotExist()

        // When
        migration.migrate(database)

        // Then - should add userId to personal data tables
        verify { database.execSQL("ALTER TABLE PersonalRecord ADD COLUMN userId TEXT") }
        verify { database.execSQL("ALTER TABLE user_exercise_maxes ADD COLUMN userId TEXT") }
        verify { database.execSQL("ALTER TABLE one_rm_history ADD COLUMN userId TEXT") }
        verify { database.execSQL("ALTER TABLE exercise_swap_history ADD COLUMN userId TEXT") }
        verify { database.execSQL("ALTER TABLE exercise_performance_tracking ADD COLUMN userId TEXT") }
        verify { database.execSQL("ALTER TABLE global_exercise_progress ADD COLUMN userId TEXT") }
    }

    @Test
    fun `migration should add createdByUserId to exercise tables`() {
        // Given - no createdByUserId columns exist
        setupColumnDoesNotExist("exercise_cores", "createdByUserId")
        setupColumnDoesNotExist("exercise_variations", "createdByUserId")

        // When
        migration.migrate(database)

        // Then - should add createdByUserId to exercise tables (using snake_case as per actual migration)
        verify { database.execSQL("ALTER TABLE exercise_cores ADD COLUMN createdByUserId TEXT") }
        verify { database.execSQL("ALTER TABLE exercise_variations ADD COLUMN createdByUserId TEXT") }
    }

    @Test
    fun `migration should create indices for userId columns`() {
        // Given - columns exist but indices don't
        setupAllColumnsExist()

        // When
        migration.migrate(database)

        // Then - should create indices with correct table names
        verify { database.execSQL("CREATE INDEX IF NOT EXISTS index_Workout_userId ON Workout(userId)") }
        verify { database.execSQL("CREATE INDEX IF NOT EXISTS index_ExerciseLog_userId ON ExerciseLog(userId)") }
        verify { database.execSQL("CREATE INDEX IF NOT EXISTS index_SetLog_userId ON SetLog(userId)") }
        verify { database.execSQL("CREATE INDEX IF NOT EXISTS index_programmes_userId ON programmes(userId)") }
        verify { database.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_cores_createdByUserId ON exercise_cores(createdByUserId)") }
        verify { database.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_variations_createdByUserId ON exercise_variations(createdByUserId)") }
    }

    @Test
    fun `migration should handle database errors gracefully`() {
        // Given - database throws exception on PRAGMA query
        every { database.query(any<String>()) } throws RuntimeException("Database error")

        // When - migration should not crash
        try {
            migration.migrate(database)
        } catch (e: RuntimeException) {
            // Expected - migration might rethrow
            // Detekt requires we do something with the exception
            assertThat(e.message).contains("Database error")
        }

        // Then - at least tried to check columns
        verify(atLeast = 1) { database.query(match { query: String -> query.startsWith("PRAGMA table_info") }) }
    }

    @Test
    fun `columnExists helper should properly check for column existence`() {
        // Given - setup for Workout table with userId column
        val pragmaQuery = "PRAGMA table_info(Workout)"
        every { database.query(pragmaQuery) } returns cursor

        // Simulate cursor with userId column
        every { cursor.moveToNext() } returnsMany listOf(true, true, true, false)
        every { cursor.getColumnIndex("name") } returns 1
        every { cursor.getString(1) } returnsMany listOf("id", "userId", "name")
        every { cursor.close() } returns Unit

        // When
        migration.migrate(database)

        // Then - should check for column and not add it since it exists
        verify { database.query(pragmaQuery) }
        verify(exactly = 0) { database.execSQL("ALTER TABLE Workout ADD COLUMN userId TEXT") }
    }

    @Test
    fun `migration should add userId to AI and analysis tables`() {
        // Given - no userId columns exist
        setupAllColumnsDoNotExist()

        // When
        migration.migrate(database)

        // Then - should add userId to AI/analysis tables
        verify { database.execSQL("ALTER TABLE training_analysis ADD COLUMN userId TEXT") }
        verify { database.execSQL("ALTER TABLE parse_requests ADD COLUMN userId TEXT") }
    }

    // Helper methods

    private fun setupColumnExists(
        table: String,
        column: String,
    ) {
        every { database.query("PRAGMA table_info($table)") } returns cursor
        every { cursor.moveToNext() } returnsMany listOf(true, false)
        every { cursor.getColumnIndex("name") } returns 1
        every { cursor.getString(1) } returns column
        every { cursor.close() } returns Unit
    }

    private fun setupColumnDoesNotExist(
        table: String,
        @Suppress("UNUSED_PARAMETER") column: String,
    ) {
        every { database.query("PRAGMA table_info($table)") } returns cursor
        every { cursor.moveToNext() } returns false
        every { cursor.close() } returns Unit
    }

    private fun setupAllColumnsDoNotExist() {
        every { database.query(any<String>()) } returns cursor
        every { cursor.moveToNext() } returns false
        every { cursor.close() } returns Unit
    }

    private fun setupAllColumnsExist() {
        every { database.query(match { query: String -> query.startsWith("PRAGMA") }) } returns cursor
        every { cursor.moveToNext() } returnsMany listOf(true, false)
        every { cursor.getColumnIndex("name") } returns 1
        every { cursor.getString(1) } returns "userId"
        every { cursor.close() } returns Unit
    }
}
