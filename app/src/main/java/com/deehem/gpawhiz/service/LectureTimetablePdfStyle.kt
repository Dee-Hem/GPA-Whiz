package com.deehem.gpawhiz.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.deehem.gpawhiz.data.StudentProfile
import java.util.Date
import java.util.Locale
import kotlin.math.max

object LectureTimetablePdfStyle {
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842
    const val LANDSCAPE_WIDTH = 842
    const val LANDSCAPE_HEIGHT = 595
    const val MARGIN = 40f
    val brandBlue = Color.parseColor("#2B5896")
    val lightGrey = Color.parseColor("#F1F5F9")
    val textGrey = Color.parseColor("#64748B")
    val borderLight = Color.parseColor("#E2E8F0")
    val darkText = Color.parseColor("#1E293B")
    val regularTypeface: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    val boldTypeface: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun drawHeader(
        canvas: Canvas,
        student: StudentProfile,
        semesterOrSession: String,
        title: String,
        pageWidth: Int,
        summary: String? = null
    ): Float {
        val right = pageWidth - MARGIN
        val titlePaint = paint(19f, brandBlue, bold = true).apply { textAlign = Paint.Align.CENTER }
        val subtitlePaint = paint(8.5f, textGrey, bold = true).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(title, pageWidth / 2f, 45f, titlePaint)
        canvas.drawText(semesterOrSession.uppercase(Locale.getDefault()), pageWidth / 2f, 64f, subtitlePaint)
        canvas.drawLine(MARGIN, 80f, right, 80f, Paint().apply { color = brandBlue; strokeWidth = 3f })

        val contentLeft = MARGIN + 10f
        val contentWidth = right - contentLeft - 10f
        val columnWidth = contentWidth / 3f
        val fields = listOf(
            Triple("STUDENT NAME", student.fullName.ifEmpty { "STUDENT NAME" }.uppercase(), 0),
            Triple("UNIVERSITY / SCHOOL", student.institution.ifEmpty { "UNIVERSITY OF ILORIN" }.uppercase(), 1),
            Triple("MATRIC NUMBER", student.matricNo.ifEmpty { "MATRIC NO" }.uppercase(), 2),
            Triple("FACULTY", student.faculty.ifEmpty { "ACADEMIC FACULTY" }.uppercase(), 0),
            Triple("DEPARTMENT", student.department.ifEmpty { "ACADEMIC DEPARTMENT" }.uppercase(), 1),
            Triple("LEVEL", student.currentLevel.ifEmpty { "LEVEL" }.uppercase(), 2)
        )

        val labelPaint = paint(7f, brandBlue, bold = true)
        val valuePaint = paint(8.5f, darkText, bold = true)
        val lineHeight = 11f
        val rowHeights = studentHeaderRowHeights(fields.map { it.second }, columnWidth, valuePaint)
        val cardBottom = 94f + 10f + rowHeights.sum()
        val card = RectF(MARGIN, 94f, right, cardBottom)
        canvas.drawRoundRect(card, 8f, 8f, Paint().apply { color = lightGrey })
        canvas.drawRoundRect(card, 8f, 8f, Paint().apply {
            color = borderLight
            strokeWidth = 1f
            style = Paint.Style.STROKE
        })

        var rowTop = 99f
        for ((index, field) in fields.withIndex()) {
            val row = index / 3
            val column = field.third
            val x = contentLeft + column * columnWidth
            val labelY = rowTop + 8f
            canvas.drawText(field.first, x, labelY, labelPaint)
            val lines = wrapPdfText(field.second, columnWidth - 12f, valuePaint.textSize) { text, size ->
                measureText(valuePaint, text, size)
            }
            lines.forEachIndexed { lineIndex, line ->
                canvas.drawText(line, x, labelY + 12f + lineIndex * lineHeight, valuePaint)
            }
            if (column == 2) rowTop += rowHeights[row]
        }

        val summaryLines = summary?.let { wrapPdfText(it, right - MARGIN * 2f, 8f) { text, size ->
            measureText(paint(8f, textGrey), text, size)
        } }.orEmpty()
        val summaryPaint = paint(8f, textGrey)
        summaryLines.forEachIndexed { index, line ->
            canvas.drawText(line, MARGIN, cardBottom + 14f + index * 10f, summaryPaint)
        }
        return headerBottom(student, pageWidth, summary)
    }

    fun measureHeaderBottom(student: StudentProfile, pageWidth: Int, summary: String? = null): Float =
        headerBottom(student, pageWidth, summary)

    private fun headerBottom(student: StudentProfile, pageWidth: Int, summary: String?): Float {
        val fields = listOf(
            student.fullName.ifEmpty { "STUDENT NAME" }.uppercase(),
            student.institution.ifEmpty { "UNIVERSITY OF ILORIN" }.uppercase(),
            student.matricNo.ifEmpty { "MATRIC NO" }.uppercase(),
            student.faculty.ifEmpty { "ACADEMIC FACULTY" }.uppercase(),
            student.department.ifEmpty { "ACADEMIC DEPARTMENT" }.uppercase(),
            student.currentLevel.ifEmpty { "LEVEL" }.uppercase()
        )
        val contentWidth = pageWidth - 2 * MARGIN - 20f
        val columnWidth = contentWidth / 3f
        val valuePaint = paint(8.5f, darkText, bold = true)
        val cardBottom = 94f + 10f + studentHeaderRowHeights(fields, columnWidth, valuePaint).sum()
        val summaryLines = summary?.let { text ->
            val width = pageWidth - 2 * MARGIN
            wrapPdfText(text, width, 8f) { value, size -> measureText(paint(8f, textGrey), value, size) }
        }.orEmpty()
        return cardBottom + if (summaryLines.isEmpty()) 15f else 14f + summaryLines.size * 10f + 8f
    }

    private fun studentHeaderRowHeights(
        values: List<String>,
        columnWidth: Float,
        paint: Paint
    ): List<Float> = values.chunked(3).map { row ->
        row.maxOf { value ->
            22f + wrapPdfText(value, columnWidth - 12f, paint.textSize) { text, size ->
                measureText(paint, text, size)
            }.size * 11f
        } + 8f
    }

    fun drawFooter(
        canvas: Canvas,
        pageNumber: Int,
        totalPages: Int,
        pageWidth: Int,
        pageHeight: Int,
        generatedAt: Date,
        use24HourTime: Boolean = false
    ) {
        val lineY = pageHeight - 34f
        canvas.drawLine(MARGIN, lineY, pageWidth - MARGIN, lineY, Paint().apply {
            color = borderLight
            strokeWidth = 0.8f
        })

        val footerPaint = paint(7.5f, textGrey)
        val generatedDate = formatPdfFooterDate(generatedAt, use24HourTime, Locale.US)
        canvas.drawText("Generated by GPA Whiz on $generatedDate", MARGIN, pageHeight - 17f, footerPaint)

        val pageText = "Page $pageNumber of $totalPages"
        footerPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(pageText, pageWidth - MARGIN, pageHeight - 17f, footerPaint)
    }

    fun paint(
        textSize: Float,
        color: Int = darkText,
        bold: Boolean = false
    ): Paint = Paint().apply {
        this.color = color
        this.textSize = textSize
        typeface = if (bold) boldTypeface else regularTypeface
        isAntiAlias = true
    }

    fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "..."
        if (paint.measureText(ellipsis) > maxWidth) return ""

        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
            end--
        }
        return text.substring(0, end).trimEnd() + ellipsis
    }

    fun drawFittedText(
        canvas: Canvas,
        text: String,
        x: Float,
        top: Float,
        maxWidth: Float,
        maxHeight: Float,
        paint: Paint,
        minimumTextSize: Float,
        lineSpacing: Float = paint.textSize * 1.25f
    ): PdfFittedText {
        val fitted = fitPdfText(
            text,
            maxWidth,
            maxHeight,
            paint.textSize,
            minimumTextSize,
            { text, size -> measureText(paint, text, size) }
        )
        val actualPaint = LectureTimetablePdfStyle.paint(paint.textSize, paint.color, paint.typeface?.style == Typeface.BOLD)
        actualPaint.textSize = fitted.textSize
        val lineHeight = max(lineSpacing, fitted.textSize * 1.25f)
        fitted.lines.forEachIndexed { index, line ->
            canvas.drawText(line, x, top + fitted.textSize + index * lineHeight, actualPaint)
        }
        return fitted
    }

    fun measureText(paint: Paint, text: String, textSize: Float): Float {
        val oldSize = paint.textSize
        paint.textSize = textSize
        val width = paint.measureText(text)
        paint.textSize = oldSize
        return width
    }
}
