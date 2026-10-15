package com.deehem.gpawhiz.service

import com.deehem.gpawhiz.data.Exam
import java.util.Calendar

data class ExamDateGroup(
    val dateMillis: Long,
    val exams: List<Exam>
)

data class ExamClashGroup(
    val dateMillis: Long,
    val exams: List<Exam>
)

data class ExamPageSection(
    val dateMillis: Long,
    val isContinuation: Boolean,
    val exams: List<Exam>
)

data class ExamTimetablePage(
    val sections: List<ExamPageSection>,
    val clashLines: List<String> = emptyList()
)

data class ExamTimetableRow(
    val exam: Exam,
    val courseTitle: String,
    val venue: String,
    val seatNumber: String
)

private data class TimedExam(
    val exam: Exam,
    val start: Long,
    val end: Long
)

fun examStartTimeMillis(exam: Exam): Long = examTimeOnDateMillis(exam.date, exam.time)

fun isExamPast(exam: Exam, nowMillis: Long): Boolean {
    val start = examStartTimeMillis(exam)
    val endTime = exam.endTime?.takeIf { it.isNotBlank() } ?: return start < nowMillis
    return examTimeOnDateMillis(exam.date, endTime) <= nowMillis
}

fun filterAndSortExams(
    exams: List<Exam>,
    upcomingOnly: Boolean,
    nowMillis: Long
): List<Exam> {
    val filtered = if (upcomingOnly) {
        exams.filterNot { isExamPast(it, nowMillis) }
    } else {
        exams
    }
    return filtered.sortedWith(compareBy<Exam> { dateStartMillis(it.date) }.thenBy { examStartTimeMillis(it) })
}

fun groupExamsByDate(exams: List<Exam>): List<ExamDateGroup> =
    exams.groupBy { dateStartMillis(it.date) }
        .toSortedMap()
        .map { (dateMillis, dayExams) ->
            ExamDateGroup(
                dateMillis,
                dayExams.sortedBy(::examStartTimeMillis)
            )
        }

fun findExamClashes(exams: List<Exam>): List<ExamClashGroup> {
    val clashes = mutableListOf<ExamClashGroup>()

    for (dayGroup in groupExamsByDate(exams)) {
        val timedExams = dayGroup.exams.map(::timedExam)
        var current = mutableListOf<TimedExam>()
        var groupEnd = Long.MIN_VALUE

        fun saveGroup() {
            if (current.size > 1) {
                clashes.add(ExamClashGroup(dayGroup.dateMillis, current.map { it.exam }))
            }
            current = mutableListOf()
            groupEnd = Long.MIN_VALUE
        }

        for (timed in timedExams) {
            if (current.isNotEmpty() && timed.start >= groupEnd) saveGroup()
            current.add(timed)
            groupEnd = maxOf(groupEnd, timed.end)
        }
        if (current.isNotEmpty()) saveGroup()
    }

    return clashes
}

fun examTimetableRow(exam: Exam, courseTitle: String?): ExamTimetableRow =
    ExamTimetableRow(
        exam = exam,
        courseTitle = courseTitle?.ifBlank { "-" } ?: "-",
        venue = exam.venue.ifBlank { "-" },
        seatNumber = exam.seatNumber.ifBlank { "-" }
    )

fun paginateExamTimetable(
    exams: List<Exam>,
    clashLines: List<String> = emptyList(),
    rowHeightFor: ((Exam) -> Float)? = null,
    firstPageContentTop: Float = 204f,
    nextPageContentTop: Float = 184f,
    contentBottom: Float = 790f,
    dateHeaderHeight: Float = 22f,
    tableHeaderHeight: Float = 20f,
    rowHeight: Float = 25f,
    sectionGap: Float = 8f,
    clashHeadingHeight: Float = 18f,
    clashLineHeight: Float = 11f
): List<ExamTimetablePage> {
    if (exams.isEmpty() && clashLines.isEmpty()) return emptyList()
    val heightFor = rowHeightFor ?: { _: Exam -> rowHeight }
    if (exams.isNotEmpty()) {
        require(firstPageContentTop + dateHeaderHeight + tableHeaderHeight + heightFor(exams.first()) <= contentBottom) {
            "Page body must fit at least one date header, table header, and exam row."
        }
    }

    val pages = mutableListOf<MutableList<ExamPageSection>>()
    var currentPage = mutableListOf<ExamPageSection>()
    var cursor = firstPageContentTop

    fun startPage() {
        if (currentPage.isNotEmpty()) pages.add(currentPage)
        currentPage = mutableListOf()
        cursor = nextPageContentTop
    }

    for (dateGroup in groupExamsByDate(exams)) {
        var offset = 0
        var isContinuation = false
        while (offset < dateGroup.exams.size) {
            val headingHeight = dateHeaderHeight + tableHeaderHeight
            if (cursor + headingHeight + heightFor(dateGroup.exams[offset]) > contentBottom) startPage()
            val sectionExams = mutableListOf<Exam>()
            var heightLeft = contentBottom - cursor - headingHeight
            while (offset < dateGroup.exams.size && heightFor(dateGroup.exams[offset]) <= heightLeft) {
                sectionExams.add(dateGroup.exams[offset])
                heightLeft -= heightFor(dateGroup.exams[offset])
                offset++
            }
            require(sectionExams.isNotEmpty()) { "An exam row does not fit on a portrait page." }
            currentPage.add(
                ExamPageSection(
                    dateMillis = dateGroup.dateMillis,
                    isContinuation = isContinuation,
                    exams = sectionExams
                )
            )
            cursor += headingHeight + sectionExams.sumOf { heightFor(it).toDouble() }.toFloat() + sectionGap

            if (offset < dateGroup.exams.size) {
                startPage()
                isContinuation = true
            }
        }
    }
    if (currentPage.isNotEmpty()) pages.add(currentPage)

    val result = pages.map { ExamTimetablePage(it) }.toMutableList()
    if (clashLines.isNotEmpty()) {
        val perPage = ((contentBottom - nextPageContentTop - clashHeadingHeight) / clashLineHeight)
            .toInt()
            .coerceAtLeast(1)
        clashLines.chunked(perPage).forEach { lines ->
            result.add(ExamTimetablePage(emptyList(), lines))
        }
    }
    return result
}

private fun timedExam(exam: Exam): TimedExam {
    val start = examTimeOnDateMillis(exam.date, exam.time)
    val end = exam.endTime?.takeIf { it.isNotBlank() }?.let { examTimeOnDateMillis(exam.date, it) }
        ?: start + EXAM_DURATION_MILLIS
    require(end > start) { "Exam end time must be after its start time." }
    return TimedExam(exam, start, end)
}

private fun dateStartMillis(dateMillis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

// Matches the existing exam calendar export's default event duration.
private const val EXAM_DURATION_MILLIS = 3 * 60 * 60 * 1000L
