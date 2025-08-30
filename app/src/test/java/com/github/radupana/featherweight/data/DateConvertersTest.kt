package com.github.radupana.featherweight.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

class DateConvertersTest {
    @Test
    fun `fromTimestamp converts ISO string to LocalDateTime`() {
        val timestamp = "2024-01-15T10:30:45"

        val result = DateConverters.fromTimestamp(timestamp)

        assertThat(result).isNotNull()
        assertThat(result?.year).isEqualTo(2024)
        assertThat(result?.monthValue).isEqualTo(1)
        assertThat(result?.dayOfMonth).isEqualTo(15)
        assertThat(result?.hour).isEqualTo(10)
        assertThat(result?.minute).isEqualTo(30)
        assertThat(result?.second).isEqualTo(45)
    }

    @Test
    fun `fromTimestamp returns null for null input`() {
        val result = DateConverters.fromTimestamp(null)

        assertThat(result).isNull()
    }

    @Test
    fun `dateToTimestamp converts LocalDateTime to ISO string`() {
        val date = LocalDateTime.of(2024, 3, 20, 14, 45, 30)

        val result = DateConverters.dateToTimestamp(date)

        assertThat(result).isEqualTo("2024-03-20T14:45:30")
    }

    @Test
    fun `dateToTimestamp returns null for null input`() {
        val result = DateConverters.dateToTimestamp(null)

        assertThat(result).isNull()
    }

    @Test
    fun `fromLocalDate converts ISO date string to LocalDate`() {
        val dateString = "2024-06-15"

        val result = DateConverters.fromLocalDate(dateString)

        assertThat(result).isNotNull()
        assertThat(result?.year).isEqualTo(2024)
        assertThat(result?.monthValue).isEqualTo(6)
        assertThat(result?.dayOfMonth).isEqualTo(15)
    }

    @Test
    fun `fromLocalDate returns null for null input`() {
        val result = DateConverters.fromLocalDate(null)

        assertThat(result).isNull()
    }

    @Test
    fun `localDateToString converts LocalDate to ISO string`() {
        val date = LocalDate.of(2024, 12, 25)

        val result = DateConverters.localDateToString(date)

        assertThat(result).isEqualTo("2024-12-25")
    }

    @Test
    fun `localDateToString returns null for null input`() {
        val result = DateConverters.localDateToString(null)

        assertThat(result).isNull()
    }

    @Test
    fun `fromDate converts Date to timestamp`() {
        val date = Date(1705315200000L)

        val result = DateConverters.fromDate(date)

        assertThat(result).isEqualTo(1705315200000L)
    }

    @Test
    fun `fromDate returns null for null input`() {
        val result = DateConverters.fromDate(null)

        assertThat(result).isNull()
    }

    @Test
    fun `toDate converts timestamp to Date`() {
        val timestamp = 1705315200000L

        val result = DateConverters.toDate(timestamp)

        assertThat(result).isNotNull()
        assertThat(result?.time).isEqualTo(timestamp)
    }

    @Test
    fun `toDate returns null for null input`() {
        val result = DateConverters.toDate(null)

        assertThat(result).isNull()
    }

    @Test
    fun `round trip conversion for LocalDateTime`() {
        val original = LocalDateTime.of(2024, 8, 26, 16, 30, 45, 123456789)

        val string = DateConverters.dateToTimestamp(original)
        val result = DateConverters.fromTimestamp(string)

        assertThat(result?.withNano(0)).isEqualTo(original.withNano(0))
    }

    @Test
    fun `round trip conversion for LocalDate`() {
        val original = LocalDate.of(2024, 7, 4)

        val string = DateConverters.localDateToString(original)
        val result = DateConverters.fromLocalDate(string)

        assertThat(result).isEqualTo(original)
    }

    @Test
    fun `round trip conversion for Date`() {
        val original = Date(System.currentTimeMillis())

        val timestamp = DateConverters.fromDate(original)
        val result = DateConverters.toDate(timestamp)

        assertThat(result).isEqualTo(original)
    }

    @Test
    fun `fromTimestamp handles leap year`() {
        val timestamp = "2024-02-29T12:00:00"

        val result = DateConverters.fromTimestamp(timestamp)

        assertThat(result).isNotNull()
        assertThat(result?.monthValue).isEqualTo(2)
        assertThat(result?.dayOfMonth).isEqualTo(29)
    }

    @Test
    fun `fromLocalDate handles leap year`() {
        val dateString = "2024-02-29"

        val result = DateConverters.fromLocalDate(dateString)

        assertThat(result).isNotNull()
        assertThat(result?.monthValue).isEqualTo(2)
        assertThat(result?.dayOfMonth).isEqualTo(29)
    }

    @Test
    fun `dateToTimestamp preserves midnight time`() {
        val midnight = LocalDateTime.of(2024, 1, 1, 0, 0, 0)

        val result = DateConverters.dateToTimestamp(midnight)

        assertThat(result).isEqualTo("2024-01-01T00:00:00")
    }

    @Test
    fun `dateToTimestamp preserves end of day time`() {
        val endOfDay = LocalDateTime.of(2024, 12, 31, 23, 59, 59)

        val result = DateConverters.dateToTimestamp(endOfDay)

        assertThat(result).isEqualTo("2024-12-31T23:59:59")
    }

    @Test
    fun `fromDate handles epoch time`() {
        val epochDate = Date(0L)

        val result = DateConverters.fromDate(epochDate)

        assertThat(result).isEqualTo(0L)
    }

    @Test
    fun `toDate handles epoch time`() {
        val result = DateConverters.toDate(0L)

        assertThat(result).isNotNull()
        assertThat(result?.time).isEqualTo(0L)
    }

    @Test
    fun `fromDate handles negative timestamp for dates before epoch`() {
        val beforeEpoch = Date(-86400000L)

        val result = DateConverters.fromDate(beforeEpoch)

        assertThat(result).isEqualTo(-86400000L)
    }

    @Test
    fun `toDate handles negative timestamp for dates before epoch`() {
        val result = DateConverters.toDate(-86400000L)

        assertThat(result).isNotNull()
        assertThat(result?.time).isEqualTo(-86400000L)
    }

    @Test
    fun `localDateToString handles start of year`() {
        val newYear = LocalDate.of(2024, 1, 1)

        val result = DateConverters.localDateToString(newYear)

        assertThat(result).isEqualTo("2024-01-01")
    }

    @Test
    fun `localDateToString handles end of year`() {
        val newYearsEve = LocalDate.of(2024, 12, 31)

        val result = DateConverters.localDateToString(newYearsEve)

        assertThat(result).isEqualTo("2024-12-31")
    }

    @Test
    fun `fromTimestamp preserves nanoseconds as zero`() {
        val withNanos = "2024-01-15T10:30:45.123456789"

        val result = DateConverters.fromTimestamp(withNanos)

        assertThat(result).isNotNull()
        assertThat(result?.nano).isGreaterThan(0)
    }

    @Test
    fun `dateToTimestamp handles single digit month and day`() {
        val date = LocalDateTime.of(2024, 3, 5, 9, 5, 3)

        val result = DateConverters.dateToTimestamp(date)

        assertThat(result).isEqualTo("2024-03-05T09:05:03")
    }

    @Test
    fun `localDateToString handles single digit month and day`() {
        val date = LocalDate.of(2024, 3, 5)

        val result = DateConverters.localDateToString(date)

        assertThat(result).isEqualTo("2024-03-05")
    }

    @Test
    fun `fromDate handles max long value`() {
        val maxDate = Date(Long.MAX_VALUE)

        val result = DateConverters.fromDate(maxDate)

        assertThat(result).isEqualTo(Long.MAX_VALUE)
    }
}
