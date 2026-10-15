package com.deehem.gpawhiz

import com.deehem.gpawhiz.data.StudyRecurrence
import com.deehem.gpawhiz.data.StudySession
import com.deehem.gpawhiz.data.StudyStatus
import com.deehem.gpawhiz.service.StudySeriesScope
import com.deehem.gpawhiz.service.generateWeeklyOccurrenceDates
import com.deehem.gpawhiz.service.nextStudySessionsToArm
import com.deehem.gpawhiz.service.oneShotReminderTimeMillis
import com.deehem.gpawhiz.service.planStudySeriesEdit
import com.deehem.gpawhiz.service.sessionsToDeleteForScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StudyRecurrenceLogicTest {
    private val originalTimeZone = TimeZone.getDefault()

    @Before
    fun useUtcTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun generatesRequestedWeeklySeriesLengths() {
        val first = date(2026, Calendar.OCTOBER, 23)

        assertEquals(1, generateWeeklyOccurrenceDates(first, weeks = 1).size)
        assertEquals(12, generateWeeklyOccurrenceDates(first, weeks = 12).size)
        assertEquals(52, generateWeeklyOccurrenceDates(first, weeks = 52).size)
    }

    @Test
    fun generatesUntilDateAcrossMonthAndYearBoundaries() {
        val first = date(2026, Calendar.DECEMBER, 25)
        val dates = generateWeeklyOccurrenceDates(
            firstDateMillis = first,
            untilDateMillis = date(2027, Calendar.JANUARY, 15)
        )

        assertEquals(
            listOf(
                date(2026, Calendar.DECEMBER, 25),
                date(2027, Calendar.JANUARY, 1),
                date(2027, Calendar.JANUARY, 8),
                date(2027, Calendar.JANUARY, 15)
            ),
            dates
        )
    }

    @Test
    fun weeklyOccurrencesKeepTheSameLocalDateTimeAcrossDst() {
        val zone = TimeZone.getTimeZone("America/New_York")
        val first = localDateTime(zone, 2026, Calendar.MARCH, 1, 8, 30)
        val dates = generateWeeklyOccurrenceDates(first, weeks = 3, timeZone = zone)

        dates.forEach { dateMillis ->
            val scheduled = oneShotReminderTimeMillis(dateMillis, "08:30", 0, 0L, zone)
            val calendar = Calendar.getInstance(zone).apply { timeInMillis = scheduled ?: 0L }
            assertEquals(8, calendar.get(Calendar.HOUR_OF_DAY))
            assertEquals(30, calendar.get(Calendar.MINUTE))
        }
    }

    @Test
    fun editingThisOccurrenceOnlyLeavesOtherRowsAndStatusUntouched() {
        val sessions = seriesSessions()
        val selected = sessions[2].copy(date = date(2026, Calendar.NOVEMBER, 4), startTime = "14:15")
        val plan = planStudySeriesEdit(sessions, selected, StudySeriesScope.THIS_ONLY)

        assertEquals(listOf(3), plan.updatedSessions.map { it.id })
        assertEquals(date(2026, Calendar.NOVEMBER, 4), plan.updatedSessions.single().date)
        assertEquals(StudyStatus.COMPLETED, plan.updatedSessions.single().status)
        assertEquals("14:15", plan.updatedSessions.single().startTime)
    }

    @Test
    fun editingThisAndFollowingKeepsStatusPerOccurrenceAndRegeneratesDates() {
        val sessions = seriesSessions()
        val edited = sessions[2].copy(date = date(2026, Calendar.NOVEMBER, 4), startTime = "14:15")
        val plan = planStudySeriesEdit(sessions, edited, StudySeriesScope.THIS_AND_FOLLOWING)

        assertEquals(listOf(3, 4, 5), plan.updatedSessions.map { it.id })
        assertEquals(date(2026, Calendar.NOVEMBER, 4), plan.updatedSessions[0].date)
        assertEquals(date(2026, Calendar.NOVEMBER, 11), plan.updatedSessions[1].date)
        assertEquals(listOf(StudyStatus.COMPLETED, StudyStatus.POSTPONED, StudyStatus.UPCOMING), plan.updatedSessions.map { it.status })
    }

    @Test
    fun editingAllOccurrencesChangesDatesButPreservesEachStatus() {
        val sessions = seriesSessions()
        val edited = sessions[1].copy(date = date(2026, Calendar.NOVEMBER, 5), startTime = "15:00")
        val plan = planStudySeriesEdit(sessions, edited, StudySeriesScope.ALL)

        assertEquals(sessions.map { it.id }, plan.updatedSessions.map { it.id })
        assertEquals(date(2026, Calendar.NOVEMBER, 5), plan.updatedSessions.first().date)
        assertEquals(date(2026, Calendar.NOVEMBER, 12), plan.updatedSessions[1].date)
        assertEquals(sessions.map { it.status }, plan.updatedSessions.map { it.status })
    }

    @Test
    fun deletionScopesSelectOnlyRequestedOccurrences() {
        val sessions = seriesSessions()
        assertEquals(listOf(3), sessionsToDeleteForScope(sessions, sessions[2], StudySeriesScope.THIS_ONLY).map { it.id })
        assertEquals(listOf(3, 4, 5), sessionsToDeleteForScope(sessions, sessions[2], StudySeriesScope.THIS_AND_FOLLOWING).map { it.id })
        assertEquals(listOf(1, 2, 3, 4, 5), sessionsToDeleteForScope(sessions, sessions[2], StudySeriesScope.ALL).map { it.id })
    }

    @Test
    fun onlyNextUpcomingOccurrenceOfEachSeriesIsSelectedForAlarm() {
        val now = date(2026, Calendar.OCTOBER, 15)
        val sessions = listOf(
            session(1, date(2026, Calendar.OCTOBER, 16), StudyStatus.UPCOMING, "series-a"),
            session(2, date(2026, Calendar.OCTOBER, 23), StudyStatus.UPCOMING, "series-a"),
            session(3, date(2026, Calendar.OCTOBER, 17), StudyStatus.UPCOMING, "series-b"),
            session(4, date(2026, Calendar.OCTOBER, 18), StudyStatus.COMPLETED, "series-b"),
            session(5, date(2026, Calendar.OCTOBER, 18), StudyStatus.CANCELLED, null),
            session(6, date(2026, Calendar.OCTOBER, 18), StudyStatus.POSTPONED, null)
        )

        assertEquals(listOf(1, 3), nextStudySessionsToArm(sessions, now).map { it.id }.sorted())
        assertTrue(nextStudySessionsToArm(sessions, now).all { it.status == StudyStatus.UPCOMING })
    }

    private fun seriesSessions() = listOf(
        session(1, date(2026, Calendar.OCTOBER, 23), StudyStatus.UPCOMING, "weekly"),
        session(2, date(2026, Calendar.OCTOBER, 30), StudyStatus.UPCOMING, "weekly"),
        session(3, date(2026, Calendar.NOVEMBER, 6), StudyStatus.COMPLETED, "weekly"),
        session(4, date(2026, Calendar.NOVEMBER, 13), StudyStatus.POSTPONED, "weekly"),
        session(5, date(2026, Calendar.NOVEMBER, 20), StudyStatus.UPCOMING, "weekly")
    )

    private fun session(id: Int, day: Long, status: String, seriesId: String?) = StudySession(
        id = id,
        courseId = 1,
        courseCode = "CSC 101",
        date = day,
        startTime = "09:00",
        durationMinutes = 60,
        status = status,
        seriesId = seriesId,
        recurrence = if (seriesId == null) StudyRecurrence.NONE else StudyRecurrence.WEEKLY
    )

    private fun date(year: Int, month: Int, day: Int): Long =
        localDateTime(TimeZone.getTimeZone("UTC"), year, month, day, 0, 0)

    private fun localDateTime(
        timeZone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long = Calendar.getInstance(timeZone).apply {
        clear()
        set(year, month, day, hour, minute)
    }.timeInMillis
}
