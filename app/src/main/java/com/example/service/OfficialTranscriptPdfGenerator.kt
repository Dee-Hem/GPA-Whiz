package com.example.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.Course
import com.example.data.Semester
import com.example.data.StudentProfile
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OfficialTranscriptPdfGenerator {

    private val colorBrandBlue = Color.parseColor("#2B5896")
    private val colorLightGrey = Color.parseColor("#F1F5F9")
    private val colorTextGrey = Color.parseColor("#64748B")
    private val colorBorderLight = Color.parseColor("#E2E8F0")

    fun generateTranscriptPdf(
        student: StudentProfile,
        semesters: List<Semester>,
        allCourses: List<Course>,
        outputStream: OutputStream
    ) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val typefaceBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val typefaceRegular = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        // General Paints
        val paintHeaderTitle = Paint().apply {
            color = colorBrandBlue
            textSize = 24f
            typeface = typefaceBold
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val paintHeaderSub = Paint().apply {
            color = colorTextGrey
            textSize = 9f
            typeface = typefaceBold
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.05f
        }

        val paintLabel = Paint().apply {
            color = colorBrandBlue
            textSize = 7f
            typeface = typefaceBold
            isAntiAlias = true
        }

        val paintValueBold = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = typefaceBold
            isAntiAlias = true
        }

        val paintValueSmallBold = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = typefaceBold
            isAntiAlias = true
        }

        val tracker = PdfPageTracker(pdfDocument, student)

        // --- PART A: HEADER ---
        tracker.yPos = 70f
        tracker.canvas.drawText("ACADEMIC TRANSCRIPT", pageWidth / 2f, tracker.yPos, paintHeaderTitle)
        tracker.yPos += 16f
        tracker.canvas.drawText("OFFICIAL PERFORMANCE STATEMENT", pageWidth / 2f, tracker.yPos, paintHeaderSub)
        
        tracker.yPos += 20f
        val paintMainRule = Paint().apply { color = colorBrandBlue; strokeWidth = 3f }
        tracker.canvas.drawLine(40f, tracker.yPos, pageWidth - 40f, tracker.yPos, paintMainRule)
        tracker.yPos += 30f

        // --- PART B: STUDENT INFO & CARDS ---
        val leftMargin = 40f
        val rightMargin = pageWidth - 40f
        
        // Name and Matric No and Institution (stacked on left)
        var infoY = tracker.yPos
        val nameMaxWidth = 160f
        tracker.canvas.drawText("STUDENT NAME", leftMargin, infoY, paintLabel)
        infoY += 14f
        infoY = drawWrappedText(tracker.canvas, student.fullName.uppercase().ifEmpty { "STUDENT NAME" }, leftMargin, infoY, paintValueBold.apply { textSize = 12f }, nameMaxWidth)
        infoY += 6f
        
        tracker.canvas.drawText("MATRIC NUMBER", leftMargin, infoY, paintLabel)
        infoY += 14f
        tracker.canvas.drawText(student.matricNo.uppercase().ifEmpty { "MATRIC NO" }, leftMargin, infoY, paintValueSmallBold)
        infoY += 18f
        
        tracker.canvas.drawText("UNIVERSITY / SCHOOL", leftMargin, infoY, paintLabel)
        infoY += 14f
        tracker.canvas.drawText(student.institution.ifEmpty { "UNIVERSITY OF ILORIN" }.uppercase(), leftMargin, infoY, paintValueSmallBold.apply { color = Color.parseColor("#475569"); textSize = 9f })

        // 3 Cards on right for Faculty, Department and Level
        val cardWidth = 110f
        val cardHeight = 65f
        val cardPaint = Paint().apply { color = colorLightGrey; style = Paint.Style.FILL }
        val cardBorderPaint = Paint().apply { color = colorBorderLight; style = Paint.Style.STROKE; strokeWidth = 1f }

        val levelCardX = rightMargin - cardWidth
        val deptCardX = levelCardX - cardWidth - 8f
        val facultyCardX = deptCardX - cardWidth - 8f
        val cardsTop = 130f

        // Move the cards slightly or adjust their layout to avoid overlap with name if name is long
        // If Name is very long, it might still overlap in a 2-column layout. 
        // Let's use a more robust layout where student info is more condensed.

        // Faculty Card
        tracker.canvas.drawRoundRect(RectF(facultyCardX, cardsTop, facultyCardX + cardWidth, cardsTop + cardHeight), 8f, 8f, cardPaint)
        tracker.canvas.drawRoundRect(RectF(facultyCardX, cardsTop, facultyCardX + cardWidth, cardsTop + cardHeight), 8f, 8f, cardBorderPaint)
        tracker.canvas.drawText("FACULTY", facultyCardX + 10f, cardsTop + 14f, paintLabel)
        val facultyName = student.faculty.ifEmpty { "ACADEMIC FACULTY" }
        drawWrappedText(tracker.canvas, facultyName, facultyCardX + 10f, cardsTop + 30f, paintValueSmallBold.apply { textSize = 8f }, cardWidth - 18f)

        // Dept Card
        tracker.canvas.drawRoundRect(RectF(deptCardX, cardsTop, deptCardX + cardWidth, cardsTop + cardHeight), 8f, 8f, cardPaint)
        tracker.canvas.drawRoundRect(RectF(deptCardX, cardsTop, deptCardX + cardWidth, cardsTop + cardHeight), 8f, 8f, cardBorderPaint)
        tracker.canvas.drawText("DEPARTMENT", deptCardX + 10f, cardsTop + 14f, paintLabel)
        val deptName = student.department.ifEmpty { "ACADEMIC DEPARTMENT" }
        drawWrappedText(tracker.canvas, deptName, deptCardX + 10f, cardsTop + 30f, paintValueSmallBold.apply { textSize = 8f }, cardWidth - 18f)

        // Level Card
        tracker.canvas.drawRoundRect(RectF(levelCardX, cardsTop, levelCardX + cardWidth, cardsTop + cardHeight), 8f, 8f, cardPaint)
        tracker.canvas.drawRoundRect(RectF(levelCardX, cardsTop, levelCardX + cardWidth, cardsTop + cardHeight), 8f, 8f, cardBorderPaint)
        tracker.canvas.drawText("LEVEL", levelCardX + 10f, cardsTop + 14f, paintLabel)
        tracker.canvas.drawText(student.currentLevel.ifEmpty { "Level" }, levelCardX + 10f, cardsTop + 32f, paintValueSmallBold.apply { textSize = 11f })

        tracker.yPos = 245f

        // --- PART C: SUMMARY BAR ---
        val cgpa = GpaCalcService.calculateCgpa(semesters, allCourses)
        val statsRect = RectF(leftMargin, tracker.yPos, rightMargin, tracker.yPos + 85f)
        tracker.canvas.drawRoundRect(statsRect, 14f, 14f, Paint().apply { color = Color.WHITE; style = Paint.Style.FILL })
        tracker.canvas.drawRoundRect(statsRect, 14f, 14f, Paint().apply { color = colorBorderLight; style = Paint.Style.STROKE; strokeWidth = 1f })
        
        val colWidth = statsRect.width() / 4f
        
        // Cumulative GPA
        tracker.canvas.drawText("CUMULATIVE GPA", leftMargin + 20f, tracker.yPos + 22f, paintLabel)
        tracker.canvas.drawText(String.format("%.2f", cgpa), leftMargin + 20f, tracker.yPos + 58f, paintValueBold.apply { textSize = 32f; color = colorBrandBlue })
        tracker.canvas.drawText("Scale: ${student.gradingScale}", leftMargin + 20f, tracker.yPos + 74f, paintLabel.apply { color = colorTextGrey })

        // Standing
        tracker.canvas.drawText("STANDING", leftMargin + colWidth + 20f, tracker.yPos + 22f, paintLabel)
        val classification = GpaCalcService.getClassification(cgpa, student.gradingScale)
        val pillColor = Color.parseColor(GpaCalcService.getColorForCgpa(cgpa, student.gradingScale))
        val pillRect = RectF(leftMargin + colWidth + 20f, tracker.yPos + 38f, leftMargin + colWidth + 115f, tracker.yPos + 60f)
        tracker.canvas.drawRoundRect(pillRect, 11f, 11f, Paint().apply { color = pillColor })
        tracker.canvas.drawText(classification.uppercase(), pillRect.centerX(), pillRect.centerY() + 3.5f, paintLabel.apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = 6.5f })

        // Total Credits
        tracker.canvas.drawText("TOTAL CREDITS", leftMargin + 2 * colWidth + 20f, tracker.yPos + 22f, paintLabel)
        tracker.canvas.drawText("${GpaCalcService.calculateTotalUnits(allCourses)}", leftMargin + 2 * colWidth + 20f, tracker.yPos + 58f, paintValueBold.apply { textSize = 28f; color = Color.BLACK })

        // Courses
        tracker.canvas.drawText("COURSES", leftMargin + 3 * colWidth + 20f, tracker.yPos + 22f, paintLabel)
        tracker.canvas.drawText("${allCourses.size}", leftMargin + 3 * colWidth + 20f, tracker.yPos + 58f, paintValueBold.apply { textSize = 28f; color = Color.BLACK })

        // Dividers
        val dividerPaint = Paint().apply { color = colorBorderLight; strokeWidth = 1f }
        tracker.canvas.drawLine(leftMargin + colWidth, tracker.yPos + 20f, leftMargin + colWidth, tracker.yPos + 65f, dividerPaint)
        tracker.canvas.drawLine(leftMargin + 2 * colWidth, tracker.yPos + 20f, leftMargin + 2 * colWidth, tracker.yPos + 65f, dividerPaint)
        tracker.canvas.drawLine(leftMargin + 3 * colWidth, tracker.yPos + 20f, leftMargin + 3 * colWidth, tracker.yPos + 65f, dividerPaint)

        tracker.yPos += 135f

        // --- PART D: ACADEMIC BREAKDOWN ---
        val breakdownTitlePaint = Paint().apply { color = Color.BLACK; textSize = 13f; typeface = typefaceBold; isAntiAlias = true }
        tracker.canvas.drawText("DETAILED ACADEMIC BREAKDOWN", leftMargin + 24f, tracker.yPos, breakdownTitlePaint)
        
        // Icon (Draw small bullet icon)
        val iconPaint = Paint().apply { color = colorBrandBlue; style = Paint.Style.FILL }
        tracker.canvas.drawCircle(leftMargin + 10f, tracker.yPos - 4f, 6f, iconPaint)
        
        tracker.yPos += 10f
        tracker.canvas.drawLine(leftMargin, tracker.yPos, rightMargin, tracker.yPos, Paint().apply { color = colorBrandBlue; strokeWidth = 1.5f })
        tracker.yPos += 35f

        // --- PART E: SEMESTERS ---
        val semHeaderPaint = Paint().apply { color = Color.parseColor("#1E293B"); textSize = 9f; typeface = typefaceBold; isAntiAlias = true }
        val courseCodePaint = Paint().apply { color = Color.parseColor("#1E293B"); textSize = 9.5f; typeface = typefaceBold; isAntiAlias = true }
        val courseTitlePaint = Paint().apply { color = Color.parseColor("#475569"); textSize = 8f; typeface = typefaceRegular; isAntiAlias = true }
        val courseInfoPaint = Paint().apply { color = Color.BLACK; textSize = 9f; isAntiAlias = true }
        val tableHeadPaint = Paint().apply { color = colorTextGrey; textSize = 8f; typeface = typefaceBold; isAntiAlias = true }

        for (semester in semesters) {
            val semCourses = allCourses.filter { it.semesterId == semester.id }
            val sgpa = GpaCalcService.calculateSgpa(semCourses, semester.gradingScale)
            
            tracker.checkAndPageBreak(120f)
            
            // Semester Title Header Bar
            tracker.canvas.drawRoundRect(RectF(leftMargin, tracker.yPos - 22f, rightMargin, tracker.yPos + 6f), 10f, 10f, Paint().apply { color = Color.parseColor("#F8FAFC"); style = Paint.Style.FILL })
            tracker.canvas.drawRoundRect(RectF(leftMargin, tracker.yPos - 22f, rightMargin, tracker.yPos + 6f), 10f, 10f, cardBorderPaint)
            
            tracker.canvas.drawText("${semester.name.uppercase()}", leftMargin + 15f, tracker.yPos - 4f, semHeaderPaint)
            tracker.canvas.drawText("SEM GPA: ${String.format("%.2f", sgpa)}", rightMargin - 90f, tracker.yPos - 4f, semHeaderPaint)

            tracker.yPos += 24f
            tracker.canvas.drawText("COURSE CODE & TITLE", leftMargin + 15f, tracker.yPos, tableHeadPaint)
            tracker.canvas.drawText("CREDITS", rightMargin - 130f, tracker.yPos, tableHeadPaint.apply { textAlign = Paint.Align.CENTER })
            tracker.canvas.drawText("GRADE", rightMargin - 50f, tracker.yPos, tableHeadPaint.apply { textAlign = Paint.Align.CENTER })
            
            tracker.yPos += 8f
            tracker.canvas.drawLine(leftMargin + 15f, tracker.yPos, rightMargin - 15f, tracker.yPos, Paint().apply { color = colorBorderLight; strokeWidth = 0.8f })
            tracker.yPos += 25f

            for (course in semCourses) {
                val courseTitleMaxWidth = 330f
                tracker.checkAndPageBreak(55f)
                
                // Course Code and Title (Stacked)
                tracker.canvas.drawText(course.code, leftMargin + 15f, tracker.yPos, courseCodePaint)
                val courseTitleY = drawWrappedText(tracker.canvas, course.title, leftMargin + 15f, tracker.yPos + 11f, courseTitlePaint, courseTitleMaxWidth)
                
                tracker.canvas.drawText("${course.units}", rightMargin - 130f, tracker.yPos + 4f, courseInfoPaint.apply { textAlign = Paint.Align.CENTER; typeface = typefaceRegular })
                
                val isAwaiting = course.grade.equals("Awaiting Grade", ignoreCase = true) || course.grade.uppercase() == "AR"
                val gColor = when {
                    isAwaiting -> Color.GRAY
                    course.grade == "A" -> Color.parseColor("#0F9D58")
                    course.grade == "B" -> Color.parseColor("#1A73E8")
                    course.grade == "C" -> Color.parseColor("#F4B400")
                    course.grade == "D" -> Color.parseColor("#FB923C")
                    else -> Color.parseColor("#DB4437")
                }
                
                val gradeCircleX = rightMargin - 50f
                val gradeCircleY = tracker.yPos + 1f
                tracker.canvas.drawCircle(gradeCircleX, gradeCircleY, 14f, Paint().apply { color = gColor })
                
                val gText = if (isAwaiting) "AR" else course.grade
                tracker.canvas.drawText(gText, gradeCircleX, gradeCircleY + 4f, Paint().apply { color = Color.WHITE; textSize = 10f; typeface = typefaceBold; textAlign = Paint.Align.CENTER })
                
                tracker.yPos = Math.max(tracker.yPos + 38f, courseTitleY + 12f)
            }
            
            tracker.yPos += 20f
        }

        // Footer
        val footerPaint = Paint().apply { color = colorTextGrey; textSize = 9f; typeface = typefaceBold; textAlign = Paint.Align.CENTER; isAntiAlias = true; letterSpacing = 0.05f }
        val sdf = SimpleDateFormat("M/d/yyyy 'AT' h:mm:ss a", Locale.US)
        val footerText = "DOCUMENT CERTIFIED VIA GPA WHIZ ENGINE • ISSUED ${sdf.format(Date())}"
        tracker.canvas.drawRect(0f, pageHeight - 50f, pageWidth.toFloat(), pageHeight.toFloat(), Paint().apply { color = colorLightGrey; alpha = 100 })
        tracker.canvas.drawText(footerText, pageWidth / 2f, pageHeight - 25f, footerPaint)

        tracker.finish()

        try {
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint, maxWidth: Float): Float {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)

        var currentY = y
        for (line in lines) {
            canvas.drawText(line, x, currentY, paint)
            currentY += paint.textSize + 2.5f
        }
        return currentY
    }

    private class PdfPageTracker(
        private val pdfDocument: PdfDocument,
        private val student: StudentProfile
    ) {
        var currentPageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        var yPos = 56f

        fun checkAndPageBreak(heightNeeded: Float) {
            if (yPos + heightNeeded > 780f) {
                pdfDocument.finishPage(currentPage)
                currentPageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                yPos = 70f
                
                // Page Indicator
                val pPaint = Paint().apply { color = Color.LTGRAY; textSize = 8f; textAlign = Paint.Align.RIGHT; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC) }
                canvas.drawText("Page $currentPageNumber", 555f, 35f, pPaint)
            }
        }

        fun finish() {
            pdfDocument.finishPage(currentPage)
        }
    }
}
