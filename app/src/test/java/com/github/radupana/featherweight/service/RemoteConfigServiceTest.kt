package com.github.radupana.featherweight.service

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for RemoteConfigService.
 *
 * Note: These tests focus on the business logic of the service (caching, validation, etc.)
 * rather than the Firebase integration details which are difficult to mock properly.
 */
class RemoteConfigServiceTest {
    private lateinit var mockRemoteConfig: FirebaseRemoteConfig
    private lateinit var service: RemoteConfigService

    @Before
    fun setUp() {
        mockRemoteConfig = mockk(relaxed = true)

        // Mock static Log calls to avoid runtime errors
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0

        // Mock Firebase.remoteConfig for getInstance() tests
        mockkStatic("com.google.firebase.remoteconfig.RemoteConfigKt")
        every { any<Firebase>().remoteConfig } returns mockRemoteConfig

        // Mock fetchAndActivate to return a completed task
        val completedTask: Task<Boolean> = Tasks.forResult(true)
        every { mockRemoteConfig.fetchAndActivate() } returns completedTask

        service = RemoteConfigService(mockRemoteConfig)
    }

    @After
    fun tearDown() {
        unmockkStatic(android.util.Log::class)
        unmockkStatic("com.google.firebase.remoteconfig.RemoteConfigKt")

        // Clear the singleton instance for next test using reflection
        try {
            val companionClass = RemoteConfigService::class.java.getDeclaredField("Companion")
            companionClass.isAccessible = true
            val companion = companionClass.get(null)
            val instanceField = companion.javaClass.getDeclaredField("instance")
            instanceField.isAccessible = true
            instanceField.set(companion, null)
        } catch (e: Exception) {
            // Ignore if reflection fails
        }
    }

    @Test
    fun `getOpenAIApiKey - with valid key - returns key`() =
        runTest {
            // Arrange
            every { mockRemoteConfig.getString("openai_api_key") } returns "valid-api-key"

            // Act
            val result = service.getOpenAIApiKey()

            // Assert
            assertEquals("valid-api-key", result)
        }

    @Test
    fun `getOpenAIApiKey - with empty key - returns null and logs warning`() =
        runTest {
            // Arrange
            every { mockRemoteConfig.getString("openai_api_key") } returns ""

            // Act
            val result = service.getOpenAIApiKey()

            // Assert
            assertNull(result)
            verify {
                android.util.Log.w(
                    "RemoteConfigService",
                    "OpenAI API key not found in remote config",
                )
            }
        }

    @Test
    fun `getOpenAIApiKey - with null string literal - returns null`() =
        runTest {
            // Arrange
            every { mockRemoteConfig.getString("openai_api_key") } returns "null"

            // Act
            val result = service.getOpenAIApiKey()

            // Assert
            assertNull(result)
        }

    @Test
    fun `getOpenAIApiKey - with placeholder key - returns null`() =
        runTest {
            // Arrange
            every { mockRemoteConfig.getString("openai_api_key") } returns "YOUR_API_KEY_HERE"

            // Act
            val result = service.getOpenAIApiKey()

            // Assert
            assertNull(result)
        }

    @Test
    fun `getOpenAIApiKey - caches valid key for subsequent calls`() =
        runTest {
            // Arrange
            every { mockRemoteConfig.getString("openai_api_key") } returns "cached-api-key"

            // Act - First call should fetch from remote config
            val result1 = service.getOpenAIApiKey()

            // Clear mocks to verify no further calls
            clearMocks(mockRemoteConfig, answers = false)
            every { mockRemoteConfig.getString("openai_api_key") } returns "different-key"

            // Second call should use cached value
            val result2 = service.getOpenAIApiKey()

            // Assert
            assertEquals("cached-api-key", result1)
            assertEquals("cached-api-key", result2) // Should still be cached value
            verify(exactly = 0) { mockRemoteConfig.getString("openai_api_key") } // No call on second attempt
        }

    @Test
    fun `getOpenAIApiKey - does not cache invalid keys`() =
        runTest {
            // Arrange - First call returns empty
            every { mockRemoteConfig.getString("openai_api_key") } returns ""

            // Act
            val result1 = service.getOpenAIApiKey()

            // Now return a valid key
            every { mockRemoteConfig.getString("openai_api_key") } returns "valid-key"

            // Second call should fetch again since first was invalid
            val result2 = service.getOpenAIApiKey()

            // Assert
            assertNull(result1)
            assertEquals("valid-key", result2)
            verify(exactly = 2) { mockRemoteConfig.getString("openai_api_key") }
        }

    @Test
    fun `init block - sets config settings and defaults`() {
        // Arrange
        val newMockRemoteConfig = mockk<FirebaseRemoteConfig>(relaxed = true)

        // Act
        RemoteConfigService(newMockRemoteConfig)

        // Assert
        verify { newMockRemoteConfig.setConfigSettingsAsync(any()) }
        verify { newMockRemoteConfig.setDefaultsAsync(any<Map<String, Any>>()) }
    }

    @Test
    fun `getInstance - returns singleton instance`() {
        // Act
        val instance1 = RemoteConfigService.getInstance()
        val instance2 = RemoteConfigService.getInstance()

        // Assert
        assertTrue(instance1 === instance2, "Should return the same instance")
    }

    @Test
    fun `getInstance - multiple calls return same instance`() {
        // Act
        val instances = mutableListOf<RemoteConfigService>()
        repeat(10) {
            instances.add(RemoteConfigService.getInstance())
        }

        // Assert
        assertTrue(instances.all { it === instances[0] }, "All instances should be the same")
    }

    @Test
    fun `getOpenAIApiKey - validates multiple invalid key formats`() =
        runTest {
            // Test various invalid key formats that are actually checked in the implementation
            val invalidKeys =
                listOf(
                    "",
                    "null",
                    "YOUR_API_KEY_HERE",
                )

            for (invalidKey in invalidKeys) {
                // Arrange
                val freshService = RemoteConfigService(mockRemoteConfig)
                every { mockRemoteConfig.getString("openai_api_key") } returns invalidKey

                // Act
                val result = freshService.getOpenAIApiKey()

                // Assert
                assertNull(result, "Key '$invalidKey' should be considered invalid")
            }

            // These should be considered valid since the code doesn't check for them
            val actuallyValidKeys =
                listOf(
                    "NULL", // Different case - would be valid
                    "your_api_key_here", // Different case - would be valid
                    "YOUR-API-KEY-HERE", // Different separator - would be valid
                )

            for (validKey in actuallyValidKeys) {
                // Arrange
                val freshService = RemoteConfigService(mockRemoteConfig)
                every { mockRemoteConfig.getString("openai_api_key") } returns validKey

                // Act
                val result = freshService.getOpenAIApiKey()

                // Assert - These are NOT filtered out by the current implementation
                assertEquals(validKey, result, "Key '$validKey' is actually accepted by the implementation")
            }
        }

    @Test
    fun `getOpenAIApiKey - accepts various valid key formats`() =
        runTest {
            // Test various valid key formats (anything that's not empty or a placeholder)
            val validKeys =
                listOf(
                    "sk-abcd1234",
                    "test-key-123",
                    "api_key_value",
                    "1234567890",
                    "very-long-api-key-with-many-characters-that-should-still-be-valid",
                )

            for (validKey in validKeys) {
                // Arrange
                clearMocks(mockRemoteConfig, answers = false)

                // Set up mocks for each new service instance
                val completedTask: Task<Boolean> = Tasks.forResult(true)
                every { mockRemoteConfig.fetchAndActivate() } returns completedTask
                every { mockRemoteConfig.getString("openai_api_key") } returns validKey

                val freshService = RemoteConfigService(mockRemoteConfig)

                // Act
                val result = freshService.getOpenAIApiKey()

                // Assert
                assertEquals(validKey, result, "Key '$validKey' should be considered valid")
            }
        }
}
