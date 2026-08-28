package com.example.ui.screens.scholarships

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Scholarship
import com.example.ui.viewmodel.GpaViewModel

@Composable
fun ScholarshipExportDialog(
    viewModel: GpaViewModel,
    singleScholarship: Scholarship? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedAction by remember { mutableStateOf<String?>(null) }

    // SAF Document Launchers
    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                when (selectedAction) {
                    "all_pdf" -> viewModel.exportScholarshipsPdf(outputStream, activeOnly = false)
                    "active_pdf" -> viewModel.exportScholarshipsPdf(outputStream, activeOnly = true)
                    "single_pdf" -> {
                        singleScholarship?.let { viewModel.exportSingleScholarshipPdf(it, outputStream) }
                    }
                }
            }
        }
        onDismiss()
    }

    val xlsxExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                when (selectedAction) {
                    "single_xlsx" -> {
                        singleScholarship?.let { viewModel.exportSingleScholarshipXlsx(it, outputStream) }
                    }
                    else -> viewModel.exportScholarshipsXlsx(outputStream)
                }
            }
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (singleScholarship != null) "Export Scholarship Record" else "Export All Scholarships",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Generate and save completely offline reports directly to your device storage:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (singleScholarship != null) {
                    ExportOptionCard(
                        title = "Export Selected Scholarship (PDF)",
                        subtitle = "Personalized dossier with requirements, critical milestones, timeline & outcome for '${singleScholarship.name}'",
                        badge = "PDF",
                        badgeColor = Color(0xFF0F9D58),
                        onClick = {
                            selectedAction = "single_pdf"
                            val cleanName = singleScholarship.name.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                            pdfExportLauncher.launch("Scholarship_Dossier_${cleanName}.pdf")
                        }
                    )

                    ExportOptionCard(
                        title = "Export Selected Scholarship (Excel .xlsx)",
                        subtitle = "Multi-sheet workbook detailing overview, requirements checklist & event log for '${singleScholarship.name}'",
                        badge = "XLSX",
                        badgeColor = Color(0xFF2E7D32),
                        onClick = {
                            selectedAction = "single_xlsx"
                            val cleanName = singleScholarship.name.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                            xlsxExportLauncher.launch("Scholarship_${cleanName}.xlsx")
                        }
                    )
                }

                ExportOptionCard(
                    title = "Export All Scholarships (PDF)",
                    subtitle = "Comprehensive report with applicant profile, status overview, upcoming deadlines & multi-page table",
                    badge = "PDF",
                    badgeColor = Color(0xFF0F9D58),
                    onClick = {
                        selectedAction = "all_pdf"
                        pdfExportLauncher.launch("GPA_Whiz_All_Scholarships.pdf")
                    }
                )

                ExportOptionCard(
                    title = "Export Active Applications Only (PDF)",
                    subtitle = "Focused report on in-progress applications and immediate deadlines",
                    badge = "PDF",
                    badgeColor = Color(0xFF1A73E8),
                    onClick = {
                        selectedAction = "active_pdf"
                        pdfExportLauncher.launch("GPA_Whiz_Active_Scholarships.pdf")
                    }
                )

                ExportOptionCard(
                    title = "Export All Scholarships to Excel (.xlsx)",
                    subtitle = "4-Sheet workbook: Dashboard with student metadata & filters, Requirements, Timeline & Summary",
                    badge = "XLSX",
                    badgeColor = Color(0xFF2E7D32),
                    onClick = {
                        selectedAction = "all_xlsx"
                        xlsxExportLauncher.launch("GPA_Whiz_Scholarships_Tracker.xlsx")
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ExportOptionCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badge,
                            color = badgeColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
