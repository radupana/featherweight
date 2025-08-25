package com.github.radupana.featherweight.service

import com.github.radupana.featherweight.data.exercise.RMScalingType
import com.github.radupana.featherweight.testutil.LogMock
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import kotlin.math.pow

/**
 * Unit tests for OneRMService
 * 
 * Tests the core 1RM calculation engine including:
 * - Different formulas (Brzycki, Epley, etc.)
 * - RPE adjustments
 * - Edge cases and validation
 */
class OneRMServiceTest {
    
    private lateinit var service: OneRMService
    
    @Before
    fun setUp() {
        LogMock.setup()
        service = OneRMService()
    }
    
    // ========== calculateEstimated1RM Tests ==========
    
    @Test
    fun `calculateEstimated1RM with standard formula returns correct value`() {
        // Arrange - Using Brzycki formula
        val weight = 100f
        val reps = 5
        // Expected: 100 / (1.0278 - 0.0278 * 5) = 100 / 0.8888 = 112.5
        
        // Act
        val result = service.calculateEstimated1RM(weight, reps, scalingType = RMScalingType.STANDARD)
        
        // Assert
        assertThat(result).isNotNull()
        assertThat(result).isWithin(0.5f).of(112.5f)
    }
    
    @Test
    fun `calculateEstimated1RM with 1 rep returns exact weight`() {
        // Arrange
        val weight = 150f
        
        // Act
        val result = service.calculateEstimated1RM(weight, 1)
        
        // Assert
        assertThat(result).isEqualTo(weight)
    }
    
    @Test
    fun `calculateEstimated1RM with RPE adjustment calculates correctly`() {
        // Arrange
        val weight = 80f
        val reps = 8
        val rpe = 8f // 2 reps in reserve
        // Total capacity = 8 + 2 = 10 reps
        // Expected: 80 / (1.0278 - 0.0278 * 10) = 80 / 0.75 = 106.67
        
        // Act
        val result = service.calculateEstimated1RM(weight, reps, rpe)
        
        // Assert
        assertThat(result).isNotNull()
        assertThat(result).isWithin(1f).of(106.67f)
    }
    
    @Test
    fun `calculateEstimated1RM with RPE 10 uses actual reps`() {
        // Arrange
        val weight = 100f
        val reps = 3
        val rpe = 10f // No reps in reserve
        
        // Act
        val result = service.calculateEstimated1RM(weight, reps, rpe)
        
        // Assert
        val expected = 100f / (1.0278f - 0.0278f * 3)
        assertThat(result).isWithin(0.5f).of(expected)
    }
    
    @Test
    fun `calculateEstimated1RM with low RPE returns null`() {
        // Arrange
        val weight = 100f
        val reps = 10
        val rpe = 6f // Too low for reliable estimate
        
        // Act
        val result = service.calculateEstimated1RM(weight, reps, rpe)
        
        // Assert
        assertThat(result).isNull()
    }
    
    @Test
    fun `calculateEstimated1RM with zero reps returns null`() {
        // Act
        val result = service.calculateEstimated1RM(100f, 0)
        
        // Assert
        assertThat(result).isNull()
    }
    
    @Test
    fun `calculateEstimated1RM with negative reps returns null`() {
        // Act
        val result = service.calculateEstimated1RM(100f, -1)
        
        // Assert
        assertThat(result).isNull()
    }
    
    @Test
    fun `calculateEstimated1RM with too many reps returns null`() {
        // Act
        val result = service.calculateEstimated1RM(100f, 16) // MAX_REPS_FOR_ESTIMATE = 15
        
        // Assert
        assertThat(result).isNull()
    }
    
    @Test
    fun `calculateEstimated1RM with weighted bodyweight scaling uses correct formula`() {
        // Arrange
        val weight = 20f // Additional weight
        val reps = 5
        // Expected: 20 * (1 + 5 * 0.035) = 20 * 1.175 = 23.5
        
        // Act
        val result = service.calculateEstimated1RM(
            weight, 
            reps, 
            scalingType = RMScalingType.WEIGHTED_BODYWEIGHT
        )
        
        // Assert
        assertThat(result).isNotNull()
        assertThat(result).isWithin(0.1f).of(23.5f)
    }
    
    @Test
    fun `calculateEstimated1RM with isolation scaling uses conservative formula`() {
        // Arrange
        val weight = 30f
        val reps = 10
        // Lombardi formula: weight * reps^0.10
        val expected = 30f * 10f.pow(0.10f)
        
        // Act
        val result = service.calculateEstimated1RM(
            weight,
            reps,
            scalingType = RMScalingType.ISOLATION
        )
        
        // Assert
        assertThat(result).isNotNull()
        assertThat(result).isWithin(0.5f).of(expected)
    }
    
    @Test
    fun `calculateEstimated1RM handles boundary case of 15 reps`() {
        // Arrange
        val weight = 50f
        val reps = 15 // Maximum allowed
        
        // Act
        val result = service.calculateEstimated1RM(weight, reps)
        
        // Assert
        assertThat(result).isNotNull()
        assertThat(result).isGreaterThan(weight) // Should be higher than working weight
    }
    
    @Test
    fun `calculateEstimated1RM with RPE 7 and high reps calculates correctly`() {
        // Arrange
        val weight = 60f
        val reps = 12
        val rpe = 7f // 3 reps in reserve
        // Total capacity = 12 + 3 = 15 reps (at boundary)
        
        // Act
        val result = service.calculateEstimated1RM(weight, reps, rpe)
        
        // Assert
        assertThat(result).isNotNull()
        val expected = 60f / (1.0278f - 0.0278f * 15)
        assertThat(result).isWithin(1f).of(expected)
    }
    
    // ========== calculateConfidence Tests ==========
    
    @Test
    fun `calculateConfidence returns high value for single rep at high load`() {
        // Act
        val confidence = service.calculateConfidence(1, 10f, 0.95f) // 1 rep at RPE 10, 95% of 1RM
        
        // Assert
        assertThat(confidence).isGreaterThan(0.9f)
    }
    
    @Test
    fun `calculateConfidence returns high value for low reps with RPE`() {
        // Act
        val confidence = service.calculateConfidence(3, 9f, 0.85f) // 3 reps at RPE 9, 85% of 1RM
        
        // Assert
        assertThat(confidence).isGreaterThan(0.7f)
    }
    
    @Test
    fun `calculateConfidence returns lower value for high reps`() {
        // Act
        val confidence = service.calculateConfidence(12, null, 0.65f) // 12 reps at 65% of 1RM
        
        // Assert
        assertThat(confidence).isLessThan(0.6f)
    }
    
    @Test
    fun `calculateConfidence increases with RPE data`() {
        // Arrange & Act
        val withoutRPE = service.calculateConfidence(5, null, 0.75f)
        val withRPE = service.calculateConfidence(5, 8f, 0.75f)
        
        // Assert
        assertThat(withRPE).isGreaterThan(withoutRPE)
    }
    
    @Test
    fun `calculateConfidence handles edge cases`() {
        // Test with 0 reps - should have very low confidence
        val zeroRepsConfidence = service.calculateConfidence(0, null, 0.8f)
        assertThat(zeroRepsConfidence).isLessThan(0.3f)
        
        // Test with very high reps - should have lower confidence
        // With 20 reps (capped at 15): repScore = 1/15 = 0.067, rpeScore = 0.3, loadScore = 0.5
        // Total = 0.067*0.5 + 0.3*0.3 + 0.5*0.2 = 0.033 + 0.09 + 0.1 = 0.223
        val highRepsConfidence = service.calculateConfidence(20, null, 0.5f)
        assertThat(highRepsConfidence).isLessThan(0.3f)
        
        // Test with perfect conditions - 1 rep at RPE 10 at 100% 1RM
        val perfectConfidence = service.calculateConfidence(1, 10f, 1.0f)
        assertThat(perfectConfidence).isGreaterThan(0.9f)
    }
}
