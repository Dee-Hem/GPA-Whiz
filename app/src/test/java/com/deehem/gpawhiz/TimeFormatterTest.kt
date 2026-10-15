package com.deehem.gpawhiz

import com.deehem.gpawhiz.service.formatHourLabel
import com.deehem.gpawhiz.service.formatStoredTime
import com.deehem.gpawhiz.service.formatTime
import com.deehem.gpawhiz.service.formatTimeRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeFormatterTest {
    @Test
    fun formatsStoredTimesAsTwelveHourAmPmText() {
        val cases = listOf(
            (0 to 0) to "12:00\u00A0AM",
            (0 to 30) to "12:30\u00A0AM",
            (9 to 5) to "9:05\u00A0AM",
            (11 to 59) to "11:59\u00A0AM",
            (12 to 0) to "12:00\u00A0PM",
            (12 to 30) to "12:30\u00A0PM",
            (13 to 0) to "1:00\u00A0PM",
            (23 to 59) to "11:59\u00A0PM"
        )

        cases.forEach { (time, expected) ->
            assertEquals(expected, formatTime(time.first, time.second))
            val stored = "${time.first.toString().padStart(2, '0')}:${time.second.toString().padStart(2, '0')}"
            assertEquals(expected, formatStoredTime(stored))
        }
    }

    @Test
    fun formatsRangesWithoutDuplicatingSamePeriodAndSupportsMidnight() {
        assertEquals("8:00 - 9:00\u00A0AM", formatTimeRange("08:00", "09:00"))
        assertEquals("11:00\u00A0AM - 1:00\u00A0PM", formatTimeRange("11:00", "13:00"))
        assertEquals("9:05\u00A0AM", formatTimeRange("09:05"))
        assertEquals("11:00\u00A0PM - 1:00\u00A0AM", formatTimeRange("23:00", "01:00"))
    }

    @Test
    fun outputUsesUppercasePeriodsAndNonbreakingSpaces() {
        val values = listOf(
            formatTime(0, 0),
            formatTime(13, 5),
            formatTimeRange("08:00", "09:00"),
            formatTimeRange("11:00", "13:00"),
            formatTimeRange("23:00", "01:00")
        )

        values.forEach { value ->
            assertTrue(value.contains("AM") || value.contains("PM"))
            assertFalse(Regex(" (AM|PM)").containsMatchIn(value))
        }
    }

    @Test
    fun formatsWeeklyGridHourLabels() {
        assertEquals("8\u00A0AM", formatHourLabel(8))
        assertEquals("12\u00A0PM", formatHourLabel(12))
        assertEquals("1\u00A0PM", formatHourLabel(13))
    }
}
