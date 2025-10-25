package com.github.radupana.featherweight.service

import android.util.Log
import androidx.sqlite.db.SupportSQLiteStatement
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class LocalDataMigrationServiceTest {
    private lateinit var database: FeatherweightDatabase
    private lateinit var service: LocalDataMigrationService

    private val targetUserId = "firebase-user-123"
    private val localUserId = "local"

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.v(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        database = mockk(relaxed = true)
        service = LocalDataMigrationService(database)
    }

    @Test
    fun `migrateLocalDataToUser returns false when targetUserId is local`() =
        runTest {
            val result = service.migrateLocalDataToUser("local")

            assertThat(result).isFalse()
        }

    @Test
    fun `migrateLocalDataToUser migrates all tables with correct SQL`() =
        runTest {
            every { database.runInTransaction(any<Runnable>()) } answers {
                firstArg<Runnable>().run()
            }

            val statementMock = mockk<SupportSQLiteStatement>(relaxed = true)
            every { statementMock.executeUpdateDelete() } returns 1

            val expectedSqlQueries =
                listOf(
                    "UPDATE workouts SET userId = ? WHERE userId = ?",
                    "UPDATE exercise_logs SET userId = ? WHERE userId = ?",
                    "UPDATE set_logs SET userId = ? WHERE userId = ?",
                    "UPDATE personal_records SET userId = ? WHERE userId = ?",
                    "UPDATE global_exercise_progress SET userId = ? WHERE userId = ?",
                    "UPDATE user_exercise_usage SET userId = ? WHERE userId = ?",
                    "UPDATE exercises SET userId = ? WHERE userId = ? AND type = 'USER'",
                    "UPDATE programmes SET userId = ? WHERE userId = ?",
                    "UPDATE programme_weeks SET userId = ? WHERE userId = ?",
                    "UPDATE programme_workouts SET userId = ? WHERE userId = ?",
                    "UPDATE programme_progress SET userId = ? WHERE userId = ?",
                    "UPDATE exercise_swap_history SET userId = ? WHERE userId = ?",
                    "UPDATE programme_exercise_tracking SET userId = ? WHERE userId = ?",
                    "UPDATE training_analyses SET userId = ? WHERE userId = ?",
                    "UPDATE parse_requests SET userId = ? WHERE userId = ?",
                    "UPDATE exercise_max_history SET userId = ? WHERE userId = ?",
                    "UPDATE workout_templates SET userId = ? WHERE userId = ?",
                    "UPDATE template_exercises SET userId = ? WHERE userId = ?",
                    "UPDATE template_sets SET userId = ? WHERE userId = ?",
                )

            expectedSqlQueries.forEach { sql ->
                every { database.compileStatement(sql) } returns statementMock
            }

            val result = service.migrateLocalDataToUser(targetUserId)

            assertThat(result).isTrue()

            expectedSqlQueries.forEach { sql ->
                verify { database.compileStatement(sql) }
            }

            verify(atLeast = expectedSqlQueries.size) {
                statementMock.bindString(1, targetUserId)
            }
            verify(atLeast = expectedSqlQueries.size) {
                statementMock.bindString(2, localUserId)
            }
        }

    @Test
    fun `migrateLocalDataToUser returns false on SQLiteException`() =
        runTest {
            every { database.runInTransaction(any<Runnable>()) } throws
                android.database.sqlite.SQLiteException("Test error")

            val result = service.migrateLocalDataToUser(targetUserId)

            assertThat(result).isFalse()
        }

    @Test
    fun `migrateLocalDataToUser returns false on IllegalStateException`() =
        runTest {
            every { database.runInTransaction(any<Runnable>()) } throws
                IllegalStateException("Test error")

            val result = service.migrateLocalDataToUser(targetUserId)

            assertThat(result).isFalse()
        }

    @Test
    fun `hasLocalData returns true when local workouts exist`() =
        runTest {
            val statementMock = mockk<SupportSQLiteStatement>(relaxed = true)
            every { statementMock.simpleQueryForLong() } returns 5L
            every {
                database.compileStatement("SELECT COUNT(*) FROM workouts WHERE userId = ?")
            } returns statementMock

            val result = service.hasLocalData()

            assertThat(result).isTrue()
            verify { statementMock.bindString(1, localUserId) }
        }

    @Test
    fun `hasLocalData returns false when no local workouts exist`() =
        runTest {
            val statementMock = mockk<SupportSQLiteStatement>(relaxed = true)
            every { statementMock.simpleQueryForLong() } returns 0L
            every {
                database.compileStatement("SELECT COUNT(*) FROM workouts WHERE userId = ?")
            } returns statementMock

            val result = service.hasLocalData()

            assertThat(result).isFalse()
        }

    @Test
    fun `hasLocalData returns false on SQLiteException`() =
        runTest {
            every {
                database.compileStatement("SELECT COUNT(*) FROM workouts WHERE userId = ?")
            } throws android.database.sqlite.SQLiteException("Test error")

            val result = service.hasLocalData()

            assertThat(result).isFalse()
        }

    @Test
    fun `cleanupLocalData deletes from all tables with correct SQL`() =
        runTest {
            every { database.runInTransaction(any<Runnable>()) } answers {
                firstArg<Runnable>().run()
            }

            val statementMock = mockk<SupportSQLiteStatement>(relaxed = true)
            every { statementMock.executeUpdateDelete() } returns 1

            val expectedSqlQueries =
                listOf(
                    "DELETE FROM workouts WHERE userId = ?",
                    "DELETE FROM exercise_logs WHERE userId = ?",
                    "DELETE FROM set_logs WHERE userId = ?",
                    "DELETE FROM personal_records WHERE userId = ?",
                    "DELETE FROM global_exercise_progress WHERE userId = ?",
                    "DELETE FROM user_exercise_usage WHERE userId = ?",
                    "DELETE FROM exercises WHERE userId = ? AND type = 'USER'",
                    "DELETE FROM programmes WHERE userId = ?",
                    "DELETE FROM programme_weeks WHERE userId = ?",
                    "DELETE FROM programme_workouts WHERE userId = ?",
                    "DELETE FROM programme_progress WHERE userId = ?",
                    "DELETE FROM exercise_swap_history WHERE userId = ?",
                    "DELETE FROM programme_exercise_tracking WHERE userId = ?",
                    "DELETE FROM training_analyses WHERE userId = ?",
                    "DELETE FROM parse_requests WHERE userId = ?",
                    "DELETE FROM exercise_max_history WHERE userId = ?",
                    "DELETE FROM workout_templates WHERE userId = ?",
                    "DELETE FROM template_exercises WHERE userId = ?",
                    "DELETE FROM template_sets WHERE userId = ?",
                )

            expectedSqlQueries.forEach { sql ->
                every { database.compileStatement(sql) } returns statementMock
            }

            val result = service.cleanupLocalData()

            assertThat(result).isTrue()

            expectedSqlQueries.forEach { sql ->
                verify { database.compileStatement(sql) }
            }

            verify(exactly = expectedSqlQueries.size) {
                statementMock.bindString(1, localUserId)
            }
        }

    @Test
    fun `cleanupLocalData returns false on SQLiteException`() =
        runTest {
            every { database.runInTransaction(any<Runnable>()) } throws
                android.database.sqlite.SQLiteException("Test error")

            val result = service.cleanupLocalData()

            assertThat(result).isFalse()
        }
}
