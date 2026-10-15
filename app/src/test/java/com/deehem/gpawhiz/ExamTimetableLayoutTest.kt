package com.deehem.gpawhiz

import com.deehem.gpawhiz.data.Exam
import com.deehem.gpawhiz.service.examTimetableRow
import com.deehem.gpawhiz.service.filterAndSortExams
import com.deehem.gpawhiz.service.findExamClashes
import com.deehem.gpawhiz.service.groupExamsByDate
import com.deehem.gpawhiz.service.isExamPast
import com.deehem.gpawhiz.service.paginateExamTimetable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExamTimetableLayoutTest {
    @Test
    fun noExamsProduceNoGroupsClashesOrPages() {
        assertTrue(filterAndSortExams(emptyList(), upcomingOnly = true, nowMillis = 0L).isEmpty())
        assertTrue(groupExamsByDate(emptyList()).isEmpty())
        assertTrue(findExamClashes(emptyList()).isEmpty())
        assertTrue(paginateExamTimetable(emptyList()).isEmpty())
    }

    @Test
    fun oneExamIsGroupedAndRenderedAsOneDateSectionAndPage() {
        val exam = exam(1, day = 12, month = Calendar.JANUARY, year = 2027, time = "09:00")

        val groups = groupExamsByDate(listOf(exam))
        val pages = paginateExamTimetable(listOf(exam))

        assertEquals(1, groups.size)
        assertEquals(listOf(exam), groups.single().exams)
        assertEquals(1, pages.size)
        assertEquals(listOf(exam), pages.single().sections.single().exams)
        assertFalse(pages.single().sections.single().isContinuation)
    }

    @Test
    fun severalExamsOnOneDateAreSortedByStartTime() {
        val early = exam(1, 12, Calendar.JANUARY, 2027, "08:00")
        val late = exam(2, 12, Calendar.JANUARY, 2027, "13:00")
        val middle = exam(3, 12, Calendar.JANUARY, 2027, "10:00")

        val group = groupExamsByDate(listOf(late, early, middle)).single()

        assertEquals(listOf(early, middle, late), group.exams)
    }

    @Test
    fun examsWithOverlappingThreeHourSlotsAreFlaggedAsClashes() {
        val first = exam(1, 12, Calendar.JANUARY, 2027, "09:00")
        val overlapping = exam(2, 12, Calendar.JANUARY, 2027, "11:00")
        val backToBack = exam(3, 12, Calendar.JANUARY, 2027, "14:00")

        val clashes = findExamClashes(listOf(first, overlapping, backToBack))

        assertEquals(1, clashes.size)
        assertEquals(listOf(first, overlapping), clashes.single().exams)
    }

    @Test
    fun explicitEndTimesUseActualOverlapAndAllowBackToBackExams() {
        val first = exam(1, 12, Calendar.JANUARY, 2027, "09:00", "10:00")
        val overlapping = exam(2, 12, Calendar.JANUARY, 2027, "09:30", "10:30")
        val backToBack = exam(3, 12, Calendar.JANUARY, 2027, "10:30", "11:00")

        val clashes = findExamClashes(listOf(first, overlapping, backToBack))

        assertEquals(1, clashes.size)
        assertEquals(listOf(first, overlapping), clashes.single().exams)
    }

    @Test
    fun missingEndTimeKeepsTheExistingThreeHourClashWindow() {
        val missingEnd = exam(1, 12, Calendar.JANUARY, 2027, "09:00")
        val explicitEnd = exam(2, 12, Calendar.JANUARY, 2027, "11:00", "11:30")

        assertEquals(listOf(missingEnd, explicitEnd), findExamClashes(listOf(missingEnd, explicitEnd)).single().exams)
    }

    @Test
    fun upcomingOnlyFiltersPastExamsAndAllExamsKeepsBoth() {
        val past = exam(1, 12, Calendar.JANUARY, 2027, "08:00")
        val upcoming = exam(2, 12, Calendar.JANUARY, 2027, "10:00")
        val now = examStartMillis(12, Calendar.JANUARY, 2027, 9)

        assertEquals(listOf(upcoming), filterAndSortExams(listOf(past, upcoming), true, now))
        assertEquals(listOf(past, upcoming), filterAndSortExams(listOf(upcoming, past), false, now))
    }

    @Test
    fun ongoingExamIsNotTreatedAsPastForUpcomingExport() {
        val ongoing = exam(1, 12, Calendar.JANUARY, 2027, "08:00", "10:00")
        val now = examStartMillis(12, Calendar.JANUARY, 2027, 9)

        assertFalse(isExamPast(ongoing, now))
        assertEquals(listOf(ongoing), filterAndSortExams(listOf(ongoing), true, now))
        assertTrue(isExamPast(ongoing, now + 2 * 60 * 60 * 1000L))
    }

    @Test
    fun missingSeatNumberAndCourseTitleDisplayAsDashes() {
        val row = examTimetableRow(exam(1, 12, Calendar.JANUARY, 2027, "09:00"), null)

        assertEquals("-", row.courseTitle)
        assertEquals("-", row.venue)
        assertEquals("-", row.seatNumber)
    }

    @Test
    fun examPdfRowUsesStoredVenueAndSeatNumber() {
        val exam = exam(1, 12, Calendar.JANUARY, 2027, "09:00").copy(
            venue = "Hall A",
            seatNumber = "B12"
        )

        val row = examTimetableRow(exam, "Algorithms")

        assertEquals("Hall A", row.venue)
        assertEquals("B12", row.seatNumber)
    }

    @Test
    fun longExamListPaginatesWithoutSplittingRowsOrDroppingExams() {
        val exams = (1..60).map { id ->
            exam(id, day = 12, month = Calendar.JANUARY, year = 2027, time = "09:00")
        }

        val pages = paginateExamTimetable(exams)

        assertTrue(pages.size > 1)
        assertTrue(pages.all { page -> page.sections.all { it.exams.isNotEmpty() } })
        assertEquals(exams.size, pages.sumOf { page -> page.sections.sumOf { it.exams.size } })
        assertTrue(pages.drop(1).all { page -> page.sections.first().isContinuation })
    }

    private fun exam(
        id: Int,
        day: Int,
        month: Int,
        year: Int,
        time: String,
        endTime: String? = null
    ): Exam = Exam(
        id = id,
        courseId = id,
        courseCode = "CSC $id",
        date = examDateMillis(day, month, year),
        time = time,
        endTime = endTime
    )

    private fun examDateMillis(day: Int, month: Int, year: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun examStartMillis(day: Int, month: Int, year: Int, hour: Int): Long =
        examDateMillis(day, month, year) + hour * 60 * 60 * 1000L
}
