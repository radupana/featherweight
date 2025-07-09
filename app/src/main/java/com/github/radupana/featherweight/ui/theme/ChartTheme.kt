package com.github.radupana.featherweight.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Centralized chart theme for consistent colors across all chart components.
 * Optimized for dark mode visibility with proper contrast ratios.
 */
object ChartTheme {
    
    // Tooltip colors with enhanced contrast for dark mode
    @Composable
    fun tooltipBackgroundColor() = MaterialTheme.colorScheme.surface
    
    @Composable
    fun tooltipContentColor() = MaterialTheme.colorScheme.onSurface
    
    @Composable
    fun tooltipBorderColor() = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    
    // Chart line and data point colors
    @Composable
    fun primaryChartColor() = MaterialTheme.colorScheme.primary
    
    @Composable
    fun secondaryChartColor() = MaterialTheme.colorScheme.secondary
    
    // Grid and axis colors
    @Composable
    fun gridLineColor() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    
    @Composable
    fun axisLabelColor() = MaterialTheme.colorScheme.onSurfaceVariant
    
    // Special markers
    val prMarkerColor = Color(0xFFFFA726) // Warmer orange instead of gold for better visibility
    val prMarkerTextColor = Color(0xFF6D4C41) // Dark brown for contrast
    
    // Chart background
    @Composable
    fun chartBackgroundColor() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    
    // Data visualization colors
    @Composable
    fun positiveChangeColor() = Color(0xFF66BB6A) // Softer green
    
    @Composable
    fun negativeChangeColor() = Color(0xFFEF5350) // Softer red
    
    // Selection states
    @Composable
    fun selectedPointColor() = MaterialTheme.colorScheme.tertiary
    
    @Composable
    fun selectedPointBorderColor() = MaterialTheme.colorScheme.onTertiary
    
    // Chart-specific elevation and shadows
    const val tooltipElevation = 8f
    const val tooltipShadowAlpha = 0.25f
}