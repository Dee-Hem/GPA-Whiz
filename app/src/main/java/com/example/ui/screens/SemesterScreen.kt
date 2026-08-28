package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Course
import com.example.data.Semester
import com.example.service.GpaCalcService
import com.example.ui.viewmodel.GpaViewModel

@Composable
fun SemesterScreen(
    viewModel: GpaViewModel,
    modifier: Modifier = Modifier
) {
    val semesters by viewModel.semesters.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val profile by viewModel.studentProfile.collectAsState()

    var selectedSemester by remember { mutableStateOf<Semester?>(null) }
    
    // Dialog / Sheet states
    var showAddSemesterDialog by remember { mutableStateOf(false) }
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var courseToEdit by remember { mutableStateOf<Course?>(null) }

    // If semesters change or update, keep selectedSemester synced
    LaunchedEffect(semesters) {
        if (selectedSemester != null) {
            selectedSemester = semesters.find { it.id == selectedSemester!!.id }
        } else if (semesters.isNotEmpty()) {
            selectedSemester = semesters.first()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Semesters Horizontal Choice bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SEMESTER MANAGER",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { showAddSemesterDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_semester_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Sem", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Term")
            }
        }

        if (semesters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No semesters mapped yet.",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Click 'Add Term' at the top to draft a new semester timeline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Horizontally scrollable semesters selector
            ScrollableTabRow(
                selectedTabIndex = semesters.indexOfFirst { it.id == selectedSemester?.id }.coerceAtLeast(0),
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                semesters.forEach { sem ->
                    Tab(
                        selected = selectedSemester?.id == sem.id,
                        onClick = { selectedSemester = sem },
                        text = { Text(sem.name) }
                    )
                }
            }

            selectedSemester?.let { sem ->
                val semCourses = courses.filter { it.semesterId == sem.id }
                val sgpa = GpaCalcService.calculateSgpa(semCourses, sem.gradingScale)
                val totalUnits = semCourses.sumOf { it.units }
                
                // Color mapping for dynamic semester feedback
                val colorHex = GpaCalcService.getColorForCgpa(sgpa, sem.gradingScale)
                val semColor = Color(android.graphics.Color.parseColor(colorHex))

                // Semester Scoreboard Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = semColor.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, semColor.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = sem.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Registered Credit Units: $totalUnits",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "SGPA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "%.2f".format(sgpa),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = semColor
                            )
                        }
                    }
                }

                // Add Course & Delete Term controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COURSES LISTING",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.deleteSemester(sem) },
                            modifier = Modifier.testTag("delete_semester_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Sem", tint = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = { showAddCourseDialog = true },
                            modifier = Modifier.testTag("add_course_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Course")
                        }
                    }
                }

                // Selected semester courses listing
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(semCourses) { course ->
                        CourseRowItem(
                            course = course,
                            scale = sem.gradingScale,
                            onEdit = {
                                courseToEdit = course
                                showAddCourseDialog = true
                            },
                            onDelete = { viewModel.deleteCourse(course) }
                        )
                    }

                    if (semCourses.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No courses added yet. Tap 'Add Course' above.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Add Semester
    if (showAddSemesterDialog) {
        var semName by remember { mutableStateOf("") }
        var semScale by remember { mutableStateOf(profile.gradingScale) }

        AlertDialog(
            onDismissRequest = { showAddSemesterDialog = false },
            title = { Text("Draft New Semester Timeline") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = semName,
                        onValueChange = { semName = it },
                        label = { Text("Semester Description") },
                        placeholder = { Text("e.g. Year 1 - S1") },
                        modifier = Modifier.fillMaxWidth().testTag("new_semester_name_input"),
                        singleLine = true
                    )
                    
                    Text("Session Grading Scale Map:", fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = semScale == 5.0,
                            onClick = { semScale = 5.0 },
                            label = { Text("5.0 Scale") }
                        )
                        FilterChip(
                            selected = semScale == 4.0,
                            onClick = { semScale = 4.0 },
                            label = { Text("4.0 Scale") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (semName.isNotEmpty()) {
                            viewModel.addSemester(semName, semScale)
                            showAddSemesterDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_semester_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSemesterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Dialog: Add / Edit Course
    if (showAddCourseDialog) {
        selectedSemester?.let { sem ->
            CourseEntryDialog(
                course = courseToEdit,
                onDismiss = {
                    showAddCourseDialog = false
                    courseToEdit = null
                },
                onSave = { code, title, units, score, grade, carryOver ->
                    if (courseToEdit == null) {
                        viewModel.addCourse(
                            semesterId = sem.id,
                            code = code,
                            title = title,
                            units = units,
                            score = score,
                            grade = grade,
                            isCarryOver = carryOver
                        )
                    } else {
                        viewModel.updateCourse(
                            courseId = courseToEdit!!.id,
                            semesterId = sem.id,
                            code = code,
                            title = title,
                            units = units,
                            score = score,
                            grade = grade,
                            isCarryOver = carryOver
                        )
                    }
                    showAddCourseDialog = false
                    courseToEdit = null
                },
                scale = sem.gradingScale
            )
        }
    }
}

@Composable
fun CourseRowItem(
    course: Course,
    scale: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isAwaiting = course.grade.equals("Awaiting Grade", ignoreCase = true) || course.grade.uppercase() == "AR"
    
    // Grade based colors
    val gradeColor = when {
        isAwaiting -> Color.Gray
        course.grade.uppercase() == "A" -> Color(0xFF0F9D58) // Emerald Green
        course.grade.uppercase() in listOf("B", "C") -> Color(0xFF1A73E8) // Deep Blue
        course.grade.uppercase() in listOf("D", "E") -> Color(0xFFF4B400) // Amber
        else -> Color(0xFFDB4437) // Crimson Red
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left color status tag
            Box(
                modifier = Modifier
                    .size(width = 6.dp, height = 40.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(gradeColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${course.code}: ${course.title}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                val pointsText = if (isAwaiting) {
                    "Pending"
                } else {
                    "%.1f".format(course.units * GpaCalcService.getPointsForGrade(course.grade, scale))
                }
                Text(
                    text = "Units: ${course.units}  |  Score: ${course.score}%  |  Points: $pointsText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Grade Badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(gradeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                ) {
                    val displayGrade = if (isAwaiting) "AR" else course.grade
                    Text(
                        text = displayGrade,
                        fontWeight = FontWeight.Black,
                        color = gradeColor,
                        fontSize = if (displayGrade.length > 2) 11.sp else 16.sp
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete course", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
        }
    }
}


