package com.deehem.gpawhiz

import com.deehem.gpawhiz.data.StudySession
import com.deehem.gpawhiz.data.StudyStatus
import com.deehem.gpawhiz.service.AppAlarmType
import com.deehem.gpawhiz.service.alarmRequestCode
import com.deehem.gpawhiz.service.nextWeeklyAlarmTimeMillis
import com.deehem.gpawhiz.service.studySessionsEligibleForRecovery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AlarmSchedulingLogicTest {
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
    fun weeklyAlarmEarlierTodayMovesToNextWeek() {
        val now = utcDate(2026, Calendar.OCTOBER, 15, 10, 0)
        val next = nextWeeklyAlarmTimeMillis(now, dayOfWeek = 4, startTime = "09:00", leadMinutes = 60)

        assertEquals(utcDate(2026, Calendar.OCTOBER, 22, 8, 0), next)
    }

    @Test
    fun weeklyAlarmLaterTodayRemainsToday() {
        val now = utcDate(2026, Calendar.OCTOBER, 15, 10, 0)
        val next = nextWeeklyAlarmTimeMillis(now, dayOfWeek = 4, startTime = "12:00", leadMinutes = 60)

        assertEquals(utcDate(2026, Calendar.OCTOBER, 15, 11, 0), next)
    }

    @Test
    fun weeklyAlarmOnDifferentWeekdayUsesNextMatchingDay() {
        val now = utcDate(2026, Calendar.OCTOBER, 15, 10, 0)
        val next = nextWeeklyAlarmTimeMillis(now, dayOfWeek = 5, startTime = "09:00", leadMinutes = 60)

        assertEquals(utcDate(2026, Calendar.OCTOBER, 16, 8, 0), next)
    }

    @Test
    fun weeklyAlarmWrapsToFollowingWeekForEarlierWeekday() {
        val now = utcDate(2026, Calendar.OCTOBER, 19, 10, 0)
        val next = nextWeeklyAlarmTimeMillis(now, dayOfWeek = 7, startTime = "09:00", leadMinutes = 60)

        assertEquals(utcDate(2026, Calendar.OCTOBER, 25, 8, 0), next)
    }

    @Test
    fun requestCodesAreStableAndDisjointAcrossAlarmTypes() {
        val lecture = alarmRequestCode(AppAlarmType.LECTURE, 42)
        val exam = alarmRequestCode(AppAlarmType.EXAM, 42)
        val study = alarmRequestCode(AppAlarmType.STUDY, 42)

        assertEquals(lecture, alarmRequestCode(AppAlarmType.LECTURE, 42))
        assertNotEquals(lecture, exam)
        assertNotEquals(lecture, study)
        assertNotEquals(exam, study)
    }

    @Test
    fun recoverySelectsOnlyEnabledUpcomingSessionsWithFutureReminderTime() {
        val now = utcDate(2026, Calendar.OCTOBER, 15, 10, 0)
        val future = studySession("11:00", StudyStatus.UPCOMING).copy(isRecurring = true, dayOfWeek = 4)
        val past = studySession("09:00", StudyStatus.UPCOMING)
        val completed = studySession("11:00", StudyStatus.COMPLETED)
        val cancelled = studySession("11:00", StudyStatus.CANCELLED)
        val postponed = studySession("11:00", StudyStatus.POSTPONED)
        val disabled = studySession("11:00", StudyStatus.UPCOMING).copy(reminderEnabled = false)

        assertEquals(
            listOf(future),
            studySessionsEligibleForRecovery(
                listOf(future, past, completed, cancelled, postponed, disabled),
                now
            )
        )
    }

    private fun studySession(time: String, status: String) = StudySession(
        courseId = 1,
        courseCode = "CSC 101",
        date = utcDate(2026, Calendar.OCTOBER, 15, 0, 0),
        startTime = time,
        durationMinutes = 60,
        status = status
    )

    private fun utcDate(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
}
