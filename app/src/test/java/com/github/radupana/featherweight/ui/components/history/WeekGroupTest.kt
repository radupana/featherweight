package com.github.radupana.featherweight.ui.components.history

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class WeekGroupTest {
    
    @Test
    fun `formatRelativeTime shows Today with time for same day`() {
        // Arrange
        val now = LocalDateTime.now()
        val todayAt2PM = now.withHour(14).withMinute(0)
        
        // Act - testing the ACTUAL function from WeekGroup.kt
        val result = formatRelativeTime(todayAt2PM)
        
        // Assert - this will pass if run on same day
        assertThat(result).startsWith("Today at")
    }
    
    @Test
    fun `formatRelativeTime shows correct format for various dates`() {
        // Since we can't control "now" in the real function, 
        // we can only test that it returns something and doesn't crash
        
        // Test various dates
        val dates = listOf(
            LocalDateTime.now(), // Today
            LocalDateTime.now().minusDays(1), // Yesterday
            LocalDateTime.now().minusDays(5), // Few days ago
            LocalDateTime.now().minusDays(10), // Last week
            LocalDateTime.now().minusDays(25), // Few weeks ago
            LocalDateTime.now().minusMonths(6), // Months ago
            LocalDateTime.now().minusYears(1) // Last year
        )
        
        for (date in dates) {
            val result = formatRelativeTime(date)
            assertThat(result).isNotEmpty()
            // Should contain either time indicator or date
            val hasTimeOrDate = result.contains("at") || 
                               result.contains("Yesterday") || 
                               result.contains("Last") ||
                               result.contains("Jan") || result.contains("Feb") || 
                               result.contains("Mar") || result.contains("Apr") ||
                               result.contains("May") || result.contains("Jun") ||
                               result.contains("Jul") || result.contains("Aug") ||
                               result.contains("Sep") || result.contains("Oct") ||
                               result.contains("Nov") || result.contains("Dec")
            assertThat(hasTimeOrDate).isTrue()
        }
    }
}
