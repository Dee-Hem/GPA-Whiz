package com.example.ui.screens.scholarships

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.service.ScholarshipCalculationHelper
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScholarshipFormDialog(
    scholarship: Scholarship? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        organization: String,
        description: String,
        amount: Double,
        currency: String,
        applicationUrl: String,
        organizationWebsite: String,
        contactEmail: String,
        notes: String,
        openingDate: Long?,
        deadlineDate: Long?,
        expectedFeedbackDate: Long?,
        testDate: Long?,
        interviewDate: Long?,
        followUpDate: Long?,
        status: String,
        minCgpa: Double?,
        minScale: Double,
        selectedPredefined: List<PredefinedRequirement>
    ) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(scholarship?.name ?: "") }
    var organization by remember { mutableStateOf(scholarship?.organization ?: "") }
    var description by remember { mutableStateOf(scholarship?.description ?: "") }
    var amountStr by remember { mutableStateOf(if ((scholarship?.amount ?: 0.0) > 0) scholarship!!.amount.toInt().toString() else "") }
    var currency by remember { mutableStateOf(scholarship?.currency ?: "₦") }
    var applicationUrl by remember { mutableStateOf(scholarship?.applicationUrl ?: "") }
    var organizationWebsite by remember { mutableStateOf(scholarship?.organizationWebsite ?: "") }
    var contactEmail by remember { mutableStateOf(scholarship?.contactEmail ?: "") }
    var notes by remember { mutableStateOf(scholarship?.notes ?: "") }

    var openingDate by remember { mutableStateOf(scholarship?.openingDate) }
    var deadlineDate by remember { mutableStateOf(scholarship?.deadlineDate) }
    var expectedFeedbackDate by remember { mutableStateOf(scholarship?.expectedFeedbackDate) }
    var testDate by remember { mutableStateOf(scholarship?.testDate) }
    var interviewDate by remember { mutableStateOf(scholarship?.interviewDate) }
    var followUpDate by remember { mutableStateOf(scholarship?.followUpDate) }

    var status by remember { mutableStateOf(scholarship?.status ?: ScholarshipStatus.NOT_STARTED) }
    var minCgpaStr by remember { mutableStateOf(scholarship?.minCgpa?.toString() ?: "") }
    var minScale by remember { mutableStateOf(scholarship?.minScale ?: 5.0) }

    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }

    // Predefined requirements quick bundling for new entries
    val selectedBundledReqs = remember { mutableStateListOf<PredefinedRequirement>() }
    var showQuickBundling by remember { mutableStateOf(scholarship == null) }

    val currencies = listOf("₦", "$", "£", "€", "CAD", "AUD", "GHS", "KES", "ZAR", "Other")

    fun showDatePicker(initialMillis: Long?, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        if (initialMillis != null && initialMillis > 0) {
            calendar.timeInMillis = initialMillis
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _, selYear, selMonth, selDay ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selYear)
                    set(Calendar.MONTH, selMonth)
                    set(Calendar.DAY_OF_MONTH, selDay)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onDateSelected(selectedCal.timeInMillis)
            },
            year,
            month,
            day
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (scholarship == null) "Add Scholarship to Track" else "Edit Scholarship Details",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Basic Information
                Text("Basic Information", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Scholarship Name *") },
                    placeholder = { Text("e.g. MTN Science & Tech Scholarship") },
                    modifier = Modifier.fillMaxWidth().testTag("scholarship_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = organization,
                    onValueChange = { organization = it },
                    label = { Text("Provider / Organization *") },
                    placeholder = { Text("e.g. MTN Foundation, Shell, Agbami") },
                    modifier = Modifier.fillMaxWidth().testTag("scholarship_org_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Currency Dropdown
                    Box(modifier = Modifier.weight(0.4f)) {
                        ExposedDropdownMenuBox(
                            expanded = currencyDropdownExpanded,
                            onExpandedChange = { currencyDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = currency,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Currency") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = currencyDropdownExpanded,
                                onDismissRequest = { currencyDropdownExpanded = false }
                            ) {
                                currencies.forEach { curr ->
                                    DropdownMenuItem(
                                        text = { Text(curr) },
                                        onClick = {
                                            currency = curr
                                            currencyDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Amount Input
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Award Amount") },
                        placeholder = { Text("e.g. 200000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.6f).testTag("scholarship_amount_input"),
                        singleLine = true
                    )
                }

                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = statusDropdownExpanded,
                    onExpandedChange = { statusDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Application Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("scholarship_status_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        ScholarshipStatus.ALL.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = {
                                    status = s
                                    statusDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Section 2: Important Dates & Timelines
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Key Dates & Deadlines", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateFieldItem(
                        label = "Opening Date",
                        timestamp = openingDate,
                        onClick = { showDatePicker(openingDate) { openingDate = it } },
                        onClear = { openingDate = null },
                        modifier = Modifier.weight(1f)
                    )
                    DateFieldItem(
                        label = "Deadline Date *",
                        timestamp = deadlineDate,
                        onClick = { showDatePicker(deadlineDate) { deadlineDate = it } },
                        onClear = { deadlineDate = null },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateFieldItem(
                        label = "Expected Feedback",
                        timestamp = expectedFeedbackDate,
                        onClick = { showDatePicker(expectedFeedbackDate) { expectedFeedbackDate = it } },
                        onClear = { expectedFeedbackDate = null },
                        modifier = Modifier.weight(1f)
                    )
                    DateFieldItem(
                        label = "Follow-Up Date",
                        timestamp = followUpDate,
                        onClick = { showDatePicker(followUpDate) { followUpDate = it } },
                        onClear = { followUpDate = null },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateFieldItem(
                        label = "Assessment / Test",
                        timestamp = testDate,
                        onClick = { showDatePicker(testDate) { testDate = it } },
                        onClear = { testDate = null },
                        modifier = Modifier.weight(1f)
                    )
                    DateFieldItem(
                        label = "Interview Date",
                        timestamp = interviewDate,
                        onClick = { showDatePicker(interviewDate) { interviewDate = it } },
                        onClear = { interviewDate = null },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Section 3: Eligibility & CGPA Check
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Eligibility Criteria (Local Comparison)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minCgpaStr,
                        onValueChange = { minCgpaStr = it },
                        label = { Text("Min CGPA (e.g. 4.50)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minScale.toString(),
                        onValueChange = { minScale = it.toDoubleOrNull() ?: 5.0 },
                        label = { Text("Scale Map") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Section 4: URLs & Offline Stored Info
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Contact & Links (Stored Offline)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = applicationUrl,
                    onValueChange = { applicationUrl = it },
                    label = { Text("Application Portal URL") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = organizationWebsite,
                    onValueChange = { organizationWebsite = it },
                    label = { Text("Organization Website") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = contactEmail,
                    onValueChange = { contactEmail = it },
                    label = { Text("Inquiries / Contact Email") },
                    placeholder = { Text("scholarships@provider.org") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Notes") },
                    placeholder = { Text("Add any specific guidelines, eligibility notes, or essay prompts...") },
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    maxLines = 4
                )

                // Quick bundle predefined requirements if creating new scholarship
                if (showQuickBundling && scholarship == null) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Quick-Add Common Requirements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Select standard documents to auto-populate the checklist:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    val commonList = listOf(
                        PredefinedRequirements.LIST.first { it.title == "Academic Transcript" },
                        PredefinedRequirements.LIST.first { it.title == "CV / Resume" },
                        PredefinedRequirements.LIST.first { it.title == "Personal Statement" },
                        PredefinedRequirements.LIST.first { it.title == "Recommendation Letter" },
                        PredefinedRequirements.LIST.first { it.title == "Passport Photograph" },
                        PredefinedRequirements.LIST.first { it.title == "Proof of Enrollment" }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        commonList.forEach { item ->
                            val isChecked = selectedBundledReqs.contains(item)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedBundledReqs.remove(item) else selectedBundledReqs.add(item)
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (it) selectedBundledReqs.add(item) else selectedBundledReqs.remove(item)
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(item.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && organization.isNotBlank()) {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        val minCgpa = minCgpaStr.toDoubleOrNull()
                        onSave(
                            name,
                            organization,
                            description,
                            amt,
                            currency,
                            applicationUrl,
                            organizationWebsite,
                            contactEmail,
                            notes,
                            openingDate,
                            deadlineDate,
                            expectedFeedbackDate,
                            testDate,
                            interviewDate,
                            followUpDate,
                            status,
                            minCgpa,
                            minScale,
                            selectedBundledReqs.toList()
                        )
                    }
                },
                modifier = Modifier.testTag("submit_scholarship_button")
            ) {
                Text(if (scholarship == null) "Create Record" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DateFieldItem(
    label: String,
    timestamp: Long?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = ScholarshipCalculationHelper.formatShortDate(timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (timestamp != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (timestamp != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = label,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
