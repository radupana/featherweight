package com.github.radupana.featherweight.manager

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class AuthenticationManagerTest {
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var manager: AuthenticationManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor

        manager = AuthenticationManagerImpl(context)
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

        assertThat(manager.isAuthenticated()).isFalse()
    }

    @Test
    fun `isAuthenticated returns true when user id exists`() {
        every { sharedPreferences.getString("user_id", null) } returns "test-user-123"

        assertThat(manager.isAuthenticated()).isTrue()
    }

    @Test
    fun `getCurrentUserId returns null when not authenticated`() {
        every { sharedPreferences.getString("user_id", null) } returns null

        assertThat(manager.getCurrentUserId()).isNull()
    }

    @Test
    fun `getCurrentUserId returns user id when authenticated`() {
        val userId = "test-user-123"
        every { sharedPreferences.getString("user_id", null) } returns userId

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
        every { sharedPreferences.getBoolean("unauthenticated_warning_shown", false) } returns false

        assertThat(manager.shouldShowWarning()).isTrue()
    }

    @Test
    fun `shouldShowWarning returns false for authenticated user`() {
        every { sharedPreferences.getString("user_id", null) } returns "test-user-123"
        every { sharedPreferences.getBoolean("unauthenticated_warning_shown", false) } returns false

        assertThat(manager.shouldShowWarning()).isFalse()
    }

    @Test
    fun `shouldShowWarning returns false after warning shown`() {
        every { sharedPreferences.getString("user_id", null) } returns null
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
