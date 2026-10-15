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
import com.deehem.gpawhiz.service.formatExamTimeRange

object ExamTimetablePdfGenerator {
    private const val DATE_HEADER_HEIGHT = 22f
    private const val TABLE_HEADER_HEIGHT = 20f
    private const val ROW_HEIGHT = 25f
    private const val CONTENT_BOTTOM = 790f
    private const val FIRST_PAGE_CONTENT_TOP = 204f
    private const val NEXT_PAGE_CONTENT_TOP = 184f

    fun generate(
        student: StudentProfile,
        exams: List<Exam>,
        courses: List<Course>,
        upcomingOnly: Boolean,
        outputStream: OutputStream,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val sortedExams = filterAndSortExams(exams, upcomingOnly, nowMillis)
        val dateGroups = groupExamsByDate(sortedExams)
        val clashes = findExamClashes(sortedExams)
        val clashPaint = LectureTimetablePdfStyle.paint(7.5f, LectureTimetablePdfStyle.textGrey)
        val clashLines = clashes.flatMap { wrapLine(
            formatClash(it),
            clashPaint,
            LectureTimetablePdfStyle.PAGE_WIDTH - 2 * LectureTimetablePdfStyle.MARGIN
        ) }
        val pages = paginateExamTimetable(
            exams = sortedExams,
            clashLines = clashLines,
            firstPageContentTop = FIRST_PAGE_CONTENT_TOP,
            nextPageContentTop = NEXT_PAGE_CONTENT_TOP,
            contentBottom = CONTENT_BOTTOM,
            dateHeaderHeight = DATE_HEADER_HEIGHT,
            tableHeaderHeight = TABLE_HEADER_HEIGHT,
            rowHeight = ROW_HEIGHT
        ).ifEmpty { listOf(ExamTimetablePage(emptyList())) }
        val courseById = courses.associateBy { it.id }
        val clashExamIds = clashes.flatMap { it.exams }.map { it.id }.toSet()
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
                    pageWidth = LectureTimetablePdfStyle.PAGE_WIDTH
                )
                if (pageIndex == 0) {
                    drawSummary(canvas, sortedExams, dateGroups.map { it.dateMillis }, y)
                    y = FIRST_PAGE_CONTENT_TOP
                } else {
                    y = NEXT_PAGE_CONTENT_TOP
                }

                for (section in pageModel.sections) {
                    drawDateHeader(canvas, section.dateMillis, section.isContinuation, y)
                    y += DATE_HEADER_HEIGHT
                    drawTableHeader(canvas, y)
                    y += TABLE_HEADER_HEIGHT

                    for (exam in section.exams) {
                        val linkedCourse = courseById[exam.courseId]
                        val row = examTimetableRow(exam, linkedCourse?.title)
                        drawExamRow(
                            canvas = canvas,
                            row = row,
                            courseCode = exam.courseCode.ifBlank { linkedCourse?.code.orEmpty() }.ifBlank { "-" },
                            top = y,
                            isPast = isExamPast(exam, nowMillis),
                            hasClash = exam.id in clashExamIds
                        )
                        y += ROW_HEIGHT
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
                    generatedAt
                )
                pdfDocument.finishPage(page)
            }
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawSummary(
        canvas: Canvas,
        exams: List<Exam>,
        sortedDates: List<Long>,
        headerBottom: Float
    ) {
        val dateFormat = dateFormatter()
        val summary = "Exams: ${exams.size}    First: ${
            sortedDates.firstOrNull()?.let { dateFormat.format(Date(it)) } ?: "-"
        }    Last: ${
            sortedDates.lastOrNull()?.let { dateFormat.format(Date(it)) } ?: "-"
        }"
        canvas.drawText(
            LectureTimetablePdfStyle.ellipsize(
                summary,
                LectureTimetablePdfStyle.paint(8f, LectureTimetablePdfStyle.darkText),
                LectureTimetablePdfStyle.PAGE_WIDTH - 2 * LectureTimetablePdfStyle.MARGIN
            ),
            LectureTimetablePdfStyle.MARGIN,
            headerBottom + 8f,
            LectureTimetablePdfStyle.paint(8f, LectureTimetablePdfStyle.darkText)
        )
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
        listOf("TIME", "COURSE CODE", "COURSE TITLE", "VENUE", "SEAT NO.").forEachIndexed { index, label ->
            canvas.drawText(label, edges[index] + 4f, top + 13f, headerPaint)
            if (index > 0) canvas.drawLine(edges[index], top, edges[index], top + TABLE_HEADER_HEIGHT, borderPaint)
        }
    }

    private fun drawExamRow(
        canvas: Canvas,
        row: ExamTimetableRow,
        courseCode: String,
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
        val textPaint = LectureTimetablePdfStyle.paint(8f, textColor)
        canvas.drawRect(edges.first(), top, right, top + ROW_HEIGHT, rowPaint)
        canvas.drawRect(edges.first(), top, right, top + ROW_HEIGHT, borderPaint)

        val values = listOf(
            formatExamTimeRange(row.exam.time, row.exam.endTime),
            courseCode,
            row.courseTitle,
            row.venue,
            row.seatNumber
        )
        values.forEachIndexed { index, value ->
            val x = edges[index] + 4f
            val width = edges[index + 1] - edges[index] - 8f
            val markerWidth = if (hasClash && index == 1) 9f else 0f
            val paint = if (index == 1) {
                LectureTimetablePdfStyle.paint(8f, textColor, bold = true)
            } else textPaint
            canvas.drawText(
                LectureTimetablePdfStyle.ellipsize(value, paint, width - markerWidth),
                x,
                top + 16f,
                paint
            )
            if (index > 0) canvas.drawLine(edges[index], top, edges[index], top + ROW_HEIGHT, borderPaint)
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

    private fun wrapLine(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (paint.measureText(text) <= maxWidth) return listOf(text)
        val words = text.split(' ')
        val result = mutableListOf<String>()
        var line = ""
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) <= maxWidth) {
                line = candidate
            } else {
                if (line.isNotEmpty()) result.add(line)
                line = LectureTimetablePdfStyle.ellipsize(word, paint, maxWidth)
            }
        }
        if (line.isNotEmpty()) result.add(line)
        return result
    }

    private fun formatClash(clash: ExamClashGroup): String {
        val date = dateFormatter().format(Date(clash.dateMillis))
        val items = clash.exams.joinToString(" and ") {
            "${it.courseCode} (${formatExamTimeRange(it.time, it.endTime)})"
        }
        return "$date: $items"
    }

    private fun columnEdges(): List<Float> {
        val left = LectureTimetablePdfStyle.MARGIN
        val width = LectureTimetablePdfStyle.PAGE_WIDTH - 2 * LectureTimetablePdfStyle.MARGIN
        return listOf(
            left,
            left + width * 0.145f,
            left + width * 0.315f,
            left + width * 0.595f,
            left + width * 0.825f,
            left + width
        )
    }

    private fun dateFormatter() = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
}
