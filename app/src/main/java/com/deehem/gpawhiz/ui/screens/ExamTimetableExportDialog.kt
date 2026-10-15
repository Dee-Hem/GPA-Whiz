package com.deehem.gpawhiz.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.deehem.gpawhiz.ui.viewmodel.GpaViewModel
import java.io.IOException

@Composable
fun ExamTimetableExportDialog(
    viewModel: GpaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var upcomingOnly by remember { mutableStateOf(true) }
    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("Could not open the selected file for writing.")
                outputStream.use { viewModel.exportExamTimetablePdf(it, upcomingOnly) }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Failed to export exam timetable: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export exam timetable (PDF)", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Choose which exams to include.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExamExportChoice(
                    label = "Upcoming exams only",
                    selected = upcomingOnly,
                    testTag = "exam_export_upcoming_only",
                    onClick = { upcomingOnly = true }
                )
                ExamExportChoice(
                    label = "All exams",
                    selected = !upcomingOnly,
                    testTag = "exam_export_all",
                    onClick = { upcomingOnly = false }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { createPdfLauncher.launch("GPA_Whiz_Exam_Timetable.pdf") },
                modifier = Modifier.heightIn(min = 48.dp).testTag("exam_timetable_save_button")
            ) {
                Text("Choose location")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("exam_timetable_export_dialog")
    )
}

@Composable
private fun ExamExportChoice(
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
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
