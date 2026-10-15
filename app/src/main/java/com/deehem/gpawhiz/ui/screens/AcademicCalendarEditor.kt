package com.deehem.gpawhiz.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.deehem.gpawhiz.data.AcademicPeriod
import com.deehem.gpawhiz.data.Semester
import com.deehem.gpawhiz.service.AcademicCalendarLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val academicPeriodTypes = listOf("LECTURES", "CA_TEST", "EXAMS", "BREAK")

@Composable
fun AcademicCalendarEditor(
    semester: Semester,
    initialPeriods: List<AcademicPeriod>,
    onDismiss: () -> Unit,
    onSave: (Long?, List<AcademicPeriod>) -> Unit
) {
    val context = LocalContext.current
    var startDate by remember(semester.id, semester.timelineStartDate) {
        mutableStateOf(semester.timelineStartDate)
    }
    var periods by remember(semester.id, initialPeriods) {
        mutableStateOf(initialPeriods.sortedBy { it.sortOrder })
    }
    var periodToDelete by remember { mutableStateOf<Int?>(null) }
    val validationError = AcademicCalendarLayout.validationError(periods)
    val canSave = validationError == null && (periods.isEmpty() || startDate != null)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Academic calendar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(semester.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("calendar_close_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close calendar editor")
                    }
                }

                OutlinedButton(
                    onClick = {
                        val initialDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = startDate ?: System.currentTimeMillis()
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                startDate = AcademicCalendarLayout.dateAtUtcMidnight(year, month, day)
                            },
                            initialDate.get(Calendar.YEAR),
                            initialDate.get(Calendar.MONTH),
                            initialDate.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("calendar_start_date_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(startDate?.let(::formatCalendarDate) ?: "Choose timeline start date")
                }

                if (periods.isNotEmpty() && startDate != null && validationError == null) {
                    AcademicTimelineBar(startDate!!, periods)
                }

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Starting points", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                periods = preset(semester.id, listOf("LECTURES" to 13, "EXAMS" to 3))
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("calendar_preset_lectures_exams")
                        ) { Text("Lectures and exams") }
                        OutlinedButton(
                            onClick = {
                                periods = preset(
                                    semester.id,
                                    listOf("LECTURES" to 7, "CA_TEST" to 1, "BREAK" to 1, "LECTURES" to 5, "EXAMS" to 3)
                                )
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("calendar_preset_ca_break")
                        ) { Text("With CA and a break") }
                    }

                    Text("Add period", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        academicPeriodTypes.take(2).forEach { type ->
                            AddPeriodButton(type, semester.id, periods) { periods = it }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        academicPeriodTypes.drop(2).forEach { type ->
                            AddPeriodButton(type, semester.id, periods) { periods = it }
                        }
                    }

                    val ranges = if (startDate != null && validationError == null) {
                        AcademicCalendarLayout.layout(startDate!!, periods)
                    } else emptyList()
                    periods.forEachIndexed { index, period ->
                        AcademicPeriodCard(
                            period = period,
                            index = index,
                            rangeText = ranges.getOrNull(index)?.let { formatRange(it.startDate, it.endDate) },
                            canMoveUp = index > 0,
                            canMoveDown = index < periods.lastIndex,
                            canExtend = period.weeks < 30 && periods.sumOf { it.weeks } < 60,
                            onWeeksChange = { weeks -> periods = periods.mapIndexed { i, item -> if (i == index) item.copy(weeks = weeks) else item } },
                            onTypeChange = { type -> periods = periods.mapIndexed { i, item -> if (i == index) item.copy(type = type) else item } },
                            onLabelChange = { label -> periods = periods.mapIndexed { i, item -> if (i == index) item.copy(label = label.ifBlank { null }) else item } },
                            onMove = { direction -> periods = AcademicCalendarLayout.moveAtIndex(periods, index, direction) },
                            onExtend = {
                                periods = AcademicCalendarLayout.extendAtIndex(periods, index)
                            },
                            onDelete = { periodToDelete = index }
                        )
                    }

                    if (periods.isEmpty()) {
                        Text("No periods added.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (periods.isNotEmpty() && startDate == null) {
                        Text("Choose a start date to set period dates.", color = MaterialTheme.colorScheme.error)
                    }
                    validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Text("${periods.sumOf { it.weeks }} of 60 weeks maximum", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            startDate = null
                            periods = emptyList()
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("calendar_clear_button")
                    ) { Text("Clear calendar") }
                    TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) { Text("Cancel") }
                    Button(
                        onClick = { onSave(startDate, periods) },
                        enabled = canSave,
                        modifier = Modifier.height(48.dp).testTag("calendar_save_button")
                    ) { Text("Save") }
                }
            }
        }
    }

    periodToDelete?.let { index ->
        val period = periods.getOrNull(index)
        AlertDialog(
            onDismissRequest = { periodToDelete = null },
            title = { Text("Delete period?") },
            text = { Text("Delete ${period?.type?.let(::periodTypeLabel) ?: "this period"} from the calendar?") },
            confirmButton = {
                Button(
                    onClick = {
                        periods = periods.filterIndexed { itemIndex, _ -> itemIndex != index }
                        periodToDelete = null
                    },
                    modifier = Modifier.testTag("calendar_confirm_delete_period")
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { periodToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RowScope.AddPeriodButton(
    type: String,
    semesterId: Int,
    periods: List<AcademicPeriod>,
    onPeriodsChange: (List<AcademicPeriod>) -> Unit
) {
    OutlinedButton(
        onClick = {
            onPeriodsChange(
                (periods + AcademicPeriod(
                    semesterId = semesterId,
                    sortOrder = periods.size,
                    type = type,
                    weeks = 1
                )).mapIndexed { index, period -> period.copy(sortOrder = index) }
            )
        },
        modifier = Modifier.weight(1f).height(48.dp).testTag("calendar_add_${type.lowercase()}_button"),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(periodTypeLabel(type), maxLines = 1, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun AcademicPeriodCard(
    period: AcademicPeriod,
    index: Int,
    rangeText: String?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canExtend: Boolean,
    onWeeksChange: (Int) -> Unit,
    onTypeChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
    onMove: (Int) -> Unit,
    onExtend: () -> Unit,
    onDelete: () -> Unit
) {
    var typeMenuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("calendar_period_card_$index"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = periodColor(period.type).copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        periodTypeLabel(period.type),
                        color = periodColor(period.type),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onMove(-1) }, enabled = canMoveUp, modifier = Modifier.testTag("calendar_move_up_$index")) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move period up")
                }
                IconButton(onClick = { onMove(1) }, enabled = canMoveDown, modifier = Modifier.testTag("calendar_move_down_$index")) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move period down")
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag("calendar_delete_period_$index")) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete period", tint = MaterialTheme.colorScheme.error)
                }
            }
            Text(rangeText ?: "Choose a start date to see dates", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    OutlinedButton(
                        onClick = { typeMenuExpanded = true },
                        modifier = Modifier.height(48.dp).testTag("calendar_period_type_$index")
                    ) { Text(periodTypeLabel(period.type)) }
                    DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                        academicPeriodTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(periodTypeLabel(type)) },
                                onClick = {
                                    onTypeChange(type)
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = { onWeeksChange(period.weeks - 1) },
                    enabled = period.weeks > 1,
                    modifier = Modifier.testTag("calendar_weeks_decrease_$index")
                ) { Icon(Icons.Default.Remove, contentDescription = "Decrease weeks") }
                Text("${period.weeks} ${if (period.weeks == 1) "week" else "weeks"}", fontWeight = FontWeight.Bold, modifier = Modifier.testTag("calendar_weeks_value_$index"))
                IconButton(
                    onClick = { onWeeksChange(period.weeks + 1) },
                    enabled = canExtend,
                    modifier = Modifier.testTag("calendar_weeks_increase_$index")
                ) { Icon(Icons.Default.Add, contentDescription = "Increase weeks") }
            }
            OutlinedTextField(
                value = period.label.orEmpty(),
                onValueChange = onLabelChange,
                label = { Text("Optional label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("calendar_period_label_$index")
            )
            OutlinedButton(
                onClick = onExtend,
                enabled = canExtend,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("calendar_extend_week_$index")
            ) { Text("Extend by 1 week") }
        }
    }
}

@Composable
private fun AcademicTimelineBar(startDate: Long, periods: List<AcademicPeriod>) {
    val totalWeeks = periods.sumOf { it.weeks }
    if (totalWeeks == 0) return
    val ranges = AcademicCalendarLayout.layout(startDate, periods)
    val today = AcademicCalendarLayout.todayAtUtcMidnight()
    val firstDay = ranges.first().startDate
    val lastDay = ranges.last().endDate
    val showMarker = today in firstDay..lastDay
    val markerFraction = if (showMarker) {
        ((today - firstDay).toFloat() / (lastDay - firstDay + 86_400_000L).toFloat()).coerceIn(0f, 1f)
    } else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(22.dp).testTag("calendar_timeline_bar")) {
            Row(
                modifier = Modifier.fillMaxWidth().height(14.dp).align(Alignment.Center).clip(RoundedCornerShape(7.dp))
            ) {
                periods.forEach { period ->
                    Box(
                        modifier = Modifier.weight(period.weeks.toFloat()).fillMaxHeight().background(periodColor(period.type))
                    )
                }
            }
            if (showMarker) {
                Box(
                    modifier = Modifier
                        .offset(x = maxWidth * markerFraction - 1.dp, y = 1.dp)
                        .width(2.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                        .testTag("calendar_today_marker")
                )
            }
        }
        Text("${periods.sumOf { it.weeks }} weeks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun preset(semesterId: Int, entries: List<Pair<String, Int>>) = entries.mapIndexed { index, (type, weeks) ->
    AcademicPeriod(semesterId = semesterId, sortOrder = index, type = type, weeks = weeks)
}

private fun periodTypeLabel(type: String): String = when (type) {
    "CA_TEST" -> "CA / Test"
    else -> type.lowercase().replaceFirstChar { it.uppercase() }
}

@Composable
private fun periodColor(type: String): Color = when (type) {
    "LECTURES" -> MaterialTheme.colorScheme.primary
    "CA_TEST" -> MaterialTheme.colorScheme.tertiary
    "EXAMS" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.secondary
}

private fun formatCalendarDate(date: Long): String = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}.format(date)

private fun formatRange(startDate: Long, endDate: Long): String {
    val start = SimpleDateFormat("d MMM", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val end = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val startYear = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = startDate }.get(Calendar.YEAR)
    val endYear = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = endDate }.get(Calendar.YEAR)
    return if (startYear == endYear) "${start.format(startDate)} to ${end.format(endDate)}"
    else "${end.format(startDate)} to ${end.format(endDate)}"
}