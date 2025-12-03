package com.github.radupana.featherweight.service

import android.util.Log
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Tests for LocalDataMigrationService.
 *
 * Note: The LocalDataMigrationService uses Android Room APIs (runInTransaction, compileStatement)
 * that are not available in JVM tests. This test file verifies the boundary condition
 * (local userId check) which doesn't require database interaction.
 *
 * Full SQL execution tests should be done via integration tests with a real Android database.
 */
class LocalDataMigrationServiceTest {
    private lateinit var database: FeatherweightDatabase
    private lateinit var service: LocalDataMigrationService

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
            // This test verifies the boundary check before any database operations
            val result = service.migrateLocalDataToUser("local")

            assertThat(result).isFalse()
        }
}
