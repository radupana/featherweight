package com.github.radupana.featherweight.data.profile

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class ProfileDaoTest {
    private lateinit var dao: ProfileDao
    private lateinit var mockProfile: UserProfile
    private lateinit var testDate: LocalDateTime

    @Before
    fun setup() {
        dao = mockk()
        testDate = LocalDateTime.of(2024, 1, 1, 10, 0)
        mockProfile = UserProfile(
            id = 1L,
            username = "testuser",
            displayName = "Test User",
            avatarEmoji = "💪",
            createdAt = testDate,
            updatedAt = testDate
        )
    }

    @Test
    fun `insertUserProfile_validProfile_returnsId`() = runTest {
        // Arrange
        coEvery { dao.insertUserProfile(any()) } returns 1L

        // Act
        val result = dao.insertUserProfile(mockProfile)

        // Assert
        assertThat(result).isEqualTo(1L)
        coVerify(exactly = 1) { dao.insertUserProfile(mockProfile) }
    }

    @Test
    fun `getUserProfile_byId_existingUser_returnsProfile`() = runTest {
        // Arrange
        coEvery { dao.getUserProfile(1L) } returns mockProfile

        // Act
        val result = dao.getUserProfile(1L)

        // Assert
        assertThat(result).isEqualTo(mockProfile)
        assertThat(result?.username).isEqualTo("testuser")
    }

    @Test
    fun `getUserProfile_byId_nonExistentUser_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getUserProfile(999L) } returns null

        // Act
        val result = dao.getUserProfile(999L)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getUserProfileFlow_existingUser_emitsProfile`() = runTest {
        // Arrange
        val flow: Flow<UserProfile?> = flowOf(mockProfile)
        every { dao.getUserProfileFlow(1L) } returns flow

        // Act
        val result = dao.getUserProfileFlow(1L).first()

        // Assert
        assertThat(result).isEqualTo(mockProfile)
    }

    @Test
    fun `getUserProfileFlow_nonExistentUser_emitsNull`() = runTest {
        // Arrange
        val flow: Flow<UserProfile?> = flowOf(null)
        every { dao.getUserProfileFlow(999L) } returns flow

        // Act
        val result = dao.getUserProfileFlow(999L).first()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `getAllUsers_hasUsers_returnsListSortedByDisplayName`() = runTest {
        // Arrange
        val users = listOf(
            mockProfile.copy(id = 1, displayName = "Alice"),
            mockProfile.copy(id = 2, displayName = "Bob"),
            mockProfile.copy(id = 3, displayName = "Charlie")
        )
        coEvery { dao.getAllUsers() } returns users

        // Act
        val result = dao.getAllUsers()

        // Assert
        assertThat(result).hasSize(3)
        assertThat(result[0].displayName).isEqualTo("Alice")
        assertThat(result[1].displayName).isEqualTo("Bob")
        assertThat(result[2].displayName).isEqualTo("Charlie")
    }

    @Test
    fun `getAllUsers_noUsers_returnsEmptyList`() = runTest {
        // Arrange
        coEvery { dao.getAllUsers() } returns emptyList()

        // Act
        val result = dao.getAllUsers()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `getUserProfile_noParams_existingUser_returnsFirstProfile`() = runTest {
        // Arrange
        coEvery { dao.getUserProfile() } returns mockProfile

        // Act
        val result = dao.getUserProfile()

        // Assert
        assertThat(result).isEqualTo(mockProfile)
    }

    @Test
    fun `getUserProfile_noParams_noUsers_returnsNull`() = runTest {
        // Arrange
        coEvery { dao.getUserProfile() } returns null

        // Act
        val result = dao.getUserProfile()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `insertUserProfile_replacesOnConflict`() = runTest {
        // Arrange
        val updatedProfile = mockProfile.copy(
            displayName = "Updated Name",
            updatedAt = testDate.plusDays(1)
        )
        coEvery { dao.insertUserProfile(updatedProfile) } returns 1L

        // Act
        val result = dao.insertUserProfile(updatedProfile)

        // Assert
        assertThat(result).isEqualTo(1L)
        coVerify(exactly = 1) { dao.insertUserProfile(updatedProfile) }
    }

    @Test
    fun `profile contains all required fields`() = runTest {
        // Assert
        assertThat(mockProfile.id).isEqualTo(1L)
        assertThat(mockProfile.username).isEqualTo("testuser")
        assertThat(mockProfile.displayName).isEqualTo("Test User")
        assertThat(mockProfile.avatarEmoji).isEqualTo("💪")
        assertThat(mockProfile.createdAt).isEqualTo(testDate)
        assertThat(mockProfile.updatedAt).isEqualTo(testDate)
    }

    @Test
    fun `insertUserProfile_withDifferentEmoji_returnsId`() = runTest {
        // Arrange
        val profileWithEmoji = mockProfile.copy(avatarEmoji = "🏋️")
        coEvery { dao.insertUserProfile(profileWithEmoji) } returns 2L

        // Act
        val result = dao.insertUserProfile(profileWithEmoji)

        // Assert
        assertThat(result).isEqualTo(2L)
        coVerify { dao.insertUserProfile(match { it.avatarEmoji == "🏋️" }) }
    }
}