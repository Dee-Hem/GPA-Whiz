package com.deehem.gpawhiz.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deehem.gpawhiz.data.Exam
import com.deehem.gpawhiz.data.StudySession
import com.deehem.gpawhiz.data.StudyStatus
import com.deehem.gpawhiz.data.TimetableSlot
import com.deehem.gpawhiz.service.formatExamDuration
import com.deehem.gpawhiz.service.formatExamTimeRange
import com.deehem.gpawhiz.service.isExamEndTimeValid
import com.deehem.gpawhiz.service.isExamInProgress
import com.deehem.gpawhiz.service.SystemSchedulerWrapper
import com.deehem.gpawhiz.ui.viewmodel.GpaViewModel
import java.text.SimpleDateFormat
import java.util.*

private fun getFullTimestamp(dateMillis: Long, timeStr: String): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        
        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return cal.timeInMillis
}

private fun getDaysDiff(t1: Long, t2: Long): Int {
    val cal1 = Calendar.getInstance().apply { 
        timeInMillis = t1
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val cal2 = Calendar.getInstance().apply { 
        timeInMillis = t2
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diff = cal2.timeInMillis - cal1.timeInMillis
    return (diff / (24 * 60 * 60 * 1000)).toInt()
}

@Composable
fun TimetableScreen(
    viewModel: GpaViewModel,
    onOpenPomodoro: (StudySession) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Lectures", "Study Planner", "Exams")
    
    var itemToDelete by remember { mutableStateOf<Any?>(null) }
    val context = LocalContext.current

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Item?") },
            text = { Text("Are you sure you want to delete ${when(val item = itemToDelete) { is TimetableSlot -> "this lecture"; is StudySession -> "this study session"; is Exam -> "this exam"; else -> "this item" }}?") },
            confirmButton = {
                Button(onClick = {
                    when(val item = itemToDelete) {
                        is TimetableSlot -> viewModel.deleteTimetableSlot(context, item)
                        is StudySession -> viewModel.deleteStudySession(item)
                        is Exam -> viewModel.deleteExam(item)
                    }
                    itemToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> LecturesTab(viewModel, onDeleteSlot = { itemToDelete = it })
            1 -> StudyPlannerTab(viewModel, onDeleteSession = { itemToDelete = it }, onOpenPomodoro = onOpenPomodoro)
            2 -> ExamsTab(viewModel, onDeleteExam = { itemToDelete = it })
        }
    }
}

@Composable
fun LecturesTab(viewModel: GpaViewModel, onDeleteSlot: (TimetableSlot) -> Unit) {
    val context = LocalContext.current
    val slots by viewModel.timetableSlots.collectAsState()
    val profile by viewModel.studentProfile.collectAsState()
    val allCourses by viewModel.courses.collectAsState()
    
    val currentSemesterCourses = remember(allCourses, profile) {
        allCourses.filter { it.semesterId == profile.currentSemesterId }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var examToEdit by remember { mutableStateOf<Exam?>(null) }
    val daysMap = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WEEKLY LECTURES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_lecture_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        if (slots.isEmpty()) {
            EmptyStatePlaceholder(
                icon = Icons.Default.DateRange,
                title = "Timetable is Blank",
                description = "Add your university lectures to set preparation reminders offline."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (dayIndex in 1..7) {
                    val daySlots = slots.filter { it.dayOfWeek == dayIndex }
                    if (daySlots.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    text = daysMap[dayIndex - 1].uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    daySlots.forEach { slot ->
                                        TimetableRowItem(
                                            slot = slot,
                                            onToggleAlarm = { viewModel.toggleAlertForSlot(context, slot) },
                                            onDelete = { onDeleteSlot(slot) },
                                            onExport = { SystemSchedulerWrapper.redirectToSystemCalendar(context, slot) },
                                            onSetDeviceAlarm = { SystemSchedulerWrapper.setSystemAlarm(context, slot) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLectureDialog(
            courses = currentSemesterCourses,
            onDismiss = { showAddDialog = false },
            onConfirm = { courseId, code, venue, day, start, end ->
                viewModel.addTimetableSlot(context, courseId, code, venue, day, start, end)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun StudyPlannerTab(
    viewModel: GpaViewModel,
    onDeleteSession: (StudySession) -> Unit,
    onOpenPomodoro: (StudySession) -> Unit = {}
) {
    val sessions by viewModel.studySessions.collectAsState()
    val profile by viewModel.studentProfile.collectAsState()
    val allCourses by viewModel.courses.collectAsState()
    
    val currentSemesterCourses = remember(allCourses, profile) {
        allCourses.filter { it.semesterId == profile.currentSemesterId }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showTimerForSession by remember { mutableStateOf<StudySession?>(null) }
    var filterCompleted by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SELF-STUDY PLANNER",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_study_session_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Plan")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterTab(
                label = "Active",
                isSelected = !filterCompleted,
                onClick = { filterCompleted = false },
                modifier = Modifier.weight(1f)
            )
            FilterTab(
                label = "History",
                isSelected = filterCompleted,
                onClick = { filterCompleted = true },
                modifier = Modifier.weight(1f)
            )
        }

        if (sessions.isEmpty()) {
            EmptyStatePlaceholder(
                icon = Icons.Default.CheckCircle,
                title = "Plan Your Study",
                description = "Schedule specific study times for your current courses. Use the built-in timer to stay focused."
            )
        } else {
            val now = System.currentTimeMillis()
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val tomorrowStart = Calendar.getInstance().apply {
                timeInMillis = todayStart
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis

            val filteredSessions = if (filterCompleted) {
                sessions.filter { it.status != StudyStatus.UPCOMING }
            } else {
                sessions.filter { it.status == StudyStatus.UPCOMING }
            }

            if (filteredSessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (filterCompleted) "No history yet." else "All caught up! No active sessions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!filterCompleted) {
                        val overdueSessions = filteredSessions.filter { 
                            val ts = getFullTimestamp(it.date, it.startTime)
                            ts < todayStart 
                        }.sortedBy { getFullTimestamp(it.date, it.startTime) }

                        val todaySessions = filteredSessions.filter { 
                            val ts = getFullTimestamp(it.date, it.startTime)
                            ts in todayStart until tomorrowStart 
                        }.sortedBy { getFullTimestamp(it.date, it.startTime) }
                        
                        val futureSessions = filteredSessions.filter { 
                            getFullTimestamp(it.date, it.startTime) >= tomorrowStart 
                        }.sortedBy { getFullTimestamp(it.date, it.startTime) }

                        if (overdueSessions.isNotEmpty()) {
                            item { SectionHeader("OVERDUE SESSIONS") }
                            items(overdueSessions) { session ->
                                StudySessionCard(
                                    session = session,
                                    onStart = { showTimerForSession = it },
                                    onOpenPomodoro = onOpenPomodoro,
                                    onStatusChange = { s, status -> viewModel.updateStudySessionStatus(s, status) },
                                    onToggleAlarm = { viewModel.toggleStudySessionAlert(it) },
                                    onDelete = { onDeleteSession(it) }
                                )
                            }
                        }

                        if (todaySessions.isNotEmpty()) {
                            item { SectionHeader("TODAY'S SESSIONS") }
                            items(todaySessions) { session ->
                                StudySessionCard(
                                    session = session,
                                    onStart = { showTimerForSession = it },
                                    onOpenPomodoro = onOpenPomodoro,
                                    onStatusChange = { s, status -> viewModel.updateStudySessionStatus(s, status) },
                                    onToggleAlarm = { viewModel.toggleStudySessionAlert(it) },
                                    onDelete = { onDeleteSession(it) }
                                )
                            }
                        }
 
                        if (futureSessions.isNotEmpty()) {
                            item { SectionHeader("UPCOMING") }
                            items(futureSessions) { session ->
                                StudySessionCard(
                                    session = session,
                                    onStart = { showTimerForSession = it },
                                    onOpenPomodoro = onOpenPomodoro,
                                    onStatusChange = { s, status -> viewModel.updateStudySessionStatus(s, status) },
                                    onToggleAlarm = { viewModel.toggleStudySessionAlert(it) },
                                    onDelete = { onDeleteSession(it) }
                                )
                            }
                        }
                    } else {
                        items(filteredSessions) { session ->
                            StudySessionCard(
                                session = session,
                                isHistory = true,
                                onStart = { },
                                onOpenPomodoro = { },
                                onStatusChange = { s, status -> viewModel.updateStudySessionStatus(s, status) },
                                onToggleAlarm = { },
                                onDelete = { onDeleteSession(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddStudySessionDialog(
            courses = currentSemesterCourses,
            onDismiss = { showAddDialog = false },
            onConfirm = { courseId, code, date, time, duration, recurring, dow ->
                viewModel.addStudySession(courseId, code, date, time, duration, recurring, dow)
                showAddDialog = false
            }
        )
    }

    if (showTimerForSession != null) {
        StudyTimerDialog(
            session = showTimerForSession!!,
            onDismiss = { showTimerForSession = null },
            onComplete = {
                viewModel.updateStudySessionStatus(it, StudyStatus.COMPLETED)
                showTimerForSession = null
            },
            onOpenPomodoro = onOpenPomodoro
        )
    }
}

@Composable
fun ExamsTab(viewModel: GpaViewModel, onDeleteExam: (Exam) -> Unit) {
    val exams by viewModel.exams.collectAsState()
    val profile by viewModel.studentProfile.collectAsState()
    val allCourses by viewModel.courses.collectAsState()
    
    val currentSemesterCourses = remember(allCourses, profile) {
        allCourses.filter { it.semesterId == profile.currentSemesterId }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXAMINATION SCHEDULE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_exam_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Schedule")
            }
        }

        val now = System.currentTimeMillis()
        
        val upcomingExams = exams.filter {
            getFullTimestamp(it.date, it.time) >= now ||
                isExamInProgress(it.date, it.time, it.endTime, now)
        }.sortedBy { getFullTimestamp(it.date, it.time) }
        val pastExams = exams.filter { exam ->
            getFullTimestamp(exam.date, exam.time) < now &&
                !isExamInProgress(exam.date, exam.time, exam.endTime, now)
        }.sortedByDescending { getFullTimestamp(it.date, it.time) }

        if (exams.isEmpty()) {
            EmptyStatePlaceholder(
                icon = Icons.Default.Info,
                title = "No Exams Scheduled",
                description = "Keep track of your exam dates and get critical reminders offline."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (upcomingExams.isNotEmpty()) {
                    item { SectionHeader("UPCOMING EXAMS") }
                    items(upcomingExams) { exam ->
                        ExamCard(
                            exam = exam,
                            onEdit = { examToEdit = exam },
                            onDelete = { onDeleteExam(exam) },
                            onToggleAlarm = { viewModel.toggleExamAlert(exam) },
                            onCalendarExport = { viewModel.exportExamToCalendar(exam) }
                        )
                    }
                }

                if (pastExams.isNotEmpty()) {
                    item { SectionHeader("PAST EXAMS") }
                    items(pastExams) { exam ->
                        ExamCard(
                            exam = exam,
                            isPast = true,
                            onEdit = { examToEdit = exam },
                            onDelete = { onDeleteExam(exam) },
                            onToggleAlarm = { },
                            onCalendarExport = { }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog || examToEdit != null) {
        val initialExam = examToEdit
        AddExamDialog(
            courses = if (initialExam == null) currentSemesterCourses else allCourses,
            initialExam = initialExam,
            onDismiss = {
                showAddDialog = false
                examToEdit = null
            },
            onConfirm = { courseId, code, date, time, endTime, venue, seatNumber ->
                if (initialExam == null) {
                    viewModel.addExam(courseId, code, date, time, endTime, venue, seatNumber)
                } else {
                    viewModel.updateExamDetails(initialExam, endTime, venue, seatNumber)
                }
                showAddDialog = false
                examToEdit = null
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
    )
}

@Composable
fun EmptyStatePlaceholder(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FilterTab(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(containerColor).clickable { onClick() }.padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = contentColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLectureDialog(
    courses: List<com.deehem.gpawhiz.data.Course>,
    onDismiss: () -> Unit,
    onConfirm: (Int?, String, String, Int, String, String) -> Unit
) {
    val context = LocalContext.current
    val daysMap = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    
    var selectedCourseId by remember { mutableStateOf<Int?>(null) }
    var manualCourseCode by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var dayIndex by remember { mutableStateOf(1) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("10:00") }
    
    var expandedCourse by remember { mutableStateOf(false) }
    var expandedDay by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Lecture") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Course Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedCourse = true }, modifier = Modifier.fillMaxWidth()) {
                        val selected = courses.find { it.id == selectedCourseId }
                        Text(selected?.code ?: manualCourseCode.ifEmpty { "Select Course" })
                    }
                    DropdownMenu(expanded = expandedCourse, onDismissRequest = { expandedCourse = false }) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text("${course.code}: ${course.title}") },
                                onClick = {
                                    selectedCourseId = course.id
                                    manualCourseCode = course.code
                                    expandedCourse = false
                                }
                            )
                        }
                    }
                }
                
                if (courses.isEmpty()) {
                    OutlinedTextField(
                        value = manualCourseCode,
                        onValueChange = { manualCourseCode = it },
                        label = { Text("Course Code (Manual)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue") },
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedDay = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Day: ${daysMap[dayIndex - 1]}")
                    }
                    DropdownMenu(expanded = expandedDay, onDismissRequest = { expandedDay = false }) {
                        daysMap.forEachIndexed { idx, day ->
                            DropdownMenuItem(text = { Text(day) }, onClick = { dayIndex = idx + 1; expandedDay = false })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeSelectionBox(label = "Start", time = startTime, onTimeSelected = { startTime = it }, modifier = Modifier.weight(1f))
                    TimeSelectionBox(label = "End", time = endTime, onTimeSelected = { endTime = it }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedCourseId, manualCourseCode, venue, dayIndex, startTime, endTime) },
                enabled = manualCourseCode.isNotEmpty() && venue.isNotEmpty()
            ) {
                Text("Schedule")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExamDialog(
    courses: List<com.deehem.gpawhiz.data.Course>,
    initialExam: Exam? = null,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, Long, String, String?, String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedCourseIndex by remember(initialExam, courses) {
        mutableStateOf(courses.indexOfFirst { it.id == initialExam?.courseId }.coerceAtLeast(0))
    }
    var selectedDate by remember(initialExam) { mutableStateOf(initialExam?.date ?: System.currentTimeMillis()) }
    var selectedTime by remember(initialExam) { mutableStateOf(initialExam?.time ?: "09:00") }
    var endTime by remember(initialExam) { mutableStateOf(initialExam?.endTime) }
    var venue by remember(initialExam) { mutableStateOf(initialExam?.venue.orEmpty()) }
    var seatNumber by remember(initialExam) { mutableStateOf(initialExam?.seatNumber.orEmpty()) }
    var expandedCourse by remember { mutableStateOf(false) }
    val validEndTime = isExamEndTimeValid(selectedTime, endTime)

    val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialExam == null) "Schedule Examination" else "Edit Exam Details") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (courses.isEmpty()) {
                    Text("No courses found. Please add courses first in the Courses tab.", color = MaterialTheme.colorScheme.error)
                } else {
                    if (initialExam == null) Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { expandedCourse = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Course: ${courses[selectedCourseIndex].code}")
                        }
                        DropdownMenu(expanded = expandedCourse, onDismissRequest = { expandedCourse = false }) {
                            courses.forEachIndexed { index, course ->
                                DropdownMenuItem(
                                    text = { Text("${course.code}: ${course.title}") },
                                    onClick = { selectedCourseIndex = index; expandedCourse = false }
                                )
                            }
                        }
                    } else {
                        Text("Course: ${initialExam.courseCode}")
                    }

                    if (initialExam == null) {
                        OutlinedCard(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                DatePickerDialog(context, { _, year, month, day ->
                                    selectedDate = Calendar.getInstance().apply {
                                        set(year, month, day, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.heightIn(min = 48.dp).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = dateFormat.format(Date(selectedDate)))
                            }
                        }
                    } else {
                        Text(dateFormat.format(Date(selectedDate)), style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        if (initialExam == null) {
                            TimeSelectionBox(
                                label = "Start time",
                                time = selectedTime,
                                onTimeSelected = { selectedTime = it },
                                modifier = Modifier.weight(1f).testTag("exam_start_time_picker")
                            )
                        } else {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Start time",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(selectedTime, modifier = Modifier.padding(top = 12.dp))
                            }
                        }
                        OptionalExamEndTimeField(
                            time = endTime,
                            onTimeSelected = { endTime = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (!validEndTime) {
                        Text(
                            "End time must be after the start time.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("exam_end_time_error")
                        )
                    }
                    OutlinedTextField(
                        value = venue,
                        onValueChange = { venue = it },
                        label = { Text("Venue") },
                        modifier = Modifier.fillMaxWidth().testTag("exam_venue_field"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = seatNumber,
                        onValueChange = { seatNumber = it },
                        label = { Text("Seat number") },
                        modifier = Modifier.fillMaxWidth().testTag("exam_seat_number_field"),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (courses.isNotEmpty()) {
                        val course = courses[selectedCourseIndex]
                        onConfirm(course.id, course.code, selectedDate, selectedTime, endTime, venue, seatNumber)
                    }
                },
                enabled = courses.isNotEmpty() && validEndTime
            ) {
                Text(if (initialExam == null) "Schedule Exam" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun OptionalExamEndTimeField(
    time: String?,
    onTimeSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        Text(
            "End time (optional)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        val parts = (time ?: "10:00").split(":")
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                onTimeSelected(String.format("%02d:%02d", hour, minute))
                            },
                            parts[0].toIntOrNull() ?: 10,
                            parts[1].toIntOrNull() ?: 0,
                            false
                        ).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("exam_end_time_picker"),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(time ?: "Not set", style = MaterialTheme.typography.bodyMedium)
            }
            if (time != null) {
                IconButton(
                    onClick = { onTimeSelected(null) },
                    modifier = Modifier.testTag("exam_end_time_clear")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Clear end time")
                }
            }
        }
    }
}

@Composable
fun ExamCard(
    exam: Exam,
    isPast: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleAlarm: () -> Unit,
    onCalendarExport: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(exam.date))
    
    // Countdown calculation
    val now = System.currentTimeMillis()
    val startTimestamp = getFullTimestamp(exam.date, exam.time)
    val daysLeft = getDaysDiff(now, startTimestamp)
    val context = LocalContext.current
    val inProgress = isExamInProgress(exam.date, exam.time, exam.endTime, now)
    val timeRange = formatExamTimeRange(exam.time, exam.endTime)
    val duration = formatExamDuration(exam.time, exam.endTime)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = exam.courseCode, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                    Text(
                        text = "$dateStr • $timeRange${duration?.let { " ($it)" }.orEmpty()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (exam.venue.isNotBlank() || exam.seatNumber.isNotBlank()) {
                        Text(
                            text = listOfNotNull(
                                exam.venue.takeIf { it.isNotBlank() },
                                exam.seatNumber.takeIf { it.isNotBlank() }?.let { "Seat $it" }
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (!isPast) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val countdownText = when {
                            inProgress -> "In progress"
                            daysLeft == 0 -> "TODAY"
                            daysLeft == 1 -> "Tomorrow"
                            daysLeft < 0 -> "Happening now!"
                            else -> "$daysLeft days left"
                        }
                        Surface(
                            color = if (inProgress || daysLeft <= 3) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (inProgress) countdownText else countdownText.uppercase(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (inProgress || daysLeft <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_exam_${exam.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit exam", tint = MaterialTheme.colorScheme.primary)
                }
                if (!isPast) {
                    Row {
                        IconButton(onClick = onCalendarExport) {
                            Icon(Icons.Default.DateRange, contentDescription = "Sync Calendar", tint = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(onClick = { SystemSchedulerWrapper.setSystemAlarmForExam(context, exam) }) {
                            Icon(Icons.Default.Alarm, contentDescription = "Alarm Clock", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onToggleAlarm) {
                            Icon(
                                imageVector = if (exam.alertEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = "Alarm",
                                tint = if (exam.alertEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun TimetableRowItem(
    slot: TimetableSlot,
    onToggleAlarm: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onSetDeviceAlarm: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(if (slot.alertEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slot.courseCode,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = "Venue", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = slot.venue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Time", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${slot.startTime} - ${slot.endTime}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSetDeviceAlarm) {
                        Icon(Icons.Default.Alarm, contentDescription = "Alarm Clock", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onExport) {
                        Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = onToggleAlarm) {
                        Icon(
                            imageVector = if (slot.alertEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = "Toggle",
                            tint = if (slot.alertEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSelectionBox(label: String, time: String, onTimeSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val parts = time.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val timePickerDialog = TimePickerDialog(context, { _, h, m -> onTimeSelected(String.format("%02d:%02d", h, m)) }, hour, minute, false)
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { timePickerDialog.show() }.padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
            Text(text = time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun StudySessionCard(
    session: StudySession,
    isHistory: Boolean = false,
    onStart: (StudySession) -> Unit,
    onOpenPomodoro: (StudySession) -> Unit = {},
    onStatusChange: (StudySession, String) -> Unit,
    onToggleAlarm: (StudySession) -> Unit,
    onDelete: (StudySession) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
    val dateStr = dateFormat.format(Date(session.date))
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = session.courseCode, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "$dateStr • ${session.startTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${session.durationMinutes} minutes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isHistory) {
                        IconButton(onClick = { onToggleAlarm(session) }) {
                            Icon(
                                imageVector = if (session.reminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = "Toggle Internal Alarm",
                                tint = if (session.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(onClick = { SystemSchedulerWrapper.setSystemAlarmForStudy(context, session) }) {
                            Icon(Icons.Default.Alarm, contentDescription = "Alarm Clock", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { SystemSchedulerWrapper.redirectToSystemCalendarForStudy(context, session) }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    StatusBadge(status = session.status)
                }
            }
            if (!isHistory) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenPomodoro(session) },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(40.dp)
                                .testTag("open_pomodoro_button"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pomodoro", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = { onStart(session) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("start_timer_button"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Timer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onStatusChange(session, StudyStatus.POSTPONED) },
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text("Postpone", fontSize = 11.sp, maxLines = 1)
                        }

                        IconButton(
                            onClick = { onDelete(session) },
                            modifier = Modifier.size(48.dp).testTag("delete_session_button_${session.id}")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { onDelete(session) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Remove from History", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        StudyStatus.COMPLETED -> Color(0xFF4CAF50)
        StudyStatus.POSTPONED -> Color(0xFFFF9800)
        StudyStatus.CANCELLED -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
        Text(text = status.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudySessionDialog(courses: List<com.deehem.gpawhiz.data.Course>, onDismiss: () -> Unit, onConfirm: (Int, String, Long, String, Int, Boolean, Int?) -> Unit) {
    val context = LocalContext.current
    var selectedCourseIndex by remember { mutableStateOf(0) }
    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val defaultTime = remember {
        val c = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
        }
        String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }
    var selectedDate by remember { mutableStateOf(todayMidnight) }
    var selectedTime by remember { mutableStateOf(defaultTime) }
    var durationMinutes by remember { mutableStateOf("60") }
    var expandedCourse by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())

    val isPastTime = remember(selectedDate, selectedTime) {
        val targetTs = getFullTimestamp(selectedDate, selectedTime)
        targetTs < System.currentTimeMillis()
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Schedule Study") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (courses.isEmpty()) { Text("No courses found. Add courses first.", color = MaterialTheme.colorScheme.error) } else {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedCourse = true }, modifier = Modifier.fillMaxWidth()) { Text("Course: ${courses[selectedCourseIndex].code}") }
                    DropdownMenu(expanded = expandedCourse, onDismissRequest = { expandedCourse = false }) {
                        courses.forEachIndexed { index, course -> DropdownMenuItem(text = { Text("${course.code}: ${course.title}") }, onClick = { selectedCourseIndex = index; expandedCourse = false }) }
                    }
                }
                OutlinedCard(onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                    DatePickerDialog(context, { _, y, m, d -> 
                        val newCal = Calendar.getInstance().apply { 
                            set(y, m, d, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        selectedDate = newCal.timeInMillis 
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }, modifier = Modifier.fillMaxWidth()) { Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.DateRange, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text(text = dateFormat.format(Date(selectedDate))) } }
                OutlinedCard(onClick = {
                    val parts = selectedTime.split(":")
                    TimePickerDialog(context, { _, h, m -> selectedTime = String.format("%02d:%02d", h, m) }, parts[0].toInt(), parts[1].toInt(), false).show()
                }, modifier = Modifier.fillMaxWidth()) { Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Notifications, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text(text = "Start Time: $selectedTime") } }
                OutlinedTextField(value = durationMinutes, onValueChange = { durationMinutes = it.filter { c -> c.isDigit() } }, label = { Text("Duration (mins)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                if (isPastTime) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Note: Scheduled time is earlier than current time. Session will appear under Today's Sessions ready for immediate focus or timer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }, confirmButton = { Button(onClick = { if (courses.isNotEmpty()) { val course = courses[selectedCourseIndex]; onConfirm(course.id, course.code, selectedDate, selectedTime, durationMinutes.toIntOrNull() ?: 60, false, null) } }, enabled = courses.isNotEmpty() && durationMinutes.isNotEmpty()) { Text("Schedule") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun StudyTimerDialog(
    session: StudySession,
    onDismiss: () -> Unit,
    onComplete: (StudySession) -> Unit,
    onOpenPomodoro: ((StudySession) -> Unit)? = null
) {
    var timeLeftSeconds by remember { mutableStateOf(session.durationMinutes * 60) }
    var isRunning by remember { mutableStateOf(true) }
    LaunchedEffect(isRunning) { if (isRunning) { while (timeLeftSeconds > 0) { kotlinx.coroutines.delay(1000); timeLeftSeconds-- }; isRunning = false } }
    AlertDialog(onDismissRequest = { }, title = { Text("Timer: ${session.courseCode}", fontWeight = FontWeight.Bold) }, text = {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val mins = timeLeftSeconds / 60; val secs = timeLeftSeconds % 60
            Text(text = String.format("%02d:%02d", mins, secs), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            LinearProgressIndicator(progress = { timeLeftSeconds.toFloat() / (session.durationMinutes * 60) }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { isRunning = !isRunning }, shape = RoundedCornerShape(8.dp)) { Icon(if (isRunning) Icons.Default.Menu else Icons.Default.PlayArrow, contentDescription = null); Spacer(modifier = Modifier.width(4.dp)); Text(if (isRunning) "Pause" else "Resume") }
                OutlinedButton(onClick = { timeLeftSeconds = session.durationMinutes * 60; isRunning = false }, shape = RoundedCornerShape(8.dp)) { Text("Reset") }
            }
            if (onOpenPomodoro != null) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenPomodoro(session)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("switch_to_pomodoro_dialog_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Switch to Fullscreen Pomodoro Mode", fontSize = 12.sp)
                }
            }
        }
    }, confirmButton = { Button(onClick = { onComplete(session) }) { Text("Mark Done") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
