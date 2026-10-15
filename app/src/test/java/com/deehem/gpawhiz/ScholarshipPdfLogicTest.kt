package com.deehem.gpawhiz

import com.deehem.gpawhiz.data.Scholarship
import com.deehem.gpawhiz.data.ScholarshipStatus
import com.deehem.gpawhiz.service.ScholarshipPdfTone
import com.deehem.gpawhiz.service.paginateScholarshipPdfRows
import com.deehem.gpawhiz.service.scholarshipPdfAppliedCount
import com.deehem.gpawhiz.service.scholarshipPdfDaysText
import com.deehem.gpawhiz.service.scholarshipPdfOpenCount
import com.deehem.gpawhiz.service.scholarshipPdfOpenDeadlines
import com.deehem.gpawhiz.service.scholarshipPdfStatusCounts
import com.deehem.gpawhiz.service.wrapPdfTableRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScholarshipPdfLogicTest {
    @Test
    fun daysTextUsesRequestedWordingAndToneForEveryDeadlineState() {
        val today = localDate(2026, Calendar.OCTOBER, 15)
        val tomorrow = localDate(2026, Calendar.OCTOBER, 16)
        val future = localDate(2026, Calendar.OCTOBER, 27)
        val yesterday = localDate(2026, Calendar.OCTOBER, 14)
        val past = localDate(2026, Calendar.JUNE, 30)

        assertEquals(ScholarshipPdfTone.MUTED, scholarshipPdfDaysText(
            ScholarshipStatus.NOT_STARTED,
            null,
            today
        ).tone)
        assertEquals("-", scholarshipPdfDaysText(ScholarshipStatus.NOT_STARTED, null, today).text)
        assertEquals(
            "Today",
            scholarshipPdfDaysText(ScholarshipStatus.IN_PROGRESS, today, today).text
        )
        assertEquals(
            ScholarshipPdfTone.DUE_TODAY,
            scholarshipPdfDaysText(ScholarshipStatus.IN_PROGRESS, today, today).tone
        )
        assertEquals(
            ScholarshipPdfTone.MUTED,
            scholarshipPdfDaysText(ScholarshipStatus.SUBMITTED, today, today).tone
        )
        assertEquals(
            "Tomorrow",
            scholarshipPdfDaysText(ScholarshipStatus.PREPARING, tomorrow, today).text
        )
        assertEquals(
            "In 12 days",
            scholarshipPdfDaysText(ScholarshipStatus.NOT_STARTED, future, today).text
        )
        assertEquals(
            ScholarshipPdfTone.STANDARD,
            scholarshipPdfDaysText(ScholarshipStatus.NOT_STARTED, future, today).tone
        )
        assertEquals(
            "Overdue by 1 day",
            scholarshipPdfDaysText(ScholarshipStatus.NOT_STARTED, yesterday, today).text
        )
        assertEquals(
            ScholarshipPdfTone.OVERDUE,
            scholarshipPdfDaysText(ScholarshipStatus.NOT_STARTED, yesterday, today).tone
        )
        assertEquals(
            "1 day ago",
            scholarshipPdfDaysText(ScholarshipStatus.SUBMITTED, yesterday, today).text
        )
        assertEquals(
            ScholarshipPdfTone.MUTED,
            scholarshipPdfDaysText(ScholarshipStatus.SUBMITTED, yesterday, today).tone
        )
        assertEquals(
            "107 days ago",
            scholarshipPdfDaysText(ScholarshipStatus.REJECTED, past, today).text
        )
    }

    @Test
    fun everyClosedStatusKeepsPastDeadlineTextMuted() {
        val today = localDate(2026, Calendar.OCTOBER, 15)
        val yesterday = localDate(2026, Calendar.OCTOBER, 14)
        val closedStatuses = listOf(
            ScholarshipStatus.SUBMITTED,
            ScholarshipStatus.AWAITING_RESULT,
            ScholarshipStatus.AWARDED,
            ScholarshipStatus.REJECTED,
            ScholarshipStatus.WITHDRAWN,
            ScholarshipStatus.EXPIRED
        )

        closedStatuses.forEach { status ->
            val result = scholarshipPdfDaysText(status, yesterday, today)
            assertEquals("$status should be muted", ScholarshipPdfTone.MUTED, result.tone)
            assertEquals("$status should not be red overdue text", "1 day ago", result.text)
        }
    }

    @Test
    fun openDeadlinesExcludeMissingPassedAndClosedItemsAndSortByDate() {
        val today = localDate(2026, Calendar.OCTOBER, 15)
        val dueTomorrow = scholarship("Tomorrow", ScholarshipStatus.NOT_STARTED, localDate(2026, Calendar.OCTOBER, 16))
        val dueToday = scholarship("Today", ScholarshipStatus.IN_PROGRESS, today)
        val passed = scholarship("Passed", ScholarshipStatus.IN_PROGRESS, localDate(2026, Calendar.OCTOBER, 14))
        val noDeadline = scholarship("No deadline", ScholarshipStatus.PREPARING, null)
        val submitted = scholarship("Submitted", ScholarshipStatus.SUBMITTED, localDate(2026, Calendar.OCTOBER, 16))
        val ready = scholarship("Ready to submit", ScholarshipStatus.READY_TO_SUBMIT, localDate(2026, Calendar.OCTOBER, 16))

        assertEquals(
            listOf(dueToday, dueTomorrow),
            scholarshipPdfOpenDeadlines(
                listOf(dueTomorrow, passed, noDeadline, submitted, ready, dueToday),
                today
            )
        )
        assertTrue(scholarshipPdfOpenDeadlines(listOf(passed, noDeadline, submitted), today).isEmpty())
    }

    @Test
    fun statusCountsAndMetricCountsReconcileForEmptySingleAndThirtyScholarships() {
        listOf(
            emptyList(),
            listOf(ScholarshipStatus.NOT_STARTED),
            List(30) { index -> ScholarshipStatus.ALL[index % ScholarshipStatus.ALL.size] }
        ).forEach { statuses ->
            val counts = scholarshipPdfStatusCounts(statuses)
            assertEquals(statuses.size, counts.sumOf { it.second })
            assertEquals(
                statuses.count { it in setOf(ScholarshipStatus.SUBMITTED, ScholarshipStatus.ASSESSMENT, ScholarshipStatus.INTERVIEW, ScholarshipStatus.AWAITING_RESULT) },
                scholarshipPdfAppliedCount(counts)
            )
            assertEquals(
                statuses.count { it in setOf(ScholarshipStatus.NOT_STARTED, ScholarshipStatus.PREPARING, ScholarshipStatus.IN_PROGRESS) },
                scholarshipPdfOpenCount(counts)
            )
        }
        assertEquals(0, scholarshipPdfStatusCounts(emptyList()).sumOf { it.second })
    }

    @Test
    fun rowsWrapLongNamesProvidersAndCurrencyValuesAndFortyRowsPaginateIntact() {
        val measure: (String, Float) -> Float = { text, size -> text.length * size * 0.52f }
        val longName = "Scholarship " + "N".repeat(100)
        val longProvider = "Provider " + "P".repeat(100)
        val awardValues = listOf(
            "₦1,234,567.89 (≈ $1,234.56)",
            "$1,234,567.89 (≈ £987.65)",
            "£1,234,567.89 (≈ €987.65)",
            "€1,234,567.89 (≈ ₦1,234,567.89)"
        )
        val row = wrapPdfTableRow(
            listOf(longName, longProvider) + awardValues,
            listOf(100f, 100f, 100f, 100f, 100f, 100f),
            textSize = 8f,
            lineHeight = 10f,
            verticalPadding = 8f,
            measure = measure
        )
        assertTrue(row.cells.all { it.size > 1 })
        assertTrue(row.cells.flatten().all { measure(it, 8f) <= 100f })

        val pages = paginateScholarshipPdfRows(
            rowHeights = List(40) { 24f },
            firstPageTop = 74f,
            continuationPageTop = 66f,
            pageBottom = 549f
        )
        assertTrue(pages.size > 1)
        assertEquals((0 until 40).toList(), pages.flatten())
        assertFalse(pages.any { it.isEmpty() })
    }

    private fun scholarship(name: String, status: String, deadline: Long?) =
        Scholarship(name = name, organization = "Provider", status = status, deadlineDate = deadline)

    private fun localDate(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
