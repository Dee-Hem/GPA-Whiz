package com.deehem.gpawhiz.service

import java.util.Calendar

fun isExamEndTimeValid(startTime: String, endTime: String?): Boolean =
    endTime.isNullOrBlank() || parseTimeMinutes(endTime) > parseTimeMinutes(startTime)

fun formatExamTimeRange(startTime: String, endTime: String?): String =
    formatTimeRange(startTime, endTime)

fun formatExamDuration(startTime: String, endTime: String?): String? {
    if (endTime.isNullOrBlank()) return null
    val minutes = parseTimeMinutes(endTime) - parseTimeMinutes(startTime)
    require(minutes > 0) { "Exam end time must be after its start time." }
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        hours == 0 -> "${remainingMinutes}m"
        remainingMinutes == 0 -> "${hours}h"
        else -> "${hours}h ${remainingMinutes}m"
    }
}

fun isExamInProgress(
    dateMillis: Long,
    startTime: String,
    endTime: String?,
    nowMillis: Long
): Boolean {
    if (endTime.isNullOrBlank()) return false
    val dayStart = Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val start = dayStart + parseTimeMinutes(startTime) * 60_000L
    val end = dayStart + parseTimeMinutes(endTime) * 60_000L
    require(end > start) { "Exam end time must be after its start time." }
    return nowMillis >= start && nowMillis < end
}

fun examTimeOnDateMillis(dateMillis: Long, time: String): Long {
    val dayStart = Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return dayStart + parseTimeMinutes(time) * 60_000L
}

fun parseTimeMinutes(time: String): Int {
    val parts = time.split(':')
    require(parts.size == 2) { "Invalid time: $time." }
    val hour = parts[0].toIntOrNull()
    val minute = parts[1].toIntOrNull()
    require(hour != null && hour in 0..23 && minute != null && minute in 0..59) {
        "Invalid time: $time."
    }
    return hour * 60 + minute
}
