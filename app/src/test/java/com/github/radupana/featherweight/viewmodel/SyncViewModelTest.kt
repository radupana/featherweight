package com.github.radupana.featherweight.viewmodel

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.github.radupana.featherweight.sync.SyncManager
import com.github.radupana.featherweight.sync.SyncState
import com.github.radupana.featherweight.testutil.CoroutineTestRule
import com.google.common.truth.Truth.assertThat
import com.google.firebase.Timestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

/**
 * Tests for SyncViewModel covering:
 * - Authentication state management
 * - Sync operations
 * - Error handling
 * - Sync state transitions
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var viewModel: SyncViewModel
    private val mockContext: Context = mockk(relaxed = true)
    private val mockSyncManager: SyncManager = mockk(relaxed = true)
    private val mockAuthManager: AuthenticationManager = mockk(relaxed = true)
    private val mockWorkManager: WorkManager = mockk(relaxed = true)

    @Before
    fun setup() {
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        // Mock Context properly
        every { mockContext.applicationContext } returns mockContext

        // Mock WorkManager - mock at the object level to avoid getInstance being called during mock setup
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any()) } returns mockWorkManager

        // Set up default auth manager behavior
        every { mockAuthManager.getCurrentUserId() } returns null
        every { mockSyncManager.getLastSyncTime() } returns 0L

        // Mock successful sync by default
        val successResult: Result<SyncState> =
            Result.success(SyncState.Success(Timestamp(Date(System.currentTimeMillis()))))
        coEvery { mockSyncManager.syncAll() } returns successResult
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        unmockkObject(WorkManager.Companion)
    }

    @Test
    fun `init sets isAuthenticated to true when user is authenticated`() =
        runTest {
            // Given: User is authenticated
            every { mockAuthManager.getCurrentUserId() } returns "test-user-id"

            // When: ViewModel is initialized
            viewModel =
                SyncViewModel(
                    context = mockContext,
                    syncManager = mockSyncManager,
                    authManager = mockAuthManager,
                )

            advanceUntilIdle()

            // Then: isAuthenticated is true
            val uiState = viewModel.uiState.value
            assertThat(uiState.isAuthenticated).isTrue()
        }

    @Test
    fun `init sets isAuthenticated to false when user is not authenticated`() =
        runTest {
            // Given: User is not authenticated
            every { mockAuthManager.getCurrentUserId() } returns null

            // When: ViewModel is initialized
            viewModel =
                SyncViewModel(
                    context = mockContext,
                    syncManager = mockSyncManager,
                    authManager = mockAuthManager,
                )

            advanceUntilIdle()

            // Then: isAuthenticated is false
            val uiState = viewModel.uiState.value
            assertThat(uiState.isAuthenticated).isFalse()
        }

    @Test
    fun `init loads last sync time when available`() =
        runTest {
            // Given: Last sync time exists
            val lastSyncTime = System.currentTimeMillis() - 3600000 // 1 hour ago
            every { mockSyncManager.getLastSyncTime() } returns lastSyncTime

            // When: ViewModel is initialized
            viewModel =
                SyncViewModel(
                    context = mockContext,
                    syncManager = mockSyncManager,
                    authManager = mockAuthManager,
                )

            advanceUntilIdle()

            // Then: Last sync time is set
            val uiState = viewModel.uiState.value
            assertThat(uiState.lastSyncTime).isNotNull()
            assertThat(uiState.lastSyncTime).contains("hour")
        }

    @Test
    fun `onUserSignedIn triggers sync when user signs in`() =
        runTest {
            // Given: User is not authenticated initially
            every { mockAuthManager.getCurrentUserId() } returns null
            viewModel =
                SyncViewModel(
                    context = mockContext,
                    syncManager = mockSyncManager,
                    authManager = mockAuthManager,
                )
            advanceUntilIdle()

            // When: User signs in
            every { mockAuthManager.getCurrentUserId() } returns "test-user-id"
            viewModel.onUserSignedIn()
            advanceUntilIdle()

            // Then: Sync is triggered
            coVerify(atLeast = 1) { mockSyncManager.syncAll() }

            // And isAuthenticated is true
            assertThat(viewModel.uiState.value.isAuthenticated).isTrue()
        }

    @Test
    fun `onUserSignedOut clears authentication state`() =
        runTest {
            // Given: User is authenticated
            every { mockAuthManager.getCurrentUserId() } returns "test-user-id"
            viewModel =
                SyncViewModel(
                    context = mockContext,
                    syncManager = mockSyncManager,
                    authManager = mockAuthManager,
                )
            advanceUntilIdle()

            // When: User signs out
            viewModel.onUserSignedOut()

            // Then: isAuthenticated is false
            val uiState = viewModel.uiState.value
            assertThat(uiState.isAuthenticated).isFalse()
            assertThat(uiState.lastSyncTime).isNull()
        }

    @Test
    fun `triggerManualSync returns false when not authenticated`() =
        runTest {
            // Given: User is not authenticated
            every { mockAuthManager.getCurrentUserId() } returns null
            viewModel =
                SyncViewModel(
                    context = mockContext,
                    syncManager = mockSyncManager,
                    authManager = mockAuthManager,
                )
            advanceUntilIdle()

            // When: Manual sync is triggered
            val result = viewModel.triggerManualSync()

            // Then: Returns false
            assertThat(result).isFalse()
        }

    @Test
    fun `triggerManualSync returns true and syncs when authenticated`() =
        runTest {
            // Given: User is authenticated
            every { mockAuthManager.getCurrentUserId() } returns "test-user-id"
            viewModel =
                SyncViewModel(
                    context = mockContext,
                    syncManager = mockSyncManager,
                    authManager = mockAuthManager,
                )
            advanceUntilIdle()

            // When: Manual sync is triggered
            val result = viewModel.triggerManualSync()
            advanceUntilIdle()

            // Then: Returns true
            assertThat(result).isTrue()

            // And sync is called
            coVerify(atLeast = 1) { mockSyncManager.syncAll() }
        }

    @Test
    fun `sync updates UI state on success`() =
        runTest {
            // Given: Authenticated user and successful sync
            every { mockAuthManager.getCurrentUserId() } returns "test-user-id"
            val successTimestamp = Timestamp(Date(System.currentTimeMillis()))
            val successResult: Result<SyncState> = Result.success(SyncState.Success(successTimestamp))
            coEvery { mockSyncManager.syncAll() } returns successResult

            viewModel =
                SyncViewModel(
                    context = mockContext,
                    syncManager = mockSyncManager,
                    authManager = mockAuthManager,
                )

            // When: Sync completes
            advanceUntilIdle()

            // Then: UI state reflects success
            val uiState = viewModel.uiState.value
            assertThat(uiState.isSyncing).isFalse()
            assertThat(uiState.syncError).isNull()
        }

    @Test
    fun `sync updates UI state on error`() =
        runTest {
            // Given: Authenticated user and sync error
            every { mockAuthManager.getCurrentUserId() } returns "test-user-id"
            val errorResult: Result<SyncState> = Result.success(SyncState.Error("Network error"))
            coEvery { mockSyncManager.syncAll() } returns errorResult

            viewModel =
                SyncViewModel(
                    context = mockContext,
                    syncManager = mockSyncManager,
                    authManager = mockAuthManager,
                )

            // When: Sync completes with error
            advanceUntilIdle()

            // Then: UI state shows error
            val uiState = viewModel.uiState.value
            assertThat(uiState.syncError).isNotNull()
        }

    @Test
    fun `onSyncCompleted updates state correctly for success`() =
        runTest {
            // Given: ViewModel initialized
            every { mockAuthManager.getCurrentUserId() } returns "test-user-id"
            viewModel =
                SyncViewModel(
                    context = mockContext,
                    syncManager = mockSyncManager,
                    authManager = mockAuthManager,
                )

            val timestamp = Timestamp(Date(System.currentTimeMillis()))

            // When: Sync completes successfully
            viewModel.onSyncCompleted(SyncState.Success(timestamp))

            // Then: State is updated
            val uiState = viewModel.uiState.value
            assertThat(uiState.isSyncing).isFalse()
            assertThat(uiState.syncError).isNull()
            assertThat(uiState.lastSyncTime).isNotNull()
        }

    @Test
    fun `onSyncCompleted updates state correctly for error`() =
        runTest {
            // Given: ViewModel initialized
            every { mockAuthManager.getCurrentUserId() } returns "test-user-id"
            viewModel =
                SyncViewModel(
                    context = mockContext,
                    syncManager = mockSyncManager,
                    authManager = mockAuthManager,
                )

            // When: Sync completes with error
            viewModel.onSyncCompleted(SyncState.Error("Test error"))

            // Then: Error is shown
            val uiState = viewModel.uiState.value
            assertThat(uiState.isSyncing).isFalse()
            assertThat(uiState.syncError).isEqualTo("Test error")
        }

    @Test
    fun `startBackgroundSync sets isSyncing to true`() =
        runTest {
            // Given: ViewModel initialized
            every { mockAuthManager.getCurrentUserId() } returns "test-user-id"
            viewModel =
                SyncViewModel(
                    context = mockContext,
                    syncManager = mockSyncManager,
                    authManager = mockAuthManager,
                )

            // When: Background sync starts
            viewModel.startBackgroundSync()

            // Then: isSyncing is true
            val uiState = viewModel.uiState.value
            assertThat(uiState.isSyncing).isTrue()
            assertThat(uiState.syncError).isNull()
        }

    @Test
    fun `format timestamp shows just now for recent sync`() =
        runTest {
            // Given: Recent sync time (less than 1 minute ago)
            val recentTime = System.currentTimeMillis() - 30000 // 30 seconds ago
            every { mockSyncManager.getLastSyncTime() } returns recentTime

            // When: ViewModel is initialized
            viewModel =
                SyncViewModel(
                    context = mockContext,
                    syncManager = mockSyncManager,
                    authManager = mockAuthManager,
                )

            // Then: Shows "Just now"
            val uiState = viewModel.uiState.value
            assertThat(uiState.lastSyncTime).isEqualTo("Just now")
        }

    @Test
    fun `format timestamp shows minutes ago for recent sync`() =
        runTest {
            // Given: Sync time 30 minutes ago
            val recentTime = System.currentTimeMillis() - 1800000 // 30 minutes ago
            every { mockSyncManager.getLastSyncTime() } returns recentTime

            // When: ViewModel is initialized
            viewModel =
                SyncViewModel(
                    context = mockContext,
                    syncManager = mockSyncManager,
                    authManager = mockAuthManager,
                )

            // Then: Shows minutes ago
            val uiState = viewModel.uiState.value
            assertThat(uiState.lastSyncTime).contains("minutes ago")
        }

    @Test
    fun `format timestamp shows hours ago for older sync`() =
        runTest {
            // Given: Sync time 2 hours ago
            val recentTime = System.currentTimeMillis() - 7200000 // 2 hours ago
            every { mockSyncManager.getLastSyncTime() } returns recentTime

            // When: ViewModel is initialized
            viewModel =
                SyncViewModel(
                    context = mockContext,
                    syncManager = mockSyncManager,
                    authManager = mockAuthManager,
                )

            // Then: Shows hours ago
            val uiState = viewModel.uiState.value
            assertThat(uiState.lastSyncTime).contains("hours ago")
        }
}
