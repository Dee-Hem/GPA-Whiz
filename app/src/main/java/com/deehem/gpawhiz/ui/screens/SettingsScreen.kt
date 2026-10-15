package com.deehem.gpawhiz.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.deehem.gpawhiz.ui.screens.scholarships.CurrencySettingsSection
import com.deehem.gpawhiz.ui.viewmodel.GpaViewModel

@Composable
fun ThemeOptionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

@Composable
fun SettingsScreen(
    viewModel: GpaViewModel,
    onNavigateToAssistant: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val timetableSlots by viewModel.timetableSlots.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val studentProfile by viewModel.studentProfile.collectAsState()

    // SAF launchers
    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    val backupText = viewModel.exportBackup()
                    os.write(backupText.toByteArray())
                    os.flush()
                }
                Toast.makeText(context, "Database exported to JSON document.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export JSON: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val openJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { ins ->
                    val jsonText = ins.bufferedReader().use { r -> r.readText() }
                    viewModel.restoreBackup(context, jsonText)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read backup file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    viewModel.exportTranscriptPdf(os)
                }
                Toast.makeText(context, "PDF Statement compiled successfully.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to compile transcript: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "DISPLAY & APPEARANCE",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val themeMode by viewModel.themeMode.collectAsState()
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Column {
                        Text(
                            text = "Application Theme",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Choose between Light, Dark or System Default",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionButton(
                        label = "System",
                        isSelected = themeMode == 0,
                        onClick = { viewModel.setThemeMode(0) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionButton(
                        label = "Light",
                        isSelected = themeMode == 1,
                        onClick = { viewModel.setThemeMode(1) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionButton(
                        label = "Dark",
                        isSelected = themeMode == 2,
                        onClick = { viewModel.setThemeMode(2) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // SECURITY & PRIVACY SECTION
        Text(
            text = "SECURITY & PRIVACY",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val biometricEnabled by viewModel.biometricEnabled.collectAsState()
        val canUseBiometric = remember { com.deehem.gpawhiz.service.BiometricAuthHelper.canAuthenticate(context) }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth().testTag("security_settings_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Biometric App Lock",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (canUseBiometric) {
                                "Require fingerprint, face, or device PIN/pattern to access GPA Whiz"
                            } else {
                                "Biometric lock unavailable (set up PIN/Pattern or fingerprint in device Settings)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { isChecking ->
                            if (isChecking) {
                                if (canUseBiometric) {
                                    val activity = context as? androidx.fragment.app.FragmentActivity
                                    if (activity != null) {
                                        com.deehem.gpawhiz.service.BiometricAuthHelper.authenticate(
                                            activity = activity,
                                            title = "Enable App Lock",
                                            subtitle = "Verify your credentials to enable biometric lock",
                                            onSuccess = { viewModel.setBiometricEnabled(true) },
                                            onError = { Toast.makeText(context, "Verification failed: $it", Toast.LENGTH_SHORT).show() }
                                        )
                                    } else {
                                        viewModel.setBiometricEnabled(true)
                                    }
                                } else {
                                    Toast.makeText(context, "Please set up device security (PIN/Fingerprint) first", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                viewModel.setBiometricEnabled(false)
                            }
                        },
                        enabled = canUseBiometric || biometricEnabled,
                        modifier = Modifier.testTag("biometric_lock_switch")
                    )
                }

                if (biometricEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "App is secured. Verification will be requested when resuming or opening GPA Whiz.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Multi-Currency & Offline Exchange Rates Section
        CurrencySettingsSection(viewModel = viewModel)

        Text(
            text = "ACADEMIC UTILITIES & TOOLS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Column {
                        Text(
                            text = "Academic Assistant",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "GPA scale converters and percentage mapping",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNavigateToAssistant,
                    modifier = Modifier.fillMaxWidth().testTag("open_academic_assistant_settings_button")
                ) {
                    Text("Launch Academic Converters")
                }
            }
        }

        Text(
            text = "TRANSCRIPT & TRANSFERS PORTABILITY",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Generate printable academic forms or transfer files locally over Bluetooth/Bluetooth-Sharing. Rest assured, your data is kept offline. Zero networks, total independence.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 1. PDF Transcript Block
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info, // Use info for docs
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Column {
                        Text(
                            text = "Official Academic Statement",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Render fully-styled A4 PDF Transcripts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        val sdf = java.text.SimpleDateFormat("yyyy_MM_dd", java.util.Locale.getDefault())
                        val defaultFilename = "gpa_whiz_transcript_${sdf.format(java.util.Date())}.pdf"
                        createPdfLauncher.launch(defaultFilename)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("compile_pdf_button")
                ) {
                    Text("Compile Printable PDF Transcript")
                }
            }
        }

        // 2. Scholarship Exports Block
        var showScholarshipExportDialog by remember { mutableStateOf(false) }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Column {
                        Text(
                            text = "Scholarship Reports & Excel",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Export dossier PDFs and multi-sheet .xlsx workbooks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showScholarshipExportDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth().testTag("export_scholarships_settings_button")
                ) {
                    Text("Export Scholarship PDF / Excel")
                }
            }
        }

        if (showScholarshipExportDialog) {
            com.deehem.gpawhiz.ui.screens.scholarships.ScholarshipExportDialog(
                viewModel = viewModel,
                singleScholarship = null,
                onDismiss = { showScholarshipExportDialog = false }
            )
        }

        // 3. Lecture Timetable Export
        var showLectureTimetableExportDialog by remember { mutableStateOf(false) }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Column {
                        Text(
                            text = "Lecture Timetable",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Save your scheduled lectures as a printable PDF",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showLectureTimetableExportDialog = true },
                    enabled = timetableSlots.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("export_timetable_pdf_button")
                ) {
                    Text("Export lecture timetable (PDF)")
                }
                if (timetableSlots.isEmpty()) {
                    Text(
                        text = "Add lectures in the Timetable tab first",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp).testTag("timetable_export_empty_hint")
                    )
                }
            }
        }

        if (showLectureTimetableExportDialog) {
            LectureTimetableExportDialog(
                viewModel = viewModel,
                student = studentProfile,
                semesters = semesters,
                courses = courses,
                slots = timetableSlots,
                onDismiss = { showLectureTimetableExportDialog = false }
            )
        }

        // 4. Data Portability Backup & Restores Blocks
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Column {
                        Text(
                            text = "Data Portability (Zero-Permission)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Save databases as plain-text JSON backup sheets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                            val defaultFilename = "gpawhiz_backup_${sdf.format(java.util.Date())}.json"
                            createJsonLauncher.launch(defaultFilename)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f).testTag("backup_json_button")
                    ) {
                        Text("Export JSON")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            openJsonLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier.weight(1f).testTag("restore_json_button")
                    ) {
                        Text("Restore Backup")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Small Local Credits
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "GPA Whiz (Nigeria)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = "Built secure. Strict locally compiled calculations.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
