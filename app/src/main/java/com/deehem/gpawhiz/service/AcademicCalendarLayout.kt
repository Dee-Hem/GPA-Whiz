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

    fun validationError(periods: List<AcademicPeriod>): String? {
        if (periods.any { it.weeks !in 1..30 }) return "Each period must be between 1 and 30 weeks."
        if (periods.any { it.type !in periodTypes }) return "Unknown academic period type."
        if (periods.sumOf { it.weeks } > MAX_TOTAL_WEEKS) return "A timeline cannot exceed 60 weeks."
        return null
    }

    fun layout(startDate: Long, periods: List<AcademicPeriod>): List<PeriodRange> {
        require(validationError(periods) == null) { validationError(periods) ?: "Invalid timeline." }
        var currentDay = utcDay(startDate)
        var currentWeek = 1
        return periods.sortedBy { it.sortOrder }.map { period ->
            val start = currentDay
            val startWeek = currentWeek
            currentDay += period.weeks * DAYS_PER_WEEK
            currentWeek += period.weeks
            PeriodRange(
                period = period,
                startDate = start * MILLIS_PER_DAY,
                endDate = (currentDay - 1) * MILLIS_PER_DAY,
                startWeek = startWeek,
                endWeek = currentWeek - 1
            )
        }
    }

    fun position(startDate: Long, periods: List<AcademicPeriod>, date: Long): Position {
        val ranges = layout(startDate, periods)
        val totalWeeks = periods.sumOf { it.weeks }
        val day = utcDay(date)
        val startDay = utcDay(startDate)
        val offset = (day - startDay).toInt()
        val matchingRange = ranges.firstOrNull { day in utcDay(it.startDate)..utcDay(it.endDate) }
        val nextRange = ranges.firstOrNull { utcDay(it.startDate) > day }
        return Position(
            weekNumber = if (matchingRange != null) offset / DAYS_PER_WEEK + 1 else null,
            totalWeeks = totalWeeks,
            period = matchingRange,
            daysUntilNextPeriod = nextRange?.let { (utcDay(it.startDate) - day).toInt() },
            daysUntilStart = if (day < startDay) (startDay - day).toInt() else null,
            hasEnded = ranges.isNotEmpty() && day > utcDay(ranges.last().endDate)
        )
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