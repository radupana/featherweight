package com.github.radupana.featherweight.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthenticationManagerTest {
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var manager: AuthenticationManager

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

    @Test
    fun `isFirstLaunch returns true by default`() {
        every { sharedPreferences.getBoolean("first_launch", true) } returns true

        assertThat(manager.isFirstLaunch()).isTrue()
    }

    @Test
    fun `isFirstLaunch returns false after setFirstLaunchComplete`() {
        every { sharedPreferences.getBoolean("first_launch", true) } returns false

        manager.setFirstLaunchComplete()
        verify { editor.putBoolean("first_launch", false) }
    }

    @Test
    fun `isAuthenticated returns false when no user id`() {
        every { sharedPreferences.getString("user_id", null) } returns null
        every { firebaseAuth.currentUser } returns null

        assertThat(manager.isAuthenticated()).isFalse()
    }

    @Test
    fun `isAuthenticated returns true when user id exists`() {
        val userId = "test-user-123"
        every { sharedPreferences.getString("user_id", null) } returns userId
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userId
        every { firebaseUser.providerData } returns emptyList() // No password provider, so no email verification needed

        assertThat(manager.isAuthenticated()).isTrue()
    }

    @Test
    fun `getCurrentUserId returns null when not authenticated`() {
        every { sharedPreferences.getString("user_id", null) } returns null
        every { firebaseAuth.currentUser } returns null

        assertThat(manager.getCurrentUserId()).isNull()
    }

    @Test
    fun `getCurrentUserId returns user id when authenticated`() {
        val userId = "test-user-123"
        every { sharedPreferences.getString("user_id", null) } returns userId
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userId

        assertThat(manager.getCurrentUserId()).isEqualTo(userId)
    }

    @Test
    fun `setCurrentUserId stores user id`() {
        val userId = "test-user-123"

        manager.setCurrentUserId(userId)

        verify { editor.putString("user_id", userId) }
    }

    @Test
    fun `setCurrentUserId can clear user id with null`() {
        manager.setCurrentUserId(null)

        verify { editor.putString("user_id", null) }
    }

    @Test
    fun `shouldShowWarning returns true for unauthenticated user who has not seen warning`() {
        every { sharedPreferences.getString("user_id", null) } returns null
        every { firebaseAuth.currentUser } returns null
        every { sharedPreferences.getBoolean("unauthenticated_warning_shown", false) } returns false

        assertThat(manager.shouldShowWarning()).isTrue()
    }

    @Test
    fun `shouldShowWarning returns false for authenticated user`() {
        val userId = "test-user-123"
        every { sharedPreferences.getString("user_id", null) } returns userId
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns userId
        every { firebaseUser.providerData } returns emptyList()
        every { sharedPreferences.getBoolean("unauthenticated_warning_shown", false) } returns false

        assertThat(manager.shouldShowWarning()).isFalse()
    }

    @Test
    fun `shouldShowWarning returns false after warning shown`() {
        every { sharedPreferences.getString("user_id", null) } returns null
        every { firebaseAuth.currentUser } returns null
        every { sharedPreferences.getBoolean("unauthenticated_warning_shown", false) } returns true

        assertThat(manager.shouldShowWarning()).isFalse()
    }

    @Test
    fun `setWarningShown marks warning as shown`() {
        manager.setWarningShown()

        verify { editor.putBoolean("unauthenticated_warning_shown", true) }
    }

    @Test
    fun `clearUserData removes user id and resets warning flags`() {
        manager.clearUserData()

        verify { editor.remove("user_id") }
        verify { editor.putBoolean("unauthenticated_warning_shown", false) }
        verify { editor.putBoolean("seen_unauthenticated_warning", false) }
        verify { editor.putBoolean("first_launch", true) } // Should reset to first launch
    }

    @Test
    fun `hasSeenUnauthenticatedWarning returns false by default`() {
        every { sharedPreferences.getBoolean("seen_unauthenticated_warning", false) } returns false

        assertThat(manager.hasSeenUnauthenticatedWarning()).isFalse()
    }

    @Test
    fun `hasSeenUnauthenticatedWarning returns true after setUnauthenticatedWarningShown`() {
        every { sharedPreferences.getBoolean("seen_unauthenticated_warning", false) } returns true

        manager.setUnauthenticatedWarningShown()
        verify { editor.putBoolean("seen_unauthenticated_warning", true) }
    }
}
