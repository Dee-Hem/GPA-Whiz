package com.deehem.gpawhiz.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deehem.gpawhiz.data.Course
import com.deehem.gpawhiz.data.Semester
import com.deehem.gpawhiz.data.StudentProfile
import com.deehem.gpawhiz.data.TimetableSlot
import com.deehem.gpawhiz.service.LectureTimetablePdfLayout
import com.deehem.gpawhiz.ui.viewmodel.GpaViewModel
import java.io.IOException

@Composable
fun LectureTimetableExportDialog(
    viewModel: GpaViewModel,
    student: StudentProfile,
    semesters: List<Semester>,
    courses: List<Course>,
    slots: List<TimetableSlot>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val courseById = remember(courses) { courses.associateBy { it.id } }
    val linkedSemesterIds = remember(slots, courseById) {
        slots.mapNotNull { slot -> slot.courseId?.let(courseById::get)?.semesterId }.toSet()
    }
    val semesterOptions = remember(semesters, linkedSemesterIds) {
        semesters.filter { it.id in linkedSemesterIds }
    }
    val defaultSemester = semesterOptions.find { it.id == student.currentSemesterId }
        ?: semesterOptions.firstOrNull()
    var selectedSemesterId by remember(semesterOptions, student.currentSemesterId) {
        mutableStateOf(defaultSemester?.id)
    }
    var selectedLayout by remember { mutableStateOf(LectureTimetablePdfLayout.WEEKLY_GRID) }
    var semesterMenuExpanded by remember { mutableStateOf(false) }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("Could not open the selected file for writing.")
                outputStream.use {
                    viewModel.exportLectureTimetablePdf(it, selectedLayout, selectedSemesterId)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Failed to export lecture timetable: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Export lecture timetable (PDF)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Choose a layout for your saved lectures.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LayoutChoice(
                    label = "Weekly grid",
                    selected = selectedLayout == LectureTimetablePdfLayout.WEEKLY_GRID,
                    testTag = "lecture_timetable_weekly_grid",
                    onClick = { selectedLayout = LectureTimetablePdfLayout.WEEKLY_GRID }
                )
                LayoutChoice(
                    label = "Daily list",
                    selected = selectedLayout == LectureTimetablePdfLayout.DAILY_LIST,
                    testTag = "lecture_timetable_daily_list",
                    onClick = { selectedLayout = LectureTimetablePdfLayout.DAILY_LIST }
                )

                if (semesterOptions.isNotEmpty()) {
                    Box {
                        OutlinedButton(
                            onClick = { semesterMenuExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("lecture_timetable_semester_picker")
                        ) {
                            Text(
                                semesterOptions.find { it.id == selectedSemesterId }?.name
                                    ?: "Choose semester"
                            )
                        }
                        DropdownMenu(
                            expanded = semesterMenuExpanded,
                            onDismissRequest = { semesterMenuExpanded = false }
                        ) {
                            semesterOptions.forEach { semester ->
                                DropdownMenuItem(
                                    text = { Text(semester.name) },
                                    onClick = {
                                        selectedSemesterId = semester.id
                                        semesterMenuExpanded = false
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedSemester = semesterOptions.find { it.id == selectedSemesterId }
                        ?: semesters.find { it.id == student.currentSemesterId }
                    val semesterName = selectedSemester?.name
                        ?: student.academicSession.ifBlank { "Current_Session" }
                    val fileSafeSemester = semesterName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                    createPdfLauncher.launch("GPA_Whiz_Lecture_Timetable_$fileSafeSemester.pdf")
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag("lecture_timetable_save_button")
            ) {
                Text("Choose location")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("lecture_timetable_export_dialog")
    )
}

@Composable
private fun LayoutChoice(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
