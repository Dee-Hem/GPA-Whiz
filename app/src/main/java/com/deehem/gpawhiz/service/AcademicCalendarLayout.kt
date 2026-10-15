package com.deehem.gpawhiz.service

import com.deehem.gpawhiz.data.AcademicPeriod
import java.util.Calendar
import java.util.TimeZone

object AcademicCalendarLayout {
    private const val DAYS_PER_WEEK = 7
    private const val MAX_TOTAL_WEEKS = 60
    private const val MILLIS_PER_DAY = 86_400_000L

    val periodTypes = setOf("LECTURES", "CA_TEST", "EXAMS", "BREAK")

    data class PeriodRange(
        val period: AcademicPeriod,
        val startDate: Long,
        val endDate: Long,
        val totalDays: Int,
        val weeks: Int,
        val startWeek: Int,
        val endWeek: Int
    )

    data class Position(
        val weekNumber: Int?,
        val totalWeeks: Int,
        val period: PeriodRange?,
        val daysUntilNextPeriod: Int?,
        val daysUntilStart: Int?,
        val hasEnded: Boolean
    )

    fun validationError(periods: List<AcademicPeriod>, timelineStartDate: Long? = null): String? {
        val ordered = periods.sortedBy { it.sortOrder }
        if (ordered.any { it.type !in periodTypes }) return "Unknown academic period type."
        var nextDay = timelineStartDate?.let(::utcDay) ?: ordered.firstOrNull()?.startDate?.let(::utcDay)
        var totalWeeks = 0
        for (period in ordered) {
            if ((period.startDate == null || period.endDate == null) && period.weeks !in 1..30) {
                return "Each period must be between 1 and 30 weeks."
            }
            val start = period.startDate?.let(::utcDay) ?: nextDay
            val end = period.endDate?.let(::utcDay) ?: start?.plus(period.weeks * DAYS_PER_WEEK - 1)
            val resolvedWeeks = if (start != null && end != null) {
                if (end < start) return "A period end date cannot be before its start date."
                val days = (end - start + 1).toInt()
                val weeks = (days + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK
                if (weeks !in 1..30) return "Each period must be between 1 and 30 weeks."
                weeks
            } else period.weeks
            totalWeeks += resolvedWeeks
            if (totalWeeks > MAX_TOTAL_WEEKS) return "A timeline cannot exceed 60 weeks."
            nextDay = end?.plus(1) ?: start?.plus(period.weeks * DAYS_PER_WEEK)
        }
        return null
    }

    fun layout(startDate: Long, periods: List<AcademicPeriod>): List<PeriodRange> {
        require(validationError(periods, startDate) == null) {
            validationError(periods, startDate) ?: "Invalid timeline."
        }
        var nextDay = utcDay(startDate)
        var currentWeek = 1
        return periods.sortedBy { it.sortOrder }.map { period ->
            val start = period.startDate?.let(::utcDay) ?: nextDay
            val end = period.endDate?.let(::utcDay) ?: start + period.weeks * DAYS_PER_WEEK - 1
            require(end >= start) { "A period end date cannot be before its start date." }
            val days = (end - start + 1).toInt()
            val weeks = (days + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK
            require(weeks in 1..30) { "Each period must be between 1 and 30 weeks." }
            val startWeek = currentWeek
            currentWeek += weeks
            nextDay = end + 1
            PeriodRange(
                period = period,
                startDate = start * MILLIS_PER_DAY,
                endDate = end * MILLIS_PER_DAY,
                totalDays = days,
                weeks = weeks,
                startWeek = startWeek,
                endWeek = currentWeek - 1
            )
        }
    }

    fun position(startDate: Long, periods: List<AcademicPeriod>, date: Long): Position {
        val ranges = layout(startDate, periods)
        val totalWeeks = ranges.sumOf { it.weeks }
        val day = utcDay(date)
        val matchingRange = ranges.firstOrNull { day in utcDay(it.startDate)..utcDay(it.endDate) }
        val nextRange = ranges.firstOrNull { utcDay(it.startDate) > day }
        val firstDay = ranges.firstOrNull()?.let { utcDay(it.startDate) } ?: utcDay(startDate)
        return Position(
            weekNumber = matchingRange?.let { ((day - utcDay(it.startDate)) / DAYS_PER_WEEK).toInt() + 1 },
            totalWeeks = matchingRange?.weeks ?: totalWeeks,
            period = matchingRange,
            daysUntilNextPeriod = nextRange?.let { (utcDay(it.startDate) - day).toInt() },
            daysUntilStart = when {
                day < firstDay -> (firstDay - day).toInt()
                matchingRange == null && nextRange != null -> (utcDay(nextRange.startDate) - day).toInt()
                else -> null
            },
            hasEnded = ranges.isNotEmpty() && day > utcDay(ranges.last().endDate)
        )
    }

    fun periodDays(startDate: Long, endDate: Long): Int? {
        val days = utcDay(endDate) - utcDay(startDate) + 1
        return days.takeIf { it > 0 }?.toInt()
    }

    fun weeksForDateRange(startDate: Long, endDate: Long): Int? =
        periodDays(startDate, endDate)?.let { (it + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK }

    fun defaultNextPeriodStartDate(timelineStartDate: Long?, periods: List<AcademicPeriod>, fallbackDate: Long): Long {
        if (periods.isEmpty()) return timelineStartDate ?: fallbackDate
        val baseDate = timelineStartDate ?: periods.first().startDate ?: fallbackDate
        return layout(baseDate, periods).last().endDate + MILLIS_PER_DAY
    }

    fun move(periods: List<AcademicPeriod>, periodId: Int, direction: Int): List<AcademicPeriod> {
        require(direction == -1 || direction == 1)
        val ordered = periods.sortedBy { it.sortOrder }.toMutableList()
        val index = ordered.indexOfFirst { it.id == periodId }
        if (index < 0) return ordered.withOrder()
        return moveAtIndex(ordered, index, direction)
    }

    fun moveAtIndex(periods: List<AcademicPeriod>, index: Int, direction: Int): List<AcademicPeriod> {
        require(direction == -1 || direction == 1)
        val ordered = periods.sortedBy { it.sortOrder }.toMutableList()
        val target = index + direction
        if (index < 0 || target !in ordered.indices) return ordered.withOrder()
        val period = ordered.removeAt(index)
        ordered.add(target, period)
        return ordered.withOrder()
    }

    fun extendByWeek(periods: List<AcademicPeriod>, periodId: Int): List<AcademicPeriod> {
        val ordered = periods.sortedBy { it.sortOrder }
        val index = ordered.indexOfFirst { it.id == periodId }
        if (index < 0) return ordered
        return extendAtIndex(ordered, index)
    }

    fun extendAtIndex(periods: List<AcademicPeriod>, index: Int): List<AcademicPeriod> {
        val ordered = periods.sortedBy { it.sortOrder }
        if (index !in ordered.indices) return ordered.withOrder()
        val updated = ordered.mapIndexed { itemIndex, period ->
            if (itemIndex == index) period.copy(weeks = period.weeks + 1) else period
        }
        require(validationError(updated) == null) { validationError(updated) ?: "Invalid timeline." }
        return updated.withOrder()
    }

    private fun List<AcademicPeriod>.withOrder() = mapIndexed { index, period -> period.copy(sortOrder = index) }

    private fun utcDay(millis: Long): Long = Math.floorDiv(millis, MILLIS_PER_DAY)

    fun dateAtUtcMidnight(year: Int, monthZeroBased: Int, dayOfMonth: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, monthZeroBased, dayOfMonth)
        }.timeInMillis

    fun todayAtUtcMidnight(): Long {
        val localToday = Calendar.getInstance()
        return dateAtUtcMidnight(
            localToday.get(Calendar.YEAR),
            localToday.get(Calendar.MONTH),
            localToday.get(Calendar.DAY_OF_MONTH)
        )
    }
}