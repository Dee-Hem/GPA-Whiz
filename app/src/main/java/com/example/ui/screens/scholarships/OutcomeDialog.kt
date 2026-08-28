package com.example.ui.screens.scholarships

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import com.example.data.Scholarship
import com.example.service.ScholarshipCalculationHelper
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutcomeDialog(
    scholarship: Scholarship,
    onDismiss: () -> Unit,
    onSave: (outcome: String, awardAmount: Double?, awardCurrency: String?, awardDate: Long?, notes: String?) -> Unit
) {
    val context = LocalContext.current

    val outcomes = listOf("Awarded", "Rejected", "Waitlisted", "Withdrawn", "Other")
    var selectedOutcome by remember { mutableStateOf(scholarship.outcome ?: "Awarded") }
    var awardAmountStr by remember { mutableStateOf(if ((scholarship.awardAmount ?: scholarship.amount) > 0) (scholarship.awardAmount ?: scholarship.amount).toInt().toString() else "") }
    var awardCurrency by remember { mutableStateOf(scholarship.awardCurrency ?: scholarship.currency) }
    var awardDate by remember { mutableStateOf(scholarship.awardDate ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf(scholarship.awardNotes ?: "") }

    var outcomeDropdownExpanded by remember { mutableStateOf(false) }

    fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = awardDate }
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
                }
                awardDate = selectedCal.timeInMillis
            },
            year,
            month,
            day
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Application Outcome", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Outcome Selector
                ExposedDropdownMenuBox(
                    expanded = outcomeDropdownExpanded,
                    onExpandedChange = { outcomeDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedOutcome,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Outcome Decision") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = outcomeDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("outcome_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = outcomeDropdownExpanded,
                        onDismissRequest = { outcomeDropdownExpanded = false }
                    ) {
                        outcomes.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    selectedOutcome = opt
                                    outcomeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // If Awarded, show amount & currency fields
                if (selectedOutcome == "Awarded") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = awardCurrency,
                            onValueChange = { awardCurrency = it },
                            label = { Text("Currency") },
                            modifier = Modifier.weight(0.35f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = awardAmountStr,
                            onValueChange = { awardAmountStr = it },
                            label = { Text("Awarded Value") },
                            placeholder = { Text("e.g. 500000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.65f),
                            singleLine = true
                        )
                    }
                }

                // Outcome Date Picker
                DateFieldItem(
                    label = "Decision / Announcement Date",
                    timestamp = awardDate,
                    onClick = { showDatePicker() },
                    onClear = { awardDate = System.currentTimeMillis() },
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Feedback Remarks") },
                    placeholder = { Text("e.g. Award letter received. Orientation scheduled for September...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = awardAmountStr.toDoubleOrNull()
                    onSave(selectedOutcome, amt, awardCurrency, awardDate, notes.ifBlank { null })
                },
                modifier = Modifier.testTag("submit_outcome_button")
            ) {
                Text("Save Outcome")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
