package com.deehem.gpawhiz.service

import com.deehem.gpawhiz.data.StudySession
import java.util.Calendar

enum class AppAlarmType(val requestCodeNamespace: Int) {
    LECTURE(0x10000000),
    EXAM(0x20000000),
    STUDY(0x30000000)
}

fun alarmRequestCode(type: AppAlarmType, rowId: Int): Int {
    require(rowId in 0..0x0FFFFFFF) { "Alarm row ID is outside the supported request-code range." }
    return type.requestCodeNamespace or rowId
}

fun nextWeeklyAlarmTimeMillis(
    nowMillis: Long,
    dayOfWeek: Int,
    startTime: String,
    leadMinutes: Int
): Long {
    require(dayOfWeek in 1..7) { "Day of week must be between 1 (Monday) and 7 (Sunday)." }
    val (hour, minute) = parseStoredHourMinute(startTime)
    val calendar = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        val currentDayOfWeek = ((get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
        val daysUntilTarget = (dayOfWeek - currentDayOfWeek + 7) % 7
        add(Calendar.DAY_OF_YEAR, daysUntilTarget)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MINUTE, -leadMinutes)
    }
    if (calendar.timeInMillis <= nowMillis) calendar.add(Calendar.DAY_OF_YEAR, 7)
    return calendar.timeInMillis
}

fun oneShotReminderTimeMillis(
    sessionDateMillis: Long,
    startTime: String,
    leadMinutes: Int,
    nowMillis: Long,
    timeZone: java.util.TimeZone = java.util.TimeZone.getDefault()
): Long? {
    val (hour, minute) = parseStoredHourMinute(startTime)
    val calendar = Calendar.getInstance(timeZone).apply {
        timeInMillis = sessionDateMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MINUTE, -leadMinutes)
    }
    return calendar.timeInMillis.takeIf { it > nowMillis }
}

fun studySessionsEligibleForRecovery(
    sessions: List<StudySession>,
    nowMillis: Long
): List<StudySession> = nextStudySessionsToArm(sessions, nowMillis)

private fun parseStoredHourMinute(value: String): Pair<Int, Int> {
    val parts = value.split(':')
    require(parts.size == 2) { "Invalid stored time: $value." }
    val hour = parts[0].toIntOrNull()
    val minute = parts[1].toIntOrNull()
    require(hour != null && hour in 0..23 && minute != null && minute in 0..59) {
        "Invalid stored time: $value."
    }
    return hour to minute
}
