package com.deehem.gpawhiz.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deehem.gpawhiz.data.Course
import com.deehem.gpawhiz.data.Semester
import com.deehem.gpawhiz.data.StudentProfile
import com.deehem.gpawhiz.service.GpaCalcService
import com.deehem.gpawhiz.ui.viewmodel.GpaViewModel

@Composable
fun DashboardScreen(
    viewModel: GpaViewModel,
    onNavigateToScholarships: () -> Unit = {},
    onNavigateToAssistant: () -> Unit = {},
    onNavigateToStudyPlanner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val profile by viewModel.studentProfile.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val carryOvers by viewModel.carryOverCourses.collectAsState()
    val scholarships by viewModel.scholarships.collectAsState()
    val scholarshipReqs by viewModel.scholarshipRequirements.collectAsState()

    val cgpa = remember(semesters, courses) {
        GpaCalcService.calculateCgpa(semesters, courses)
    }
    
    val totalUnits = remember(courses) {
        GpaCalcService.calculateTotalUnits(courses)
    }

    val standingColorHex = remember(cgpa, profile) {
        GpaCalcService.getColorForCgpa(cgpa, profile.gradingScale)
    }
    
    val standingLabel = remember(cgpa, profile) {
        GpaCalcService.getStandingForCgpa(cgpa, profile.gradingScale)
    }

    val themeColor = remember(standingColorHex) {
        Color(android.graphics.Color.parseColor(standingColorHex))
    }

    val scholarshipStats = remember(scholarships, scholarshipReqs) {
        com.deehem.gpawhiz.service.ScholarshipCalculationHelper.calculateStatistics(scholarships, scholarshipReqs)
    }

    val creditsCompleted = remember(courses) {
        GpaCalcService.calculateCreditsCompleted(courses)
    }
    
    val currentSemester = remember(semesters, profile) {
        semesters.find { it.id == profile.currentSemesterId }
    }
    
    val currentSemesterCourses = remember(courses, profile) {
        courses.filter { it.semesterId == profile.currentSemesterId }
    }
    
    val currentSemesterGpa = remember(currentSemesterCourses, currentSemester) {
        GpaCalcService.calculateSgpa(currentSemesterCourses, currentSemester?.gradingScale ?: profile.gradingScale)
    }
    
    val currentSemesterCredits = remember(currentSemesterCourses) {
        currentSemesterCourses.sumOf { it.units }
    }
    
    val progress = remember(creditsCompleted, profile) {
        if (profile.totalRequiredCredits > 0) (creditsCompleted.toFloat() / profile.totalRequiredCredits).coerceIn(0f, 1f) else 0f
    }

    // Toggle for Profile Editing
    var isEditingProfile by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Institution Badge
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.fullName.ifEmpty { "Welcome to GPA Whiz!" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (profile.matricNo.isNotEmpty()) {
                            Text(
                                text = "Matric: ${profile.matricNo}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { isEditingProfile = !isEditingProfile },
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(
                            imageVector = if (isEditingProfile) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (profile.institution.isNotEmpty() || profile.department.isNotEmpty()) {
                    Text(
                        text = "${profile.institution} | ${profile.department}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    text = "Level: ${profile.currentLevel} | Faculty: ${profile.faculty.ifEmpty { "N/A" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Current Semester Indicator
        if (profile.academicSession.isNotEmpty() || currentSemester != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "CURRENT SEMESTER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "${profile.academicSession.ifEmpty { "Active Session" }} • ${currentSemester?.name ?: "No Semester Selected"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Profile Editor Form (collapsible)
        AnimatedVisibility(visible = isEditingProfile) {
            ProfileForm(
                profile = profile,
                semesters = semesters,
                onSave = { updatedProfile ->
                    viewModel.updateProfile(
                        name = updatedProfile.fullName,
                        institution = updatedProfile.institution,
                        matric = updatedProfile.matricNo,
                        faculty = updatedProfile.faculty,
                        dept = updatedProfile.department,
                        level = updatedProfile.currentLevel,
                        session = updatedProfile.academicSession,
                        semesterId = updatedProfile.currentSemesterId,
                        gradYear = updatedProfile.graduationYear,
                        scale = updatedProfile.gradingScale,
                        targetCgpa = updatedProfile.targetCgpa,
                        totalRequiredCredits = updatedProfile.totalRequiredCredits
                    )
                    isEditingProfile = false
                }
            )
        }

        // Vibrant Color-Coded CGPA Hero Card (Professional Polish Style)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, themeColor.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "OVERALL CGPA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColor,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "%.2f".format(cgpa),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    color = themeColor,
                    letterSpacing = (-1).sp
                )

                Text(
                    text = standingLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$creditsCompleted / ${profile.totalRequiredCredits} Credits",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = themeColor.copy(alpha = 0.8f)
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(themeColor.copy(alpha = 0.4f), CircleShape)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}% Progress",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = themeColor.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Academic Foundation & Credit tracking
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ACADEMIC FOUNDATION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Semester GPA
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Term GPA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        Text("%.2f".format(currentSemesterGpa), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, softWrap = false)
                    }
                    // Semester Credits
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Term Units", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        Text("$currentSemesterCredits", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, softWrap = false)
                    }
                    // Course Count
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Courses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        Text("${currentSemesterCourses.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, softWrap = false)
                    }
                }
                
                HorizontalDivider(modifier = Modifier.alpha(0.3f))
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Credit Progress", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("$creditsCompleted / ${profile.totalRequiredCredits} Units", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${(progress * 100).toInt()}% Completed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text("${profile.totalRequiredCredits - creditsCompleted} Units Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Dynamic Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TOTAL COURSES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${courses.size}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Across ${semesters.size} Semesters",
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TARGET CGPA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "%.2f".format(profile.targetCgpa),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Target achievements progress pointer
                    val targetProgress = if (profile.targetCgpa > 0) (cgpa / profile.targetCgpa).toFloat().coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { targetProgress },
                        color = if (targetProgress >= 0.95f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        // Outstanding Carry-Over Courses Reminder Widget (Automatic)
        AnimatedVisibility(visible = carryOvers.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Reminding Alerts",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "OUTSTANDING CARRY-OVERS (${carryOvers.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "The following courses received a failing grade or require a carry-over retake. They are appended to your upcoming registrations:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    carryOvers.forEach { course ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${course.code}: ${course.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "${course.units} Units [Grade: ${course.grade}]",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Scholarship Tracker Overview Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SCHOLARSHIP TRACKER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${scholarshipStats.activeApplications} Active Applications",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    TextButton(
                        onClick = onNavigateToScholarships,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp).testTag("open_scholarships_dashboard_button")
                    ) {
                        Text("View Tracker", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Upcoming Deadlines", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Text("${scholarshipStats.upcomingDeadlinesCount} in 30 days", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (scholarshipStats.upcomingDeadlinesCount > 0) Color(0xFFDB4437) else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Awaiting Feedback", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Text("${scholarshipStats.awaitingResultsCount} submitted", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A73E8))
                        }
                    }
                }
            }
        }

        // Study Planner Widget (Phase 3)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "STUDY PLANNER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Manage Self-Study & Timer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onNavigateToStudyPlanner,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(36.dp).testTag("open_study_planner_button")
                ) {
                    Text("Plan Now", fontSize = 12.sp)
                }
            }
        }

        // Academic Utilities (Phase 2)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ACADEMIC UTILITIES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "GPA Scale & % Converters",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onNavigateToAssistant,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(36.dp).testTag("open_academic_assistant_button")
                ) {
                    Text("Open Tools", fontSize = 12.sp)
                }
            }
        }

        // Target CGPA Simulator Widget ("What-If Analysis")
        TargetCgpaSimulator(
            currentCgpa = cgpa,
            currentUnits = totalUnits,
            gradingScale = profile.gradingScale
        )
    }
}

@Composable
fun ProfileForm(
    profile: StudentProfile,
    semesters: List<Semester>,
    onSave: (StudentProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile.fullName) }
    var institution by remember { mutableStateOf(profile.institution) }
    var matric by remember { mutableStateOf(profile.matricNo) }
    var faculty by remember { mutableStateOf(profile.faculty) }
    var dept by remember { mutableStateOf(profile.department) }
    var level by remember { mutableStateOf(profile.currentLevel) }
    var session by remember { mutableStateOf(profile.academicSession) }
    var currentSemesterId by remember { mutableStateOf(profile.currentSemesterId) }
    var totalRequiredCreditsStr by remember { mutableStateOf(profile.totalRequiredCredits.toString()) }
    var gradYear by remember { mutableStateOf(profile.graduationYear) }
    var targetCgpaStr by remember { mutableStateOf(profile.targetCgpa.toString()) }
    var scale by remember { mutableStateOf(profile.gradingScale) }

    val levels = listOf("100L", "200L", "300L", "400L", "500L", "600L", "Postgraduate")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SETUP STUDENT PROFILE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = institution,
                onValueChange = { institution = it },
                label = { Text("Institution (e.g. Unilag, UI, ABU)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = matric,
                    onValueChange = { matric = it },
                    label = { Text("Matric No") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = gradYear,
                    onValueChange = { gradYear = it },
                    label = { Text("Graduation Year") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = faculty,
                    onValueChange = { faculty = it },
                    label = { Text("Faculty") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dept,
                    onValueChange = { dept = it },
                    label = { Text("Department") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = session,
                onValueChange = { session = it },
                label = { Text("Academic Session (e.g. 2026/2027)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Current Semester Selector
            var expandedSemester by remember { mutableStateOf(false) }
            val currentSemesterName = semesters.find { it.id == currentSemesterId }?.name ?: "None Selected"
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expandedSemester = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Current Semester: $currentSemesterName")
                }
                DropdownMenu(
                    expanded = expandedSemester,
                    onDismissRequest = { expandedSemester = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None Selected") },
                        onClick = {
                            currentSemesterId = 0
                            expandedSemester = false
                        }
                    )
                    for (sem in semesters) {
                        DropdownMenuItem(
                            text = { Text(sem.name) },
                            onClick = {
                                currentSemesterId = sem.id
                                expandedSemester = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = totalRequiredCreditsStr,
                onValueChange = { totalRequiredCreditsStr = it },
                label = { Text("Total Programme Credit Requirement") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Spinner for level select
            var expandedLevel by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expandedLevel = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Current Level: $level")
                }
                DropdownMenu(
                    expanded = expandedLevel,
                    onDismissRequest = { expandedLevel = false }
                ) {
                    levels.forEach { lvl ->
                        DropdownMenuItem(
                            text = { Text(lvl) },
                            onClick = {
                                level = lvl
                                expandedLevel = false
                            }
                        )
                    }
                }
            }

            // Grading Scale Toggle
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Grading Scale Map", fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = scale == 5.0,
                        onClick = { scale = 5.0 },
                        label = { Text("5.0 Scale (Univ)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = scale == 4.0,
                        onClick = { scale = 4.0 },
                        label = { Text("4.0 Scale (Poly/Univ)", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Detailed Grading Mapping Content (Fixed missing section)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val grades = if (scale == 5.0) {
                            listOf("A" to "5", "B" to "4", "C" to "3", "D" to "2", "E" to "1", "F" to "0")
                        } else {
                            listOf("A" to "4", "B" to "3", "C" to "2", "D" to "1", "F" to "0")
                        }

                        grades.forEach { (grade, points) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = grade, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(text = points, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = targetCgpaStr,
                onValueChange = { targetCgpaStr = it },
                label = { Text("Target CGPA Goal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    val doubleVal = targetCgpaStr.toDoubleOrNull() ?: 4.5
                    val creditsVal = totalRequiredCreditsStr.toIntOrNull() ?: 120
                    onSave(
                        profile.copy(
                            fullName = name,
                            institution = institution,
                            matricNo = matric,
                            faculty = faculty,
                            department = dept,
                            currentLevel = level,
                            academicSession = session,
                            currentSemesterId = currentSemesterId,
                            graduationYear = gradYear,
                            gradingScale = scale,
                            targetCgpa = if (doubleVal <= scale) doubleVal else scale,
                            totalRequiredCredits = creditsVal
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("save_profile_button")
            ) {
                Text("Save Profile Records")
            }
        }
    }
}

@Composable
fun TargetCgpaSimulator(
    currentCgpa: Double,
    currentUnits: Int,
    gradingScale: Double
) {
    var targetStr by remember { mutableStateOf("") }
    var remainingSemsStr by remember { mutableStateOf("") }
    var avgUnitsStr by remember { mutableStateOf("20") }
    
    var simulationResult by remember { mutableStateOf<com.deehem.gpawhiz.service.TargetSimulationResult?>(null) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "TARGET CGPA SIMULATOR (WHAT-IF)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Enter your desired final CGPA targets and outline your remaining terms to calculate the required GPA grades to sustain your GPA goal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = { Text("Target CGPA") },
                    placeholder = { Text("e.g. 4.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).testTag("target_cgpa_simulator_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = remainingSemsStr,
                    onValueChange = { remainingSemsStr = it },
                    label = { Text("Left Semesters") },
                    placeholder = { Text("e.g. 4") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = avgUnitsStr,
                onValueChange = { avgUnitsStr = it },
                label = { Text("Average Course Units / Semester") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    val t = targetStr.toDoubleOrNull() ?: 0.0
                    val r = remainingSemsStr.toIntOrNull() ?: 0
                    val u = avgUnitsStr.toIntOrNull() ?: 20
                    simulationResult = GpaCalcService.simulateRequiredSgpa(
                        currentCgpa = currentCgpa,
                        currentUnits = currentUnits,
                        targetCgpa = t,
                        remainingSemesters = r,
                        avgUnitsPerSemester = u,
                        scale = gradingScale
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("simulate_button")
            ) {
                Text("Analyze What-If Goal")
            }

            simulationResult?.let { result ->
                val boxBg = if (result.isPossible) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
                val textTint = if (result.isPossible) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(boxBg)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = if (result.isPossible) "SUCCESSFULLY FEASIBLE" else "CRITICAL PATH",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = textTint
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textTint
                        )
                        if (result.isPossible && result.requiredSgpa > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Required SGPA in remaining semesters: %.2f / %.1f".format(result.requiredSgpa, gradingScale),
                                style = MaterialTheme.typography.bodySmall,
                                color = textTint.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
