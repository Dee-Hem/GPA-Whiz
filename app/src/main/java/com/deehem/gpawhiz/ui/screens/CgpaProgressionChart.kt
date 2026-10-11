package com.deehem.gpawhiz.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deehem.gpawhiz.data.Course
import com.deehem.gpawhiz.data.Semester
import com.deehem.gpawhiz.service.GpaCalcService

data class CgpaDataPoint(
    val semesterId: Int,
    val semesterName: String,
    val cgpa: Double,
    val trendNote: String
)

@Composable
fun CgpaProgressionChartCard(
    semesters: List<Semester>,
    courses: List<Course>,
    targetCgpa: Double,
    gradingScale: Double,
    modifier: Modifier = Modifier
) {
    // 1. Compute cumulative CGPA after each completed semester
    val completedSemesters = remember(semesters, courses) {
        semesters
            .filter { sem ->
                courses.any {
                    it.semesterId == sem.id &&
                    !it.grade.equals("Awaiting Grade", ignoreCase = true) &&
                    !it.grade.equals("AR", ignoreCase = true)
                }
            }
            .sortedBy { it.rank }
    }

    val points = remember(completedSemesters, courses) {
        val list = mutableListOf<CgpaDataPoint>()
        for (i in completedSemesters.indices) {
            val sem = completedSemesters[i]
            val runningSems = completedSemesters.take(i + 1)
            val cumulativeCgpa = GpaCalcService.calculateCgpa(runningSems, courses)
            val prevCgpa = if (i > 0) list[i - 1].cgpa else null
            val trend = when {
                prevCgpa == null -> "First term"
                cumulativeCgpa - prevCgpa > 0.005 -> "Up +%.2f from last term".format(cumulativeCgpa - prevCgpa)
                cumulativeCgpa - prevCgpa < -0.005 -> "Down %.2f from last term".format(cumulativeCgpa - prevCgpa)
                else -> "Steady (0.00)"
            }
            list.add(
                CgpaDataPoint(
                    semesterId = sem.id,
                    semesterName = sem.name,
                    cgpa = cumulativeCgpa,
                    trendNote = trend
                )
            )
        }
        list
    }

    val summaryText = remember(points, targetCgpa) {
        if (points.isEmpty()) {
            "No completed semester grades recorded yet."
        } else {
            val latest = points.last()
            "CGPA Progression Chart: ${points.size} terms plotted. Current CGPA is %.2f. ${latest.trendNote}. Target goal is %.2f.".format(
                latest.cgpa,
                targetCgpa
            )
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = summaryText }
            .testTag("cgpa_progression_chart")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ACADEMIC MOMENTUM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "CGPA Progression Curve",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (targetCgpa > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Goal: %.2f".format(targetCgpa),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // EDGE CASE 1: NO COMPLETED SEMESTERS
            if (points.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No Completed Semesters Yet",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Add course scores in the Courses tab to track your cumulative CGPA trend over time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (points.size == 1) {
                // EDGE CASE 2: SINGLE SEMESTER
                val single = points.first()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = single.semesterName.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "%.2f".format(single.cgpa),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "First Term Baseline (Scale: %.1f)".format(gradingScale),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // MULTIPLE SEMESTERS: CANVAS LINE CHART
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val surfaceColor = MaterialTheme.colorScheme.surface
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

                val latestPoint = points.last()

                // Latest Trend Callout
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Latest: %.2f CGPA".format(latestPoint.cgpa),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = primaryColor
                        )
                        Text(
                            text = latestPoint.trendNote,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (latestPoint.trendNote.startsWith("Up")) Color(0xFF0F9D58)
                                   else if (latestPoint.trendNote.startsWith("Down")) Color(0xFFDB4437)
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Scrollable container if many semesters (> 5)
                val scrollState = rememberScrollState()
                val isScrollable = points.size > 5
                val chartWidth = if (isScrollable) (points.size * 72).dp else 0.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isScrollable) Modifier.horizontalScroll(scrollState) else Modifier)
                ) {
                    Canvas(
                        modifier = Modifier
                            .then(if (isScrollable) Modifier.width(chartWidth) else Modifier.fillMaxWidth())
                            .height(200.dp)
                            .padding(top = 16.dp, bottom = 28.dp, start = 20.dp, end = 20.dp)
                    ) {
                        val w = size.width
                        val h = size.height

                        // Gridlines at 1.0, 2.0, 3.0, 4.0, (5.0)
                        val stepCount = if (gradingScale >= 5.0) 5 else 4
                        for (step in 1..stepCount) {
                            val y = h - (step.toFloat() / gradingScale.toFloat()) * h
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Target line (dashed)
                        if (targetCgpa > 0.0 && targetCgpa <= gradingScale) {
                            val targetY = h - (targetCgpa.toFloat() / gradingScale.toFloat()) * h
                            drawLine(
                                color = secondaryColor,
                                start = Offset(0f, targetY),
                                end = Offset(w, targetY),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                            )
                        }

                        // Calculate points coordinates
                        val pointOffsets = mutableListOf<Offset>()
                        val xStep = if (points.size > 1) w / (points.size - 1) else w / 2

                        points.forEachIndexed { index, pt ->
                            val x = index * xStep
                            val y = h - (pt.cgpa.toFloat() / gradingScale.toFloat()) * h
                            pointOffsets.add(Offset(x, y))
                        }

                        // Gradient fill under curve
                        if (pointOffsets.size >= 2) {
                            val fillPath = Path().apply {
                                moveTo(pointOffsets.first().x, pointOffsets.first().y)
                                for (i in 1 until pointOffsets.size) {
                                    val p0 = pointOffsets[i - 1]
                                    val p1 = pointOffsets[i]
                                    val midX = (p0.x + p1.x) / 2
                                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                }
                                lineTo(pointOffsets.last().x, h)
                                lineTo(pointOffsets.first().x, h)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.25f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = h
                                )
                            )

                            // Connecting stroke line
                            val strokePath = Path().apply {
                                moveTo(pointOffsets.first().x, pointOffsets.first().y)
                                for (i in 1 until pointOffsets.size) {
                                    val p0 = pointOffsets[i - 1]
                                    val p1 = pointOffsets[i]
                                    val midX = (p0.x + p1.x) / 2
                                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                }
                            }
                            drawPath(
                                path = strokePath,
                                color = primaryColor,
                                style = Stroke(
                                    width = 3.5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // Draw Point circles & Labels
                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas

                            val textPaint = Paint().apply {
                                isAntiAlias = true
                                textSize = 11.sp.toPx()
                                color = android.graphics.Color.argb(
                                    (onSurfaceColor.alpha * 255).toInt(),
                                    (onSurfaceColor.red * 255).toInt(),
                                    (onSurfaceColor.green * 255).toInt(),
                                    (onSurfaceColor.blue * 255).toInt()
                                )
                                textAlign = Paint.Align.CENTER
                                typeface = Typeface.DEFAULT_BOLD
                            }

                            val valPaint = Paint().apply {
                                isAntiAlias = true
                                textSize = 11.sp.toPx()
                                color = android.graphics.Color.argb(
                                    (primaryColor.alpha * 255).toInt(),
                                    (primaryColor.red * 255).toInt(),
                                    (primaryColor.green * 255).toInt(),
                                    (primaryColor.blue * 255).toInt()
                                )
                                textAlign = Paint.Align.CENTER
                                typeface = Typeface.DEFAULT_BOLD
                            }

                            pointOffsets.forEachIndexed { index, offset ->
                                val pt = points[index]

                                // Point circles
                                drawCircle(
                                    color = primaryColor,
                                    radius = 5.dp.toPx(),
                                    center = offset
                                )
                                drawCircle(
                                    color = surfaceColor,
                                    radius = 2.5.dp.toPx(),
                                    center = offset
                                )

                                // Value text above point
                                nativeCanvas.drawText(
                                    "%.2f".format(pt.cgpa),
                                    offset.x,
                                    offset.y - 10.dp.toPx(),
                                    valPaint
                                )

                                // Semester label below chart
                                val shortSemName = if (pt.semesterName.length > 8) {
                                    "S${index + 1}"
                                } else {
                                    pt.semesterName
                                }
                                nativeCanvas.drawText(
                                    shortSemName,
                                    offset.x,
                                    h + 18.dp.toPx(),
                                    textPaint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
