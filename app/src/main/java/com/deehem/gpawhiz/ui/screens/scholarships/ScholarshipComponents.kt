package com.deehem.gpawhiz.ui.screens.scholarships

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.deehem.gpawhiz.data.Scholarship
import com.deehem.gpawhiz.ui.viewmodel.GpaViewModel

@Composable
fun CurrencySettingsSection(viewModel: GpaViewModel) {
    // Multi-currency settings card
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Default Currency: $defaultCurrency",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ScholarshipExportDialog(
    viewModel: GpaViewModel,
    singleScholarship: Scholarship?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf("PDF") }

    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                if (singleScholarship != null) {
                    viewModel.exportSingleScholarshipPdf(singleScholarship, os)
                } else {
                    viewModel.exportScholarshipsPdf(os)
                }
            }
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Export Scholarship Report") },
        text = {
            Column {
                Text("Select export format:")
                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    FilterChip(
                        selected = selectedFormat == "PDF",
                        onClick = { selectedFormat = "PDF" },
                        label = { Text("PDF Report") }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                createDocLauncher.launch("Scholarship_Report_${System.currentTimeMillis()}.pdf")
            }) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
