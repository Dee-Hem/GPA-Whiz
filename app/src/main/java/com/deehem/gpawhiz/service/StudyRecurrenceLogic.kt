package com.deehem.gpawhiz.service

import com.deehem.gpawhiz.data.StudyRecurrence
import com.deehem.gpawhiz.data.StudySession
import com.deehem.gpawhiz.data.StudyStatus
import java.util.Calendar
import java.util.TimeZone

enum class StudySeriesScope {
    THIS_ONLY,
    THIS_AND_FOLLOWING,
    ALL
}

data class StudySeriesEditPlan(
    val updatedSessions: List<StudySession>,
    val deletedSessions: List<StudySession>,
    val insertedSessions: List<StudySession> = emptyList()
)

fun generateWeeklyOccurrenceDates(
    firstDateMillis: Long,
    weeks: Int? = null,
    untilDateMillis: Long? = null,
    timeZone: TimeZone = TimeZone.getDefault()
): List<Long> {
    require((weeks == null) xor (untilDateMillis == null)) {
        "Provide either a week count or an end date."
    }
    require(weeks == null || weeks in 1..52) { "Weekly series length must be between 1 and 52 weeks." }

    val first = Calendar.getInstance(timeZone).apply {
        timeInMillis = firstDateMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val endDate = untilDateMillis?.let {
        Calendar.getInstance(timeZone).apply {
            timeInMillis = it
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    require(endDate == null || endDate.timeInMillis >= first.timeInMillis) {
        "The end date must be on or after the first session."
    }

    val dates = ArrayList<Long>(weeks ?: 52)
    val occurrence = Calendar.getInstance(timeZone).apply { timeInMillis = first.timeInMillis }
    repeat(52) {
        if (weeks != null && dates.size >= weeks) return@repeat
        if (endDate != null && occurrence.timeInMillis > endDate.timeInMillis) return dates
        dates += occurrence.timeInMillis
        occurrence.add(Calendar.WEEK_OF_YEAR, 1)
    }
    return dates
}

fun planStudySeriesEdit(
    existingSessions: List<StudySession>,
    editedSession: StudySession,
    scope: StudySeriesScope,
    plannedDates: List<Long>? = null
): StudySeriesEditPlan {
    val ordered = existingSessions.sortedWith(compareBy<StudySession> { it.date }.thenBy { it.startTime }.thenBy { it.id })
    val selectedIndex = ordered.indexOfFirst { it.id == editedSession.id }
    require(selectedIndex >= 0) { "The selected study session is not part of this series." }

    if (scope == StudySeriesScope.THIS_ONLY) {
        val original = ordered[selectedIndex]
        return StudySeriesEditPlan(
            updatedSessions = listOf(
                editedSession.copy(
                    status = original.status,
                    seriesId = if (editedSession.recurrence == StudyRecurrence.WEEKLY) original.seriesId else null,
                    recurrence = editedSession.recurrence
                )
            ),
            deletedSessions = emptyList()
        )
    }

    val affected = if (scope == StudySeriesScope.ALL) ordered else ordered.drop(selectedIndex)
    val dates = plannedDates ?: when {
        editedSession.recurrence == StudyRecurrence.NONE -> affected.map { it.date }
        affected.size == 1 -> listOf(editedSession.date)
        else -> generateWeeklyOccurrenceDates(editedSession.date, weeks = affected.size.coerceAtMost(52))
    }
    val existingToUpdate = affected.take(dates.size)
    val updated = existingToUpdate.mapIndexed { index, original ->
        editedSession.copy(
            id = original.id,
            date = dates[index],
            status = original.status,
            seriesId = if (editedSession.recurrence == StudyRecurrence.WEEKLY) original.seriesId else null,
            recurrence = editedSession.recurrence
        )
    }
    val deleted = affected.drop(dates.size)
    val newSessions = dates.drop(existingToUpdate.size).map { date ->
        editedSession.copy(
            id = 0,
            date = date,
            status = StudyStatus.UPCOMING,
            seriesId = if (editedSession.recurrence == StudyRecurrence.WEEKLY) ordered.first().seriesId else null,
            recurrence = editedSession.recurrence
        )
    }
    return StudySeriesEditPlan(updatedSessions = updated, deletedSessions = deleted, insertedSessions = newSessions)
}

fun sessionsToDeleteForScope(
    existingSessions: List<StudySession>,
    selectedSession: StudySession,
    scope: StudySeriesScope
): List<StudySession> {
    val ordered = existingSessions.sortedWith(compareBy<StudySession> { it.date }.thenBy { it.startTime }.thenBy { it.id })
    val selectedIndex = ordered.indexOfFirst { it.id == selectedSession.id }
    require(selectedIndex >= 0) { "The selected study session is not part of this series." }
    return when (scope) {
        StudySeriesScope.THIS_ONLY -> listOf(ordered[selectedIndex])
        StudySeriesScope.THIS_AND_FOLLOWING -> ordered.drop(selectedIndex)
        StudySeriesScope.ALL -> ordered
    }
}

fun nextStudySessionsToArm(sessions: List<StudySession>, nowMillis: Long): List<StudySession> {
    val candidates = sessions.mapNotNull { session ->
        if (!session.reminderEnabled || session.status != "Upcoming") return@mapNotNull null
        val fireAt = oneShotReminderTimeMillis(session.date, session.startTime, 15, nowMillis) ?: return@mapNotNull null
        val groupKey = if (session.recurrence == StudyRecurrence.WEEKLY && session.seriesId != null) {
            session.seriesId
        } else {
            "single:${session.id}"
        }
        Triple(groupKey, fireAt, session)
    }
    return candidates.groupBy { it.first }
        .values
        .mapNotNull { group -> group.minByOrNull { it.second }?.third }
}
