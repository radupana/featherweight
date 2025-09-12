package com.github.radupana.featherweight.service

import android.util.Log
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field

class ConfigServiceFactoryTest {
    @Before
    fun setUp() {
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any(), any()) } returns 0

        // Reset the singleton state before each test
        resetConfigServiceFactory()
    }

    @After
    fun tearDown() {
        unmockkAll()
        resetConfigServiceFactory()
    }

    private fun resetConfigServiceFactory() {
        // Use reflection to reset the singleton state
        val configServiceField: Field = ConfigServiceFactory::class.java.getDeclaredField("configService")
        configServiceField.isAccessible = true
        configServiceField.set(null, null)

        ConfigServiceFactory.isTestMode = false
    }

    @Test
    fun `getConfigService returns TestConfigService when in test mode`() {
        // Given
        ConfigServiceFactory.isTestMode = true

        // When
        val service = ConfigServiceFactory.getConfigService()

        // Then
        assertThat(service).isNotNull()
        verify { Log.d("ConfigServiceFactory", "Using test config service") }
    }

    @Test
    fun `getConfigService returns same instance on multiple calls`() {
        // Given
        ConfigServiceFactory.isTestMode = true

        // When
        val service1 = ConfigServiceFactory.getConfigService()
        val service2 = ConfigServiceFactory.getConfigService()
        val service3 = ConfigServiceFactory.getConfigService()

        // Then
        assertThat(service1).isSameInstanceAs(service2)
        assertThat(service2).isSameInstanceAs(service3)
    }

    @Test
    fun `getConfigService attempts RemoteConfigService when not in test mode`() {
        // Given
        ConfigServiceFactory.isTestMode = false
        mockkObject(RemoteConfigService)
        val mockRemoteService = mockk<RemoteConfigService>(relaxed = true)
        every { RemoteConfigService.getInstance() } returns mockRemoteService

        // When
        val service = ConfigServiceFactory.getConfigService()

        // Then
        assertThat(service).isSameInstanceAs(mockRemoteService)
        verify { Log.d("ConfigServiceFactory", "Using remote config service") }
    }

    @Test
    fun `getConfigService falls back to TestConfigService when RemoteConfigService fails`() {
        // Given
        ConfigServiceFactory.isTestMode = false
        mockkObject(RemoteConfigService)
        every { RemoteConfigService.getInstance() } throws RuntimeException("Firebase not available")

        // When
        val service = ConfigServiceFactory.getConfigService()

        // Then
        assertThat(service).isNotNull()
        verify { Log.w("ConfigServiceFactory", "Failed to initialize RemoteConfigService, using test config", any()) }
    }

    @Test
    fun `TestConfigService returns null for API key`() =
        runTest {
            // Given
            ConfigServiceFactory.isTestMode = true
            val service = ConfigServiceFactory.getConfigService()

            // When
            val apiKey = service.getOpenAIApiKey()

            // Then
            assertThat(apiKey).isNull()
        }

    @Test
    fun `getConfigService is thread-safe`() =
        runTest {
            // Given
            ConfigServiceFactory.isTestMode = true
            val services = mutableListOf<ConfigService>()

            // When - Call from multiple coroutines
            val deferreds =
                (1..10).map {
                    async {
                        ConfigServiceFactory.getConfigService()
                    }
                }
            val results = deferreds.awaitAll()
            services.addAll(results)

            // Then - All should be the same instance
            assertThat(services).isNotEmpty()
            val firstService = services.first()
            services.forEach { service ->
                assertThat(service).isSameInstanceAs(firstService)
            }
        }

    @Test
    fun `isTestMode can be changed at runtime`() {
        // Given
        assertThat(ConfigServiceFactory.isTestMode).isFalse()

        // When
        ConfigServiceFactory.isTestMode = true

        // Then
        assertThat(ConfigServiceFactory.isTestMode).isTrue()

        // And when changed back
        ConfigServiceFactory.isTestMode = false
        assertThat(ConfigServiceFactory.isTestMode).isFalse()
    }

    @Test
    fun `getConfigService logs appropriate messages`() {
        // Given
        ConfigServiceFactory.isTestMode = true

        // When
        ConfigServiceFactory.getConfigService()

        // Then
        verify(exactly = 1) { Log.d("ConfigServiceFactory", "Using test config service") }
    }
}
