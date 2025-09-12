package com.github.radupana.featherweight.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class RestTimerNotificationServiceTest {
    private lateinit var soundProvider: SoundProvider
    private lateinit var vibrationProvider: VibrationProvider
    private lateinit var service: RestTimerNotificationService

    @Before
    fun setUp() {
        soundProvider = mockk(relaxed = true)
        vibrationProvider = mockk(relaxed = true)
        service = RestTimerNotificationService(soundProvider, vibrationProvider)
    }

    @Test
    fun `notifyTimerCompleted triggers both sound and vibration`() {
        service.notifyTimerCompleted()

        verify { soundProvider.playNotificationSound() }
        verify { vibrationProvider.vibratePattern(any()) }
    }

    @Test
    fun `notifyTimerCompleted uses correct vibration pattern`() {
        service.notifyTimerCompleted()

        val expectedPattern = longArrayOf(0, 100, 80, 60, 50, 60, 50, 60)
        verify { vibrationProvider.vibratePattern(expectedPattern) }
    }

    @Test
    fun `notifyTimerCompleted handles sound provider failure gracefully`() {
        every { soundProvider.playNotificationSound() } throws RuntimeException("Sound failure")

        // Should not throw
        service.notifyTimerCompleted()

        // Vibration should still be attempted
        verify { vibrationProvider.vibratePattern(any()) }
    }

    @Test
    fun `notifyTimerCompleted handles vibration provider failure gracefully`() {
        every { vibrationProvider.vibratePattern(any()) } throws RuntimeException("Vibration failure")

        // Should not throw
        service.notifyTimerCompleted()

        // Sound should still be attempted
        verify { soundProvider.playNotificationSound() }
    }

    @Test
    fun `notifyTimerCompleted handles both providers failing gracefully`() {
        every { soundProvider.playNotificationSound() } throws RuntimeException("Sound failure")
        every { vibrationProvider.vibratePattern(any()) } throws RuntimeException("Vibration failure")

        // Should not throw exception
        service.notifyTimerCompleted()

        // Both should have been attempted
        verify { soundProvider.playNotificationSound() }
        verify { vibrationProvider.vibratePattern(any()) }
    }

    @Test
    fun `notifyTimerCompleted calls providers in correct order`() {
        service.notifyTimerCompleted()

        verify(ordering = io.mockk.Ordering.ORDERED) {
            soundProvider.playNotificationSound()
            vibrationProvider.vibratePattern(any())
        }
    }

    @Test
    fun `vibration pattern matches expected celebration pattern`() {
        val expectedPattern = longArrayOf(0, 100, 80, 60, 50, 60, 50, 60)

        service.notifyTimerCompleted()

        verify(exactly = 1) { vibrationProvider.vibratePattern(expectedPattern) }
    }

    @Test
    fun `notifyTimerCompleted can be called multiple times`() {
        service.notifyTimerCompleted()
        service.notifyTimerCompleted()
        service.notifyTimerCompleted()

        verify(exactly = 3) { soundProvider.playNotificationSound() }
        verify(exactly = 3) { vibrationProvider.vibratePattern(any()) }
    }

    @Test
    fun `service works with null exceptions from providers`() {
        every { soundProvider.playNotificationSound() } throws NullPointerException()
        every { vibrationProvider.vibratePattern(any()) } throws NullPointerException()

        // Should handle gracefully
        service.notifyTimerCompleted()

        verify { soundProvider.playNotificationSound() }
        verify { vibrationProvider.vibratePattern(any()) }
    }

    @Test
    fun `vibration continues even when sound throws checked exception`() {
        every { soundProvider.playNotificationSound() } throws Exception("Checked exception")

        service.notifyTimerCompleted()

        verify { soundProvider.playNotificationSound() }
        verify { vibrationProvider.vibratePattern(any()) }
    }
}
