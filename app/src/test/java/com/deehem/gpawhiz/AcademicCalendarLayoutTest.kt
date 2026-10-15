package com.deehem.gpawhiz

import com.deehem.gpawhiz.data.AcademicPeriod
import com.deehem.gpawhiz.service.AcademicCalendarLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AcademicCalendarLayoutTest {
    private val startDate = utcDate(2026, Calendar.OCTOBER, 12)

    @Test
    fun laysOutInclusiveRangesAndSemesterWeekNumbers() {
        val periods = listOf(period(1, 0, "LECTURES", 7), period(2, 1, "EXAMS", 3))

        val ranges = AcademicCalendarLayout.layout(startDate, periods)
        assertEquals(utcDate(2026, Calendar.OCTOBER, 12), ranges[0].startDate)
        assertEquals(utcDate(2026, Calendar.NOVEMBER, 29), ranges[0].endDate)
        assertEquals(utcDate(2026, Calendar.NOVEMBER, 30), ranges[1].startDate)
        assertEquals(utcDate(2026, Calendar.DECEMBER, 20), ranges[1].endDate)

        val position = AcademicCalendarLayout.position(startDate, periods, utcDate(2026, Calendar.NOVEMBER, 9))
        assertEquals(5, position.weekNumber)
        assertEquals(7, position.totalWeeks)
        assertEquals("LECTURES", position.period?.period?.type)
        assertEquals(21, position.daysUntilNextPeriod)
    }

    @Test
    fun explicitDateRangesSupportMidweekBoundariesAndFractionalWeeks() {
        val lectureStart = utcDate(2026, Calendar.OCTOBER, 14)
        val lectureEnd = utcDate(2026, Calendar.NOVEMBER, 13)
        val periods = listOf(
            period(1, 0, "LECTURES", 0).copy(startDate = lectureStart, endDate = lectureEnd),
            period(2, 1, "EXAMS", 2)
        )

        val ranges = AcademicCalendarLayout.layout(startDate, periods)
        assertEquals(31, ranges[0].totalDays)
        assertEquals(5, ranges[0].weeks)
        assertEquals(lectureStart, ranges[0].startDate)
        assertEquals(lectureEnd, ranges[0].endDate)
        assertEquals(lectureEnd + DAY, ranges[1].startDate)
        assertEquals(lectureEnd + 14 * DAY, ranges[1].endDate)

        val position = AcademicCalendarLayout.position(startDate, periods, utcDate(2026, Calendar.OCTOBER, 30))
        assertEquals(3, position.weekNumber)
        assertEquals(5, position.totalWeeks)
        assertEquals("LECTURES", position.period?.period?.type)
        assertEquals(31, AcademicCalendarLayout.periodDays(lectureStart, lectureEnd))
        assertEquals(5, AcademicCalendarLayout.weeksForDateRange(lectureStart, lectureEnd))
        assertEquals("LECTURES", AcademicCalendarLayout.position(startDate, periods, lectureEnd).period?.period?.type)
        assertEquals("EXAMS", AcademicCalendarLayout.position(startDate, periods, lectureEnd + DAY).period?.period?.type)
    }

    @Test
    fun explicitGapCountsDownToTheNextPeriod() {
        val firstEnd = startDate + 6 * DAY
        val nextStart = startDate + 10 * DAY
        val periods = listOf(
            period(1, 0, "LECTURES", 1).copy(startDate = startDate, endDate = firstEnd),
            period(2, 1, "EXAMS", 1).copy(startDate = nextStart, endDate = nextStart + 6 * DAY)
        )

        val position = AcademicCalendarLayout.position(startDate, periods, startDate + 8 * DAY)
        assertNull(position.period)
        assertEquals(2, position.daysUntilStart)
        assertEquals(2, position.daysUntilNextPeriod)
    }

    @Test
    fun legacyRangesChainFromPreviousResolvedEndAndNextStartHelper() {
        val first = period(1, 0, "LECTURES", 2).copy(startDate = startDate, endDate = startDate + 13 * DAY)
        val second = period(2, 1, "CA_TEST", 1)
        val periods = listOf(first, second)

        val range = AcademicCalendarLayout.layout(startDate, periods).last()
        assertEquals(first.endDate!! + DAY, range.startDate)
        assertEquals(first.endDate!! + 7 * DAY, range.endDate)
        assertEquals(range.endDate + DAY, AcademicCalendarLayout.defaultNextPeriodStartDate(startDate, periods, startDate))
    }

    @Test
    fun boundaryDaysBelongToAdjacentPeriodsCorrectly() {
        val periods = listOf(period(1, 0, "LECTURES", 1), period(2, 1, "CA_TEST", 1))

        assertEquals("LECTURES", AcademicCalendarLayout.position(startDate, periods, startDate).period?.period?.type)
        assertEquals("LECTURES", AcademicCalendarLayout.position(startDate, periods, startDate + 6 * DAY).period?.period?.type)
        assertEquals("CA_TEST", AcademicCalendarLayout.position(startDate, periods, startDate + 7 * DAY).period?.period?.type)
        assertNull(AcademicCalendarLayout.position(startDate, periods, startDate - DAY).period)
        assertEquals(1, AcademicCalendarLayout.position(startDate, periods, startDate - DAY).daysUntilStart)
        assertTrue(AcademicCalendarLayout.position(startDate, periods, startDate + 14 * DAY).hasEnded)
    }

    @Test
    fun validatesPerPeriodAndTimelineWeekLimits() {
        assertTrue(AcademicCalendarLayout.validationError(listOf(period(1, 0, "LECTURES", 1))) == null)
        assertNull(AcademicCalendarLayout.validationError(listOf(period(1, 0, "LECTURES", 30), period(2, 1, "EXAMS", 30))))
        assertTrue(AcademicCalendarLayout.validationError(listOf(period(1, 0, "LECTURES", 31))) != null)
        assertTrue(AcademicCalendarLayout.validationError(listOf(period(1, 0, "LECTURES", 30), period(2, 1, "EXAMS", 30), period(3, 2, "BREAK", 1))) != null)
        assertTrue(AcademicCalendarLayout.validationError(listOf(period(1, 0, "UNKNOWN", 1))) != null)
    }

    @Test
    fun reordersAndExtendsPeriodsWithinLimits() {
        val periods = listOf(period(1, 0, "LECTURES", 2), period(2, 1, "CA_TEST", 1))

        val moved = AcademicCalendarLayout.move(periods, periodId = 2, direction = -1)
        assertEquals(listOf(2, 1), moved.map { it.id })
        assertEquals(listOf(0, 1), moved.map { it.sortOrder })
        val extended = AcademicCalendarLayout.extendByWeek(periods, periodId = 1)
        assertEquals(3, extended.first().weeks)
        assertEquals(1, extended.last().weeks)

        val draftPeriods = listOf(period(0, 0, "LECTURES", 2), period(0, 1, "CA_TEST", 1))
        assertEquals(listOf("CA_TEST", "LECTURES"), AcademicCalendarLayout.moveAtIndex(draftPeriods, 1, -1).map { it.type })
        assertEquals(listOf(3, 1), AcademicCalendarLayout.extendAtIndex(draftPeriods, 0).map { it.weeks })
    }

    private fun period(id: Int, order: Int, type: String, weeks: Int) =
        AcademicPeriod(id = id, semesterId = 1, sortOrder = order, type = type, weeks = weeks)

    private fun utcDate(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month, day)
        }.timeInMillis

    private companion object {
        const val DAY = 86_400_000L
    }
}