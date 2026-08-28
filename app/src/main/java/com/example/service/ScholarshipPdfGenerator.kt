package com.example.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.RequirementCategory
import com.example.data.RequirementStatus
import com.example.data.Scholarship
import com.example.data.ScholarshipRequirement
import com.example.data.ScholarshipStatus
import com.example.data.ScholarshipTimelineEvent
import com.example.data.StudentProfile
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
    private val sdfDateTime = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())

    /**
     * Generate Comprehensive All Scholarships Multi-Section Report in A4 Landscape
     */
    fun generateScholarshipReportPdf(
        student: StudentProfile,
        calculatedCgpa: Double,
        scholarships: List<Scholarship>,
        allRequirements: List<ScholarshipRequirement>,
        outputStream: OutputStream,
        reportTitle: String = "ACTIVE SCHOLARSHIP APPLICATIONS REPORT"
    ) {
        val pdfDocument = PdfDocument()
        val pageWidth = A4_LANDSCAPE_WIDTH
        val pageHeight = A4_LANDSCAPE_HEIGHT
        val contentWidth = pageWidth - (2 * MARGIN_X) // 770 pt

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

        // Top Accent Stripe
        val paintStripe = Paint().apply { color = COLOR_NAVY_PRIMARY; style = Paint.Style.FILL }
        canvas.drawRect(MARGIN_X, yPos, pageWidth - MARGIN_X, yPos + 3.5f, paintStripe)
        yPos += 15f

        // Document Header: Brand & Report Title
        canvas.drawText("GPA WHIZ", MARGIN_X, yPos + 2f, paintMainTitle)
        val brandW = paintMainTitle.measureText("GPA WHIZ")
        val paintSubHeader = Paint(paintMainTitle).apply { color = COLOR_NAVY_ACCENT; textSize = 11.5f }
        canvas.drawText("  •  $reportTitle", MARGIN_X + brandW, yPos + 1.5f, paintSubHeader)

        val dateStr = "Report Generated: ${sdfDateTime.format(Date())} (Local Time)"
        val dateWidth = paintSubtitle.measureText(dateStr)
        canvas.drawText(dateStr, pageWidth - MARGIN_X - dateWidth, yPos, paintSubtitle)
        yPos += 12f

        canvas.drawLine(MARGIN_X, yPos, pageWidth - MARGIN_X, yPos, paintGridLine)
        yPos += 10f

        // --- SECTION 1: STUDENT PROFILE BANNER ---
        val profileHeight = 54f
        val profileRect = RectF(MARGIN_X, yPos, pageWidth - MARGIN_X, yPos + profileHeight)
        canvas.drawRoundRect(profileRect, 4f, 4f, paintCardBg)
        canvas.drawRoundRect(profileRect, 4f, 4f, paintCardBorder)

        val pCol1 = MARGIN_X + 14f
        val pCol2 = MARGIN_X + 220f
        val pCol3 = MARGIN_X + 440f
        val pCol4 = MARGIN_X + 630f

        val studentName = student.fullName.ifBlank { "Registered Student" }.uppercase()
        val institution = student.institution.ifBlank { "Not Specified" }
        val faculty = student.faculty.ifBlank { "Not Specified" }
        val department = student.department.ifBlank { "Not Specified" }
        val currentLevel = student.currentLevel.ifBlank { "Undergraduate" }

        val cgpaDisplay = if (calculatedCgpa > 0.0) {
            "%.2f / %.1f Scale (Current)".format(calculatedCgpa, student.gradingScale)
        } else if (student.targetCgpa > 0.0) {
            "%.2f / %.1f Scale (Target)".format(student.targetCgpa, student.gradingScale)
        } else {
            "0.00 / %.1f Scale".format(student.gradingScale)
        }

        // Row 1 of Profile
        canvas.drawText("STUDENT NAME", pCol1, yPos + 13f, paintLabel)
        canvas.drawText(studentName, pCol1, yPos + 25f, paintValueBold)

        canvas.drawText("INSTITUTION / UNIVERSITY", pCol2, yPos + 13f, paintLabel)
        canvas.drawText(institution.take(32), pCol2, yPos + 25f, paintValueBold)

        canvas.drawText("FACULTY & DEPARTMENT", pCol3, yPos + 13f, paintLabel)
        val facDept = if (faculty != "Not Specified" && department != "Not Specified") "$faculty • $department" else if (faculty != "Not Specified") faculty else department
        val facDeptLines = wrapText(facDept, paintValueBold, 180f)
        canvas.drawText(facDeptLines[0], pCol3, yPos + 24f, paintValueBold)
        if (facDeptLines.size > 1) {
            var secondLine = facDeptLines[1]
            if (facDeptLines.size > 2) secondLine = secondLine.take(25) + "..."
            canvas.drawText(secondLine, pCol3, yPos + 34f, paintValueBold)
        }

        canvas.drawText("ACADEMIC LEVEL & CGPA", pCol4, yPos + 13f, paintLabel)
        canvas.drawText("$cgpaDisplay ($currentLevel)", pCol4, yPos + 25f, paintValueBold)

        // Row 2 Sub-details
        canvas.drawText("Grading System: %.1f Maximum Point Scale".format(student.gradingScale), pCol1, yPos + 43f, paintSubtitle)
        val fundingCount = scholarships.count { it.status == ScholarshipStatus.AWARDED }
        canvas.drawText("Scholarship Pipeline: ${scholarships.size} Tracked ($fundingCount Awarded)", pCol3, yPos + 43f, paintSubtitle)

        yPos += profileHeight + 12f

        // --- SECTION 2: APPLICATION OVERVIEW (KPI CARDS) ---
        canvas.drawText("APPLICATION OVERVIEW & KEY METRICS", MARGIN_X, yPos, paintSectionHeading)
        yPos += 7f

        val stats = ScholarshipCalculationHelper.calculateScholarshipStats(scholarships, allRequirements)
        val kpiCardWidth = (contentWidth - (5 * 10f)) / 6f
        val kpiCardHeight = 44f

        val kpiItems = listOf(
            Triple("TOTAL", "${scholarships.size}", "Tracked"),
            Triple("ACTIVE", "${stats.active}", "In Progress"),
            Triple("SUBMITTED", "${stats.awaitingResults}", "Awaiting Review"),
            Triple("AWARDED", "${stats.awarded}", if (stats.awarded > 0) "Won" else "0 Won"),
            Triple("REJECTED", "${stats.rejected}", "Declined"),
            Triple("AWARDED SUM", if (stats.awardedFunding.isNotEmpty()) {
                stats.awardedFunding.entries.joinToString(", ") { "${it.key}%,.0f".format(it.value) }
            } else "—", "Total Funding")
        )

        for (i in kpiItems.indices) {
            val (kpiTitle, kpiVal, kpiSub) = kpiItems[i]
            val cardX = MARGIN_X + i * (kpiCardWidth + 10f)
            val cardRect = RectF(cardX, yPos, cardX + kpiCardWidth, yPos + kpiCardHeight)
            canvas.drawRoundRect(cardRect, 4f, 4f, paintCardBg)
            canvas.drawRoundRect(cardRect, 4f, 4f, paintCardBorder)

            canvas.drawText(kpiTitle, cardX + 8f, yPos + 12f, paintLabel)

            val valPaint = when (kpiTitle) {
                "AWARDED" -> Paint(paintValueBold).apply { color = COLOR_SUCCESS_GREEN; textSize = 11f }
                "REJECTED" -> Paint(paintValueBold).apply { color = if (stats.rejected > 0) COLOR_DANGER_RED else COLOR_TEXT_DARK; textSize = 11f }
                "ACTIVE" -> Paint(paintValueBold).apply { color = COLOR_NAVY_ACCENT; textSize = 11f }
                else -> Paint(paintValueBold).apply { textSize = 11f }
            }
            canvas.drawText(kpiVal.take(14), cardX + 8f, yPos + 26f, valPaint)
            canvas.drawText(kpiSub, cardX + 8f, yPos + 38f, paintSubtitle)
        }

        yPos += kpiCardHeight + 14f

        // --- SECTION 3: STATUS VISUALIZATION & UPCOMING DEADLINES (SIDE-BY-SIDE) ---
        val columnWidth = (contentWidth - 14f) / 2f
        val bottomSectionHeight = 225f

        // 3A. Left Box: Status Distribution Visualization
        val leftBoxX = MARGIN_X
        val leftRect = RectF(leftBoxX, yPos, leftBoxX + columnWidth, yPos + bottomSectionHeight)
        canvas.drawRoundRect(leftRect, 4f, 4f, paintCardBg)
        canvas.drawRoundRect(leftRect, 4f, 4f, paintCardBorder)

        canvas.drawText("APPLICATION STATUS DISTRIBUTION", leftBoxX + 12f, yPos + 16f, paintSectionHeading)
        drawStatusDistributionChart(canvas, leftBoxX + 12f, yPos + 26f, columnWidth - 24f, bottomSectionHeight - 34f, scholarships)

        // 3B. Right Box: Upcoming Deadlines & Urgency Alerts
        val rightBoxX = MARGIN_X + columnWidth + 14f
        val rightRect = RectF(rightBoxX, yPos, rightBoxX + columnWidth, yPos + bottomSectionHeight)
        canvas.drawRoundRect(rightRect, 4f, 4f, paintCardBg)
        canvas.drawRoundRect(rightRect, 4f, 4f, paintCardBorder)

        canvas.drawText("UPCOMING DEADLINES & TIMELINE ALERTS", rightBoxX + 12f, yPos + 16f, paintSectionHeading)

        val upcomingList = scholarships
            .filter { it.status != ScholarshipStatus.AWARDED && it.status != ScholarshipStatus.REJECTED && (it.deadlineDate ?: 0L) > 0L }
            .sortedBy { it.deadlineDate ?: Long.MAX_VALUE }
            .take(4)

        var deadY = yPos + 32f
        if (upcomingList.isEmpty()) {
            canvas.drawText("No active deadlines recorded.", rightBoxX + 12f, deadY + 14f, paintSubtitle)
            canvas.drawText("Add deadlines to your applications to monitor milestones.", rightBoxX + 12f, deadY + 28f, paintSubtitle)
        } else {
            for (item in upcomingList) {
                val itemDeadline = item.deadlineDate ?: 0L
                val countdown = ScholarshipCalculationHelper.getDeadlineCountdown(itemDeadline)
                val deadlineFormatted = sdfDate.format(Date(itemDeadline))

                // Item container - Increased height to 38f to avoid truncation
                val itemRect = RectF(rightBoxX + 8f, deadY, rightBoxX + columnWidth - 8f, deadY + 38f)
                val paintItemBg = Paint().apply { color = COLOR_WHITE; style = Paint.Style.FILL }
                canvas.drawRoundRect(itemRect, 3f, 3f, paintItemBg)
                canvas.drawRoundRect(itemRect, 3f, 3f, paintGridLine)

                // Urgency tag - Measure first to know available width for name
                val tagText = formatCountdownBadge(countdown)
                val tagPaint = when (countdown.urgency) {
                    UrgencyLevel.CRITICAL, UrgencyLevel.PASSED -> Paint(paintValueBold).apply { color = COLOR_DANGER_RED; textSize = 7.8f }
                    UrgencyLevel.HIGH -> Paint(paintValueBold).apply { color = COLOR_WARNING_ORANGE; textSize = 7.8f }
                    else -> Paint(paintValueBold).apply { color = COLOR_NAVY_PRIMARY; textSize = 7.8f }
                }
                val rightLabel = "$deadlineFormatted ($tagText)"
                val labelW = tagPaint.measureText(rightLabel)

                // Scholarship title - Use wrapText to avoid hard truncation
                val nameMaxWidth = columnWidth - labelW - 32f
                val nameLines = wrapText(item.name, paintValueBold, nameMaxWidth)
                canvas.drawText(nameLines[0], rightBoxX + 14f, deadY + 13f, paintValueBold)

                // Status & Provider - Use wrapText and allow two lines if needed
                val subOrg = "Status: ${item.status} • Provider: ${item.organization.ifBlank { "Not Specified" }}"
                val subLines = wrapText(subOrg, paintSubtitle, columnWidth - 24f)
                canvas.drawText(subLines[0], rightBoxX + 14f, deadY + 24f, paintSubtitle)
                if (subLines.size > 1) {
                    val secondLine = if (subLines.size > 2) subLines[1].take(45) + "..." else subLines[1]
                    canvas.drawText(secondLine, rightBoxX + 14f, deadY + 33f, paintSubtitle)
                }

                // Draw urgency tag
                canvas.drawText(rightLabel, rightBoxX + columnWidth - 14f - labelW, deadY + 13f, tagPaint)

                deadY += 43f
            }
        }

        drawFooter(canvas, pageNumber, pageWidth, pageHeight)
        pdfDocument.finishPage(page)
        pageNumber++

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
        val colWidths = floatArrayOf(175f, 135f, 70f, 90f, 95f, 95f, 110f)
        val colStarts = FloatArray(colWidths.size)
        var cumulativeX = MARGIN_X
        for (i in colWidths.indices) {
            colStarts[i] = cumulativeX
            cumulativeX += colWidths[i]
        }

        val paintTableHeaderBg = Paint().apply { color = COLOR_NAVY_HEADER; style = Paint.Style.FILL }
        val paintTableHeaderText = Paint().apply {
            color = COLOR_WHITE
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintTableText = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paintTableTextBold = Paint().apply {
            color = COLOR_TEXT_DARK
            textSize = 7.8f
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
        canvas.drawText("SCHOLARSHIP APPLICATIONS (SUMMARY OVERVIEW)", MARGIN_X, yPos + 8f, paintSectionHeading)
        val tableSub = "Comprehensive register of tracked scholarship opportunities with current status and deadlines."
        canvas.drawText(tableSub, MARGIN_X, yPos + 19f, paintSubtitle)
        yPos += 26f

        yPos = drawTableHeaders(canvas, yPos)
        val maxPageBottom = pageHeight - MARGIN_BOTTOM - 20f

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
                val reqs = allRequirements.filter { it.scholarshipId == scholarship.id }
                val progress = ScholarshipCalculationHelper.getRequirementsProgress(reqs)
                val deadlineTs = scholarship.deadlineDate ?: 0L
                val countdown = ScholarshipCalculationHelper.getDeadlineCountdown(deadlineTs)

                // Calculate wrapped lines for name and provider
                val nameLines = wrapText(scholarship.name, paintTableTextBold, colWidths[0] - 12f)
                val providerLines = wrapText(scholarship.organization.ifBlank { "Not Specified" }, paintTableText, colWidths[1] - 12f)

                val lineCount = maxOf(nameLines.size, providerLines.size, 1)
                val calculatedRowH = maxOf(20f, 8f + (lineCount * 11f))

                // Page split check for table rows
                if (yPos + calculatedRowH > maxPageBottom) {
                    drawFooter(canvas, pageNumber, pageWidth, pageHeight)
                    pdfDocument.finishPage(page)
                    pageNumber++
                    page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    yPos = MARGIN_TOP

                    canvas.drawText("SCHOLARSHIP APPLICATIONS (Continued - Page $pageNumber)", MARGIN_X, yPos + 8f, paintSectionHeading)
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

                // Draw Col 0: Scholarship Name (Wrapped & Clipped)
                canvas.save()
                canvas.clipRect(colStarts[0] + 1f, yPos + 1f, colStarts[0] + colWidths[0] - 1f, yPos + calculatedRowH - 1f)
                var textLineY = yPos + 12f
                for (line in nameLines) {
                    canvas.drawText(line, colStarts[0] + 5f, textLineY, paintTableTextBold)
                    textLineY += 11f
                }
                canvas.restore()

                // Draw Col 1: Provider / Org (Wrapped & Clipped)
                canvas.save()
                canvas.clipRect(colStarts[1] + 1f, yPos + 1f, colStarts[1] + colWidths[1] - 1f, yPos + calculatedRowH - 1f)
                textLineY = yPos + 12f
                for (line in providerLines) {
                    canvas.drawText(line, colStarts[1] + 5f, textLineY, paintTableText)
                    textLineY += 11f
                }
                canvas.restore()

                // Draw Col 2: Deadline (Clipped)
                canvas.save()
                canvas.clipRect(colStarts[2] + 1f, yPos + 1f, colStarts[2] + colWidths[2] - 1f, yPos + calculatedRowH - 1f)
                val deadlineStr = if (deadlineTs > 0L) sdfDate.format(Date(deadlineTs)) else "Not Set"
                canvas.drawText(deadlineStr, colStarts[2] + 5f, yPos + 13f, paintTableText)
                canvas.restore()

                // Draw Col 3: Days Remaining (Clipped & Urgency Formatted)
                canvas.save()
                canvas.clipRect(colStarts[3] + 1f, yPos + 1f, colStarts[3] + colWidths[3] - 1f, yPos + calculatedRowH - 1f)
                val daysPaint = when (countdown.urgency) {
                    UrgencyLevel.CRITICAL, UrgencyLevel.PASSED -> Paint(paintTableTextBold).apply { color = COLOR_DANGER_RED }
                    UrgencyLevel.HIGH -> Paint(paintTableTextBold).apply { color = COLOR_WARNING_ORANGE }
                    else -> paintTableText
                }
                val daysText = formatCountdownBadge(countdown)
                canvas.drawText(daysText, colStarts[3] + 5f, yPos + 13f, daysPaint)
                canvas.restore()

                // Draw Col 4: Status (Styled Pill Badge & Clipped)
                val statusColX = colStarts[4]
                val statusColW = colWidths[4]
                val statusBgColor = when (scholarship.status) {
                    ScholarshipStatus.AWARDED -> COLOR_SUCCESS_BG
                    ScholarshipStatus.REJECTED -> COLOR_DANGER_BG
                    ScholarshipStatus.SUBMITTED, ScholarshipStatus.AWAITING_RESULT -> COLOR_STATUS_BG
                    ScholarshipStatus.PREPARING, ScholarshipStatus.IN_PROGRESS -> COLOR_WARNING_BG
                    else -> COLOR_STATUS_BG
                }
                val statusTextColor = when (scholarship.status) {
                    ScholarshipStatus.AWARDED -> COLOR_SUCCESS_GREEN
                    ScholarshipStatus.REJECTED -> COLOR_DANGER_RED
                    ScholarshipStatus.SUBMITTED, ScholarshipStatus.AWAITING_RESULT -> COLOR_NAVY_PRIMARY
                    ScholarshipStatus.PREPARING, ScholarshipStatus.IN_PROGRESS -> COLOR_WARNING_ORANGE
                    else -> COLOR_TEXT_DARK
                }
                val pillPaint = Paint().apply { color = statusBgColor; style = Paint.Style.FILL }
                val pillTextPaint = Paint(paintTableTextBold).apply { color = statusTextColor; textSize = 7.5f }

                val statusPillRect = RectF(statusColX + 4f, yPos + 3f, statusColX + statusColW - 4f, yPos + calculatedRowH - 3f)
                canvas.drawRoundRect(statusPillRect, 3f, 3f, pillPaint)

                canvas.save()
                canvas.clipRect(statusColX + 2f, yPos + 1f, statusColX + statusColW - 2f, yPos + calculatedRowH - 1f)
                canvas.drawText(scholarship.status, statusColX + 8f, yPos + 13f, pillTextPaint)
                canvas.restore()

                // Draw Col 5: Requirements (Clipped)
                canvas.save()
                canvas.clipRect(colStarts[5] + 1f, yPos + 1f, colStarts[5] + colWidths[5] - 1f, yPos + calculatedRowH - 1f)
                val reqStr = if (progress.total > 0) "${progress.completed}/${progress.total} (${progress.percentage.toInt()}%)" else "0 items"
                canvas.drawText(reqStr, colStarts[5] + 5f, yPos + 13f, paintTableText)
                canvas.restore()

                // Draw Col 6: Award (Clipped)
                canvas.save()
                canvas.clipRect(colStarts[6] + 1f, yPos + 1f, colStarts[6] + colWidths[6] - 1f, yPos + calculatedRowH - 1f)
                val awardStr = if (scholarship.amount > 0) "${scholarship.currency}%,.0f".format(scholarship.amount) else "Unstated"
                canvas.drawText(awardStr, colStarts[6] + 5f, yPos + 13f, paintTableTextBold)
                canvas.restore()

                yPos += calculatedRowH
            }

            // Outer border for table
            canvas.drawRect(MARGIN_X, MARGIN_TOP + 26f, pageWidth - MARGIN_X, yPos, paintGridLine)
            yPos += 18f
        }

        // =========================================================================
        // SCHOLARSHIP APPLICATION DETAILS & REQUIREMENTS DOSSIER SECTION
        // Removed per user request.
        // =========================================================================

        drawFooter(canvas, pageNumber, pageWidth, pageHeight)
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
        scholarships: List<Scholarship>
    ) {
        val total = scholarships.size
        val categories = listOf(
            Pair("Not Started", scholarships.count { it.status == ScholarshipStatus.NOT_STARTED }),
            Pair("In Preparation", scholarships.count { it.status == ScholarshipStatus.PREPARING || it.status == ScholarshipStatus.IN_PROGRESS || it.status == ScholarshipStatus.READY_TO_SUBMIT }),
            Pair("Submitted", scholarships.count { it.status == ScholarshipStatus.SUBMITTED || it.status == ScholarshipStatus.ASSESSMENT || it.status == ScholarshipStatus.INTERVIEW }),
            Pair("Awaiting Decision", scholarships.count { it.status == ScholarshipStatus.AWAITING_RESULT }),
            Pair("Awarded", scholarships.count { it.status == ScholarshipStatus.AWARDED }),
            Pair("Rejected", scholarships.count { it.status == ScholarshipStatus.REJECTED })
        )

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

        val barHeight = 11f
        val spacing = 21f

        for (i in categories.indices) {
            val (label, count) = categories[i]
            val barY = y + 4f + (i * spacing)

            // Bar Color
            paintBar.color = when (label) {
                "Awarded" -> COLOR_SUCCESS_GREEN
                "Rejected" -> COLOR_DANGER_RED
                "Submitted", "Awaiting Decision" -> COLOR_NAVY_ACCENT
                "In Preparation" -> COLOR_WARNING_ORANGE
                else -> COLOR_NAVY_HEADER
            }

            // Category Label
            canvas.drawText(label, barStartX - 6f, barY + 8.5f, paintLabel)

            // Horizontal Bar
            val barW = if (maxVal > 0 && total > 0) (count.toFloat() / maxVal) * usableW else 0f
            if (barW > 0f) {
                val barRect = RectF(barStartX, barY, barStartX + barW, barY + barHeight)
                canvas.drawRoundRect(barRect, 2f, 2f, paintBar)
            }

            // Count & Percentage Label
            val pct = if (total > 0) ((count * 100f) / total).toInt() else 0
            val countText = "$count ($pct%)"
            canvas.drawText(countText, barStartX + barW + 5f, barY + 8.5f, paintCount)
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
        outputStream: OutputStream
    ) {
        generateSingleScholarshipPdf(
            student = student,
            calculatedCgpa = calculatedCgpa,
            scholarship = scholarship,
            requirements = requirements,
            timelineEvents = timelineEvents,
            outputStream = outputStream
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
        outputStream: OutputStream
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

        val paintBanner = Paint().apply {
            color = COLOR_NAVY_PRIMARY
            style = Paint.Style.FILL
        }

        var yPos = MARGIN_TOP

        // Top Accent Banner
        canvas.drawRect(MARGIN_X, yPos, pageWidth - MARGIN_X, yPos + 3.5f, paintBanner)
        yPos += 15f

        // Header: Brand & Document Type
        canvas.drawText("GPA WHIZ", MARGIN_X, yPos, paintTitle)
        val brandWidth = paintTitle.measureText("GPA WHIZ")
        canvas.drawText("  •  INDIVIDUAL SCHOLARSHIP DOSSIER", MARGIN_X + brandWidth, yPos, paintSection)

        val genDateStr = sdfDateTime.format(Date())
        val dateWidth = paintSubtitle.measureText(genDateStr)
        canvas.drawText(genDateStr, pageWidth - MARGIN_X - dateWidth, yPos, paintSubtitle)
        yPos += 10f

        canvas.drawLine(MARGIN_X, yPos, pageWidth - MARGIN_X, yPos, paintLine)
        yPos += 10f

        // Student Info Block
        val studentBoxHeight = 32f
        canvas.drawRoundRect(RectF(MARGIN_X, yPos, pageWidth - MARGIN_X, yPos + studentBoxHeight), 3f, 3f, paintCardBg)
        canvas.drawRoundRect(RectF(MARGIN_X, yPos, pageWidth - MARGIN_X, yPos + studentBoxHeight), 3f, 3f, paintLine)

        val cgpaDisplay = if (calculatedCgpa > 0.0) {
            "%.2f / %.1f Scale".format(calculatedCgpa, student.gradingScale)
        } else if (student.targetCgpa > 0.0) {
            "%.2f / %.1f Scale (Target)".format(student.targetCgpa, student.gradingScale)
        } else {
            "0.00 / %.1f Scale".format(student.gradingScale)
        }

        canvas.drawText("APPLICANT: ${student.fullName.ifBlank { "STUDENT" }.uppercase()} (${student.currentLevel})", MARGIN_X + 8f, yPos + 13f, paintBold)
        canvas.drawText("INSTITUTION: ${student.institution.ifBlank { "N/A" }}  |  DEPT: ${student.department.ifBlank { "N/A" }}  |  CGPA: $cgpaDisplay", MARGIN_X + 8f, yPos + 24f, paintTextSmall)
        yPos += studentBoxHeight + 12f

        // Main Scholarship Header Banner
        val headerHeight = 44f
        canvas.drawRoundRect(RectF(MARGIN_X, yPos, pageWidth - MARGIN_X, yPos + headerHeight), 4f, 4f, paintBanner)

        val paintTitleLight = Paint(paintTitle).apply { color = COLOR_WHITE; textSize = 13f }
        val paintSubLight = Paint(paintSubtitle).apply { color = COLOR_STATUS_BG; textSize = 8.5f }

        canvas.drawText(scholarship.name.uppercase(), MARGIN_X + 12f, yPos + 18f, paintTitleLight)
        val orgLine = "Provider: ${scholarship.organization.ifBlank { "Not Specified" }}  •  Status: ${scholarship.status.uppercase()}"
        canvas.drawText(orgLine, MARGIN_X + 12f, yPos + 34f, paintSubLight)

        // Award badge on the right
        val awardText = if (scholarship.amount > 0) "${scholarship.currency}%,.2f".format(scholarship.amount) else "Unstated"
        val awardPaint = Paint(paintTitleLight).apply { textSize = 12f }
        val awardWidth = awardPaint.measureText("Award: $awardText")
        canvas.drawText("Award: $awardText", pageWidth - MARGIN_X - 12f - awardWidth, yPos + 26f, awardPaint)

        yPos += headerHeight + 12f

        // 2-Column Split: Left = Details & Eligibility, Right = Requirements & Timeline
        val halfW = (contentWidth - 12f) / 2f

        // Left Column (Key Parameters & Verification)
        val leftX = MARGIN_X
        val leftBoxHeight = 310f
        canvas.drawRoundRect(RectF(leftX, yPos, leftX + halfW, yPos + leftBoxHeight), 4f, 4f, paintCardBg)
        canvas.drawRoundRect(RectF(leftX, yPos, leftX + halfW, yPos + leftBoxHeight), 4f, 4f, paintLine)

        var ly = yPos + 16f
        canvas.drawText("APPLICATION PARAMETERS & ELIGIBILITY", leftX + 12f, ly, paintSection)
        ly += 16f

        fun drawParam(label: String, value: String) {
            canvas.drawText(label, leftX + 12f, ly, paintBold)
            val valLines = wrapText(value, paintText, halfW - 130f)
            var vy = ly
            for (line in valLines) {
                canvas.drawText(line, leftX + 120f, vy, paintText)
                vy += 11f
            }
            ly = maxOf(ly + 15f, vy + 4f)
        }

        val deadlineTs = scholarship.deadlineDate ?: 0L
        val countdown = ScholarshipCalculationHelper.getDeadlineCountdown(deadlineTs)
        val deadlineFull = if (deadlineTs > 0L) "${sdfDate.format(Date(deadlineTs))} (${formatCountdownBadge(countdown)})" else "Not Specified"
        val appliedDateStr = if (scholarship.dateApplied != null && scholarship.dateApplied > 0) sdfDate.format(Date(scholarship.dateApplied)) else "Not yet applied"
        val feedbackDateStr = if (scholarship.expectedFeedbackDate != null && scholarship.expectedFeedbackDate > 0) sdfDate.format(Date(scholarship.expectedFeedbackDate)) else "Not set"

        drawParam("Deadline Date:", deadlineFull)
        drawParam("Application Status:", scholarship.status)
        drawParam("Date Applied:", appliedDateStr)
        drawParam("Expected Feedback:", feedbackDateStr)

        val isTargetCgpa = calculatedCgpa <= 0.0 && student.targetCgpa > 0.0
        val effectiveStudentCgpa = if (calculatedCgpa > 0.0) calculatedCgpa else student.targetCgpa
        val eligibility = ScholarshipCalculationHelper.checkEligibility(effectiveStudentCgpa, student.gradingScale, scholarship, isTargetCgpa)
        drawParam("Min CGPA Req:", if (scholarship.minCgpa != null && scholarship.minCgpa > 0) "%.2f / %.1f".format(scholarship.minCgpa, student.gradingScale) else "None")
        drawParam("Eligibility Result:", "${eligibility.badgeText} (${eligibility.description})")

        if (scholarship.awardAmount != null && scholarship.awardAmount > 0) {
            val awStr = "${scholarship.awardCurrency ?: scholarship.currency}%,.2f".format(scholarship.awardAmount)
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
        canvas.drawText("REQUIREMENTS CHECKLIST (${reqProgress.completed}/${reqProgress.total} - ${reqProgress.percentage.toInt()}%)", rightX + 12f, ry, paintSection)
        ry += 14f

        if (requirements.isEmpty()) {
            canvas.drawText("No specific requirements documented.", rightX + 12f, ry, paintSubtitle)
            ry += 16f
        } else {
            for (req in requirements.take(6)) {
                val isDone = req.status == RequirementStatus.COMPLETED || req.status == RequirementStatus.SUBMITTED
                val marker = if (isDone) "[✓]" else "[○]"
                val mPaint = if (isDone) Paint(paintBold).apply { color = COLOR_SUCCESS_GREEN } else paintSubtitle

                canvas.drawText("$marker ${req.title.take(30)}", rightX + 12f, ry, mPaint)
                canvas.drawText("${req.category} | ${req.status}", rightX + halfW - 120f, ry, paintTextSmall)
                ry += 13f
            }
        }

        ry += 8f
        canvas.drawLine(rightX + 12f, ry, rightX + halfW - 12f, ry, paintLine)
        ry += 14f

        canvas.drawText("TIMELINE & AUDIT LOGS", rightX + 12f, ry, paintSection)
        ry += 14f

        if (timelineEvents.isEmpty()) {
            canvas.drawText("No timeline events logged yet.", rightX + 12f, ry, paintSubtitle)
        } else {
            for (ev in timelineEvents.take(4)) {
                val dateStr = sdfDate.format(Date(ev.date))
                canvas.drawText("• $dateStr: ${ev.title}", rightX + 12f, ry, paintBold)
                if (ev.description.isNotBlank()) {
                    canvas.drawText("  ${ev.description.take(45)}", rightX + 12f, ry + 10f, paintTextSmall)
                    ry += 10f
                }
                ry += 13f
            }
        }

        drawFooter(canvas, pageNumber, pageWidth, pageHeight)
        pdfDocument.finishPage(page)

        try {
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Helper to format human-friendly countdown badge
     */
    private fun formatCountdownBadge(countdown: DeadlineCountdownResult): String {
        return when {
            countdown.daysRemaining > 1 -> "${countdown.daysRemaining} days left"
            countdown.daysRemaining == 1L -> "Tomorrow"
            countdown.daysRemaining == 0L -> "Today"
            countdown.daysRemaining == -1L -> "Passed (1d ago)"
            countdown.daysRemaining < -1L -> "Passed (${-countdown.daysRemaining}d ago)"
            else -> "No deadline"
        }
    }

    /**
     * Draw standard page footer
     */
    private fun drawFooter(canvas: Canvas, pageNumber: Int, pageWidth: Int, pageHeight: Int) {
        val paintFooter = Paint().apply {
            color = COLOR_TEXT_MUTED
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val footerY = pageHeight - MARGIN_BOTTOM + 12f
        val footerLeft = "GPA Whiz • Scholarship Tracker Report | Generated ${sdfDate.format(Date())}"
        canvas.drawText(footerLeft, MARGIN_X, footerY, paintFooter)

        val pageStr = "Page $pageNumber"
        val pageW = paintFooter.measureText(pageStr)
        canvas.drawText(pageStr, pageWidth - MARGIN_X - pageW, footerY, paintFooter)
    }

    /**
     * Utility to break a string into multiple lines that fit within a specified pixel width.
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = StringBuilder(candidate)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                } else {
                    lines.add(word)
                    currentLine = StringBuilder()
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return if (lines.isEmpty()) listOf(text) else lines
    }
}
