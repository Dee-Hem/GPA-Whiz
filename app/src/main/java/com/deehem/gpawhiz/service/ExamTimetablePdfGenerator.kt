package com.deehem.gpawhiz.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.deehem.gpawhiz.data.Course
import com.deehem.gpawhiz.data.Exam
import com.deehem.gpawhiz.data.StudentProfile
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExamTimetablePdfGenerator {
    private const val DATE_HEADER_HEIGHT = 22f
    private const val TABLE_HEADER_HEIGHT = 20f
    private const val CONTENT_BOTTOM = 790f

    private data class ExamPdfRow(
        val exam: Exam,
        val courseTitle: String,
        val courseCode: String,
        val venue: String,
        val seatNumber: String,
        val wrapped: PdfWrappedRow
    )

    fun generate(
        student: StudentProfile,
        exams: List<Exam>,
        courses: List<Course>,
        upcomingOnly: Boolean,
        outputStream: OutputStream,
        nowMillis: Long = System.currentTimeMillis(),
        use24HourTime: Boolean = false
    ) {
        val sortedExams = filterAndSortExams(exams, upcomingOnly, nowMillis)
        val dateGroups = groupExamsByDate(sortedExams)
        val clashes = findExamClashes(sortedExams)
        val clashPaint = LectureTimetablePdfStyle.paint(7.5f, LectureTimetablePdfStyle.textGrey)
        val clashLines = clashes.flatMap {
            wrapPdfText(formatClash(it), LectureTimetablePdfStyle.PAGE_WIDTH - 2 * LectureTimetablePdfStyle.MARGIN, 7.5f) { text, size ->
                LectureTimetablePdfStyle.measureText(clashPaint, text, size)
            }
        }
        val courseById = courses.associateBy { it.id }
        val clashExamIds = clashes.flatMap { it.exams }.map { it.id }.toSet()
        val rowById = sortedExams.associate { exam ->
            exam.id to createExamPdfRow(exam, courseById[exam.courseId], exam.id in clashExamIds)
        }
        val summary = examSummaryText(
            sortedExams.size,
            dateGroups.firstOrNull()?.dateMillis?.let { formatPdfShortDate(Date(it)) },
            dateGroups.lastOrNull()?.dateMillis?.let { formatPdfShortDate(Date(it)) }
        )
        val bodyTop = LectureTimetablePdfStyle.measureHeaderBottom(
            student,
            LectureTimetablePdfStyle.PAGE_WIDTH,
            summary
        ) + 8f
        val pages = paginateExamTimetable(
            exams = sortedExams,
            clashLines = clashLines,
            rowHeightFor = { exam -> rowById.getValue(exam.id).wrapped.height },
            firstPageContentTop = bodyTop,
            nextPageContentTop = bodyTop,
            contentBottom = CONTENT_BOTTOM,
            dateHeaderHeight = DATE_HEADER_HEIGHT,
            tableHeaderHeight = TABLE_HEADER_HEIGHT,
            clashHeadingHeight = 35f,
            clashLineHeight = 11f
        ).ifEmpty { listOf(ExamTimetablePage(emptyList())) }
        val pdfDocument = PdfDocument()
        val generatedAt = Date(nowMillis)

        try {
            pages.forEachIndexed { pageIndex, pageModel ->
                val page = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(
                        LectureTimetablePdfStyle.PAGE_WIDTH,
                        LectureTimetablePdfStyle.PAGE_HEIGHT,
                        pageIndex + 1
                    ).create()
                )
                val canvas = page.canvas
                var y = LectureTimetablePdfStyle.drawHeader(
                    canvas = canvas,
                    student = student,
                    semesterOrSession = student.academicSession.ifBlank { "EXAM SCHEDULE" },
                    title = "EXAMINATION TIMETABLE",
                    pageWidth = LectureTimetablePdfStyle.PAGE_WIDTH,
                    summary = summary
                )
                y += 8f

                for (section in pageModel.sections) {
                    drawDateHeader(canvas, section.dateMillis, section.isContinuation, y)
                    y += DATE_HEADER_HEIGHT
                    drawTableHeader(canvas, y)
                    y += TABLE_HEADER_HEIGHT

                    for (exam in section.exams) {
                        val row = rowById.getValue(exam.id)
                        drawExamRow(
                            canvas = canvas,
                            row = row,
                            top = y,
                            isPast = isExamPast(exam, nowMillis),
                            hasClash = exam.id in clashExamIds
                        )
                        y += row.wrapped.height
                    }
                    y += 8f
                }

                if (pageModel.clashLines.isNotEmpty()) {
                    drawClashNotes(canvas, pageModel.clashLines, y)
                } else if (sortedExams.isEmpty() && pageIndex == 0) {
                    canvas.drawText(
                        "No upcoming exams scheduled.",
                        LectureTimetablePdfStyle.MARGIN,
                        y + 12f,
                        LectureTimetablePdfStyle.paint(9f, LectureTimetablePdfStyle.textGrey)
                    )
                }

                LectureTimetablePdfStyle.drawFooter(
                    canvas,
                    pageIndex + 1,
                    pages.size,
                    LectureTimetablePdfStyle.PAGE_WIDTH,
                    LectureTimetablePdfStyle.PAGE_HEIGHT,
                    generatedAt,
                    use24HourTime
                )
                pdfDocument.finishPage(page)
            }
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawDateHeader(
        canvas: Canvas,
        dateMillis: Long,
        isContinuation: Boolean,
        top: Float
    ) {
        val left = LectureTimetablePdfStyle.MARGIN
        val right = LectureTimetablePdfStyle.PAGE_WIDTH - LectureTimetablePdfStyle.MARGIN
        val dateBar = RectF(left, top, right, top + DATE_HEADER_HEIGHT - 2f)
        canvas.drawRoundRect(dateBar, 5f, 5f, Paint().apply { color = LectureTimetablePdfStyle.lightGrey })
        canvas.drawRoundRect(dateBar, 5f, 5f, Paint().apply {
            color = LectureTimetablePdfStyle.borderLight
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        })
        val continued = if (isContinuation) " (CONTINUED)" else ""
        canvas.drawText(
            dateFormatter().format(Date(dateMillis)) + continued,
            left + 8f,
            top + 14f,
            LectureTimetablePdfStyle.paint(8.5f, LectureTimetablePdfStyle.brandBlue, bold = true)
        )
    }

    private fun createExamPdfRow(exam: Exam, linkedCourse: Course?, hasClash: Boolean): ExamPdfRow {
        val courseCode = exam.courseCode.ifBlank { linkedCourse?.code.orEmpty() }.ifBlank { "-" }
        val values = listOf(
            formatExamTimeRange(exam.time, exam.endTime),
            courseCode,
            linkedCourse?.title?.ifBlank { "-" } ?: "-",
            formatCourseUnits(linkedCourse?.units),
            exam.venue.ifBlank { "-" },
            exam.seatNumber.ifBlank { "-" }
        )
        val edges = columnEdges()
        val widths = edges.zipWithNext { left, right -> right - left - 8f }.toMutableList().apply {
            if (hasClash) this[1] -= 12f
        }
        val textPaint = LectureTimetablePdfStyle.paint(8f, LectureTimetablePdfStyle.darkText)
        val measure: (String, Float) -> Float = { text, size ->
            LectureTimetablePdfStyle.measureText(textPaint, text, size)
        }
        val wrapped = wrapPdfTableRow(values, widths, 8f, 10f, 10f, measure)
        return ExamPdfRow(
            exam,
            values[2],
            courseCode,
            values[4],
            values[5],
            wrapped
        )
    }

    private fun drawTableHeader(canvas: Canvas, top: Float) {
        val edges = columnEdges()
        val right = LectureTimetablePdfStyle.PAGE_WIDTH - LectureTimetablePdfStyle.MARGIN
        val fillPaint = Paint().apply { color = LectureTimetablePdfStyle.lightGrey }
        val borderPaint = Paint().apply {
            color = LectureTimetablePdfStyle.borderLight
            strokeWidth = 0.7f
        }
        canvas.drawRect(edges.first(), top, right, top + TABLE_HEADER_HEIGHT, fillPaint)
        canvas.drawRect(edges.first(), top, right, top + TABLE_HEADER_HEIGHT, borderPaint.apply { style = Paint.Style.STROKE })
        val headerPaint = LectureTimetablePdfStyle.paint(7f, LectureTimetablePdfStyle.brandBlue, bold = true)
        listOf("TIME", "COURSE CODE", "COURSE TITLE", "Units", "VENUE", "SEAT NO.")
            .forEachIndexed { index, label ->
            val x = if (index == 3) (edges[index] + edges[index + 1]) / 2f else edges[index] + 4f
            val paint = if (index == 3) {
                LectureTimetablePdfStyle.paint(7f, LectureTimetablePdfStyle.brandBlue, bold = true)
                    .apply { textAlign = Paint.Align.CENTER }
            } else headerPaint
            canvas.drawText(label, x, top + 13f, paint)
            if (index > 0) canvas.drawLine(edges[index], top, edges[index], top + TABLE_HEADER_HEIGHT, borderPaint)
        }
    }

    private fun drawExamRow(
        canvas: Canvas,
        row: ExamPdfRow,
        top: Float,
        isPast: Boolean,
        hasClash: Boolean
    ) {
        val edges = columnEdges()
        val right = LectureTimetablePdfStyle.PAGE_WIDTH - LectureTimetablePdfStyle.MARGIN
        val textColor = if (isPast) Color.parseColor("#94A3B8") else LectureTimetablePdfStyle.darkText
        val borderColor = if (isPast) Color.parseColor("#D7DCE2") else LectureTimetablePdfStyle.borderLight
        val rowPaint = Paint().apply { color = if (isPast) Color.parseColor("#F8FAFC") else Color.WHITE }
        val borderPaint = Paint().apply {
            color = borderColor
            strokeWidth = 0.65f
            style = Paint.Style.STROKE
        }
        canvas.drawRect(edges.first(), top, right, top + row.wrapped.height, rowPaint)
        canvas.drawRect(edges.first(), top, right, top + row.wrapped.height, borderPaint)

        row.wrapped.cells.forEachIndexed { index, lines ->
            val isUnits = index == 3
            val x = if (isUnits) (edges[index] + edges[index + 1]) / 2f else edges[index] + 4f
            val paint = if (isUnits) {
                LectureTimetablePdfStyle.paint(8f, textColor).apply { textAlign = Paint.Align.CENTER }
            } else if (index == 1) {
                LectureTimetablePdfStyle.paint(8f, textColor, bold = true)
            } else LectureTimetablePdfStyle.paint(8f, textColor)
            lines.forEachIndexed { lineIndex, line ->
                canvas.drawText(line, x, top + 8f + paint.textSize + lineIndex * 10f, paint)
            }
            if (index > 0) canvas.drawLine(edges[index], top, edges[index], top + row.wrapped.height, borderPaint)
            if (hasClash && index == 1) {
                val markerPaint = LectureTimetablePdfStyle.paint(8f, textColor, bold = true).apply {
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("!", edges[index + 1] - 3f, top + 16f, markerPaint)
            }
        }
    }

    private fun drawClashNotes(canvas: Canvas, lines: List<String>, top: Float) {
        val left = LectureTimetablePdfStyle.MARGIN
        val headingPaint = LectureTimetablePdfStyle.paint(8f, LectureTimetablePdfStyle.darkText, bold = true)
        val bodyPaint = LectureTimetablePdfStyle.paint(7.5f, LectureTimetablePdfStyle.textGrey)
        canvas.drawText("CLASHES", left, top + 9f, headingPaint)
        var y = top + 20f
        for (line in lines) {
            canvas.drawText(line, left, y, bodyPaint)
            y += 10f
        }
    }

    private fun formatClash(clash: ExamClashGroup): String {
        val date = dateFormatter().format(Date(clash.dateMillis))
        val items = clash.exams.joinToString(" and ") {
            "${it.courseCode} (${formatExamTimeRange(it.time, it.endTime)})"
        }
        return "$date: $items"
    }

    private fun columnEdges(): List<Float> {
        return examTimetableColumnEdges(
            LectureTimetablePdfStyle.MARGIN,
            LectureTimetablePdfStyle.PAGE_WIDTH - LectureTimetablePdfStyle.MARGIN
        )
    }

    private fun dateFormatter() = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
}
