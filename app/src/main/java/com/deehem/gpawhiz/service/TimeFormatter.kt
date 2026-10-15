package com.deehem.gpawhiz.service

fun formatTime(hour: Int, minute: Int): String {
    require(hour in 0..23) { "Hour must be between 0 and 23." }
    require(minute in 0..59) { "Minute must be between 0 and 59." }
    val displayHour = when (val twelveHour = hour % 12) {
        0 -> 12
        else -> twelveHour
    }
    val displayMinute = if (minute < 10) "0$minute" else minute.toString()
    val period = if (hour < 12) "AM" else "PM"
    return "$displayHour:$displayMinute\u00A0$period"
}

fun formatStoredTime(time: String): String {
    val parts = time.split(':')
    require(parts.size == 2) { "Invalid time: $time." }
    val hour = parts[0].toIntOrNull()
    val minute = parts[1].toIntOrNull()
    require(hour != null && hour in 0..23 && minute != null && minute in 0..59) {
        "Invalid time: $time."
    }
    return formatTime(hour, minute)
}

fun formatTimeRange(startTime: String, endTime: String? = null): String {
    val formattedStart = formatStoredTime(startTime)
    if (endTime.isNullOrBlank()) return formattedStart
    val formattedEnd = formatStoredTime(endTime)
    val samePeriod = startTime.substringBefore(':').toInt() / 12 ==
        endTime.substringBefore(':').toInt() / 12
    return if (samePeriod) {
        "${formattedStart.substringBefore('\u00A0')} - $formattedEnd"
    } else {
        "$formattedStart - $formattedEnd"
    }
}

fun formatHourLabel(hour: Int): String {
    require(hour in 0..23) { "Hour must be between 0 and 23." }
    val displayHour = when (val twelveHour = hour % 12) {
        0 -> 12
        else -> twelveHour
    }
    val period = if (hour < 12) "AM" else "PM"
    return "$displayHour\u00A0$period"
}
