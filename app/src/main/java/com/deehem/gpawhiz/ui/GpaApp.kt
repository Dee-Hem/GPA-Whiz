package com.deehem.gpawhiz.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deehem.gpawhiz.ui.screens.DashboardScreen
import com.deehem.gpawhiz.ui.screens.SemesterScreen
import com.deehem.gpawhiz.ui.screens.SettingsScreen
import com.deehem.gpawhiz.ui.screens.TimetableScreen
import com.deehem.gpawhiz.ui.screens.scholarships.ScholarshipDetailsScreen
import com.deehem.gpawhiz.ui.screens.scholarships.ScholarshipsScreen
import com.deehem.gpawhiz.ui.viewmodel.GpaViewModel

enum class MainTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Home),
    SEMESTERS("Courses", Icons.AutoMirrored.Filled.List),
    SCHOLARSHIPS("Scholarships", Icons.Default.Star),
    TIMETABLE("Timetable", Icons.Default.DateRange),
    PORTABILITY("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpaApp(
    viewModel: GpaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    var selectedScholarshipId by remember { mutableStateOf<Int?>(null) }
    var isShowingAssistant by remember { mutableStateOf(false) }
    var isShowingAchievements by remember { mutableStateOf(false) }
    var activePomodoroSession by remember { mutableStateOf<com.deehem.gpawhiz.data.StudySession?>(null) }
    
    val profile by viewModel.studentProfile.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    var isAppUnlocked by remember { mutableStateOf(false) }

    val activity = context as? androidx.fragment.app.FragmentActivity
    val promptBiometricAuth = {
        if (activity != null) {
            com.deehem.gpawhiz.service.BiometricAuthHelper.authenticate(
                activity = activity,
                title = "Unlock GPA Whiz",
                subtitle = "Confirm identity to access your academic dashboard",
                onSuccess = { isAppUnlocked = true },
                onError = { err ->
                    Toast.makeText(context, "Authentication failed: $err", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            isAppUnlocked = true
        }
    }

    // Trigger biometric prompt on launch if lock is enabled
    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled && !isAppUnlocked) {
            promptBiometricAuth()
        }
    }

    // App Lock Overlay when locked
    if (biometricEnabled && !isAppUnlocked) {
        Surface(
            modifier = Modifier.fillMaxSize().testTag("app_lock_screen"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                        contentDescription = "App Locked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "GPA Whiz is Locked",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Authenticate with biometric fingerprint or device PIN/pattern to access your records.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { promptBiometricAuth() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("unlock_app_button")
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock with Biometrics / PIN", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    // Toast updates listener
    val uiMsg by viewModel.uiMessage.collectAsState()
    LaunchedEffect(uiMsg) {
        uiMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearUiMessage()
        }
    }

    if (activePomodoroSession != null) {
        com.deehem.gpawhiz.ui.screens.PomodoroFocusScreen(
            session = activePomodoroSession!!,
            onClose = { focusedMinutes ->
                if (focusedMinutes > 0) {
                    val currentSession = activePomodoroSession!!
                    viewModel.updateStudySession(
                        currentSession.copy(
                            durationMinutes = currentSession.durationMinutes + focusedMinutes,
                            status = com.deehem.gpawhiz.data.StudyStatus.COMPLETED
                        )
                    )
                }
                activePomodoroSession = null
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "GW",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text(
                                text = "GPA Whiz",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "OFFLINE ACADEMIC SUITE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "%.1f SCALE".format(profile.gradingScale),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("app_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                MainTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentTab != tab) {
                                currentTab = tab
                                selectedScholarshipId = null
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                letterSpacing = (-0.2).sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        alwaysShowLabel = true,
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isShowingAssistant) {
                com.deehem.gpawhiz.ui.screens.AcademicAssistantScreen(
                    onNavigateBack = { isShowingAssistant = false }
                )
            } else if (isShowingAchievements) {
                com.deehem.gpawhiz.ui.screens.AchievementsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { isShowingAchievements = false }
                )
            } else {
                when (currentTab) {
                    MainTab.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToScholarships = {
                            currentTab = MainTab.SCHOLARSHIPS
                            selectedScholarshipId = null
                        },
                        onNavigateToAssistant = { isShowingAssistant = true },
                        onNavigateToStudyPlanner = { currentTab = MainTab.TIMETABLE },
                        onNavigateToAchievements = { isShowingAchievements = true },
                        onStartPomodoro = { activePomodoroSession = it }
                    )
                    MainTab.SEMESTERS -> SemesterScreen(viewModel = viewModel)
                    MainTab.SCHOLARSHIPS -> {
                        if (selectedScholarshipId != null) {
                            ScholarshipDetailsScreen(
                                scholarshipId = selectedScholarshipId!!,
                                viewModel = viewModel,
                                onNavigateBack = { selectedScholarshipId = null }
                            )
                        } else {
                            ScholarshipsScreen(
                                viewModel = viewModel,
                                onSelectScholarship = { selectedScholarshipId = it }
                            )
                        }
                    }
                    MainTab.TIMETABLE -> TimetableScreen(
                        viewModel = viewModel,
                        onOpenPomodoro = { activePomodoroSession = it }
                    )
                    MainTab.PORTABILITY -> SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToAssistant = { isShowingAssistant = true }
                    )
                }
            }
        }
    }
}
