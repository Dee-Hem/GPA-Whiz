package com.deehem.gpawhiz.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deehem.gpawhiz.data.StudySession
import com.deehem.gpawhiz.data.StudyStatus
import kotlinx.coroutines.delay
import kotlin.math.max

enum class PomodoroPhase(val label: String) {
    FOCUS("Focus Round"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroFocusScreen(
    session: StudySession,
    onClose: (focusedMinutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Session durations in minutes
    var focusDurationMin by rememberSaveable { mutableIntStateOf(25) }
    var shortBreakDurationMin by rememberSaveable { mutableIntStateOf(5) }
    var longBreakDurationMin by rememberSaveable { mutableIntStateOf(15) }
    val totalRounds = 4

    var currentRound by rememberSaveable { mutableIntStateOf(1) }
    var phase by rememberSaveable { mutableStateOf(PomodoroPhase.FOCUS) }
    var isRunning by rememberSaveable { mutableStateOf(true) }

    var targetEndTimeMillis by rememberSaveable {
        mutableLongStateOf(System.currentTimeMillis() + focusDurationMin * 60 * 1000L)
    }
    var remainingMillis by rememberSaveable {
        mutableLongStateOf(focusDurationMin * 60 * 1000L)
    }
    var totalFocusedMinutesLogged by rememberSaveable { mutableIntStateOf(0) }
    var showStopConfirmDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Keep screen on while timer is running
    DisposableEffect(isRunning) {
        val activity = context as? Activity
        if (isRunning) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Sound & Vibration helper
    fun notifyIntervalCompleted() {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (am != null && am.ringerMode != AudioManager.RINGER_MODE_SILENT) {
            try {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            } catch (_: Exception) { }
        }
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(250)
            }
        } catch (_: Exception) { }
    }

    // Timer Ticking Loop
    LaunchedEffect(isRunning, phase, targetEndTimeMillis) {
        while (isRunning) {
            val now = System.currentTimeMillis()
            val rem = max(0L, targetEndTimeMillis - now)
            remainingMillis = rem

            if (rem <= 0L) {
                notifyIntervalCompleted()

                // If ending a FOCUS phase, accumulate focus minutes
                if (phase == PomodoroPhase.FOCUS) {
                    totalFocusedMinutesLogged += focusDurationMin
                    if (currentRound >= totalRounds) {
                        phase = PomodoroPhase.LONG_BREAK
                        val durMs = longBreakDurationMin * 60 * 1000L
                        remainingMillis = durMs
                        targetEndTimeMillis = System.currentTimeMillis() + durMs
                    } else {
                        phase = PomodoroPhase.SHORT_BREAK
                        val durMs = shortBreakDurationMin * 60 * 1000L
                        remainingMillis = durMs
                        targetEndTimeMillis = System.currentTimeMillis() + durMs
                    }
                } else if (phase == PomodoroPhase.SHORT_BREAK) {
                    phase = PomodoroPhase.FOCUS
                    currentRound += 1
                    val durMs = focusDurationMin * 60 * 1000L
                    remainingMillis = durMs
                    targetEndTimeMillis = System.currentTimeMillis() + durMs
                } else {
                    // Long break finished -> restart cycle
                    phase = PomodoroPhase.FOCUS
                    currentRound = 1
                    val durMs = focusDurationMin * 60 * 1000L
                    remainingMillis = durMs
                    targetEndTimeMillis = System.currentTimeMillis() + durMs
                }
            }
            delay(250)
        }
    }

    // Back button handling
    BackHandler {
        showStopConfirmDialog = true
    }

    if (showStopConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStopConfirmDialog = false },
            title = { Text("Exit Focus Mode?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "You have completed $totalFocusedMinutesLogged focus minute(s) so far. " +
                    "Ending this session will log your completed study minutes into ${session.courseCode}."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStopConfirmDialog = false
                        onClose(totalFocusedMinutesLogged)
                    }
                ) {
                    Text("Exit & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmDialog = false }) {
                    Text("Stay Focused")
                }
            }
        )
    }

    // Duration Settings Dialog
    if (showSettingsDialog) {
        var tempFocus by remember { mutableIntStateOf(focusDurationMin) }
        var tempShort by remember { mutableIntStateOf(shortBreakDurationMin) }
        var tempLong by remember { mutableIntStateOf(longBreakDurationMin) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Customize Focus Intervals", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Focus Duration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 25, 45, 50).forEach { m ->
                            FilterChip(
                                selected = tempFocus == m,
                                onClick = { tempFocus = m },
                                label = { Text("${m}m") }
                            )
                        }
                    }

                    Text("Short Break", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 10).forEach { m ->
                            FilterChip(
                                selected = tempShort == m,
                                onClick = { tempShort = m },
                                label = { Text("${m}m") }
                            )
                        }
                    }

                    Text("Long Break (after 4 rounds)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 20, 30).forEach { m ->
                            FilterChip(
                                selected = tempLong == m,
                                onClick = { tempLong = m },
                                label = { Text("${m}m") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusDurationMin = tempFocus
                        shortBreakDurationMin = tempShort
                        longBreakDurationMin = tempLong
                        val currentTotalMs = when (phase) {
                            PomodoroPhase.FOCUS -> focusDurationMin * 60 * 1000L
                            PomodoroPhase.SHORT_BREAK -> shortBreakDurationMin * 60 * 1000L
                            PomodoroPhase.LONG_BREAK -> longBreakDurationMin * 60 * 1000L
                        }
                        remainingMillis = currentTotalMs
                        targetEndTimeMillis = System.currentTimeMillis() + currentTotalMs
                        showSettingsDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Total duration for current phase
    val currentPhaseTotalMs = when (phase) {
        PomodoroPhase.FOCUS -> focusDurationMin * 60 * 1000L
        PomodoroPhase.SHORT_BREAK -> shortBreakDurationMin * 60 * 1000L
        PomodoroPhase.LONG_BREAK -> longBreakDurationMin * 60 * 1000L
    }

    val progressFraction = if (currentPhaseTotalMs > 0) {
        (remainingMillis.toFloat() / currentPhaseTotalMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 250),
        label = "pomodoro_progress"
    )

    val activeColor by animateColorAsState(
        targetValue = when (phase) {
            PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.primary
            PomodoroPhase.SHORT_BREAK -> Color(0xFF00897B) // Teal
            PomodoroPhase.LONG_BREAK -> Color(0xFF1E88E5) // Blue
        },
        label = "pomodoro_color"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "FOCUS MODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = activeColor
                        )
                        Text(
                            text = session.courseCode,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { showStopConfirmDialog = true },
                        modifier = Modifier.testTag("pomodoro_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Focus Mode")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Customize Durations")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize().testTag("pomodoro_focus_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // PHASE BADGE & ROUND CHIPS
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = activeColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, activeColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = when (phase) {
                            PomodoroPhase.FOCUS -> "ROUND $currentRound OF $totalRounds"
                            PomodoroPhase.SHORT_BREAK -> "REST & HYDRATE"
                            PomodoroPhase.LONG_BREAK -> "LONG RECHARGE"
                        },
                        color = activeColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4 Round Dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (r in 1..totalRounds) {
                        val isDone = r < currentRound
                        val isCurrent = r == currentRound && phase == PomodoroPhase.FOCUS
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> activeColor
                                        isCurrent -> activeColor.copy(alpha = 0.8f)
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    }
                                )
                        )
                    }
                }
            }

            // LARGE CIRCULAR TIMER CANVAS
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(280.dp)
                    .padding(8.dp)
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Background Track
                    drawCircle(
                        color = trackColor,
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )

                    // Foreground Sweep Arc
                    val sweepAngle = animatedProgress * 360f
                    drawArc(
                        color = activeColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Center Information
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val totalSec = remainingMillis / 1000
                    val minutes = totalSec / 60
                    val seconds = totalSec % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = phase.label.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = activeColor
                    )
                    if (totalFocusedMinutesLogged > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalFocusedMinutesLogged min focused",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // CONTROLS ROW
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset / Skip Back
                    IconButton(
                        onClick = {
                            val durMs = when (phase) {
                                PomodoroPhase.FOCUS -> focusDurationMin * 60 * 1000L
                                PomodoroPhase.SHORT_BREAK -> shortBreakDurationMin * 60 * 1000L
                                PomodoroPhase.LONG_BREAK -> longBreakDurationMin * 60 * 1000L
                            }
                            remainingMillis = durMs
                            targetEndTimeMillis = System.currentTimeMillis() + durMs
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart interval", modifier = Modifier.size(28.dp))
                    }

                    // Large Play / Pause Button
                    Button(
                        onClick = {
                            if (isRunning) {
                                isRunning = false
                            } else {
                                targetEndTimeMillis = System.currentTimeMillis() + remainingMillis
                                isRunning = true
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("pomodoro_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Menu else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Resume",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Skip Button
                    IconButton(
                        onClick = {
                            if (phase == PomodoroPhase.FOCUS) {
                                totalFocusedMinutesLogged += focusDurationMin
                                if (currentRound >= totalRounds) {
                                    phase = PomodoroPhase.LONG_BREAK
                                    val durMs = longBreakDurationMin * 60 * 1000L
                                    remainingMillis = durMs
                                    targetEndTimeMillis = System.currentTimeMillis() + durMs
                                } else {
                                    phase = PomodoroPhase.SHORT_BREAK
                                    val durMs = shortBreakDurationMin * 60 * 1000L
                                    remainingMillis = durMs
                                    targetEndTimeMillis = System.currentTimeMillis() + durMs
                                }
                            } else {
                                phase = PomodoroPhase.FOCUS
                                if (phase == PomodoroPhase.LONG_BREAK) currentRound = 1 else currentRound += 1
                                val durMs = focusDurationMin * 60 * 1000L
                                remainingMillis = durMs
                                targetEndTimeMillis = System.currentTimeMillis() + durMs
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("pomodoro_skip_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Skip interval", modifier = Modifier.size(28.dp))
                    }
                }

                // Stop & Save Button
                OutlinedButton(
                    onClick = { showStopConfirmDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pomodoro_stop_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("End & Save Session ($totalFocusedMinutesLogged min)")
                }
            }
        }
    }
}
