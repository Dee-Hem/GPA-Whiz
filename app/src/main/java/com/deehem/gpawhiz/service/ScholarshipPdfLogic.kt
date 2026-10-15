package com.deehem.gpawhiz.service

import com.deehem.gpawhiz.data.Scholarship
import com.deehem.gpawhiz.data.ScholarshipStatus
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

enum class ScholarshipPdfTone {
    MUTED,
    STANDARD,
    DUE_TODAY,
    OVERDUE
}

data class ScholarshipPdfDaysText(
    val text: String,
    val tone: ScholarshipPdfTone
)

data class ScholarshipPdfStatusCategory(
    val label: String,
    val statuses: Set<String>
)

private val scholarshipPdfStatusCategories = listOf(
    ScholarshipPdfStatusCategory("Not Started", setOf(ScholarshipStatus.NOT_STARTED)),
    ScholarshipPdfStatusCategory("In Preparation", setOf(ScholarshipStatus.PREPARING)),
    ScholarshipPdfStatusCategory("In Progress", setOf(ScholarshipStatus.IN_PROGRESS)),
    ScholarshipPdfStatusCategory("Ready to Submit", setOf(ScholarshipStatus.READY_TO_SUBMIT)),
    ScholarshipPdfStatusCategory(
        "Submitted",
        setOf(ScholarshipStatus.SUBMITTED, ScholarshipStatus.ASSESSMENT, ScholarshipStatus.INTERVIEW)
    ),
    ScholarshipPdfStatusCategory("Awaiting Result", setOf(ScholarshipStatus.AWAITING_RESULT)),
    ScholarshipPdfStatusCategory("Awarded", setOf(ScholarshipStatus.AWARDED)),
    ScholarshipPdfStatusCategory("Rejected", setOf(ScholarshipStatus.REJECTED)),
    ScholarshipPdfStatusCategory("Withdrawn", setOf(ScholarshipStatus.WITHDRAWN)),
    ScholarshipPdfStatusCategory("Expired", setOf(ScholarshipStatus.EXPIRED)),
    ScholarshipPdfStatusCategory("Other", emptySet())
)

fun scholarshipPdfIsOpenStatus(status: String): Boolean =
    status in setOf(
        ScholarshipStatus.NOT_STARTED,
        ScholarshipStatus.PREPARING,
        ScholarshipStatus.IN_PROGRESS
    )

fun scholarshipPdfDaysText(
    status: String,
    deadlineMillis: Long?,
    todayMillis: Long = System.currentTimeMillis()
): ScholarshipPdfDaysText {
    if (deadlineMillis == null || deadlineMillis <= 0L) {
        return ScholarshipPdfDaysText("-", ScholarshipPdfTone.MUTED)
    }

    val days = localDateOrdinal(deadlineMillis) - localDateOrdinal(todayMillis)
    val isOpen = scholarshipPdfIsOpenStatus(status)

    return when {
        days < 0 && isOpen -> {
            val overdue = -days
            ScholarshipPdfDaysText(
                "Overdue by $overdue ${if (overdue == 1L) "day" else "days"}",
                ScholarshipPdfTone.OVERDUE
            )
        }
        days < 0 -> {
            val past = -days
            ScholarshipPdfDaysText(
                "$past ${if (past == 1L) "day" else "days"} ago",
                ScholarshipPdfTone.MUTED
            )
        }
        days == 0L -> ScholarshipPdfDaysText(
            "Today",
            if (isOpen) ScholarshipPdfTone.DUE_TODAY else ScholarshipPdfTone.MUTED
        )
        days == 1L -> ScholarshipPdfDaysText(
            "Tomorrow",
            if (isOpen) ScholarshipPdfTone.STANDARD else ScholarshipPdfTone.MUTED
        )
        else -> ScholarshipPdfDaysText(
            "In $days days",
            if (isOpen) ScholarshipPdfTone.STANDARD else ScholarshipPdfTone.MUTED
        )
    }
}

fun scholarshipPdfOpenDeadlines(
    scholarships: List<Scholarship>,
    todayMillis: Long = System.currentTimeMillis()
): List<Scholarship> {
    val today = localDateOrdinal(todayMillis)
    return scholarships
        .filter { scholarship ->
            scholarshipPdfIsOpenStatus(scholarship.status) &&
                scholarship.deadlineDate?.let { deadline ->
                    deadline > 0L && localDateOrdinal(deadline) >= today
                } == true
        }
        .sortedBy { it.deadlineDate }
}

private fun localDateOrdinal(timestamp: Long): Long {
    val localDate = Calendar.getInstance().apply { timeInMillis = timestamp }
    val utcDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            localDate.get(Calendar.YEAR),
            localDate.get(Calendar.MONTH),
            localDate.get(Calendar.DAY_OF_MONTH)
        )
    }
    return TimeUnit.MILLISECONDS.toDays(utcDate.timeInMillis)
}

fun scholarshipPdfStatusCounts(statuses: List<String>): List<Pair<String, Int>> {
    val knownStatuses = scholarshipPdfStatusCategories
        .flatMap { it.statuses }
        .toSet()
    return scholarshipPdfStatusCategories.map { category ->
        val count = if (category.label == "Other") {
            statuses.count { it !in knownStatuses }
        } else {
            statuses.count { it in category.statuses }
        }
        category.label to count
    }
}

fun scholarshipPdfCategoryForStatus(status: String): String =
    scholarshipPdfStatusCategories.firstOrNull { status in it.statuses }?.label ?: "Other"

fun scholarshipPdfAppliedCount(counts: List<Pair<String, Int>>): Int =
    counts.filter { it.first == "Submitted" || it.first == "Awaiting Result" }.sumOf { it.second }

fun scholarshipPdfOpenCount(counts: List<Pair<String, Int>>): Int =
    counts.filter { it.first in setOf("Not Started", "In Preparation", "In Progress") }
        .sumOf { it.second }

fun paginateScholarshipPdfRows(
    rowHeights: List<Float>,
    firstPageTop: Float,
    continuationPageTop: Float,
    pageBottom: Float
): List<List<Int>> {
    require(firstPageTop < pageBottom && continuationPageTop < pageBottom) {
        "Scholarship PDF page content must have positive height."
    }
    require(rowHeights.all { it > 0f }) { "Scholarship PDF row heights must be positive."     }

    val pages = mutableListOf<MutableList<Int>>(mutableListOf())
    var y = firstPageTop
    rowHeights.forEachIndexed { index, height ->
        if (y + height > pageBottom && pages.last().isNotEmpty()) {
            pages.add(mutableListOf())
            y = continuationPageTop
        }
        pages.last().add(index)
        y += height
    }
    return pages
}
