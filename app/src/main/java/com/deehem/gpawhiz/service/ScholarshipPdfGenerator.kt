package com.deehem.gpawhiz.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.deehem.gpawhiz.data.ExchangeRate
import com.deehem.gpawhiz.data.RequirementCategory
import com.deehem.gpawhiz.data.RequirementStatus
import com.deehem.gpawhiz.data.Scholarship
import com.deehem.gpawhiz.data.ScholarshipRequirement
import com.deehem.gpawhiz.data.ScholarshipStatus
import com.deehem.gpawhiz.data.ScholarshipTimelineEvent
import com.deehem.gpawhiz.data.StudentProfile
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScholarshipPdfGenerator {

    // A4 Dimensions in Points (72 dpi) - Landscape (842 x 595 pt)
    private const val A4_LANDSCAPE_WIDTH = 842
    private const val A4_LANDSCAPE_HEIGHT = 595

    private const val MARGIN_X = 36f
    private const val MARGIN_TOP = 28f
    private const val MARGIN_BOTTOM = 26f

    // Theme Colors - Professional Academic Brand
    private val COLOR_NAVY_PRIMARY = Color.parseColor("#0F2942") // Deep Midnight Navy
    private val COLOR_NAVY_HEADER = Color.parseColor("#17375E")  // Elegant Slate Navy
    private val COLOR_NAVY_ACCENT = Color.parseColor("#2563EB")  // Accent Royal Blue
    private val COLOR_SUCCESS_GREEN = Color.parseColor("#166534") // Emerald Green
    private val COLOR_SUCCESS_BG = Color.parseColor("#DCFCE7")    // Light Green Bg
    private val COLOR_WARNING_ORANGE = Color.parseColor("#C2410C") // Amber / Orange
    private val COLOR_WARNING_BG = Color.parseColor("#FEF3C7")   // Amber Bg
    private val COLOR_DANGER_RED = Color.parseColor("#B91C1C")   // Crimson Red
    private val COLOR_DANGER_BG = Color.parseColor("#FEE2E2")    // Light Red Bg
    private val COLOR_STATUS_BG = Color.parseColor("#EEF2F6")    // Neutral Slate Pill
    private val COLOR_CARD_BG = Color.parseColor("#F8FAFC")      // Slate 50
    private val COLOR_CARD_BORDER = Color.parseColor("#E2E8F0")  // Slate 200
    private val COLOR_ROW_ALT = Color.parseColor("#F8FAFC")      // Alternating Row
    private val COLOR_GRID_LINE = Color.parseColor("#CBD5E1")    // Slate 300
    private val COLOR_TEXT_DARK = Color.parseColor("#0F172A")    // Slate 900
    private val COLOR_TEXT_MUTED = Color.parseColor("#64748B")   // Slate 500
    private val COLOR_WHITE = Color.WHITE

    private val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private data class OpenDeadlineRow(
        val days: ScholarshipPdfDaysText,
        val nameLines: List<String>,
        val detailLines: List<String>,
        val dateLine: String,
        val height: Float
    )
    private data class PreparedKpi(
        val title: String,
        val valuePaint: Paint,
        val valueLines: List<String>,
        val subtitlePaint: Paint,
        val subtitleLines: List<String>,
        val height: Float
    )

    /**
     * Generate Comprehensive All Scholarships Multi-Section Report in A4 Landscape
     */
    fun generateScholarshipReportPdf(
        student: StudentProfile,
        calculatedCgpa: Double,
        scholarships: List<Scholarship>,
        allRequirements: List<ScholarshipRequirement>,
        outputStream: OutputStream,
        reportTitle: String = "ACTIVE SCHOLARSHIP APPLICATIONS REPORT",
        defaultCurrency: String = "NGN",
        exchangeRates: List<ExchangeRate> = emptyList()
    ) {
        val pdfDocument = PdfDocument()
        val pageWidth = A4_LANDSCAPE_WIDTH
        val pageHeight = A4_LANDSCAPE_HEIGHT
        val contentWidth = pageWidth - (2 * MARGIN_X) // 770 pt
        val colWidths = floatArrayOf(175f, 135f, 70f, 90f, 95f, 95f, 110f)
        val maxPageBottom = pageHeight - MARGIN_BOTTOM - 20f
        val openDeadlines = scholarshipPdfOpenDeadlines(scholarships)
        val continuationDeadlinePaint = Paint().apply {
            textSize = 7.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val continuationDeadlineDetailPaint = Paint().apply {
            textSize = 7.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val deadlineRows = openDeadlines.drop(4).map { scholarship ->
            val days = scholarshipPdfDaysText(scholarship.status, scholarship.deadlineDate)
            val nameLines = wrapText(scholarship.name, continuationDeadlinePaint, contentWidth - 24f)
            val details = "${scholarship.organization.ifBlank { "Not Specified" }} • ${scholarship.status}"
            val detailLines = wrapText(details, continuationDeadlineDetailPaint, contentWidth - 24f)
            val dateLine = "${scholarship.deadlineDate?.let { sdfDate.format(Date(it)) } ?: "-"} • ${days.text}"
            OpenDeadlineRow(
                days,
                nameLines,
                detailLines,
                dateLine,
                maxOf(38f, 12f + nameLines.size * 8f + detailLines.size * 8f + 14f)
            )
        }
        val deadlinePageRows = if (deadlineRows.isEmpty()) emptyList() else {
            paginateScholarshipPdfRows(
                deadlineRows.map { it.height + 4f },
                MARGIN_TOP + 49f + 24f,
                MARGIN_TOP + 49f + 24f,
                maxPageBottom
            )
        }
        val deadlinePages = deadlinePageRows.size
        val measurePaints = colWidths.indices.map { index ->
            Paint().apply {
                textSize = 7.2f
                typeface = if (index == 0 || index == 4 || index == 6) {
                    Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                } else {
                    Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                isAntiAlias = true
            }
        }
        val deadlineDisplays = scholarships.map {
            scholarshipPdfDaysText(it.status, it.deadlineDate)
        }
        val reportRows = scholarships.mapIndexed { scholarshipIndex, scholarship ->
            val requirements = allRequirements.filter { it.scholarshipId == scholarship.id }
            val progress = ScholarshipCalculationHelper.getRequirementsProgress(requirements)
            val deadline = scholarship.deadlineDate?.takeIf { it > 0L }
            listOf(
                scholarship.name,
                scholarship.organization.ifBlank { "Not Specified" },
                deadline?.let { sdfDate.format(Date(it)) } ?: "-",
                deadlineDisplays[scholarshipIndex].text,
                scholarship.status,
                if (progress.total > 0) "${progress.completed}/${progress.total} (${progress.percentage.toInt()}%)" else "0 items",
                scholarshipAwardText(scholarship, defaultCurrency, exchangeRates)
            )
        }
        val reportRowHeights = reportRows.map { values ->
            val lines = values.mapIndexed { index, value ->
                wrapText(value, measurePaints[index], colWidths[index] - 10f).size
            }.maxOrNull() ?: 1
            maxOf(20f, 7f + lines * 9.5f)
        }
        val tablePages = paginateScholarshipPdfRows(
            reportRowHeights,
            MARGIN_TOP + 35f + 20f,
            MARGIN_TOP + 18f + 20f,
            maxPageBottom
        ).size
        val totalPages = tablePages + deadlinePages + 1

        var pageNumber = 1

        // ==========================================
        // PAGE 1: COVER & EXECUTIVE OVERVIEW
        // ==========================================
        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas

        // Setup Base Paints
        val paintMainTitle = Paint().apply {
            color = COLOR_NAVY_PRIMARY
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSubtitle = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintSectionHeading = Paint().apply {
            color = COLOR_NAVY_PRIMARY
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintLabel = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 7.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintValueBold = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintCardBg = Paint().apply {
            color = COLOR_CARD_BG
            style = Paint.Style.FILL
        }

        val paintCardBorder = Paint().apply {
            color = COLOR_CARD_BORDER
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        val paintGridLine = Paint().apply {
            color = COLOR_GRID_LINE
            strokeWidth = 0.6f
            style = Paint.Style.STROKE
        }

        var yPos = MARGIN_TOP

        yPos = drawScholarshipHeader(canvas, pageWidth, reportTitle, paintMainTitle, paintSubtitle, paintGridLine)
        yPos = drawStudentCard(
            canvas,
            student,
            calculatedCgpa,
            MARGIN_X,
            yPos,
            contentWidth,
            paintLabel,
            paintValueBold,
            paintCardBg,
            paintCardBorder
        ) + 12f

        // --- SECTION 2: APPLICATION OVERVIEW (KPI CARDS) ---
        canvas.drawText("APPLICATION OVERVIEW & KEY METRICS", MARGIN_X, yPos, paintSectionHeading)
        yPos += 7f

        val stats = ScholarshipCalculationHelper.calculateScholarshipStats(scholarships, allRequirements, defaultCurrency, exchangeRates)
        val statusCounts = scholarshipPdfStatusCounts(scholarships.map { it.status })
        val countByStatus = statusCounts.toMap(        )
        val kpiCardWidth = (contentWidth - (5 * 10f)) / 6f
        val kpiItems = listOf(
            Triple("TOTAL", "${scholarships.size}", "Tracked"),
            Triple("OPEN", "${scholarshipPdfOpenCount(statusCounts)}", "Not Started + In Preparation + In Progress"),
            Triple("APPLIED", "${scholarshipPdfAppliedCount(statusCounts)}", "Submitted + awaiting result"),
            Triple("AWARDED", "${countByStatus["Awarded"] ?: 0}", "Awarded applications"),
            Triple("REJECTED", "${countByStatus["Rejected"] ?: 0}", "Rejected applications"),
            Triple("AWARDED SUM", stats.formattedAwardedFunding, "Base: ${stats.baseCurrency}")
        )

        val preparedKpis = kpiItems.map { (kpiTitle, kpiVal, kpiSub) ->
            val valPaint = when (kpiTitle) {
                "AWARDED" -> Paint(paintValueBold).apply { color = COLOR_SUCCESS_GREEN; textSize = 11f }
                "REJECTED" -> Paint(paintValueBold).apply { color = COLOR_DANGER_RED; textSize = 11f }
                "APPLIED" -> Paint(paintValueBold).apply { color = Color.parseColor("#1D4ED8"); textSize = 11f }
                "OPEN" -> Paint(paintValueBold).apply { color = COLOR_TEXT_MUTED; textSize = 11f }
                "AWARDED SUM" -> Paint(paintValueBold).apply { textSize = 8f }
                else -> Paint(paintValueBold).apply { textSize = 11f }
            }
            val subPaint = Paint(paintSubtitle).apply { textSize = 5.8f }
            val valueLines = wrapText(kpiVal, valPaint, kpiCardWidth - 16f)
            val subtitleLines = wrapText(kpiSub, subPaint, kpiCardWidth - 16f)
            val contentHeight = 42f + (valueLines.size - 1).coerceAtLeast(0) * 9f +
                (subtitleLines.size - 1).coerceAtLeast(0) * 6f
            PreparedKpi(
                kpiTitle,
                valPaint,
                valueLines,
                subPaint,
                subtitleLines,
                maxOf(44f, contentHeight)
            )
        }
        val kpiCardHeight = preparedKpis.maxOfOrNull { it.height } ?: 44f

        for (i in preparedKpis.indices) {
            val kpi = preparedKpis[i]
            val kpiTitle = kpi.title
            val cardX = MARGIN_X + i * (kpiCardWidth + 10f)
            val cardRect = RectF(cardX, yPos, cardX + kpiCardWidth, yPos + kpiCardHeight)
            canvas.drawRoundRect(cardRect, 4f, 4f, paintCardBg)
            canvas.drawRoundRect(cardRect, 4f, 4f, paintCardBorder)

            canvas.drawText(kpiTitle, cardX + 8f, yPos + 12f, paintLabel)

            kpi.valueLines.forEachIndexed { lineIndex, line ->
                canvas.drawText(line, cardX + 8f, yPos + 25f + lineIndex * 9f, kpi.valuePaint)
            }
            val subtitleY = yPos + 37f + (kpi.valueLines.size - 1).coerceAtLeast(0) * 9f
            kpi.subtitleLines.forEachIndexed { lineIndex, line ->
                canvas.drawText(line, cardX + 8f, subtitleY + lineIndex * 6f, kpi.subtitlePaint)
            }
        }

        yPos += kpiCardHeight + 6f

        // Multi-currency disclaimer note
        val currencyNote = buildString {
            append("Funding totals normalized to ${stats.baseCurrency}. Converted values indicated by ≈ using local offline exchange rates.")
            if (stats.ratesUsedForAwarded.isNotEmpty()) {
                append(" (Rates: ")
                append(stats.ratesUsedForAwarded.joinToString(", ") { "1 ${it.fromCurrency} = ${CurrencyConverter.formatRateValue(it.rate)} ${it.toCurrency}" })
                append(")")
            }
        }
        val currencyNoteLines = wrapText(currencyNote, paintSubtitle, contentWidth)
        currencyNoteLines.forEachIndexed { index, line ->
            canvas.drawText(line, MARGIN_X, yPos + 6f + index * 9f, paintSubtitle)
        }
        yPos += maxOf(12f, currencyNoteLines.size * 9f + 3f)

        // --- SECTION 3: STATUS VISUALIZATION & UPCOMING DEADLINES (SIDE-BY-SIDE) ---
        val columnWidth = (contentWidth - 14f) / 2f
        val bottomSectionHeight = 225f

        // 3A. Left Box: Status Distribution Visualization
        val leftBoxX = MARGIN_X
        val leftRect = RectF(leftBoxX, yPos, leftBoxX + columnWidth, yPos + bottomSectionHeight)
        canvas.drawRoundRect(leftRect, 4f, 4f, paintCardBg)
        canvas.drawRoundRect(leftRect, 4f, 4f, paintCardBorder)

        canvas.drawText("APPLICATION STATUS DISTRIBUTION", leftBoxX + 12f, yPos + 16f, paintSectionHeading)
        drawStatusDistributionChart(canvas, leftBoxX + 12f, yPos + 26f, columnWidth - 24f, bottomSectionHeight - 34f, statusCounts, scholarships.size)

        // 3B. Right Box: Upcoming Deadlines & Urgency Alerts
        val rightBoxX = MARGIN_X + columnWidth + 14f
        val rightRect = RectF(rightBoxX, yPos, rightBoxX + columnWidth, yPos + bottomSectionHeight)
        canvas.drawRoundRect(rightRect, 4f, 4f, paintCardBg)
        canvas.drawRoundRect(rightRect, 4f, 4f, paintCardBorder)

        canvas.drawText("OPEN DEADLINES", rightBoxX + 12f, yPos + 16f, paintSectionHeading)

        val upcomingList = openDeadlines.take(4)

        var deadY = yPos + 32f
        if (upcomingList.isEmpty()) {
            val emptyText = "No open deadlines. Every tracked application is submitted or closed."
            wrapText(emptyText, paintSubtitle, columnWidth - 24f).forEachIndexed { index, line ->
                canvas.drawText(line, rightBoxX + 12f, deadY + 14f + index * 11f, paintSubtitle)
            }
        } else {
            for (item in upcomingList) {
                val itemDeadline = item.deadlineDate ?: 0L
                val dayText = scholarshipPdfDaysText(item.status, itemDeadline)
                val deadlineFormatted = sdfDate.format(Date(itemDeadline))

                val namePaint = Paint(paintValueBold).apply { textSize = 7.2f }
                val nameLines = wrapText(item.name, namePaint, columnWidth - 24f)
                val detail = "${item.organization.ifBlank { "Not Specified" }} • ${item.status}"
                val detailPaint = Paint(paintSubtitle).apply { textSize = 6.4f }
                val detailLines = wrapText(detail, detailPaint, columnWidth - 24f)
                val itemHeight = maxOf(38f, 10f + nameLines.size * 7f + detailLines.size * 7f + 8f)
                val itemRect = RectF(rightBoxX + 8f, deadY, rightBoxX + columnWidth - 8f, deadY + itemHeight)
                val paintItemBg = Paint().apply { color = COLOR_WHITE; style = Paint.Style.FILL }
                canvas.drawRoundRect(itemRect, 3f, 3f, paintItemBg)
                canvas.drawRoundRect(itemRect, 3f, 3f, paintGridLine)

                nameLines.forEachIndexed { index, line ->
                    canvas.drawText(line, rightBoxX + 14f, deadY + 9f + index * 7f, namePaint)
                }
                detailLines.forEachIndexed { index, line ->
                    canvas.drawText(line, rightBoxX + 14f, deadY + 10f + nameLines.size * 7f + index * 7f, detailPaint)
                }
                val dateLine = "$deadlineFormatted • ${dayText.text}"
                val datePaint = Paint(paintSubtitle).apply {
                    textSize = 6.4f
                    color = scholarshipPdfToneColor(dayText.tone)
                }
                canvas.drawText(dateLine, rightBoxX + 14f, deadY + itemHeight - 4f, datePaint)

                deadY += itemHeight + 5f
            }
        }

        drawFooter(canvas, pageNumber, totalPages, pageWidth, pageHeight)
        pdfDocument.finishPage(page)
        pageNumber++

        for (rowsOnPage in deadlinePageRows) {
            page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            yPos = drawScholarshipHeader(
                canvas,
                pageWidth,
                "Upcoming scholarship application deadlines",
                paintMainTitle,
                paintSubtitle,
                paintGridLine
            )
            drawSectionBar(canvas, MARGIN_X, yPos, contentWidth, "OPEN DEADLINES (CONTINUED)", paintSectionHeading)
            yPos += 24f

            for (rowIndex in rowsOnPage) {
                val row = deadlineRows[rowIndex]
                val cardBottom = yPos + row.height
                canvas.drawRoundRect(
                    RectF(MARGIN_X, yPos, pageWidth - MARGIN_X, cardBottom),
                    3f,
                    3f,
                    Paint().apply { color = COLOR_WHITE }
                )
                canvas.drawRoundRect(
                    RectF(MARGIN_X, yPos, pageWidth - MARGIN_X, cardBottom),
                    3f,
                    3f,
                    paintGridLine
                )
                row.nameLines.forEachIndexed { lineIndex, line ->
                    canvas.drawText(line, MARGIN_X + 10f, yPos + 12f + lineIndex * 8f, continuationDeadlinePaint)
                }
                row.detailLines.forEachIndexed { lineIndex, line ->
                    canvas.drawText(
                        line,
                        MARGIN_X + 10f,
                        yPos + 12f + row.nameLines.size * 8f + lineIndex * 8f,
                        continuationDeadlineDetailPaint
                    )
                }
                val datePaint = Paint(continuationDeadlineDetailPaint).apply {
                    color = scholarshipPdfToneColor(row.days.tone)
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(row.dateLine, MARGIN_X + 10f, cardBottom - 5f, datePaint)
                yPos += row.height + 4f
            }

            drawFooter(canvas, pageNumber, totalPages, pageWidth, pageHeight)
            pdfDocument.finishPage(page)
            pageNumber++
        }

        // =========================================================================
        // PAGE 2+: MAIN APPLICATION SUMMARY TABLE & IN-LINE APPLICATION DOSSIERS
        // =========================================================================
        page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        yPos = MARGIN_TOP

        // Table Column Widths Definition across 770 pt content width:
        // 0. Scholarship Name: 175 pt
        // 1. Provider / Org: 135 pt
        // 2. Deadline: 70 pt
        // 3. Days Left: 90 pt
        // 4. Status: 95 pt
        // 5. Requirements: 95 pt
        // 6. Award Value: 110 pt
        // Total = 175 + 135 + 70 + 90 + 95 + 95 + 110 = 770 pt
        val colStarts = FloatArray(colWidths.size)
        var cumulativeX = MARGIN_X
        for (i in colWidths.indices) {
            colStarts[i] = cumulativeX
            cumulativeX += colWidths[i]
        }

        val paintTableHeaderBg = Paint().apply { color = Color.parseColor("#EFF6FF"); style = Paint.Style.FILL }
        val paintTableHeaderText = Paint().apply {
            color = COLOR_NAVY_ACCENT
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintTableText = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 7.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintTableTextBold = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 7.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintRowAltBg = Paint().apply {
            color = COLOR_ROW_ALT
            style = Paint.Style.FILL
        }

        fun drawTableHeaders(c: Canvas, currentY: Float): Float {
            val headerH = 20f
            c.drawRect(MARGIN_X, currentY, pageWidth - MARGIN_X, currentY + headerH, paintTableHeaderBg)
            c.drawRect(MARGIN_X, currentY, pageWidth - MARGIN_X, currentY + headerH, paintGridLine)

            val headers = arrayOf(
                "SCHOLARSHIP NAME",
                "PROVIDER / ORGANIZATION",
                "DEADLINE",
                "DAYS REMAINING",
                "STATUS",
                "REQUIREMENTS",
                "AWARD VALUE"
            )

            for (i in headers.indices) {
                val colX = colStarts[i]
                if (i > 0) {
                    c.drawLine(colX, currentY, colX, currentY + headerH, paintGridLine)
                }
                c.drawText(headers[i], colX + 6f, currentY + 13f, paintTableHeaderText)
            }

            return currentY + headerH
        }

        // Draw Section Title on Page 2
        drawSectionBar(canvas, MARGIN_X, yPos, contentWidth, "SCHOLARSHIP APPLICATIONS (SUMMARY OVERVIEW)", paintSectionHeading)
        val tableSub = "Comprehensive register of tracked scholarship opportunities with current status and deadlines."
        canvas.drawText(tableSub, MARGIN_X, yPos + 29f, paintSubtitle)
        yPos += 35f

        yPos = drawTableHeaders(canvas, yPos)
        if (scholarships.isEmpty()) {
            val emptyH = 34f
            val emptyRect = RectF(MARGIN_X, yPos, pageWidth - MARGIN_X, yPos + emptyH)
            canvas.drawRect(emptyRect, paintCardBg)
            canvas.drawRect(emptyRect, paintGridLine)
            canvas.drawText("No scholarship applications have been added yet.", MARGIN_X + 12f, yPos + 15f, paintValueBold)
            canvas.drawText("Add scholarships to begin tracking your applications and deadlines.", MARGIN_X + 12f, yPos + 26f, paintSubtitle)
            yPos += emptyH + 16f
        } else {
            for (index in scholarships.indices) {
                val scholarship = scholarships[index]
                val rowValues = reportRows[index]
                val deadlineTs = scholarship.deadlineDate
                val cellPaints = listOf(paintTableTextBold, paintTableText, paintTableText, paintTableText, paintTableTextBold, paintTableText, paintTableTextBold)
                val wrappedCells = rowValues.mapIndexed { cellIndex, value ->
                    wrapText(value, cellPaints[cellIndex], colWidths[cellIndex] - 10f)
                }
                val calculatedRowH = reportRowHeights[index]
                val daysDisplay = deadlineDisplays[index]

                // Page split check for table rows
                if (yPos + calculatedRowH > maxPageBottom) {
                    drawFooter(canvas, pageNumber, totalPages, pageWidth, pageHeight)
                    pdfDocument.finishPage(page)
                    pageNumber++
                    page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    yPos = MARGIN_TOP

                    drawSectionBar(canvas, MARGIN_X, yPos, contentWidth, "SCHOLARSHIP APPLICATIONS (CONTINUED)", paintSectionHeading)
                    yPos += 18f
                    yPos = drawTableHeaders(canvas, yPos)
                }

                // Row Background (Alternating)
                if (index % 2 == 1) {
                    canvas.drawRect(MARGIN_X, yPos, pageWidth - MARGIN_X, yPos + calculatedRowH, paintRowAltBg)
                }

                // Horizontal and Vertical Grid Lines
                canvas.drawLine(MARGIN_X, yPos + calculatedRowH, pageWidth - MARGIN_X, yPos + calculatedRowH, paintGridLine)
                for (colX in colStarts) {
                    canvas.drawLine(colX, yPos, colX, yPos + calculatedRowH, paintGridLine)
                }
                canvas.drawLine(pageWidth - MARGIN_X, yPos, pageWidth - MARGIN_X, yPos + calculatedRowH, paintGridLine)

                val daysPaint = Paint(paintTableTextBold).apply { color = scholarshipPdfToneColor(daysDisplay.tone) }
                val statusColX = colStarts[4]
                val statusColW = colWidths[4]
                val (statusTextColor, statusBgColor) = scholarshipPdfStatusColors(scholarship.status)
                val pillPaint = Paint().apply { color = statusBgColor; style = Paint.Style.FILL }
                val pillTextPaint = Paint(paintTableTextBold).apply { color = statusTextColor; textSize = 7.2f }

                val statusPillRect = RectF(statusColX + 4f, yPos + 3f, statusColX + statusColW - 4f, yPos + calculatedRowH - 3f)
                canvas.drawRoundRect(statusPillRect, 3f, 3f, pillPaint)

                for (columnIndex in wrappedCells.indices) {
                    if (columnIndex == 4) continue
                    val cellPaint = if (columnIndex == 3) daysPaint else cellPaints[columnIndex]
                    wrappedCells[columnIndex].forEachIndexed { lineIndex, line ->
                        canvas.drawText(line, colStarts[columnIndex] + 5f, yPos + 12f + lineIndex * 9.5f, cellPaint)
                    }
                }
                wrappedCells[4].forEachIndexed { lineIndex, line ->
                    canvas.drawText(line, statusColX + 8f, yPos + 12f + lineIndex * 9.5f, pillTextPaint)
                }

                yPos += calculatedRowH
            }

        }

        // =========================================================================
        // SCHOLARSHIP APPLICATION DETAILS & REQUIREMENTS DOSSIER SECTION
        // Removed per user request.
        // =========================================================================

        drawFooter(canvas, pageNumber, totalPages, pageWidth, pageHeight)
        pdfDocument.finishPage(page)

        try {
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Draw Horizontal Bar Chart of Application Status Distribution with Mutually Exclusive Categories
     */
    private fun drawStatusDistributionChart(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        categories: List<Pair<String, Int>>,
        total: Int
    ) {
        val displayedCategories = categories
        val maxVal = maxOf(categories.maxOfOrNull { it.second } ?: 1, 1).toFloat()

        val paintLabel = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        val paintCount = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintBar = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val labelAreaW = 82f
        val barStartX = x + labelAreaW + 6f
        val barEndX = x + width - 35f
        val usableW = barEndX - barStartX

        val barHeight = minOf(8f, height / (displayedCategories.size.coerceAtLeast(1) * 1.8f))
        val spacing = height / displayedCategories.size.coerceAtLeast(1)

        for ((i, category) in displayedCategories.withIndex()) {
            val (label, count) = category
            val barY = y + i * spacing + (spacing - barHeight) / 2f

            // Bar Color
            paintBar.color = scholarshipPdfStatusColorsForCategory(label).first

            // Category Label
            canvas.drawText(label, barStartX - 6f, barY + barHeight, paintLabel)

            // Horizontal Bar
            val barW = if (maxVal > 0 && total > 0) (count.toFloat() / maxVal) * usableW else 0f
            if (barW > 0f) {
                val barRect = RectF(barStartX, barY, barStartX + barW, barY + barHeight)
                canvas.drawRoundRect(barRect, 2f, 2f, paintBar)
            }

            // Count & Percentage Label
            val pct = if (total > 0) ((count * 100f) / total).toInt() else 0
            val countText = "$count ($pct%)"
            canvas.drawText(countText, barStartX + barW + 5f, barY + barHeight, paintCount)
        }
    }

    /**
     * Generate Single Scholarship Detailed Dossier PDF Export (A4 Landscape)
     */
    fun generateSingleScholarshipDossierPdf(
        student: StudentProfile,
        calculatedCgpa: Double,
        scholarship: Scholarship,
        requirements: List<ScholarshipRequirement>,
        timelineEvents: List<ScholarshipTimelineEvent>,
        outputStream: OutputStream,
        defaultCurrency: String = "NGN",
        exchangeRates: List<ExchangeRate> = emptyList()
    ) {
        generateSingleScholarshipPdf(
            student = student,
            calculatedCgpa = calculatedCgpa,
            scholarship = scholarship,
            requirements = requirements,
            timelineEvents = timelineEvents,
            outputStream = outputStream,
            defaultCurrency = defaultCurrency,
            exchangeRates = exchangeRates
        )
    }

    /**
     * Generate Single Scholarship Detailed Dossier PDF Export (A4 Landscape)
     */
    fun generateSingleScholarshipPdf(
        student: StudentProfile,
        calculatedCgpa: Double,
        scholarship: Scholarship,
        requirements: List<ScholarshipRequirement>,
        timelineEvents: List<ScholarshipTimelineEvent>,
        outputStream: OutputStream,
        defaultCurrency: String = "NGN",
        exchangeRates: List<ExchangeRate> = emptyList()
    ) {
        val pdfDocument = PdfDocument()
        val pageWidth = A4_LANDSCAPE_WIDTH
        val pageHeight = A4_LANDSCAPE_HEIGHT
        val contentWidth = pageWidth - (2 * MARGIN_X)

        var pageNumber = 1
        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas

        // Paints
        val paintTitle = Paint().apply {
            color = COLOR_NAVY_PRIMARY
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSubtitle = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintSection = Paint().apply {
            color = COLOR_NAVY_PRIMARY
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintBold = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintText = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 8f
            isAntiAlias = true
        }

        val paintTextSmall = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 7.2f
            isAntiAlias = true
        }

        val paintLine = Paint().apply {
            color = COLOR_GRID_LINE
            strokeWidth = 0.6f
            style = Paint.Style.STROKE
        }

        val paintCardBg = Paint().apply {
            color = COLOR_CARD_BG
            style = Paint.Style.FILL
        }

        var yPos = drawScholarshipHeader(
            canvas,
            pageWidth,
            "Individual scholarship application dossier",
            paintTitle,
            paintSubtitle,
            paintLine,
            title = "SCHOLARSHIP DOSSIER"
        )
        yPos = drawStudentCard(
            canvas,
            student,
            calculatedCgpa,
            MARGIN_X,
            yPos,
            contentWidth,
            paintTextSmall,
            paintBold,
            paintCardBg,
            paintLine
        ) + 12f

        // Main Scholarship Header Banner
        val bannerTextPaint = Paint(paintBold).apply { textSize = 9f }
        val bannerDetailPaint = Paint(paintTextSmall).apply { textSize = 7.2f }
        val statusText = scholarship.status
        val (statusTextColor, statusBgColor) = scholarshipPdfStatusColors(scholarship.status)
        val statusTextPaint = Paint(paintBold).apply { color = statusTextColor; textSize = 7f }
        val statusWidth = statusTextPaint.measureText(statusText) + 12f
        val scholarshipNameLines = wrapText(scholarship.name, bannerTextPaint, contentWidth - statusWidth - 36f)
        val organizationLines = wrapText(
            "Provider: ${scholarship.organization.ifBlank { "Not Specified" }}",
            bannerDetailPaint,
            contentWidth - 24f
        )
        val awardText = scholarshipAwardText(scholarship, defaultCurrency, exchangeRates)
        val awardLines = wrapText("Award: $awardText", bannerDetailPaint, contentWidth - 24f)
        val bannerHeight = 16f + scholarshipNameLines.size * 11f +
            organizationLines.size * 9f + awardLines.size * 9f + 8f
        val bannerRect = RectF(MARGIN_X, yPos, pageWidth - MARGIN_X, yPos + bannerHeight)
        canvas.drawRoundRect(bannerRect, 4f, 4f, paintCardBg)
        canvas.drawRoundRect(bannerRect, 4f, 4f, paintLine)
        val statusPaint = Paint().apply { color = statusBgColor }
        canvas.drawRoundRect(
            RectF(pageWidth - MARGIN_X - statusWidth - 10f, yPos + 5f, pageWidth - MARGIN_X - 10f, yPos + 18f),
            3f,
            3f,
            statusPaint
        )
        canvas.drawText(statusText, pageWidth - MARGIN_X - statusWidth - 4f, yPos + 14f, statusTextPaint)
        scholarshipNameLines.forEachIndexed { index, line ->
            canvas.drawText(line, MARGIN_X + 12f, yPos + 14f + index * 11f, bannerTextPaint)
        }
        organizationLines.forEachIndexed { index, line ->
            canvas.drawText(line, MARGIN_X + 12f, yPos + 18f + scholarshipNameLines.size * 11f + index * 9f, bannerDetailPaint)
        }
        awardLines.forEachIndexed { index, line ->
            canvas.drawText(
                line,
                MARGIN_X + 12f,
                yPos + 18f + scholarshipNameLines.size * 11f + organizationLines.size * 9f + index * 9f,
                bannerDetailPaint
            )
        }
        yPos += bannerHeight + 12f

        // 2-Column Split: Left = Details & Eligibility, Right = Requirements & Timeline
        val halfW = (contentWidth - 12f) / 2f

        // Left Column (Key Parameters & Verification)
        val leftX = MARGIN_X
        val leftBoxHeight = minOf(310f, (pageHeight - MARGIN_BOTTOM - 20f - yPos).coerceAtLeast(0f))
        canvas.drawRoundRect(RectF(leftX, yPos, leftX + halfW, yPos + leftBoxHeight), 4f, 4f, paintCardBg)
        canvas.drawRoundRect(RectF(leftX, yPos, leftX + halfW, yPos + leftBoxHeight), 4f, 4f, paintLine)

        var ly = yPos + 16f
        drawSectionBar(canvas, leftX + 8f, ly - 11f, halfW - 16f, "APPLICATION PARAMETERS & ELIGIBILITY", paintSection)
        ly += 16f

        fun drawParam(label: String, value: String, valueColor: Int = COLOR_TEXT_DARK) {
            val labelPaint = Paint(paintBold).apply { textSize = 7.4f }
            val valuePaint = Paint(paintText).apply { color = valueColor; textSize = 7.2f }
            canvas.drawText(label, leftX + 12f, ly, labelPaint)
            val valLines = wrapText(value, valuePaint, halfW - 130f)
            var vy = ly
            for (line in valLines) {
                canvas.drawText(line, leftX + 120f, vy, valuePaint)
                vy += 9f
            }
            ly = maxOf(ly + 13f, vy + 4f)
        }

        val deadlineTs = scholarship.deadlineDate ?: 0L
        val dayDisplay = scholarshipPdfDaysText(scholarship.status, scholarship.deadlineDate)
        val deadlineFull = if (deadlineTs > 0L) "${sdfDate.format(Date(deadlineTs))} • ${dayDisplay.text}" else "-"
        val appliedDateStr = if (scholarship.dateApplied != null && scholarship.dateApplied > 0) sdfDate.format(Date(scholarship.dateApplied)) else "Not yet applied"
        val feedbackDateStr = if (scholarship.expectedFeedbackDate != null && scholarship.expectedFeedbackDate > 0) sdfDate.format(Date(scholarship.expectedFeedbackDate)) else "Not set"

        drawParam("Deadline Date:", deadlineFull, scholarshipPdfToneColor(dayDisplay.tone))
        drawParam("Application Status:", scholarship.status)
        drawParam("Date Applied:", appliedDateStr)
        drawParam("Expected Feedback:", feedbackDateStr)

        val isTargetCgpa = calculatedCgpa <= 0.0 && student.targetCgpa > 0.0
        val effectiveStudentCgpa = if (calculatedCgpa > 0.0) calculatedCgpa else student.targetCgpa
        val eligibility = ScholarshipCalculationHelper.checkEligibility(effectiveStudentCgpa, student.gradingScale, scholarship, isTargetCgpa)
        drawParam("Min CGPA Req:", if (scholarship.minCgpa != null && scholarship.minCgpa > 0) "%.2f / %.1f".format(scholarship.minCgpa, student.gradingScale) else "None")
        drawParam("Eligibility Result:", "${eligibility.badgeText} (${eligibility.description})")

        val origAmt = scholarship.effectiveAmount
        val origCurr = scholarship.effectiveCurrency
        if (origAmt > 0) {
            val awStr = CurrencyConverter.format(origAmt, origCurr)
            drawParam("Award Funding:", awStr)
            if (origCurr != defaultCurrency) {
                val conv = CurrencyConverter.convert(origAmt, origCurr, defaultCurrency, exchangeRates)
                if (conv is CurrencyConverter.ConversionResult.Success) {
                    drawParam("Converted Value:", "${CurrencyConverter.formatConverted(conv.convertedAmount, defaultCurrency)} (${conv.pairLabel})")
                } else {
                    drawParam("Converted Value:", "Rate to $defaultCurrency unconfigured")
                }
            }
        }

        if (scholarship.awardAmount != null && scholarship.awardAmount > 0) {
            val awStr = CurrencyConverter.format(scholarship.awardAmount, scholarship.awardCurrency ?: scholarship.currency)
            drawParam("Disbursed Award:", awStr)
        }
        if (scholarship.notes.isNotBlank()) {
            drawParam("General Notes:", scholarship.notes)
        }
        val sAwardNotes = scholarship.awardNotes
        if (!sAwardNotes.isNullOrBlank()) {
            drawParam("Award Notes:", sAwardNotes)
        }

        // Right Column (Requirements & Timeline)
        val rightX = MARGIN_X + halfW + 12f
        canvas.drawRoundRect(RectF(rightX, yPos, rightX + halfW, yPos + leftBoxHeight), 4f, 4f, paintCardBg)
        canvas.drawRoundRect(RectF(rightX, yPos, rightX + halfW, yPos + leftBoxHeight), 4f, 4f, paintLine)

        var ry = yPos + 16f
        val reqProgress = ScholarshipCalculationHelper.getRequirementsProgress(requirements)
        drawSectionBar(
            canvas,
            rightX + 8f,
            ry - 11f,
            halfW - 16f,
            "REQUIREMENTS CHECKLIST (${reqProgress.completed}/${reqProgress.total} - ${reqProgress.percentage.toInt()}%)",
            paintSection
        )
        ry += 14f

        if (requirements.isEmpty()) {
            canvas.drawText("No specific requirements documented.", rightX + 12f, ry, paintSubtitle)
            ry += 16f
        } else {
            for (req in requirements) {
                val isDone = req.status == RequirementStatus.COMPLETED || req.status == RequirementStatus.SUBMITTED
                val marker = if (isDone) "[x]" else "[ ]"
                val mPaint = if (isDone) Paint(paintBold).apply { color = COLOR_SUCCESS_GREEN } else paintSubtitle
                val requirementText = "$marker ${req.title} — ${req.category} | ${req.status}"
                wrapText(requirementText, mPaint, halfW - 24f).forEach { line ->
                    canvas.drawText(line, rightX + 12f, ry, mPaint)
                    ry += 9f
                }
                if (req.details.isNotBlank()) {
                    wrapText("Details: ${req.details}", paintTextSmall, halfW - 24f).forEach { line ->
                        canvas.drawText(line, rightX + 12f, ry, paintTextSmall)
                        ry += 8f
                    }
                }
                if (req.notes.isNotBlank()) {
                    wrapText("Notes: ${req.notes}", paintTextSmall, halfW - 24f).forEach { line ->
                        canvas.drawText(line, rightX + 12f, ry, paintTextSmall)
                        ry += 8f
                    }
                }
                ry += 3f
            }
        }

        ry += 8f
        canvas.drawLine(rightX + 12f, ry, rightX + halfW - 12f, ry, paintLine)
        ry += 14f

        drawSectionBar(canvas, rightX + 8f, ry - 11f, halfW - 16f, "TIMELINE & AUDIT LOGS", paintSection)
        ry += 14f

        if (timelineEvents.isEmpty()) {
            canvas.drawText("No timeline events logged yet.", rightX + 12f, ry, paintSubtitle)
        } else {
            for (ev in timelineEvents) {
                val dateStr = sdfDate.format(Date(ev.date))
                val entryLines = wrapText("$dateStr: ${ev.title}", paintBold, halfW - 24f)
                entryLines.forEach { line ->
                    canvas.drawText(line, rightX + 12f, ry, paintBold)
                    ry += 9f
                }
                if (ev.description.isNotBlank()) {
                    wrapText(ev.description, paintTextSmall, halfW - 24f).forEach { line ->
                        canvas.drawText(line, rightX + 12f, ry, paintTextSmall)
                        ry += 8f
                    }
                }
                ry += 3f
            }
        }

        drawFooter(canvas, pageNumber, 1, pageWidth, pageHeight)
        pdfDocument.finishPage(page)

        try {
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawScholarshipHeader(
        canvas: Canvas,
        pageWidth: Int,
        subtitle: String,
        titlePaint: Paint,
        subtitlePaint: Paint,
        rulePaint: Paint,
        title: String = "SCHOLARSHIP TRACKER REPORT"
    ): Float {
        val titleStyle = Paint(titlePaint).apply {
            color = COLOR_NAVY_ACCENT
            textSize = 15f
            textAlign = Paint.Align.CENTER
        }
        val subtitleStyle = Paint(subtitlePaint).apply {
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }
        val centerX = pageWidth / 2f
        canvas.drawText(title, centerX, MARGIN_TOP + 15f, titleStyle)
        val subtitleLines = wrapText(subtitle, subtitleStyle, pageWidth - 2 * MARGIN_X)
        subtitleLines.forEachIndexed { index, line ->
            canvas.drawText(line, centerX, MARGIN_TOP + 28f + index * 9f, subtitleStyle)
        }
        val ruleY = MARGIN_TOP + 28f + subtitleLines.size * 9f
        val rule = Paint(rulePaint).apply {
            color = COLOR_NAVY_ACCENT
            strokeWidth = 1.2f
        }
        canvas.drawLine(MARGIN_X, ruleY, pageWidth - MARGIN_X, ruleY, rule)
        return ruleY + 12f
    }

    private fun drawStudentCard(
        canvas: Canvas,
        student: StudentProfile,
        calculatedCgpa: Double,
        x: Float,
        y: Float,
        width: Float,
        labelPaint: Paint,
        valuePaint: Paint,
        fillPaint: Paint,
        linePaint: Paint
    ): Float {
        val cgpa = when {
            calculatedCgpa > 0.0 -> "%.2f / %.1f scale".format(calculatedCgpa, student.gradingScale)
            student.targetCgpa > 0.0 -> "%.2f / %.1f scale (target)".format(student.targetCgpa, student.gradingScale)
            else -> "0.00 / %.1f scale".format(student.gradingScale)
        }
        val fields = listOf(
            "STUDENT NAME" to student.fullName.ifBlank { "Registered Student" },
            "UNIVERSITY" to student.institution.ifBlank { "Not Specified" },
            "FACULTY" to student.faculty.ifBlank { "Not Specified" },
            "DEPARTMENT" to student.department.ifBlank { "Not Specified" },
            "MATRIC NUMBER" to student.matricNo.ifBlank { "Not Specified" },
            "LEVEL" to student.currentLevel.ifBlank { "Undergraduate" },
            "CGPA" to cgpa
        )
        val innerWidth = width - 28f
        val columnWidth = (innerWidth - 24f) / 3f
        val valueStyle = Paint(valuePaint).apply { textSize = 7.8f }
        val labelStyle = Paint(labelPaint).apply { textSize = 6.7f }
        val rows = fields.chunked(3)
        val rowHeights = rows.map { row ->
            val maxValueLines = row.maxOf { (_, value) -> wrapText(value, valueStyle, columnWidth).size }
            maxOf(24f, 14f + maxValueLines * 9f)
        }
        val cardHeight = 12f + rowHeights.sum() + 8f
        val cardRect = RectF(x, y, x + width, y + cardHeight)
        canvas.drawRoundRect(cardRect, 4f, 4f, fillPaint)
        canvas.drawRoundRect(cardRect, 4f, 4f, Paint(linePaint).apply { style = Paint.Style.STROKE })
        var rowY = y + 5f
        rows.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, (label, value) ->
                val fieldX = x + 14f + columnIndex * (columnWidth + 12f)
                canvas.drawText(label, fieldX, rowY + 8f, labelStyle)
                wrapText(value, valueStyle, columnWidth).forEachIndexed { lineIndex, line ->
                    canvas.drawText(line, fieldX, rowY + 18f + lineIndex * 9f, valueStyle)
                }
            }
            rowY += rowHeights[rowIndex]
        }
        return y + cardHeight
    }

    private fun scholarshipAwardText(
        scholarship: Scholarship,
        defaultCurrency: String,
        exchangeRates: List<ExchangeRate>
    ): String {
        val amount = scholarship.effectiveAmount
        val currency = scholarship.effectiveCurrency
        if (amount <= 0.0) return "Unstated"
        val formatted = CurrencyConverter.format(amount, currency)
        if (currency == defaultCurrency) return formatted
        val converted = CurrencyConverter.convert(amount, currency, defaultCurrency, exchangeRates)
        return if (converted is CurrencyConverter.ConversionResult.Success) {
            "$formatted (≈ ${CurrencyConverter.format(converted.convertedAmount, defaultCurrency)})"
        } else {
            formatted
        }
    }

    private fun scholarshipPdfStatusColors(status: String): Pair<Int, Int> =
        scholarshipPdfStatusColorsForCategory(scholarshipPdfCategoryForStatus(status))

    private fun scholarshipPdfStatusColorsForCategory(category: String): Pair<Int, Int> =
        when (category) {
            "In Preparation" -> Color.parseColor("#7E22CE") to Color.parseColor("#F3E8FF")
            "In Progress" -> Color.parseColor("#4338CA") to Color.parseColor("#E0E7FF")
            "Submitted" -> Color.parseColor("#1D4ED8") to Color.parseColor("#DBEAFE")
            "Awaiting Result" -> Color.parseColor("#B45309") to Color.parseColor("#FEF3C7")
            "Awarded" -> COLOR_SUCCESS_GREEN to COLOR_SUCCESS_BG
            "Rejected" -> COLOR_DANGER_RED to COLOR_DANGER_BG
            "Not Started", "Withdrawn", "Expired" -> COLOR_TEXT_MUTED to Color.parseColor("#E2E8F0")
            else -> COLOR_TEXT_MUTED to COLOR_STATUS_BG
        }

    private fun scholarshipPdfToneColor(tone: ScholarshipPdfTone): Int =
        when (tone) {
            ScholarshipPdfTone.OVERDUE -> COLOR_DANGER_RED
            ScholarshipPdfTone.DUE_TODAY -> COLOR_WARNING_ORANGE
            ScholarshipPdfTone.MUTED -> COLOR_TEXT_MUTED
            ScholarshipPdfTone.STANDARD -> COLOR_TEXT_DARK
        }

    /**
     * Draw standard page footer
     */
    private fun drawFooter(canvas: Canvas, pageNumber: Int, totalPages: Int, pageWidth: Int, pageHeight: Int) {
        val paintFooter = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 7.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val footerY = pageHeight - MARGIN_BOTTOM + 12f
        val footerLeft = "Generated by GPA Whiz on ${formatDateTime(Date(), "dd MMM yyyy", ", ", Locale.US)}"
        canvas.drawText(footerLeft, MARGIN_X, footerY, paintFooter)

        val pageStr = "Page $pageNumber of $totalPages"
        val pageW = paintFooter.measureText(pageStr)
        canvas.drawText(pageStr, pageWidth - MARGIN_X - pageW, footerY, paintFooter)
    }

    private fun drawSectionBar(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        title: String,
        textPaint: Paint
    ) {
        val barPaint = Paint().apply { color = Color.parseColor("#EFF6FF") }
        canvas.drawRoundRect(RectF(x, y, x + width, y + 18f), 2f, 2f, barPaint)
        val sectionPaint = Paint(textPaint).apply {
            color = COLOR_NAVY_ACCENT
            textSize = 8f
        }
        canvas.drawText(title, x + 6f, y + 12f, sectionPaint)
    }

    /**
     * Utility to break a string into multiple lines that fit within a specified pixel width.
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        return wrapPdfText(text, maxWidth, paint.textSize) { line, size ->
            Paint(paint).apply { textSize = size }.measureText(line)
        }
    }
}
