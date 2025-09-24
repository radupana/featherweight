package com.github.radupana.featherweight.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthenticationManagerImplTest {
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var manager: AuthenticationManagerImpl

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)
        firebaseUser = mockk(relaxed = true)

        every { context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } returns Unit
        every { editor.commit() } returns true

        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0

        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns firebaseAuth

        manager = AuthenticationManagerImpl(context)
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseAuth::class)
        unmockkStatic(Log::class)
    }

    // Test corrupted auth state detection
    @Test
    fun `isAuthenticated detects corrupted state when stored user exists but Firebase user is null`() {
        // Given: Stored user exists but Firebase Auth is null (corrupted state)
        every { sharedPreferences.getString("user_id", null) } returns "stored-user-123"
        every { firebaseAuth.currentUser } returns null

        // When: checking authentication
        val result = manager.isAuthenticated()

        // Then: Should detect corruption, clear data, and return false
        assertThat(result).isFalse()
        verify { editor.remove("user_id") }
        verify { editor.putBoolean("first_launch", true) }
        verify { editor.putBoolean("unauthenticated_warning_shown", false) }
        verify { editor.putBoolean("seen_unauthenticated_warning", false) }
    }

    @Test
    fun `isAuthenticated returns true when stored user matches Firebase user with Google provider`() {
        // Given: Stored user matches Firebase user signed in with Google
        val userId = "test-user-123"
        val googleProvider = mockk<UserInfo>()
        every { googleProvider.providerId } returns "google.com"
        every { sharedPreferences.getString("user_id", null) } returns userId
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userId
        every { firebaseUser.providerData } returns listOf(googleProvider)

        // When: checking authentication
        val result = manager.isAuthenticated()

        // Then: Should return true (Google doesn't need email verification)
        assertThat(result).isTrue()
    }

    @Test
    fun `isAuthenticated returns true when email verified user matches stored user`() {
        // Given: Stored user matches Firebase user with verified email
        val userId = "test-user-123"
        val passwordProvider = mockk<UserInfo>()
        every { passwordProvider.providerId } returns "password"
        every { sharedPreferences.getString("user_id", null) } returns userId
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userId
        every { firebaseUser.providerData } returns listOf(passwordProvider)
        every { firebaseUser.isEmailVerified } returns true

        // When: checking authentication
        val result = manager.isAuthenticated()

        // Then: Should return true
        assertThat(result).isTrue()
    }

    @Test
    fun `isAuthenticated returns false when email not verified for password provider`() {
        // Given: Stored user matches Firebase user but email not verified
        val userId = "test-user-123"
        val passwordProvider = mockk<UserInfo>()
        every { passwordProvider.providerId } returns "password"
        every { sharedPreferences.getString("user_id", null) } returns userId
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userId
        every { firebaseUser.providerData } returns listOf(passwordProvider)
        every { firebaseUser.isEmailVerified } returns false

        // When: checking authentication
        val result = manager.isAuthenticated()

        // Then: Should return false due to unverified email
        assertThat(result).isFalse()
    }

    @Test
    fun `isAuthenticated updates stored user when Firebase user differs`() {
        // Given: Stored user differs from Firebase user (with Google provider)
        val storedUserId = "old-user-123"
        val firebaseUserId = "new-user-456"
        val googleProvider = mockk<UserInfo>()
        every { googleProvider.providerId } returns "google.com"
        every { sharedPreferences.getString("user_id", null) } returns storedUserId
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns firebaseUserId
        every { firebaseUser.providerData } returns listOf(googleProvider)

        // When: checking authentication
        val result = manager.isAuthenticated()

        // Then: Should update stored user and return true (Google doesn't need verification)
        assertThat(result).isTrue()
        verify { editor.putString("user_id", firebaseUserId) }
    }

    @Test
    fun `isAuthenticated handles multiple providers with password provider`() {
        // Given: User has both Google and password providers, email verified
        val userId = "test-user-123"
        val googleProvider = mockk<UserInfo>()
        val passwordProvider = mockk<UserInfo>()
        every { googleProvider.providerId } returns "google.com"
        every { passwordProvider.providerId } returns "password"
        every { sharedPreferences.getString("user_id", null) } returns userId
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userId
        every { firebaseUser.providerData } returns listOf(googleProvider, passwordProvider)
        every { firebaseUser.isEmailVerified } returns true

        // When: checking authentication
        val result = manager.isAuthenticated()

        // Then: Should return true (email is verified)
        assertThat(result).isTrue()
    }

    @Test
    fun `isAuthenticated returns false when no stored user and no Firebase user`() {
        // Given: No stored user and no Firebase user
        every { sharedPreferences.getString("user_id", null) } returns null
        every { firebaseAuth.currentUser } returns null

        // When: checking authentication
        val result = manager.isAuthenticated()

        // Then: Should return false
        assertThat(result).isFalse()
    }

    // Test getCurrentUserId with Firebase validation
    @Test
    fun `getCurrentUserId clears corrupted state and returns null`() {
        // Given: Stored user exists but Firebase Auth is null
        every { sharedPreferences.getString("user_id", null) } returns "stored-user-123"
        every { firebaseAuth.currentUser } returns null

        // When: getting current user ID
        val result = manager.getCurrentUserId()

        // Then: Should clear data and return null
        assertThat(result).isNull()
        verify { editor.remove("user_id") }
        verify { editor.putBoolean("first_launch", true) }
    }

    @Test
    fun `getCurrentUserId returns stored user when matches Firebase user`() {
        // Given: Stored user matches Firebase user
        val userId = "test-user-123"
        every { sharedPreferences.getString("user_id", null) } returns userId
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userId

        // When: getting current user ID
        val result = manager.getCurrentUserId()

        // Then: Should return the user ID
        assertThat(result).isEqualTo(userId)
    }

    @Test
    fun `getCurrentUserId updates and returns Firebase user when mismatch`() {
        // Given: Stored user differs from Firebase user
        val storedUserId = "old-user-123"
        val firebaseUserId = "new-user-456"
        every { sharedPreferences.getString("user_id", null) } returns storedUserId
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns firebaseUserId

        // When: getting current user ID
        val result = manager.getCurrentUserId()

        // Then: Should update and return Firebase user ID
        assertThat(result).isEqualTo(firebaseUserId)
        verify { editor.putString("user_id", firebaseUserId) }
    }

    @Test
    fun `getCurrentUserId returns null when no stored user`() {
        // Given: No stored user
        every { sharedPreferences.getString("user_id", null) } returns null
        every { firebaseAuth.currentUser } returns null

        // When: getting current user ID
        val result = manager.getCurrentUserId()

        // Then: Should return null
        assertThat(result).isNull()
    }

    // Test clearUserData resets first launch
    @Test
    fun `clearUserData resets first launch flag to true`() {
        // When: clearing user data
        manager.clearUserData()

        // Then: Should reset first launch flag
        verify { editor.putBoolean("first_launch", true) }
        verify { editor.remove("user_id") }
        verify { editor.putBoolean("unauthenticated_warning_shown", false) }
        verify { editor.putBoolean("seen_unauthenticated_warning", false) }
    }

    // Test recovery scenarios
    @Test
    fun `multiple calls to isAuthenticated with corrupted state only clear once`() {
        // Given: Corrupted state
        val clearCallsSlot = slot<String>()
        every { sharedPreferences.getString("user_id", null) } returns "stored-user-123"
        every { firebaseAuth.currentUser } returns null
        every { editor.remove(capture(clearCallsSlot)) } returns editor

        // When: calling isAuthenticated twice
        manager.isAuthenticated()

        // After first call, simulate cleared state
        every { sharedPreferences.getString("user_id", null) } returns null

        manager.isAuthenticated()

        // Then: Should only clear once
        assertThat(clearCallsSlot.isCaptured).isTrue()
        verify(exactly = 1) { editor.remove("user_id") }
    }

    @Test
    fun `Firebase user without stored user gets saved`() {
        // Given: Firebase user exists but no stored user
        val firebaseUserId = "firebase-user-123"
        every { sharedPreferences.getString("user_id", null) } returns null
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns firebaseUserId

        // When: checking authentication
        val result = manager.isAuthenticated()

        // Then: Should return false (no stored user)
        assertThat(result).isFalse()
    }

    @Test
    fun `setCurrentUserId works with Firebase validation`() {
        // Given: Setting a new user ID
        val newUserId = "new-user-789"

        // When: setting current user ID
        manager.setCurrentUserId(newUserId)

        // Then: Should store the user ID
        verify { editor.putString("user_id", newUserId) }
    }

    // Edge cases
    @Test
    fun `handles empty stored user ID`() {
        // Given: Empty stored user ID
        every { sharedPreferences.getString("user_id", null) } returns ""
        every { firebaseAuth.currentUser } returns null

        // When: checking authentication
        val result = manager.isAuthenticated()

        // Then: Should return false
        assertThat(result).isFalse()
    }

    @Test
    fun `handles empty Firebase user UID`() {
        // Given: Firebase user with empty UID and Google provider
        val googleProvider = mockk<UserInfo>()
        every { googleProvider.providerId } returns "google.com"
        every { sharedPreferences.getString("user_id", null) } returns "stored-user"
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns ""
        every { firebaseUser.providerData } returns listOf(googleProvider)

        // When: checking authentication
        val result = manager.isAuthenticated()

        // Then: Should update to empty string and return true (Google doesn't need verification)
        verify { editor.putString("user_id", "") }
        assertThat(result).isTrue()
    }

    @Test
    fun `isAuthenticated with empty provider list defaults to requiring no verification`() {
        // Given: User with no providers (edge case)
        val userId = "test-user-123"
        every { sharedPreferences.getString("user_id", null) } returns userId
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userId
        every { firebaseUser.providerData } returns emptyList()

        // When: checking authentication
        val result = manager.isAuthenticated()

        // Then: Should return true (no password provider means no verification needed)
        assertThat(result).isTrue()
    }
}
