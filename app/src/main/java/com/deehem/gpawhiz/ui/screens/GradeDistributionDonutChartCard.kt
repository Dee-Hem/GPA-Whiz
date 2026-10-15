package com.deehem.gpawhiz.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deehem.gpawhiz.data.Course
import com.deehem.gpawhiz.data.Semester

sealed class GradeDistributionFilter {
    object WholeProgram : GradeDistributionFilter() {
        override fun toString(): String = "Whole Program"
    }
    data class ByLevel(val level: String) : GradeDistributionFilter() {
        override fun toString(): String = level
    }
    data class BySemester(val semesterId: Int, val semesterName: String) : GradeDistributionFilter() {
        override fun toString(): String = semesterName
    }
}

data class GradeCount(
    val letter: String,
    val count: Int,
    val percentage: Float,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeDistributionDonutChartCard(
    semesters: List<Semester>,
    courses: List<Course>,
    gradingScale: Double,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<GradeDistributionFilter>(GradeDistributionFilter.WholeProgram) }
    var filterMenuExpanded by remember { mutableStateOf(false) }

    // Distinct accessible colors for light & dark themes
    val gradeColors = remember(gradingScale) {
        if (gradingScale >= 5.0) {
            mapOf(
                "A" to Color(0xFF2E7D32), // Forest Green
                "B" to Color(0xFF1976D2), // Blue
                "C" to Color(0xFFF57C00), // Amber / Deep Orange
                "D" to Color(0xFF7B1FA2), // Purple
                "E" to Color(0xFF0097A7), // Teal / Cyan
                "F" to Color(0xFFD32F2F)  // Crimson Red
            )
        } else {
            mapOf(
                "A" to Color(0xFF2E7D32),
                "B" to Color(0xFF1976D2),
                "C" to Color(0xFFF57C00),
                "D" to Color(0xFF7B1FA2),
                "F" to Color(0xFFD32F2F)
            )
        }
    }

    // Filtered courses based on user selection
    val filteredCourses = remember(courses, semesters, selectedFilter) {
        when (val filter = selectedFilter) {
            is GradeDistributionFilter.WholeProgram -> courses
            is GradeDistributionFilter.ByLevel -> {
                // Match courses whose semester name or course code indicates this level (e.g. 100L, 200L, etc.)
                val prefix = filter.level.take(1) // "1" for 100L
                val matchingSemIds = semesters.filter { sem ->
                    sem.name.contains(filter.level, ignoreCase = true) ||
                    sem.name.contains("${prefix}00", ignoreCase = true) ||
                    sem.name.contains("Year $prefix", ignoreCase = true)
                }.map { it.id }.toSet()

                courses.filter { c ->
                    c.semesterId in matchingSemIds ||
                    c.code.filter { it.isDigit() }.startsWith(prefix)
                }
            }
            is GradeDistributionFilter.BySemester -> {
                courses.filter { it.semesterId == filter.semesterId }
            }
        }
    }

    // Graded courses only (exclude "Awaiting Grade", "AR", empty)
    val gradedCourses = remember(filteredCourses) {
        filteredCourses.filter {
            val g = it.grade.trim().uppercase()
            g.isNotEmpty() && g != "AWAITING GRADE" && g != "AR"
        }
    }

    val totalGraded = gradedCourses.size

    // Determine expected letter grades for scale
    val letterGrades = remember(gradingScale) {
        if (gradingScale >= 5.0) listOf("A", "B", "C", "D", "E", "F")
        else listOf("A", "B", "C", "D", "F")
    }

    val gradeDistribution = remember(gradedCourses, letterGrades, gradeColors, totalGraded) {
        letterGrades.map { letter ->
            val count = gradedCourses.count { it.grade.trim().uppercase() == letter }
            val pct = if (totalGraded > 0) (count.toFloat() / totalGraded) * 100f else 0f
            GradeCount(
                letter = letter,
                count = count,
                percentage = pct,
                color = gradeColors[letter] ?: Color.Gray
            )
        }
    }

    // Calculate insight line
    val insightText = remember(gradeDistribution, totalGraded) {
        if (totalGraded == 0) {
            "No graded courses recorded yet for this selection."
        } else {
            val nonZero = gradeDistribution.filter { it.count > 0 }
            if (nonZero.size == 1) {
                val single = nonZero.first()
                "All of your grades are ${single.letter}'s (100%)"
            } else {
                val topGrade = gradeDistribution.maxByOrNull { it.count }
                if (topGrade != null && topGrade.count > 0) {
                    "Most of your grades are ${topGrade.letter}'s (${"%.0f".format(topGrade.percentage)}%)"
                } else {
                    "Grades evenly distributed."
                }
            }
        }
    }

    val standardLevels = listOf("100L", "200L", "300L", "400L", "500L")

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier.testTag("grade_distribution_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row with Filter Chip/Dropdown
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "GRADE DISTRIBUTION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Letter Grade Breakdown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Filter Dropdown Anchor
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { filterMenuExpanded = true }
                            .testTag("grade_distribution_filter_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = when (val f = selectedFilter) {
                                    is GradeDistributionFilter.WholeProgram -> "All Terms"
                                    is GradeDistributionFilter.ByLevel -> f.level
                                    is GradeDistributionFilter.BySemester -> f.semesterName.take(12)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = filterMenuExpanded,
                        onDismissRequest = { filterMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Whole Program", fontWeight = if (selectedFilter is GradeDistributionFilter.WholeProgram) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                selectedFilter = GradeDistributionFilter.WholeProgram
                                filterMenuExpanded = false
                            }
                        )

                        HorizontalDivider()
                        standardLevels.forEach { lvl ->
                            DropdownMenuItem(
                                text = { Text("Level: $lvl") },
                                onClick = {
                                    selectedFilter = GradeDistributionFilter.ByLevel(lvl)
                                    filterMenuExpanded = false
                                }
                            )
                        }

                        if (semesters.isNotEmpty()) {
                            HorizontalDivider()
                            semesters.forEach { sem ->
                                DropdownMenuItem(
                                    text = { Text(sem.name) },
                                    onClick = {
                                        selectedFilter = GradeDistributionFilter.BySemester(sem.id, sem.name)
                                        filterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Insight line
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = insightText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            if (totalGraded == 0) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No graded courses found",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Add course grades to visualize your performance breakdown.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                // Donut Chart + Legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Donut Chart drawn with Jetpack Compose Canvas
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val emptyRingColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        Canvas(modifier = Modifier.fillMaxSize().testTag("grade_donut_canvas")) {
                            val strokeWidth = 32.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(
                                (size.width - diameter) / 2f,
                                (size.height - diameter) / 2f
                            )
                            val arcSize = Size(diameter, diameter)

                            // Background guide ring
                            drawArc(
                                color = emptyRingColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth)
                            )

                            var currentStartAngle = -90f
                            val activeGrades = gradeDistribution.filter { it.count > 0 }

                            if (activeGrades.size == 1) {
                                // Single grade edge case: full circle
                                drawArc(
                                    color = activeGrades.first().color,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            } else {
                                activeGrades.forEach { item ->
                                    val sweep = (item.percentage / 100f) * 360f
                                    if (sweep > 0f) {
                                        drawArc(
                                            color = item.color,
                                            startAngle = currentStartAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = arcSize,
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                        )
                                        currentStartAngle += sweep
                                    }
                                }
                            }
                        }

                        // Center content of donut
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalGraded",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (totalGraded == 1) "Course" else "Courses",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Legend Column with counts and percentages
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        gradeDistribution.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.testTag("grade_legend_${item.letter}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(item.color)
                                )
                                Text(
                                    text = "Grade ${item.letter}:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${item.count} (${"%.0f".format(item.percentage)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
