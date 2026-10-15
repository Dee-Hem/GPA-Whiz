package com.deehem.gpawhiz

import com.deehem.gpawhiz.service.PdfTextRect
import com.deehem.gpawhiz.service.dailyTimetableColumnEdges
import com.deehem.gpawhiz.service.examSummaryText
import com.deehem.gpawhiz.service.examTimetableColumnEdges
import com.deehem.gpawhiz.service.fitPdfText
import com.deehem.gpawhiz.service.formatCourseUnits
import com.deehem.gpawhiz.service.formatPdfFooterDate
import com.deehem.gpawhiz.service.formatPdfShortDate
import com.deehem.gpawhiz.service.lectureSummaryText
import com.deehem.gpawhiz.service.textRectsDoNotOverlap
import com.deehem.gpawhiz.service.textRectsStayInside
import com.deehem.gpawhiz.service.timeColumnMinimumWidth
import com.deehem.gpawhiz.service.timetableCourseColors
import com.deehem.gpawhiz.service.wrapPdfTableRow
import com.deehem.gpawhiz.service.wrapPdfText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class PdfTextLayoutTest {
    private val measure: (String, Float) -> Float = { text, size -> text.length * size * 0.5f }

    @Test
    fun textFittingWrapsNormalLongAndUnspacedTextWithoutClippingLines() {
        val normal = fitPdfText("Room 12", 80f, 30f, 10f, 7f, measure)
        val title = fitPdfText("An exceptionally long course title for the printed timetable", 72f, 80f, 10f, 7f, measure)
        val code = fitPdfText("CSC1234567890123456789012345678901234567", 28f, 80f, 10f, 7f, measure)

        assertEquals(listOf("Room 12"), normal.lines)
        assertTrue(title.lines.size > 1)
        assertTrue(code.lines.size > 1)
        title.lines.forEach { assertTrue(measure(it, title.textSize) <= 72f) }
        code.lines.forEach { assertTrue(measure(it, code.textSize) <= 28f) }
    }

    @Test
    fun textFittingPreservesEmptyAndSpecialCharacterText() {
        assertTrue(fitPdfText("", 100f, 30f, 10f, 7f, measure).lines.isEmpty())
        assertEquals(listOf("Café & R&D"), wrapPdfText("Café & R&D", 100f, 10f, measure))
    }

    @Test
    fun textTooSmallToFitReturnsAnExplicitTruncatedResultWithoutOverflow() {
        val fitted = fitPdfText("Text", 1f, 1f, 10f, 7f, measure)

        assertTrue(fitted.truncated)
        assertTrue(fitted.lines.isEmpty())
    }

    @Test
    fun wrappedRowsGrowToFitLongTitleVenueAndCourseCode() {
        val values = listOf(
            "CSC1234567890123456789012345678901234567",
            "A course title with enough words to require several wrapped lines",
            "V".repeat(60),
            "Seat 12"
        )
        val widths = listOf(90f, 180f, 100f, 70f)
        val row = wrapPdfTableRow(values, widths, 8f, 10f, 10f, measure)

        assertTrue(row.height > 30f)
        row.cells.forEachIndexed { index, lines ->
            lines.forEach { line -> assertTrue(measure(line, 8f) <= widths[index]) }
        }
        var cellLeft = 0f
        val rectangles = row.cells.flatMapIndexed { index, lines ->
            val left = cellLeft
            cellLeft += widths[index]
            lines.mapIndexed { lineIndex, line ->
                val textLeft = left + 4f
                val textTop = 5f + lineIndex * 10f
                PdfTextRect(textLeft, textTop, textLeft + measure(line, 8f), textTop + 8f)
            }
        }
        assertTrue(textRectsStayInside(rectangles, PdfTextRect(0f, 0f, widths.sum(), row.height)))
        assertTrue(textRectsDoNotOverlap(rectangles))
    }

    @Test
    fun timetableUnitsFormatWholeDecimalMissingAndZeroValues() {
        assertEquals("4", formatCourseUnits(4))
        assertEquals("4.5", formatCourseUnits(4.5))
        assertEquals("-", formatCourseUnits(null))
        assertEquals("0", formatCourseUnits(0))
        assertEquals("4", formatCourseUnits(4.0))
    }

    @Test
    fun timetableColumnWidthsStayAlignedForLongTableValues() {
        val left = 40f
        val right = 555f
        val cases = listOf(
            listOf("08:00-09:00", "CSC 101", "Algorithms", "3", "Hall A", "B12"),
            listOf("08:00-09:00", "CSC 101", "Algorithms", "3", "V".repeat(60), "B12"),
            listOf("08:00-09:00", "CSC 101", "T".repeat(40), "3", "Hall A", "B12"),
            listOf("08:00-09:00", "C".repeat(45), "Algorithms", "3", "Hall A", "B12")
        )
        val dailyCases = cases.map { values ->
            listOf(values[0], values[1], values[2], values[3], values[4])
        }

        @Test
        fun timetableTimeColumnFitsWidestFormattedRangeAndKeepsPageWidthAligned() {
            val left = 40f
            val right = 555f
            val ranges = listOf("8:00 - 9:00\u00A0AM", "11:00\u00A0AM - 12:00\u00A0PM")
            val minimumTimeWidth = timeColumnMinimumWidth(ranges, 8f, 8f, measure)
            val dailyEdges = dailyTimetableColumnEdges(left, right, minimumTimeWidth)
            val examEdges = examTimetableColumnEdges(left, right, minimumTimeWidth)

            assertTrue(dailyEdges[1] - dailyEdges[0] + 0.001f >= minimumTimeWidth)
            assertTrue(examEdges[1] - examEdges[0] + 0.001f >= minimumTimeWidth)
            assertEquals(right - left, dailyEdges.zipWithNext().sumOf { (a, b) -> (b - a).toDouble() }.toFloat(), 0.001f)
            assertEquals(right - left, examEdges.zipWithNext().sumOf { (a, b) -> (b - a).toDouble() }.toFloat(), 0.001f)
        }
        val examEdges = examTimetableColumnEdges(left, right)
        val dailyEdges = dailyTimetableColumnEdges(left, right)

        fun assertRowsFit(valuesList: List<List<String>>, edges: List<Float>, horizontalPadding: Float) {
            assertEquals(valuesList.first().size + 1, edges.size)
            assertEquals(left, edges.first(), 0f)
            assertEquals(right, edges.last(), 0f)
            assertTrue(edges.zipWithNext().all { (first, second) -> second > first })
            val totalWidth = edges.zipWithNext().sumOf { (it.second - it.first).toDouble() }.toFloat()
            assertEquals(right - left, totalWidth, 0.001f)
            val widths = edges.zipWithNext { first, second -> second - first - horizontalPadding }
            assertTrue(widths.all { it > 0f })
            for (values in valuesList) {
                val row = wrapPdfTableRow(values, widths, 8f, 10f, 10f, measure)
                assertTrue(row.height >= 20f)
                row.cells.forEachIndexed { index, lines ->
                    lines.forEach { line -> assertTrue(measure(line, 8f) <= widths[index]) }
                }
                val rectangles = row.cells.flatMapIndexed { index, lines ->
                    lines.mapIndexed { lineIndex, line ->
                        val textLeft = edges[index] + horizontalPadding / 2f
                        val textTop = lineIndex * 10f
                        PdfTextRect(
                            textLeft,
                            textTop,
                            textLeft + measure(line, 8f),
                            textTop + 8f
                        )
                    }
                }
                assertTrue(textRectsStayInside(rectangles, PdfTextRect(left, 0f, right, row.height)))
                assertTrue(textRectsDoNotOverlap(rectangles))
            }
        }

        assertRowsFit(cases, examEdges, 8f)
        assertRowsFit(dailyCases, dailyEdges, 10f)
    }

    @Test
    fun courseColorsArePositionAssignedDistinctAndRepeatOnlyAfterPalette() {
        val codes = (1..15).map { "CSC%03d".format(Locale.ROOT, it) }
        val one = timetableCourseColors(codes.take(1))
        val five = timetableCourseColors(codes.take(5))
        val twelve = timetableCourseColors(codes.take(12))
        val fifteen = timetableCourseColors(codes)

        assertEquals(1, one.values.distinct().size)
        assertEquals(5, five.values.distinct().size)
        assertEquals(12, twelve.values.distinct().size)
        assertEquals(fifteen.getValue("CSC001"), fifteen.getValue("CSC013"))
        assertEquals(fifteen.getValue("CSC002"), fifteen.getValue("CSC014"))
        assertEquals(fifteen.getValue("CSC003"), fifteen.getValue("CSC015"))
        assertEquals(fifteen, timetableCourseColors(codes.reversed()))
        assertEquals(fifteen.getValue("CSC001"), timetableCourseColors(codes + "csc001").getValue("CSC001"))
    }

    @Test
    fun longFacultyValueWrapsWithoutLosingText() {
        val faculty = ("FACULTY " + "OF VERY LONG ACADEMIC STUDIES ".repeat(4)).trimEnd()
        val lines = wrapPdfText(faculty, 70f, 8.5f, measure)

        assertTrue(lines.size > 1)
        assertEquals(faculty, lines.joinToString(" "))
    }

    @Test
    fun summaryAndFooterUseRequestedSingularPluralAndDateFormats() {
        val date = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 2, 3, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        assertEquals("1 lecture across 1 day", lectureSummaryText(1, 1))
        assertEquals("5 lectures across 4 days", lectureSummaryText(5, 4))
        assertEquals("1 exam, 15 Oct 2026 to 15 Oct 2026", examSummaryText(1, "15 Oct 2026", "15 Oct 2026"))
        assertEquals("15 Oct 2026, 2:03\u00A0AM", formatPdfFooterDate(date, Locale.US))
        assertEquals("15 Oct 2026", formatPdfShortDate(date, Locale.US))
    }

    @Test
    fun textRectangleHelpersDetectContainmentAndOverlap() {
        val bounds = PdfTextRect(0f, 0f, 300f, 60f)
        val cells = listOf(
            PdfTextRect(4f, 4f, 80f, 56f),
            PdfTextRect(84f, 4f, 280f, 56f)
        )
        assertTrue(textRectsStayInside(cells, bounds))
        assertTrue(textRectsDoNotOverlap(cells))
        assertFalse(textRectsStayInside(listOf(PdfTextRect(0f, 0f, 301f, 10f)), bounds))
        assertFalse(textRectsDoNotOverlap(cells + PdfTextRect(70f, 10f, 90f, 20f)))
    }
}
