package com.deehem.gpawhiz.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deehem.gpawhiz.data.Exam
import com.deehem.gpawhiz.data.StudySession
import com.deehem.gpawhiz.data.StudyStatus
import com.deehem.gpawhiz.data.TimetableSlot
import java.text.SimpleDateFormat
import java.util.*

private fun parseTimeToMinutes(timeStr: String): Int {
    val parts = timeStr.trim().split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return h * 60 + m
}

@Composable
fun HappeningTodayCard(
    timetableSlots: List<TimetableSlot>,
    studySessions: List<StudySession>,
    exams: List<Exam>,
    onNavigateToTimetable: () -> Unit = {},
    onStartPomodoro: (StudySession) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val now = remember { Calendar.getInstance() }
    val currentDayOfWeek = remember(now) {
        when (now.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    val currentMinutes = remember(now) {
        now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    }

    // Today's lectures in time order
    val todayLectures = remember(timetableSlots, currentDayOfWeek) {
        timetableSlots
            .filter { it.dayOfWeek == currentDayOfWeek }
            .sortedBy { parseTimeToMinutes(it.startTime) }
    }

    // Identify current and next lecture
    val currentLecture = remember(todayLectures, currentMinutes) {
        todayLectures.firstOrNull { slot ->
            val start = parseTimeToMinutes(slot.startTime)
            val end = parseTimeToMinutes(slot.endTime)
            currentMinutes in start..end
        }
    }

    val nextLecture = remember(todayLectures, currentMinutes, currentLecture) {
        todayLectures.firstOrNull { slot ->
            slot != currentLecture && parseTimeToMinutes(slot.startTime) > currentMinutes
        }
    }

    // Today's self-study sessions that are still Upcoming
    val todayStudySessions = remember(studySessions) {
        val todayYear = now.get(Calendar.YEAR)
        val todayDayOfYear = now.get(Calendar.DAY_OF_YEAR)
        studySessions.filter { session ->
            val sessionCal = Calendar.getInstance().apply { timeInMillis = session.date }
            sessionCal.get(Calendar.YEAR) == todayYear &&
            sessionCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear &&
            session.status.equals(StudyStatus.UPCOMING, ignoreCase = true)
        }.sortedBy { parseTimeToMinutes(it.startTime) }
    }

    // Exams within the next 7 days
    val upcomingExams = remember(exams) {
        val todayMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        exams.mapNotNull { exam ->
            val examCal = Calendar.getInstance().apply {
                timeInMillis = exam.date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffDays = ((examCal.timeInMillis - todayMidnight) / (24 * 60 * 60 * 1000L)).toInt()
            if (diffDays in 0..7) exam to diffDays else null
        }.sortedBy { it.second }
    }

    val isAgendaEmpty = todayLectures.isEmpty() && todayStudySessions.isEmpty() && upcomingExams.isEmpty()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("happening_today_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CARD HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "HAPPENING TODAY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val dateFormatted = remember {
                            SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(Date())
                        }
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!isAgendaEmpty) {
                    TextButton(
                        onClick = onNavigateToTimetable,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("View Timetable", fontSize = 12.sp)
                    }
                }
            }

            // EMPTY STATE
            if (isAgendaEmpty) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF0F9D58),
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nothing Scheduled Today",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Take a breather, catch up on readings, or schedule a self-study block.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // 1. TODAY'S LECTURES
                if (todayLectures.isNotEmpty()) {
                    Text(
                        text = "LECTURES TODAY (${todayLectures.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )

                    todayLectures.forEach { slot ->
                        val isNow = slot == currentLecture
                        val isNext = slot == nextLecture

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isNow) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else if (isNext) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (isNow) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else if (isNext) MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = slot.courseCode,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )

                                        if (isNow) {
                                            Surface(
                                                color = Color(0xFF0F9D58),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "NOW",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (isNext) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondary,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "NEXT",
                                                    color = MaterialTheme.colorScheme.onSecondary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (slot.venue.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = slot.venue,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "${slot.startTime} - ${slot.endTime}",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 2. TODAY'S SELF-STUDY SESSIONS
                if (todayStudySessions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "UPCOMING STUDY SESSIONS (${todayStudySessions.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 0.8.sp
                    )

                    todayStudySessions.forEach { session ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.courseCode,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${session.startTime} • ${session.durationMinutes} mins",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { onStartPomodoro(session) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp).testTag("today_start_pomodoro_${session.id}")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Focus", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 3. UPCOMING EXAMS (NEXT 7 DAYS)
                if (upcomingExams.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "EXAMS WITHIN 7 DAYS (${upcomingExams.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFDB4437),
                        letterSpacing = 0.8.sp
                    )

                    upcomingExams.forEach { (exam, daysRemaining) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFDB4437).copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color(0xFFDB4437).copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exam.courseCode,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFFDB4437)
                                    )
                                    Text(
                                        text = "Time: ${exam.time}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (daysRemaining <= 1) Color(0xFFDB4437) else Color(0xFFF4B400)
                                ) {
                                    Text(
                                        text = when (daysRemaining) {
                                            0 -> "TODAY"
                                            1 -> "TOMORROW"
                                            else -> "$daysRemaining DAYS"
                                        },
                                        color = if (daysRemaining <= 1) Color.White else Color.Black,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
