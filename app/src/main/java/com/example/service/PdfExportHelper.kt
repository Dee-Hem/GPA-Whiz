package com.example.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.Course
import com.example.data.Semester
import com.example.data.StudentProfile
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

    fun generateTranscriptPdf(
        student: StudentProfile,
        semesters: List<Semester>,
        allCourses: List<Course>,
        outputStream: OutputStream
    ) {
        val pdfDocument = PdfDocument()
        
        // Define A4 size coordinates
        val pageWidth = 595
        val pageHeight = 842
        
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        
        // Paints
        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }
        
        val paintBold = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val paintTitle = Paint().apply {
            color = Color.parseColor("#1B5E20") // Rich Green Header
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val paintSubtitle = Paint().apply {
            color = Color.parseColor("#424242")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        val paintLine = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val paintHeaderBg = Paint().apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
        }

        var yPos = 35f

        // Draw Header Border Banner
        val paintBanner = Paint().apply {
            color = Color.parseColor("#0F9D58") // Emerald Green Banner
            style = Paint.Style.FILL
        }
        canvas.drawRect(25f, yPos, pageWidth - 25f, yPos + 6f, paintBanner)
        yPos += 24f

        // Title
        canvas.drawText("GPA WHIZ ACADEMIC TRANSCRIPT PLAN", 25f, yPos, paintTitle)
        yPos += 14f
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
        canvas.drawText("Generated: ${sdf.format(Date())} - Playable Offline Backup", 25f, yPos, paintSubtitle)
        yPos += 20f

        canvas.drawLine(25f, yPos, pageWidth - 25f, yPos, paintLine)
        yPos += 20f

        // Student Profile Header Section
        canvas.drawText("STUDENT METADATA PROFILE", 25f, yPos, paintBold)
        yPos += 15f

        // Two Column Info Grid
        val xCol1 = 25f
        val xCol2 = 300f
        
        canvas.drawText("Full Name: ${student.fullName.ifEmpty { "N/A" }}", xCol1, yPos, paintText)
        canvas.drawText("Institution: ${student.institution.ifEmpty { "N/A" }}", xCol2, yPos, paintText)
        yPos += 15f
        
        canvas.drawText("Matric/Reg No: ${student.matricNo.ifEmpty { "N/A" }}", xCol1, yPos, paintText)
        canvas.drawText("Faculty: ${student.faculty.ifEmpty { "N/A" }}", xCol2, yPos, paintText)
        yPos += 15f
        
        canvas.drawText("Department: ${student.department.ifEmpty { "N/A" }}", xCol1, yPos, paintText)
        canvas.drawText("Graduation Year: ${student.graduationYear.ifEmpty { "N/A" }}", xCol2, yPos, paintText)
        yPos += 15f
        
        canvas.drawText("Current Level: ${student.currentLevel}", xCol1, yPos, paintText)
        canvas.drawText("Grading Scale: ${student.gradingScale} Scale Map", xCol2, yPos, paintText)
        yPos += 25f

        canvas.drawLine(25f, yPos, pageWidth - 25f, yPos, paintLine)
        yPos += 20f

        // OVERALL STANDINGS
        val cgpa = GpaCalcService.calculateCgpa(semesters, allCourses)
        val totalUnits = GpaCalcService.calculateTotalUnits(allCourses)
        val standingHex = GpaCalcService.getColorForCgpa(cgpa, student.gradingScale)
        val standingLabel = GpaCalcService.getStandingForCgpa(cgpa, student.gradingScale)

        canvas.drawText("OVERALL CUMULATIVE STANDING", 25f, yPos, paintBold)
        yPos += 18f

        // Overall badge card style
        val badgeColor = Paint().apply {
            color = Color.parseColor(standingHex)
            style = Paint.Style.FILL
        }
        canvas.drawRect(25f, yPos, pageWidth - 25f, yPos + 40f, paintHeaderBg)
        canvas.drawRect(25f, yPos, 35f, yPos + 40f, badgeColor) // Vertical Left Side colored tag

        canvas.drawText("CUMULATIVE GPA (CGPA): %.2f / %.2f".format(cgpa, student.gradingScale), 50f, yPos + 16f, paintBold)
        canvas.drawText("Classification: $standingLabel | Registered Credit Units: $totalUnits", 50f, yPos + 30f, paintText)
        yPos += 60f

        canvas.drawLine(25f, yPos, pageWidth - 25f, yPos, paintLine)
        yPos += 20f

        // SEMESTERS & COURSE GRID
        canvas.drawText("COURSE-BY-COURSE ACADEMIC SCHEDULING", 25f, yPos, paintBold)
        yPos += 15f

        if (semesters.isEmpty()) {
            canvas.drawText("No semester academic records found to print.", 25f, yPos, paintText)
        } else {
            // Draw Semesters
            for (semester in semesters) {
                // Check if page overflow is imminent (leaving some safe margin at bottom)
                if (yPos > pageHeight - 120f) {
                    pdfDocument.finishPage(page)
                    val nextPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create())
                    val nextCanvas = nextPage.canvas
                    // Reset position and print a little header
                    yPos = 40f
                    nextCanvas.drawText("CONTINUED REPORT PLAN FOR ${student.fullName.uppercase(Locale.getDefault())}", 25f, yPos, paintBold)
                    yPos += 10f
                    nextCanvas.drawLine(25f, yPos, pageWidth - 25f, yPos, paintLine)
                    yPos += 20f
                    drawSemesterTable(nextCanvas, semester, allCourses, paintBold, paintText, paintLine, paintHeaderBg, xCol1, pageWidth, refY = yPos).let {
                        yPos = it
                    }
                } else {
                    drawSemesterTable(canvas, semester, allCourses, paintBold, paintText, paintLine, paintHeaderBg, xCol1, pageWidth, refY = yPos).let {
                        yPos = it
                    }
                }
                yPos += 15f
            }
        }

        // Add a signature block at bottom if space permits
        if (yPos > pageHeight - 80f) {
            pdfDocument.finishPage(page)
            val finalPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3).create())
            val finalCanvas = finalPage.canvas
            yPos = 50f
            drawFooterSign(finalCanvas, yPos, paintLine, paintText, paintBold, pageWidth)
        } else {
            drawFooterSign(canvas, yPos, paintLine, paintText, paintBold, pageWidth)
        }

        pdfDocument.finishPage(page)

        try {
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawSemesterTable(
        canvas: Canvas,
        semester: Semester,
        allCourses: List<Course>,
        paintBold: Paint,
        paintText: Paint,
        paintLine: Paint,
        paintHeaderBg: Paint,
        xStart: Float,
        pageWidth: Int,
        refY: Float
    ): Float {
        var currentY = refY
        val semCourses = allCourses.filter { it.semesterId == semester.id }
        val sgpa = GpaCalcService.calculateSgpa(semCourses, semester.gradingScale)
        val activeSemCourses = semCourses.filter { !it.grade.equals("Awaiting Grade", ignoreCase = true) && !it.grade.equals("AR", ignoreCase = true) }
        val totalSemUnits = activeSemCourses.sumOf { it.units }

        // Semester Info Header Bar
        canvas.drawRect(xStart, currentY, pageWidth - 25f, currentY + 20f, paintHeaderBg)
        canvas.drawText("${semester.name.uppercase()} (Scale: ${semester.gradingScale})", xStart + 8f, currentY + 14f, paintBold)
        canvas.drawText("Semester GPA: %.2f  |  Total Units: %d".format(sgpa, totalSemUnits), pageWidth - 240f, currentY + 14f, paintBold)
        currentY += 21f

        // Table column headers
        canvas.drawRect(xStart, currentY, pageWidth - 25f, currentY + 15f, paintHeaderBg)
        canvas.drawText("CODE", xStart + 5f, currentY + 12f, paintBold)
        canvas.drawText("COURSE TITLE", xStart + 80f, currentY + 12f, paintBold)
        canvas.drawText("UNITS", xStart + 360f, currentY + 12f, paintBold)
        canvas.drawText("SCORE", xStart + 420f, currentY + 12f, paintBold)
        canvas.drawText("GRADE", xStart + 480f, currentY + 12f, paintBold)
        canvas.drawText("STATUS", xStart + 530f, currentY + 12f, paintBold)
        currentY += 16f

        canvas.drawLine(xStart, currentY, pageWidth - 25f, currentY, paintLine)

        if (semCourses.isEmpty()) {
            canvas.drawText("No courses registered in this semester.", xStart + 5f, currentY + 15f, paintText)
            currentY += 20f
        } else {
            for (course in semCourses) {
                canvas.drawText(course.code, xStart + 5f, currentY + 14f, paintText)
                
                // Truncate course title if too long
                val maxChars = 40
                val displayTitle = if (course.title.length > maxChars) {
                    course.title.take(maxChars) + "..."
                } else course.title
                canvas.drawText(displayTitle, xStart + 80f, currentY + 14f, paintText)
                
                canvas.drawText("${course.units}", xStart + 360f, currentY + 14f, paintText)
                canvas.drawText("${course.score}", xStart + 420f, currentY + 14f, paintText)
                
                val isAwaiting = course.grade.equals("Awaiting Grade", ignoreCase = true) || course.grade.uppercase() == "AR"
                val displayGrade = if (isAwaiting) "AR" else course.grade
                canvas.drawText(displayGrade, xStart + 480f, currentY + 14f, paintBold)
                
                val statusText = if (isAwaiting) "Pending" else if (course.isCarryOver) "CarryOver" else "Passed"
                val paintStatus = Paint(paintBold).apply {
                    color = when {
                        isAwaiting -> Color.GRAY
                        course.isCarryOver -> Color.RED
                        else -> Color.parseColor("#4CAF50")
                    }
                }
                canvas.drawText(statusText, xStart + 530f, currentY + 14f, paintStatus)
                currentY += 18f
                
                canvas.drawLine(xStart, currentY, pageWidth - 25f, currentY, paintLine)
            }
        }
        return currentY
    }

    private fun drawFooterSign(
        canvas: Canvas,
        startY: Float,
        paintLine: Paint,
        paintText: Paint,
        paintBold: Paint,
        pageWidth: Int
    ) {
        var currentY = startY + 25f
        canvas.drawLine(25f, currentY, pageWidth - 25f, currentY, paintLine)
        currentY += 20f
        
        canvas.drawText("GPA Whiz Verification Code: 0X-OFFLINE-PLAN-SECURE", 25f, currentY, paintText)
        canvas.drawText("Student Plan Signature: _______________________", pageWidth - 320f, currentY, paintBold)
        currentY += 15f
        canvas.drawText("Nigerian Higher Education Grade Computation Standard", 25f, currentY, paintText)
    }
}
