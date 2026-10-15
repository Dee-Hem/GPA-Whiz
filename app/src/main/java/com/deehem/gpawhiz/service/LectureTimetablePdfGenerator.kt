package com.deehem.gpawhiz.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.deehem.gpawhiz.data.Course
import com.deehem.gpawhiz.data.StudentProfile
import com.deehem.gpawhiz.data.TimetableSlot
import java.io.OutputStream
import java.util.Date
import java.util.Locale
import kotlin.math.max

enum class LectureTimetablePdfLayout {
    WEEKLY_GRID,
    DAILY_LIST
}

object LectureTimetablePdfGenerator {
    private const val DAY_HEADER_HEIGHT = 22f
    private const val TABLE_HEADER_HEIGHT = 19f
    private const val DAILY_CONTENT_BOTTOM = 793f

    private data class LegendCourse(
        val code: String,
        val title: String,
        val units: String
    )

    private data class DailyDaySection(
        val dayOfWeek: Int,
        val isContinuation: Boolean,
        val rows: List<DailySlotRow>
    )

    private data class DailySlotRow(
        val slot: TimetableSlot,
        val course: Course?,
        val wrapped: PdfWrappedRow
    )

    private data class ClashDetailRow(
        val cells: List<List<String>>,
        val height: Float
    )

    private data class LegendRow(
        val course: LegendCourse,
        val codeLines: List<String>,
        val titleLines: List<String>,
        val unitsLines: List<String>,
        val height: Float
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
        val summary = lectureSummaryText(slots.size, slots.map { it.dayOfWeek }.distinct().size)
        val courseById = courses.associateBy { it.id }
        val legendCourses = buildLegendCourses(slots, courseById)
        val courseColors = timetableCourseColors(legendCourses.map { it.code })
        val legendRows = createLegendRows(legendCourses, pageWidth)
        val clashes = lectureClashGroups(slots)
        val notePaint = LectureTimetablePdfStyle.paint(7.5f, LectureTimetablePdfStyle.textGrey)
        val clashLines = clashes.flatMap { clash ->
            wrapPdfText(clashDescription(listOf(clash)), pageWidth - 2 * LectureTimetablePdfStyle.MARGIN - 48f, 7.5f) { text, size ->
                LectureTimetablePdfStyle.measureText(notePaint, text, size)
            }
        }
        val hourRange = requireNotNull(lectureHourRange(slots))
        val days = lectureVisibleDays(slots)
        val dayWidth = weeklyDayColumnWidth(
            pageWidth.toFloat(),
            days.size,
            LectureTimetablePdfStyle.MARGIN,
            39f
        )
        val placements = lectureBlockPlacements(slots, hourRange)
        val slotById = slots.associateBy { it.id }
        val blockMeasurePaint = LectureTimetablePdfStyle.paint(8f, bold = true)
        val blockMeasure: (String, Float) -> Float = { text, size ->
            LectureTimetablePdfStyle.measureText(blockMeasurePaint, text, size)
        }
        val clashDetailSlots = placements.filter { placement ->
            slotById.containsKey(placement.slotId) && placement.hasClash && weeklyBlockNeedsClashDetails(
                weeklyBlockTextWidth(dayWidth, placement.laneCount, hasClash = true)
            )
        }.mapNotNull { slotById[it.slotId] }.distinctBy { it.id }
        val clashDetailRows = createClashDetailRows(clashDetailSlots, pageWidth)
        val clashNotesHeight = if (clashLines.isEmpty()) 0f else 11f + clashLines.size * 9f
        val clashDetailsHeight = if (clashDetailRows.isEmpty()) 0f else {
            5f + 28f + clashDetailRows.sumOf { it.height.toDouble() }.toFloat()
        }
        val clashHeight = clashNotesHeight + clashDetailsHeight
        val requiredGridHeight = placements.maxOfOrNull { placement ->
            val slot = slotById.getValue(placement.slotId)
            val textWidth = weeklyBlockTextWidth(dayWidth, placement.laneCount, placement.hasClash)
            val minimumLayout = weeklyBlockTextLayout(slot, textWidth, 0f, blockMeasure)
            (minimumLayout.requiredHeight + 3.6f) / placement.heightFraction
        } ?: 0f
        val headerBottom = LectureTimetablePdfStyle.measureHeaderBottom(student, pageWidth, summary)
        val gridTop = headerBottom + 48f
        val contentBottom = pageHeight - 42f
        val legendHeight = if (legendRows.isEmpty()) 0f else 36f + legendRows.sumOf { it.height.toDouble() }.toFloat()
        val legendGap = if (legendHeight == 0f) 0f else 17f
        val clashGap = if (clashHeight == 0f) 0f else 9f
        val minimumGridHeight = max(
            max(55f, hourRange.durationMinutes / 60f * 12f),
            requiredGridHeight
        )
        val drawLegendOnGridPage =
            gridTop + minimumGridHeight + clashHeight + clashGap + legendGap + legendHeight <= contentBottom
        val gridBottom = contentBottom - clashHeight - clashGap -
            (if (drawLegendOnGridPage) legendGap + legendHeight else 0f)
        require(gridBottom - gridTop >= minimumGridHeight) {
            "The weekly grid cannot fit its time range at the minimum readable size."
        }
        val gridPage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
        val canvas = gridPage.canvas
        val drawnHeaderBottom = LectureTimetablePdfStyle.drawHeader(
            canvas, student, semesterOrSession, "LECTURE TIMETABLE", pageWidth, summary
        )
        canvas.drawText(
            "WEEKLY GRID",
            LectureTimetablePdfStyle.MARGIN,
            drawnHeaderBottom + 18f,
            LectureTimetablePdfStyle.paint(9f, LectureTimetablePdfStyle.brandBlue, bold = true)
        )
        drawWeeklyGridArea(canvas, slots, gridTop, gridBottom, pageWidth, courseColors)

        var noteTop = gridBottom + clashGap
        if (clashLines.isNotEmpty()) {
            val headingPaint = LectureTimetablePdfStyle.paint(7.5f, LectureTimetablePdfStyle.darkText, bold = true)
            canvas.drawText("CLASHES", LectureTimetablePdfStyle.MARGIN, noteTop + 8f, headingPaint)
            clashLines.forEachIndexed { index, line ->
                canvas.drawText(line, LectureTimetablePdfStyle.MARGIN + 48f, noteTop + 8f + index * 9f, notePaint)
            }
            noteTop += clashNotesHeight
        }
        if (clashDetailRows.isNotEmpty()) {
            noteTop += 5f
            drawClashDetails(canvas, clashDetailRows, noteTop, pageWidth)
            noteTop += 28f + clashDetailRows.sumOf { it.height.toDouble() }.toFloat()
        }
        val nextLegendTop = LectureTimetablePdfStyle.measureHeaderBottom(student, pageWidth, summary) + 18f
        val legendPages = paginateLegendRows(
            legendRows,
            firstTop = if (drawLegendOnGridPage) noteTop + legendGap else contentBottom,
            nextTop = nextLegendTop,
            pageBottom = contentBottom
        )
        val samePageRows = if (drawLegendOnGridPage) legendPages.firstOrNull().orEmpty() else emptyList()
        if (samePageRows.isNotEmpty()) drawLegend(canvas, samePageRows, noteTop + legendGap, pageWidth, courseColors)
        val extraLegendPages = if (drawLegendOnGridPage) legendPages.drop(1) else legendPages
        val pageCount = 1 + extraLegendPages.size
        LectureTimetablePdfStyle.drawFooter(canvas, 1, pageCount, pageWidth, pageHeight, generatedAt)
        document.finishPage(gridPage)

        extraLegendPages.forEachIndexed { pageIndex, rows ->
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 2).create()
            )
            val legendCanvas = page.canvas
            val pageHeaderBottom = LectureTimetablePdfStyle.drawHeader(
                legendCanvas, student, semesterOrSession, "LECTURE TIMETABLE", pageWidth, summary
            )
            drawLegend(legendCanvas, rows, pageHeaderBottom + 18f, pageWidth, courseColors)
            LectureTimetablePdfStyle.drawFooter(
                legendCanvas, pageIndex + 2, pageCount, pageWidth, pageHeight, generatedAt
            )
            document.finishPage(page)
        }
    }

    private fun drawWeeklyGridArea(
        canvas: Canvas,
        slots: List<TimetableSlot>,
        gridTop: Float,
        gridBottom: Float,
        pageWidth: Int,
        courseColors: Map<String, TimetableCourseColor>
    ) {
        val hourRange = lectureHourRange(slots)
            ?: throw IllegalArgumentException("There are no lectures to display.")
        val days = lectureVisibleDays(slots)
        val axisWidth = 39f
        val gridLeft = LectureTimetablePdfStyle.MARGIN + axisWidth
        val gridRight = pageWidth - LectureTimetablePdfStyle.MARGIN
        val dayWidth = weeklyDayColumnWidth(pageWidth.toFloat(), days.size, LectureTimetablePdfStyle.MARGIN, axisWidth)
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

        val hourPositions = hourAxisLabelPositions(hourRange, gridTop, gridBottom)
        for ((index, hour) in (hourRange.startHour..hourRange.endHour).withIndex()) {
            val y = hourPositions[index]
            canvas.drawLine(gridLeft, y, gridRight, y, gridLinePaint)
            canvas.drawText(formatHourLabel(hour), LectureTimetablePdfStyle.MARGIN, y + 2.5f, axisPaint)
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

            val colors = courseColors.getValue(slot.courseCode.trim().uppercase(Locale.ROOT))
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
            drawLectureBlockText(canvas, slot, left, top, right, bottom, placement.hasClash, colors)
        }
    }

    private fun drawLectureBlockText(
        canvas: Canvas,
        slot: TimetableSlot,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        hasClash: Boolean,
        colors: TimetableCourseColor
    ) {
        val textRight = right - if (hasClash) 8f else 3f
        val x = left + 3f
        val maxWidth = (textRight - x).coerceAtLeast(1f)
        val measurePaint = LectureTimetablePdfStyle.paint(8f, colors.text, bold = true)
        val layout = weeklyBlockTextLayout(
            slot,
            maxWidth,
            bottom - top - 2f
        ) { text, size -> LectureTimetablePdfStyle.measureText(measurePaint, text, size) }
        require(layout.fits) { "Weekly lecture block text does not fit its allocated block." }
        for (line in layout.lines) {
            val bold = line.kind == WeeklyBlockTextKind.COURSE_CODE
            val paint = LectureTimetablePdfStyle.paint(line.textSize, colors.text, bold)
            canvas.drawText(line.text, x + line.bounds.left, top + line.bounds.bottom, paint)
        }

        if (hasClash) {
            drawClashMarker(canvas, right, top, colors)
        }
    }

    private fun weeklyBlockTextWidth(dayWidth: Float, laneCount: Int, hasClash: Boolean): Float {
        val laneWidth = dayWidth / laneCount
        return (laneWidth - 2.4f - 3f - if (hasClash) 8f else 3f).coerceAtLeast(1f)
    }

    private fun createClashDetailRows(slots: List<TimetableSlot>, pageWidth: Int): List<ClashDetailRow> {
        val left = LectureTimetablePdfStyle.MARGIN
        val right = pageWidth - LectureTimetablePdfStyle.MARGIN
        val edges = listOf(left, left + (right - left) * 0.22f, left + (right - left) * 0.38f, right)
        val paint = LectureTimetablePdfStyle.paint(6.5f)
        val measure: (String, Float) -> Float = { text, size ->
            LectureTimetablePdfStyle.measureText(paint, text, size)
        }
        return slots.map { slot ->
            val values = listOf(
                slot.courseCode.ifBlank { "-" },
                formatTimeRange(slot.startTime, slot.endTime),
                slot.venue.ifBlank { "-" }
            )
            val widths = edges.zipWithNext { first, second -> second - first - 8f }
            val cells = values.mapIndexed { index, value ->
                wrapPdfText(value, widths[index], 6.5f, measure).ifEmpty { listOf("-") }
            }
            ClashDetailRow(cells, maxOf(cells.maxOf { it.size }, 1) * 8f + 4f)
        }
    }

    private fun drawClashDetails(
        canvas: Canvas,
        rows: List<ClashDetailRow>,
        top: Float,
        pageWidth: Int
    ) {
        val left = LectureTimetablePdfStyle.MARGIN
        val right = pageWidth - LectureTimetablePdfStyle.MARGIN
        val width = right - left
        val edges = listOf(left, left + width * 0.22f, left + width * 0.38f, right)
        val headingPaint = LectureTimetablePdfStyle.paint(7f, LectureTimetablePdfStyle.darkText, bold = true)
        val headerPaint = LectureTimetablePdfStyle.paint(6.5f, LectureTimetablePdfStyle.brandBlue, bold = true)
        val borderPaint = Paint().apply { color = LectureTimetablePdfStyle.borderLight; strokeWidth = 0.55f }
        canvas.drawText("Clash details", left, top + 8f, headingPaint)
        val headerTop = top + 13f
        val headerBottom = headerTop + 15f
        canvas.drawRect(left, headerTop, right, headerBottom, Paint().apply { color = LectureTimetablePdfStyle.lightGrey })
        canvas.drawRect(left, headerTop, right, headerBottom, borderPaint.apply { style = Paint.Style.STROKE })
        listOf("CODE", "TIME", "VENUE").forEachIndexed { index, label ->
            canvas.drawText(label, edges[index] + 4f, headerTop + 10f, headerPaint)
            if (index > 0) canvas.drawLine(edges[index], headerTop, edges[index], headerBottom, borderPaint)
        }
        var rowTop = headerBottom
        for (row in rows) {
            val rowBottom = rowTop + row.height
            canvas.drawRect(left, rowTop, right, rowBottom, Paint().apply { color = Color.WHITE })
            canvas.drawRect(left, rowTop, right, rowBottom, borderPaint)
            row.cells.forEachIndexed { index, lines ->
                val rowPaint = LectureTimetablePdfStyle.paint(6.5f, LectureTimetablePdfStyle.darkText)
                lines.forEachIndexed { lineIndex, line ->
                    canvas.drawText(line, edges[index] + 4f, rowTop + 7f + lineIndex * 8f, rowPaint)
                }
                if (index > 0) canvas.drawLine(edges[index], rowTop, edges[index], rowBottom, borderPaint)
            }
            rowTop = rowBottom
        }
    }

    private fun drawClashMarker(canvas: Canvas, right: Float, top: Float, colors: TimetableCourseColor) {
        val markerPaint = LectureTimetablePdfStyle.paint(7f, colors.text, bold = true).apply {
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("!", right - 4f, top + 7f, markerPaint)
    }

    private fun drawLegend(
        canvas: Canvas,
        rows: List<LegendRow>,
        top: Float,
        pageWidth: Int,
        courseColors: Map<String, TimetableCourseColor>
    ) {
        if (rows.isEmpty()) return
        val left = LectureTimetablePdfStyle.MARGIN
        val right = pageWidth - LectureTimetablePdfStyle.MARGIN
        val width = right - left
        val codeRight = left + width * 0.2f
        val titleRight = left + width * 0.84f
        val headerPaint = LectureTimetablePdfStyle.paint(8f, LectureTimetablePdfStyle.brandBlue, bold = true)
        val rowPaint = LectureTimetablePdfStyle.paint(8f, LectureTimetablePdfStyle.darkText)
        val cellBorder = Paint().apply { color = LectureTimetablePdfStyle.borderLight; strokeWidth = 0.6f }
        val tablePaint = Paint().apply { color = LectureTimetablePdfStyle.lightGrey }
        val headerBottom = top + 36f

        canvas.drawText("COURSE LEGEND", left, top + 10f, headerPaint)
        canvas.drawRect(left, top + 14f, right, headerBottom, tablePaint)
        canvas.drawText("CODE", left + 17f, top + 29f, headerPaint)
        canvas.drawText("COURSE TITLE", codeRight + 5f, top + 29f, headerPaint)
        canvas.drawText("UNITS", titleRight + 5f, top + 29f, headerPaint)
        var rowTop = headerBottom
        for (row in rows) {
            val rowBottom = rowTop + row.height
            canvas.drawRect(left, rowTop, right, rowBottom, Paint().apply { color = Color.WHITE })
            canvas.drawRect(left, rowTop, right, rowBottom, Paint().apply {
                color = LectureTimetablePdfStyle.borderLight
                strokeWidth = 0.6f
                style = Paint.Style.STROKE
            })
            val color = courseColors.getValue(row.course.code.trim().uppercase(Locale.ROOT))
            canvas.drawRoundRect(RectF(left + 4f, rowTop + 4f, left + 11f, rowTop + 11f), 1.5f, 1.5f, Paint().apply { this.color = color.fill })
            canvas.drawRoundRect(RectF(left + 4f, rowTop + 4f, left + 11f, rowTop + 11f), 1.5f, 1.5f, Paint().apply {
                this.color = color.border
                strokeWidth = 0.7f
                style = Paint.Style.STROKE
            })
            drawLines(canvas, row.codeLines, left + 17f, rowTop + 5f, rowPaint, 10f)
            drawLines(canvas, row.titleLines, codeRight + 5f, rowTop + 5f, rowPaint, 10f)
            drawLines(canvas, row.unitsLines, titleRight + 5f, rowTop + 5f, rowPaint, 10f)
            canvas.drawLine(codeRight, rowTop, codeRight, rowBottom, cellBorder)
            canvas.drawLine(titleRight, rowTop, titleRight, rowBottom, cellBorder)
            rowTop = rowBottom
        }
    }

    private fun createLegendRows(courses: List<LegendCourse>, pageWidth: Int): List<LegendRow> {
        val width = pageWidth - 2 * LectureTimetablePdfStyle.MARGIN
        val codeWidth = width * 0.2f - 22f
        val titleWidth = width * 0.64f - 10f
        val unitsWidth = width * 0.16f - 10f
        val paint = LectureTimetablePdfStyle.paint(8f)
        val measure: (String, Float) -> Float = { text, size ->
            LectureTimetablePdfStyle.measureText(paint, text, size)
        }
        return courses.map { course ->
            val codeLines = wrapPdfText(course.code, codeWidth, 8f, measure)
            val titleLines = wrapPdfText(course.title.ifBlank { "-" }, titleWidth, 8f, measure)
            val unitsLines = wrapPdfText(course.units.ifBlank { "-" }, unitsWidth, 8f, measure)
            LegendRow(
                course,
                codeLines,
                titleLines,
                unitsLines,
                maxOf(codeLines.size, titleLines.size, unitsLines.size) * 10f + 8f
            )
        }
    }

    private fun paginateLegendRows(
        rows: List<LegendRow>,
        firstTop: Float,
        nextTop: Float,
        pageBottom: Float
    ): List<List<LegendRow>> {
        if (rows.isEmpty()) return emptyList()
        val pages = mutableListOf<List<LegendRow>>()
        var page = mutableListOf<LegendRow>()
        var cursor = if (firstTop + 36f <= pageBottom) firstTop + 36f else pageBottom
        for (row in rows) {
            if (cursor + row.height > pageBottom) {
                if (page.isNotEmpty()) pages.add(page)
                page = mutableListOf()
                cursor = nextTop + 36f
            }
            require(cursor + row.height <= pageBottom) { "A course legend row does not fit on a page." }
            page.add(row)
            cursor += row.height
        }
        if (page.isNotEmpty()) pages.add(page)
        return pages
    }

    private fun drawLines(
        canvas: Canvas,
        lines: List<String>,
        x: Float,
        top: Float,
        paint: Paint,
        lineHeight: Float
    ) {
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, x, top + paint.textSize + index * lineHeight, paint)
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
        val courseById = courses.associateBy { it.id }
        val pageWidth = LectureTimetablePdfStyle.PAGE_WIDTH
        val pageHeight = LectureTimetablePdfStyle.PAGE_HEIGHT
        val summary = lectureSummaryText(slots.size, slots.map { it.dayOfWeek }.distinct().size)
        val bodyTop = LectureTimetablePdfStyle.measureHeaderBottom(student, pageWidth, summary) + 16f
        val timePaint = LectureTimetablePdfStyle.paint(8f, LectureTimetablePdfStyle.darkText)
        val minimumTimeColumnWidth = timeColumnMinimumWidth(
            slots.map { formatTimeRange(it.startTime, it.endTime) },
            horizontalPadding = 10f,
            textSize = 8f
        ) { text, size -> LectureTimetablePdfStyle.measureText(timePaint, text, size) }
        val columns = dailyTimetableColumnEdges(
            LectureTimetablePdfStyle.MARGIN,
            pageWidth - LectureTimetablePdfStyle.MARGIN,
            minimumTimeColumnWidth
        )
        val pages = paginateDailySlots(slots, courseById, bodyTop, columns)

        pages.forEachIndexed { pageIndex, sections ->
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            )
            val canvas = page.canvas
            var y = LectureTimetablePdfStyle.drawHeader(
                canvas,
                student,
                semesterOrSession,
                "LECTURE TIMETABLE",
                pageWidth,
                summary
            ) + 16f

            for (section in sections) {
                drawDayHeader(canvas, section.dayOfWeek, section.isContinuation, y, pageWidth)
                y += DAY_HEADER_HEIGHT
                drawDailyTableHeader(canvas, y, columns)
                y += TABLE_HEADER_HEIGHT

                section.rows.forEach { row ->
                    drawDailyRow(canvas, row, y, columns)
                    y += row.wrapped.height
                }
                y += 8f
            }

            LectureTimetablePdfStyle.drawFooter(canvas, pageIndex + 1, pages.size, pageWidth, pageHeight, generatedAt)
            document.finishPage(page)
        }
    }

    private fun paginateDailySlots(
        slots: List<TimetableSlot>,
        courseById: Map<Int, Course>,
        bodyTop: Float,
        columns: List<Float>
    ): List<List<DailyDaySection>> {
        val pages = mutableListOf<MutableList<DailyDaySection>>()
        var page = mutableListOf<DailyDaySection>()
        var cursor = bodyTop

        fun newPage() {
            if (page.isNotEmpty()) pages.add(page)
            page = mutableListOf()
            cursor = bodyTop
        }

        for (day in 1..7) {
            val dayRows = slots.filter { it.dayOfWeek == day }
                .map(::timedLecture)
                .sortedWith(compareBy<TimedLecture> { it.startMinute }.thenBy { it.endMinute })
                .map { it.slot }
                .map { slot -> createDailySlotRow(slot, slot.courseId?.let(courseById::get), columns) }
            if (dayRows.isEmpty()) continue
            var offset = 0
            var isContinuation = false
            while (offset < dayRows.size) {
                val headingHeight = DAY_HEADER_HEIGHT + TABLE_HEADER_HEIGHT
                if (cursor + headingHeight + dayRows[offset].wrapped.height > DAILY_CONTENT_BOTTOM) newPage()
                val rows = mutableListOf<DailySlotRow>()
                var availableBottom = DAILY_CONTENT_BOTTOM - cursor - headingHeight
                while (offset < dayRows.size && dayRows[offset].wrapped.height <= availableBottom) {
                    rows.add(dayRows[offset])
                    availableBottom -= dayRows[offset].wrapped.height
                    offset++
                }
                require(rows.isNotEmpty()) { "A lecture row does not fit on a portrait page." }
                page.add(DailyDaySection(day, isContinuation, rows))
                cursor += headingHeight + rows.sumOf { it.wrapped.height.toDouble() }.toFloat() + 8f
                if (offset < dayRows.size) {
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

    private fun drawDailyTableHeader(canvas: Canvas, top: Float, columns: List<Float>) {
        val left = LectureTimetablePdfStyle.MARGIN
        val right = columns.last()
        val headerPaint = Paint().apply { color = LectureTimetablePdfStyle.lightGrey }
        val borderPaint = Paint().apply { color = LectureTimetablePdfStyle.borderLight; strokeWidth = 0.7f }
        canvas.drawRect(left, top, right, top + TABLE_HEADER_HEIGHT, headerPaint)
        canvas.drawRect(left, top, right, top + TABLE_HEADER_HEIGHT, borderPaint.apply { style = Paint.Style.STROKE })
        val labels = listOf("TIME", "COURSE CODE", "COURSE TITLE", "Units", "VENUE")
        val textPaint = LectureTimetablePdfStyle.paint(7f, LectureTimetablePdfStyle.brandBlue, bold = true)
        labels.forEachIndexed { index, label ->
            val isUnits = index == 3
            val headerPaint = if (isUnits) {
                LectureTimetablePdfStyle.paint(7f, LectureTimetablePdfStyle.brandBlue, bold = true)
                    .apply { textAlign = Paint.Align.CENTER }
            } else textPaint
            val x = if (isUnits) (columns[index] + columns[index + 1]) / 2f else columns[index] + 5f
            canvas.drawText(label, x, top + 13f, headerPaint)
            if (index > 0) canvas.drawLine(columns[index], top, columns[index], top + TABLE_HEADER_HEIGHT, borderPaint)
        }
    }

    private fun drawDailyRow(
        canvas: Canvas,
        row: DailySlotRow,
        top: Float,
        columns: List<Float>
    ) {
        val left = LectureTimetablePdfStyle.MARGIN
        val right = columns.last()
        val borderPaint = Paint().apply { color = LectureTimetablePdfStyle.borderLight; strokeWidth = 0.65f }
        canvas.drawRect(left, top, right, top + row.wrapped.height, Paint().apply { color = android.graphics.Color.WHITE })
        canvas.drawRect(left, top, right, top + row.wrapped.height, borderPaint.apply { style = Paint.Style.STROKE })
        row.wrapped.cells.forEachIndexed { index, lines ->
            val isUnits = index == 3
            val x = if (isUnits) (columns[index] + columns[index + 1]) / 2f else columns[index] + 5f
            val textPaint = LectureTimetablePdfStyle.paint(
                8f,
                LectureTimetablePdfStyle.darkText,
                bold = index == 1
            ).apply {
                if (isUnits) textAlign = Paint.Align.CENTER
            }
            lines.forEachIndexed { lineIndex, line ->
                canvas.drawText(line, x, top + 9f + textPaint.textSize + lineIndex * 10f, textPaint)
            }
            if (index > 0) canvas.drawLine(columns[index], top, columns[index], top + row.wrapped.height, borderPaint)
        }
    }

    private fun createDailySlotRow(slot: TimetableSlot, course: Course?, columns: List<Float>): DailySlotRow {
        val values = listOf(
            formatTimeRange(slot.startTime, slot.endTime),
            slot.courseCode,
            course?.title?.ifBlank { "-" } ?: "-",
            formatCourseUnits(course?.units),
            slot.venue.ifBlank { "-" }
        )
        val widths = columns.zipWithNext { left, right -> right - left - 10f }
        val paint = LectureTimetablePdfStyle.paint(8f, LectureTimetablePdfStyle.darkText)
        val measure: (String, Float) -> Float = { text, size ->
            LectureTimetablePdfStyle.measureText(paint, text, size)
        }
        return DailySlotRow(
            slot,
            course,
            wrapPdfTableRow(values, widths, 8f, 10f, 10f, measure)
        )
    }

    private fun buildLegendCourses(
        slots: List<TimetableSlot>,
        courseById: Map<Int, Course>
    ): List<LegendCourse> =
        slots.map { slot ->
            val course = slot.courseId?.let(courseById::get)
            LegendCourse(slot.courseCode, course?.title.orEmpty(), formatCourseUnits(course?.units))
        }.distinctBy { it.code.trim().uppercase(Locale.ROOT) }
            .sortedBy { it.code.uppercase(Locale.ROOT) }

    private fun clashDescription(clashes: List<LectureClashGroup>): String {
        if (clashes.isEmpty()) return ""
        val descriptions = clashes.map { group ->
            val times = group.slots.minOf { timedLecture(it).startMinute }
            val hour = times / 60
            val minute = times % 60
            val time = formatTime(hour, minute)
            val codes = group.slots.joinToString("/") { it.courseCode }
            "${dayName(group.dayOfWeek).take(3).uppercase(Locale.getDefault())} $time $codes"
        }
        return descriptions.joinToString(" · ")
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
