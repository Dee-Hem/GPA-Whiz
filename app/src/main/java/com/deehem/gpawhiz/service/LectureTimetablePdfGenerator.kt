package com.deehem.gpawhiz.service

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.deehem.gpawhiz.data.Course
import com.deehem.gpawhiz.data.StudentProfile
import com.deehem.gpawhiz.data.TimetableSlot
import java.io.OutputStream
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

enum class LectureTimetablePdfLayout {
    WEEKLY_GRID,
    DAILY_LIST
}

object LectureTimetablePdfGenerator {
    private const val DAY_HEADER_HEIGHT = 22f
    private const val TABLE_HEADER_HEIGHT = 19f
    private const val DAILY_ROW_HEIGHT = 25f
    private const val DAILY_CONTENT_BOTTOM = 793f

    private data class LegendCourse(
        val code: String,
        val title: String,
        val units: String
    )

    private data class DailyDaySection(
        val dayOfWeek: Int,
        val isContinuation: Boolean,
        val slots: List<TimetableSlot>
    )

    fun generate(
        student: StudentProfile,
        semesterOrSession: String,
        slots: List<TimetableSlot>,
        courses: List<Course>,
        layout: LectureTimetablePdfLayout,
        outputStream: OutputStream
    ) {
        require(slots.isNotEmpty()) { "There are no lectures to export." }
        slots.forEach(::timedLecture)
        val pdfDocument = PdfDocument()
        val generatedAt = Date()
        try {
            when (layout) {
                LectureTimetablePdfLayout.WEEKLY_GRID -> drawWeeklyGrid(
                    pdfDocument, student, semesterOrSession, slots, courses, generatedAt
                )
                LectureTimetablePdfLayout.DAILY_LIST -> drawDailyList(
                    pdfDocument, student, semesterOrSession, slots, courses, generatedAt
                )
            }
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawWeeklyGrid(
        document: PdfDocument,
        student: StudentProfile,
        semesterOrSession: String,
        slots: List<TimetableSlot>,
        courses: List<Course>,
        generatedAt: Date
    ) {
        val pageWidth = LectureTimetablePdfStyle.LANDSCAPE_WIDTH
        val pageHeight = LectureTimetablePdfStyle.LANDSCAPE_HEIGHT
        val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
        val canvas = page.canvas
        val headerBottom = LectureTimetablePdfStyle.drawHeader(
            canvas, student, semesterOrSession, "LECTURE TIMETABLE", pageWidth
        )
        val courseById = courses.associateBy { it.id }
        val legendCourses = buildLegendCourses(slots, courseById)
        val clashes = lectureClashGroups(slots)
        val notePaint = LectureTimetablePdfStyle.paint(7.5f, LectureTimetablePdfStyle.textGrey)
        val clashLines = wrapText(
            clashDescription(clashes),
            notePaint,
            pageWidth - 2 * LectureTimetablePdfStyle.MARGIN - 45f
        )
        val legendRows = ceil(legendCourses.size / 2f).toInt()
        val legendHeight = if (legendCourses.isEmpty()) 0f else 16f + legendRows * 11f
        val clashHeight = clashLines.size * 10f
        val contentBottom = pageHeight - 42f
        val legendTop = contentBottom - legendHeight
        val clashTop = legendTop - if (clashLines.isEmpty()) 0f else clashHeight + 7f
        val gridTop = headerBottom + 34f
        val gridBottom = clashTop - if (clashLines.isEmpty()) 18f else 9f
        require(gridBottom - gridTop >= 55f) {
            "Too many timetable entries to fit the weekly grid and legend on one page."
        }

        canvas.drawText(
            "WEEKLY GRID",
            LectureTimetablePdfStyle.MARGIN,
            headerBottom + 15f,
            LectureTimetablePdfStyle.paint(9f, LectureTimetablePdfStyle.brandBlue, bold = true)
        )

        drawWeeklyGridArea(canvas, slots, gridTop, gridBottom, pageWidth)
        if (clashLines.isNotEmpty()) {
            val headingPaint = LectureTimetablePdfStyle.paint(7.5f, LectureTimetablePdfStyle.darkText, bold = true)
            canvas.drawText("CLASHES", LectureTimetablePdfStyle.MARGIN, clashTop + 8f, headingPaint)
            clashLines.forEachIndexed { index, line ->
                canvas.drawText(
                    line,
                    LectureTimetablePdfStyle.MARGIN + 45f,
                    clashTop + 8f + index * 10f,
                    notePaint
                )
            }
        }
        drawLegend(canvas, legendCourses, legendTop, pageWidth)
        LectureTimetablePdfStyle.drawFooter(canvas, 1, 1, pageWidth, pageHeight, generatedAt)
        document.finishPage(page)
    }

    private fun drawWeeklyGridArea(
        canvas: Canvas,
        slots: List<TimetableSlot>,
        gridTop: Float,
        gridBottom: Float,
        pageWidth: Int
    ) {
        val hourRange = lectureHourRange(slots)
            ?: throw IllegalArgumentException("There are no lectures to display.")
        val days = lectureVisibleDays(slots)
        val axisWidth = 39f
        val gridLeft = LectureTimetablePdfStyle.MARGIN + axisWidth
        val gridRight = pageWidth - LectureTimetablePdfStyle.MARGIN
        val dayWidth = (gridRight - gridLeft) / days.size
        val gridHeight = gridBottom - gridTop
        val gridLinePaint = Paint().apply {
            color = LectureTimetablePdfStyle.borderLight
            strokeWidth = 0.65f
        }
        val gridOutlinePaint = Paint().apply {
            color = LectureTimetablePdfStyle.borderLight
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val dayPaint = LectureTimetablePdfStyle.paint(7.5f, LectureTimetablePdfStyle.darkText, bold = true).apply {
            textAlign = Paint.Align.CENTER
        }
        val axisPaint = LectureTimetablePdfStyle.paint(7f, LectureTimetablePdfStyle.textGrey)

        days.forEachIndexed { index, day ->
            canvas.drawText(
                dayName(day).uppercase(Locale.getDefault()),
                gridLeft + dayWidth * (index + 0.5f),
                gridTop - 8f,
                dayPaint
            )
        }

        for (hour in hourRange.startHour..hourRange.endHour) {
            val y = gridTop + (hour - hourRange.startHour).toFloat() / (hourRange.endHour - hourRange.startHour) * gridHeight
            canvas.drawLine(gridLeft, y, gridRight, y, gridLinePaint)
            canvas.drawText(String.format(Locale.US, "%02d:00", hour), LectureTimetablePdfStyle.MARGIN, y + 2.5f, axisPaint)
        }
        canvas.drawRect(gridLeft, gridTop, gridRight, gridBottom, gridOutlinePaint)
        for (index in 1 until days.size) {
            val x = gridLeft + dayWidth * index
            canvas.drawLine(x, gridTop, x, gridBottom, gridLinePaint)
        }

        val slotById = slots.associateBy { it.id }
        val placements = lectureBlockPlacements(slots, hourRange)
        for (placement in placements) {
            val slot = slotById[placement.slotId] ?: continue
            val dayIndex = days.indexOf(slot.dayOfWeek)
            if (dayIndex < 0) continue

            val cellLeft = gridLeft + dayWidth * dayIndex
            val laneWidth = dayWidth / placement.laneCount
            val left = cellLeft + laneWidth * placement.laneIndex + 1.2f
            val right = cellLeft + laneWidth * (placement.laneIndex + 1) - 1.2f
            val top = gridTop + placement.topFraction * gridHeight + 0.8f
            val bottom = gridTop + (placement.topFraction + placement.heightFraction) * gridHeight - 0.8f
            if (bottom <= top) continue

            val colors = timetableCourseColor(slot.courseCode)
            canvas.drawRoundRect(
                RectF(left, top, right, bottom),
                3f,
                3f,
                Paint().apply { color = colors.fill }
            )
            canvas.drawRoundRect(
                RectF(left, top, right, bottom),
                3f,
                3f,
                Paint().apply {
                    color = colors.border
                    strokeWidth = 0.8f
                    style = Paint.Style.STROKE
                }
            )
            drawLectureBlockText(canvas, slot, left, top, right, bottom, placement.hasClash)
        }
    }

    private fun drawLectureBlockText(
        canvas: Canvas,
        slot: TimetableSlot,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        hasClash: Boolean
    ) {
        val colors = timetableCourseColor(slot.courseCode)
        val textRight = right - if (hasClash) 8f else 3f
        val x = left + 3f
        val maxWidth = (textRight - x).coerceAtLeast(1f)
        val blockHeight = bottom - top
        val smallPaint = LectureTimetablePdfStyle.paint(7f, colors.text)
        val boldPaint = LectureTimetablePdfStyle.paint(7f, colors.text, bold = true)
        val timeRange = "${slot.startTime}-${slot.endTime}"

        if (blockHeight >= 28f) {
            drawBlockLine(canvas, slot.courseCode, x, top + 8f, maxWidth, boldPaint)
            drawBlockLine(canvas, timeRange, x, top + 16f, maxWidth, smallPaint)
            drawBlockLine(canvas, slot.venue, x, top + 24f, maxWidth, smallPaint)
        } else if (blockHeight >= 18f) {
            drawBlockLine(canvas, slot.courseCode, x, top + 8f, maxWidth, boldPaint)
            drawBlockLine(canvas, "$timeRange · ${slot.venue}", x, top + 15f, maxWidth, smallPaint)
        } else {
            drawBlockLine(
                canvas,
                "${slot.courseCode} · $timeRange · ${slot.venue}",
                x,
                top + 7f,
                maxWidth,
                boldPaint
            )
        }

        if (hasClash) {
            val markerPaint = LectureTimetablePdfStyle.paint(7f, colors.text, bold = true).apply {
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("!", right - 4f, top + 7f, markerPaint)
        }
    }

    private fun drawBlockLine(
        canvas: Canvas,
        text: String,
        x: Float,
        baseline: Float,
        maxWidth: Float,
        paint: Paint
    ) {
        canvas.drawText(LectureTimetablePdfStyle.ellipsize(text, paint, maxWidth), x, baseline, paint)
    }

    private fun drawLegend(
        canvas: Canvas,
        courses: List<LegendCourse>,
        top: Float,
        pageWidth: Int
    ) {
        if (courses.isEmpty()) return
        val left = LectureTimetablePdfStyle.MARGIN
        val right = pageWidth - LectureTimetablePdfStyle.MARGIN
        val halfWidth = (right - left) / 2f
        val rows = ceil(courses.size / 2f).toInt()
        val headerPaint = LectureTimetablePdfStyle.paint(7f, LectureTimetablePdfStyle.brandBlue, bold = true)
        val rowPaint = LectureTimetablePdfStyle.paint(7f, LectureTimetablePdfStyle.darkText)
        val cellBorder = Paint().apply { color = LectureTimetablePdfStyle.borderLight; strokeWidth = 0.6f }
        val tablePaint = Paint().apply { color = LectureTimetablePdfStyle.lightGrey }
        val headerBottom = top + 16f

        canvas.drawText("COURSE LEGEND", left, top - 3f, headerPaint)
        for (half in 0..1) {
            val x = left + half * halfWidth
            canvas.drawRect(x, top, x + halfWidth - 5f, headerBottom, tablePaint)
            canvas.drawText("CODE", x + 14f, top + 11f, headerPaint)
            canvas.drawText("COURSE TITLE", x + 68f, top + 11f, headerPaint)
            canvas.drawText("UNITS", x + halfWidth - 43f, top + 11f, headerPaint)
        }

        for (row in 0 until rows) {
            for (half in 0..1) {
                val courseIndex = row + half * rows
                val course = courses.getOrNull(courseIndex) ?: continue
                val x = left + half * halfWidth
                val y = headerBottom + row * 11f
                val color = timetableCourseColor(course.code)
                canvas.drawRoundRect(
                    RectF(x + 4f, y + 2f, x + 10f, y + 8f),
                    1.5f,
                    1.5f,
                    Paint().apply { this.color = color.fill }
                )
                canvas.drawRoundRect(
                    RectF(x + 4f, y + 2f, x + 10f, y + 8f),
                    1.5f,
                    1.5f,
                    Paint().apply {
                        this.color = color.border
                        strokeWidth = 0.6f
                        style = Paint.Style.STROKE
                    }
                )
                canvas.drawLine(x, y + 11f, x + halfWidth - 5f, y + 11f, cellBorder)
                canvas.drawText(LectureTimetablePdfStyle.ellipsize(course.code, rowPaint, 48f), x + 14f, y + 8f, rowPaint)
                canvas.drawText(
                    LectureTimetablePdfStyle.ellipsize(course.title, rowPaint, halfWidth - 130f),
                    x + 68f,
                    y + 8f,
                    rowPaint
                )
                canvas.drawText(course.units, x + halfWidth - 43f, y + 8f, rowPaint)
            }
        }
    }

    private fun drawDailyList(
        document: PdfDocument,
        student: StudentProfile,
        semesterOrSession: String,
        slots: List<TimetableSlot>,
        courses: List<Course>,
        generatedAt: Date
    ) {
        val pages = paginateDailySlots(slots)
        val courseById = courses.associateBy { it.id }
        val pageWidth = LectureTimetablePdfStyle.PAGE_WIDTH
        val pageHeight = LectureTimetablePdfStyle.PAGE_HEIGHT

        pages.forEachIndexed { pageIndex, sections ->
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            )
            val canvas = page.canvas
            var y = LectureTimetablePdfStyle.drawHeader(
                canvas, student, semesterOrSession, "LECTURE TIMETABLE", pageWidth
            ) + 16f

            for (section in sections) {
                drawDayHeader(canvas, section.dayOfWeek, section.isContinuation, y, pageWidth)
                y += DAY_HEADER_HEIGHT
                drawDailyTableHeader(canvas, y, pageWidth)
                y += TABLE_HEADER_HEIGHT

                section.slots.forEach { slot ->
                    drawDailyRow(canvas, slot, slot.courseId?.let(courseById::get), y, pageWidth)
                    y += DAILY_ROW_HEIGHT
                }
                y += 8f
            }

            LectureTimetablePdfStyle.drawFooter(canvas, pageIndex + 1, pages.size, pageWidth, pageHeight, generatedAt)
            document.finishPage(page)
        }
    }

    private fun paginateDailySlots(slots: List<TimetableSlot>): List<List<DailyDaySection>> {
        val pages = mutableListOf<MutableList<DailyDaySection>>()
        var page = mutableListOf<DailyDaySection>()
        var cursor = LectureTimetablePdfStyle.PROFILE_BOTTOM + 31f
        val bodyTop = cursor

        fun newPage() {
            if (page.isNotEmpty()) pages.add(page)
            page = mutableListOf()
            cursor = bodyTop
        }

        for (day in 1..7) {
            val daySlots = slots.filter { it.dayOfWeek == day }
                .map(::timedLecture)
                .sortedWith(compareBy<TimedLecture> { it.startMinute }.thenBy { it.endMinute })
                .map { it.slot }
            if (daySlots.isEmpty()) continue
            var offset = 0
            var isContinuation = false
            while (offset < daySlots.size) {
                val headingHeight = DAY_HEADER_HEIGHT + TABLE_HEADER_HEIGHT
                if (cursor + headingHeight + DAILY_ROW_HEIGHT > DAILY_CONTENT_BOTTOM) newPage()
                val rowsAvailable = floor(
                    (DAILY_CONTENT_BOTTOM - cursor - headingHeight) / DAILY_ROW_HEIGHT
                ).toInt().coerceAtLeast(1)
                val count = minOf(rowsAvailable, daySlots.size - offset)
                val sectionSlots = daySlots.subList(offset, offset + count)
                page.add(DailyDaySection(day, isContinuation, sectionSlots))
                cursor += headingHeight + count * DAILY_ROW_HEIGHT + 8f
                offset += count
                if (offset < daySlots.size) {
                    newPage()
                    isContinuation = true
                } else {
                    isContinuation = false
                }
            }
        }
        if (page.isNotEmpty()) pages.add(page)
        return pages
    }

    private fun drawDayHeader(
        canvas: Canvas,
        dayOfWeek: Int,
        isContinuation: Boolean,
        top: Float,
        pageWidth: Int
    ) {
        val left = LectureTimetablePdfStyle.MARGIN
        val right = pageWidth - LectureTimetablePdfStyle.MARGIN
        val rect = RectF(left, top, right, top + DAY_HEADER_HEIGHT - 2f)
        canvas.drawRoundRect(rect, 5f, 5f, Paint().apply { color = LectureTimetablePdfStyle.lightGrey })
        canvas.drawRoundRect(rect, 5f, 5f, Paint().apply {
            color = LectureTimetablePdfStyle.borderLight
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        })
        val suffix = if (isContinuation) " (CONTINUED)" else ""
        canvas.drawText(
            dayName(dayOfWeek).uppercase(Locale.getDefault()) + suffix,
            left + 9f,
            top + 14f,
            LectureTimetablePdfStyle.paint(8.5f, LectureTimetablePdfStyle.brandBlue, bold = true)
        )
    }

    private fun drawDailyTableHeader(canvas: Canvas, top: Float, pageWidth: Int) {
        val left = LectureTimetablePdfStyle.MARGIN
        val right = pageWidth - LectureTimetablePdfStyle.MARGIN
        val columns = dailyColumnEdges(left, right)
        val headerPaint = Paint().apply { color = LectureTimetablePdfStyle.lightGrey }
        val borderPaint = Paint().apply { color = LectureTimetablePdfStyle.borderLight; strokeWidth = 0.7f }
        canvas.drawRect(left, top, right, top + TABLE_HEADER_HEIGHT, headerPaint)
        canvas.drawRect(left, top, right, top + TABLE_HEADER_HEIGHT, borderPaint.apply { style = Paint.Style.STROKE })
        val labels = listOf("TIME", "COURSE CODE", "COURSE TITLE", "VENUE")
        val textPaint = LectureTimetablePdfStyle.paint(7f, LectureTimetablePdfStyle.brandBlue, bold = true)
        labels.forEachIndexed { index, label ->
            canvas.drawText(label, columns[index] + 5f, top + 13f, textPaint)
            if (index > 0) canvas.drawLine(columns[index], top, columns[index], top + TABLE_HEADER_HEIGHT, borderPaint)
        }
    }

    private fun drawDailyRow(
        canvas: Canvas,
        slot: TimetableSlot,
        course: Course?,
        top: Float,
        pageWidth: Int
    ) {
        val left = LectureTimetablePdfStyle.MARGIN
        val right = pageWidth - LectureTimetablePdfStyle.MARGIN
        val columns = dailyColumnEdges(left, right)
        val borderPaint = Paint().apply { color = LectureTimetablePdfStyle.borderLight; strokeWidth = 0.65f }
        val textPaint = LectureTimetablePdfStyle.paint(8f, LectureTimetablePdfStyle.darkText)
        canvas.drawRect(left, top, right, top + DAILY_ROW_HEIGHT, Paint().apply { color = android.graphics.Color.WHITE })
        canvas.drawRect(left, top, right, top + DAILY_ROW_HEIGHT, borderPaint.apply { style = Paint.Style.STROKE })

        val values = listOf(
            "${slot.startTime}-${slot.endTime}",
            slot.courseCode,
            course?.title?.ifBlank { "-" } ?: "-",
            slot.venue.ifBlank { "-" }
        )
        values.forEachIndexed { index, value ->
            val x = columns[index] + 5f
            val maxWidth = columns[index + 1] - columns[index] - 10f
            canvas.drawText(
                LectureTimetablePdfStyle.ellipsize(value, textPaint, maxWidth),
                x,
                top + 16f,
                textPaint
            )
            if (index > 0) canvas.drawLine(columns[index], top, columns[index], top + DAILY_ROW_HEIGHT, borderPaint)
        }
    }

    private fun dailyColumnEdges(left: Float, right: Float): List<Float> {
        val total = right - left
        return listOf(left, left + total * 0.17f, left + total * 0.37f, left + total * 0.75f, right)
    }

    private fun buildLegendCourses(
        slots: List<TimetableSlot>,
        courseById: Map<Int, Course>
    ): List<LegendCourse> =
        slots.map { slot ->
            val course = slot.courseId?.let(courseById::get)
            LegendCourse(slot.courseCode, course?.title.orEmpty(), course?.units?.toString().orEmpty())
        }.distinctBy { it.code.trim().uppercase(Locale.ROOT) }
            .sortedBy { it.code.uppercase(Locale.ROOT) }

    private fun clashDescription(clashes: List<LectureClashGroup>): String {
        if (clashes.isEmpty()) return ""
        val descriptions = clashes.map { group ->
            val times = group.slots.minOf { timedLecture(it).startMinute }
            val hour = times / 60
            val minute = times % 60
            val time = String.format(Locale.US, "%02d:%02d", hour, minute)
            val codes = group.slots.joinToString("/") { it.courseCode }
            "${dayName(group.dayOfWeek).take(3).uppercase(Locale.getDefault())} $time $codes"
        }
        return descriptions.joinToString(" · ")
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return emptyList()
        val lines = mutableListOf<String>()
        var line = ""
        for (word in text.split(' ')) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) <= maxWidth) {
                line = candidate
            } else {
                if (line.isNotEmpty()) lines.add(line)
                line = LectureTimetablePdfStyle.ellipsize(word, paint, maxWidth)
            }
        }
        if (line.isNotEmpty()) lines.add(line)
        return lines
    }

    private fun dayName(dayOfWeek: Int): String = when (dayOfWeek) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> throw IllegalArgumentException("Lecture day must be between 1 and 7.")
    }
}
