package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TimetableSlot
import com.example.service.AlarmScheduler
import com.example.service.SystemSchedulerWrapper
import com.example.ui.viewmodel.GpaViewModel
import java.util.Calendar

@Composable
fun TimetableScreen(
    viewModel: GpaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val slots by viewModel.timetableSlots.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val daysMap = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WEEKLY LECTURE TIMETABLE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_lecture_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Schedule")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Schedule")
            }
        }

        // List scheduled days of week with cards
        if (slots.isEmpty()) {
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
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your timetable is currently blank.",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Schedule your university lectures to set exact 1-hour preparation reminders offline.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Group slices by day of the week
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
                                            onDelete = { viewModel.deleteTimetableSlot(context, slot) },
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
        var slotCode by remember { mutableStateOf("") }
        var slotVenue by remember { mutableStateOf("") }
        var slotDayIndex by remember { mutableStateOf(1) } // Default Monday
        var slotStart by remember { mutableStateOf("08:00") }
        var slotEnd by remember { mutableStateOf("10:00") }

        var expandedDay by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Schedule New Lecture Session") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = slotCode,
                        onValueChange = { slotCode = it },
                        label = { Text("Course Code (e.g. CSC 202)") },
                        modifier = Modifier.fillMaxWidth().testTag("lecture_code_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = slotVenue,
                        onValueChange = { slotVenue = it },
                        label = { Text("Venue (e.g. LT 1, Faculty Annex)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Day of Week spinner dropdown selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedDay = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Weekday: ${daysMap[slotDayIndex - 1]}")
                        }
                        DropdownMenu(
                            expanded = expandedDay,
                            onDismissRequest = { expandedDay = false }
                        ) {
                            daysMap.forEachIndexed { idx, day ->
                                DropdownMenuItem(
                                    text = { Text(day) },
                                    onClick = {
                                        slotDayIndex = idx + 1
                                        expandedDay = false
                                    }
                                )
                            }
                        }
                    }

                    // Times Select
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeSelectionBox(
                            label = "Start",
                            time = slotStart,
                            onTimeSelected = { slotStart = it },
                            modifier = Modifier.weight(1f)
                        )
                        TimeSelectionBox(
                            label = "End",
                            time = slotEnd,
                            onTimeSelected = { slotEnd = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (slotCode.isNotEmpty() && slotVenue.isNotEmpty()) {
                            viewModel.addTimetableSlot(
                                context = context,
                                courseCode = slotCode,
                                venue = slotVenue,
                                dayOfWeek = slotDayIndex,
                                startTime = slotStart,
                                endTime = slotEnd
                            )
                            // Auto-redirect pre-filled details out to Native calendar standard
                            val tempSlot = TimetableSlot(
                                courseCode = slotCode,
                                venue = slotVenue,
                                dayOfWeek = slotDayIndex,
                                startTime = slotStart,
                                endTime = slotEnd
                            )
                            SystemSchedulerWrapper.redirectToSystemCalendar(context, tempSlot)
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_lecture_button")
                ) {
                    Text("Add Slot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
                    // Send to Native Alarm Clock App
                    IconButton(onClick = onSetDeviceAlarm) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Set System Clock Alarm",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Calendar Sync Quick Button
                    IconButton(onClick = onExport) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Sync to Calendar App",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Toggle notification alerts (Exact Alarm toggle)
                    IconButton(onClick = onToggleAlarm) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Toggle Alarm",
                            tint = if (slot.alertEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSelectionBox(
    label: String,
    time: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parts = time.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerDialog = TimePickerDialog(
        context,
        { _, selHour, selMin ->
            val formatted = String.format("%02d:%02d", selHour, selMin)
            onTimeSelected(formatted)
        },
        hour,
        minute,
        true // 24 hour mode
    )

    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { timePickerDialog.show() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
