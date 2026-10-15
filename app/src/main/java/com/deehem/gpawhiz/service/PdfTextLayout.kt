package com.deehem.gpawhiz.service

import java.math.BigDecimal
import kotlin.math.floor

fun formatCourseUnits(units: Number?): String =
    units?.let { BigDecimal(it.toString()).stripTrailingZeros().toPlainString() } ?: "-"

fun examTimetableColumnEdges(left: Float, right: Float): List<Float> {
    require(right > left) { "Timetable table width must be positive." }
    val width = right - left
    return listOf(
        left,
        left + width * 0.10f,
        left + width * 0.22f,
        left + width * 0.58f,
        left + width * 0.64f,
        left + width * 0.90f,
        right
    )
}

fun dailyTimetableColumnEdges(left: Float, right: Float): List<Float> {
    require(right > left) { "Timetable table width must be positive." }
    val width = right - left
    return listOf(
        left,
        left + width * 0.10f,
        left + width * 0.23f,
        left + width * 0.66f,
        left + width * 0.72f,
        right
    )
}

data class PdfFittedText(
    val lines: List<String>,
    val textSize: Float,
    val truncated: Boolean
)

data class PdfWrappedRow(
    val cells: List<List<String>>,
    val height: Float
)

fun wrapPdfTableRow(
    values: List<String>,
    widths: List<Float>,
    textSize: Float,
    lineHeight: Float,
    verticalPadding: Float,
    measure: (String, Float) -> Float
): PdfWrappedRow {
    require(values.size == widths.size) { "Each table value requires a matching width." }
    val cells = values.mapIndexed { index, value ->
        wrapPdfText(value, widths[index], textSize, measure).ifEmpty { listOf("") }
    }
    return PdfWrappedRow(cells, maxOf(1, cells.maxOfOrNull { it.size } ?: 1) * lineHeight + verticalPadding)
}

fun wrapPdfText(
    text: String,
    maxWidth: Float,
    textSize: Float,
    measure: (String, Float) -> Float
): List<String> {
    if (text.isEmpty()) return emptyList()
    require(maxWidth > 0f) { "Text width must be positive." }
    val lines = mutableListOf<String>()
    var line = StringBuilder()
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

    fun emitLine() {
        if (line.isNotEmpty()) {
            lines.add(line.toString())
            line = StringBuilder()
        }
    }

    for (word in words) {
        val candidate = if (line.isEmpty()) word else "${line} $word"
        if (measure(candidate, textSize) <= maxWidth) {
            line = StringBuilder(candidate)
            continue
        }
        emitLine()
        var piece = StringBuilder()
        for (character in word) {
            val next = piece.toString() + character
            if (piece.isNotEmpty() && measure(next, textSize) > maxWidth) {
                lines.add(piece.toString())
                piece = StringBuilder()
            }
            piece.append(character)
        }
        line = piece
    }
    emitLine()
    return lines
}

fun fitPdfText(
    text: String,
    maxWidth: Float,
    maxHeight: Float,
    preferredTextSize: Float,
    minimumTextSize: Float,
    measure: (String, Float) -> Float
): PdfFittedText {
    require(preferredTextSize >= minimumTextSize && minimumTextSize > 0f) {
        "Text size bounds must be positive and ordered."
    }
    if (text.isEmpty()) return PdfFittedText(emptyList(), preferredTextSize, false)
    if (maxWidth <= 0f || maxHeight <= 0f) return PdfFittedText(emptyList(), minimumTextSize, true)

    val sizes = generateSequence(preferredTextSize) { previous ->
        (previous - 0.5f).takeIf { it >= minimumTextSize }
    }.toList() + listOf(minimumTextSize)
    var lastLines = emptyList<String>()
    var lastSize = minimumTextSize
    for (size in sizes.distinct()) {
        val lines = wrapPdfText(text, maxWidth, size, measure)
        val lineHeight = size * 1.25f
        val capacity = floor(maxHeight / lineHeight).toInt()
        lastLines = lines
        lastSize = size
        val hasUnrenderableCharacter = lines.any { measure(it, size) > maxWidth }
        if (!hasUnrenderableCharacter && lines.size <= capacity) return PdfFittedText(lines, size, false)
        if (size == minimumTextSize) {
            if (hasUnrenderableCharacter) return PdfFittedText(emptyList(), size, true)
            if (capacity == 0) return PdfFittedText(emptyList(), size, true)
            val visible = lines.take(capacity).toMutableList()
            val ellipsized = ellipsizePdfText(
                visible.last(),
                maxWidth,
                size,
                measure
            )
            if (ellipsized.isEmpty()) return PdfFittedText(emptyList(), size, true)
            visible[visible.lastIndex] = ellipsized
            return PdfFittedText(visible, size, true)
        }
    }
    return PdfFittedText(lastLines, lastSize, true)
}

private fun ellipsizePdfText(
    text: String,
    maxWidth: Float,
    textSize: Float,
    measure: (String, Float) -> Float
): String {
    val ellipsis = "..."
    if (measure(ellipsis, textSize) > maxWidth) return ""
    var end = text.length
    while (end > 0 && measure(text.substring(0, end) + ellipsis, textSize) > maxWidth) end--
    return text.substring(0, end).trimEnd() + ellipsis
}

fun pdfSummaryText(itemName: String, count: Int, first: String? = null, last: String? = null): String? {
    if (count <= 0) return null
    val noun = if (count == 1) itemName else "${itemName}s"
    return if (first != null && last != null) {
        "$count $noun, $first to $last"
    } else {
        "$count $noun"
    }
}

fun formatPdfFooterDate(
    date: java.util.Date,
    use24HourTime: Boolean,
    locale: java.util.Locale = java.util.Locale.getDefault()
): String {
    val pattern = if (use24HourTime) "d MMM yyyy, HH:mm" else "d MMM yyyy, h:mm a"
    return java.text.SimpleDateFormat(pattern, locale).format(date)
}

fun formatPdfShortDate(date: java.util.Date, locale: java.util.Locale = java.util.Locale.getDefault()): String =
    java.text.SimpleDateFormat("d MMM yyyy", locale).format(date)

fun lectureSummaryText(lectureCount: Int, dayCount: Int): String? {
    if (lectureCount <= 0) return null
    val lectures = if (lectureCount == 1) "lecture" else "lectures"
    val days = if (dayCount == 1) "day" else "days"
    return "$lectureCount $lectures across $dayCount $days"
}

fun examSummaryText(count: Int, first: String? = null, last: String? = null): String? =
    if (count <= 0) null else pdfSummaryText("exam", count, first, last)

data class PdfTextRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

fun textRectsDoNotOverlap(rectangles: List<PdfTextRect>): Boolean =
    rectangles.indices.all { first ->
        (first + 1 until rectangles.size).all { second ->
            val a = rectangles[first]
            val b = rectangles[second]
            a.right <= b.left || b.right <= a.left || a.bottom <= b.top || b.bottom <= a.top
        }
    }

fun textRectsStayInside(rectangles: List<PdfTextRect>, bounds: PdfTextRect): Boolean =
    rectangles.all {
        it.left >= bounds.left && it.top >= bounds.top && it.right <= bounds.right && it.bottom <= bounds.bottom
    }
