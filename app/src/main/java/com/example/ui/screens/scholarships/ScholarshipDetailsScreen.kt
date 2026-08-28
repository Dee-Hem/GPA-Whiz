package com.example.ui.screens.scholarships

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.service.*
import com.example.ui.viewmodel.GpaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScholarshipDetailsScreen(
    scholarshipId: Int,
    viewModel: GpaViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val scholarships by viewModel.scholarships.collectAsState()
    val allRequirements by viewModel.scholarshipRequirements.collectAsState()
    val allTimelineEvents by viewModel.scholarshipTimelineEvents.collectAsState()
    val studentProfile by viewModel.studentProfile.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val courses by viewModel.courses.collectAsState()

    val currentCgpa = remember(semesters, courses) {
        GpaCalcService.calculateCgpa(semesters, courses)
    }
    val effectiveCgpa = if (currentCgpa > 0.0) currentCgpa else studentProfile.targetCgpa
    val isTargetCgpa = currentCgpa <= 0.0 && studentProfile.targetCgpa > 0.0

    val scholarship = scholarships.find { it.id == scholarshipId }

    var showEditDialog by remember { mutableStateOf(false) }
    var showAddRequirementDialog by remember { mutableStateOf(false) }
    var showOutcomeDialog by remember { mutableStateOf(false) }
    var showTimelineDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }

    var requirementFilter by remember { mutableStateOf("All") }

    if (scholarship == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Scholarship record not found.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onNavigateBack) { Text("Back to Scholarships") }
            }
        }
        return
    }

    val requirements = allRequirements.filter { it.scholarshipId == scholarship.id }
    val timelineEvents = allTimelineEvents.filter { it.scholarshipId == scholarship.id }.sortedByDescending { it.date }

    val progress = ScholarshipCalculationHelper.getRequirementsProgress(requirements)
    val deadlineCountdown = ScholarshipCalculationHelper.getDeadlineCountdown(scholarship.deadlineDate)
    val feedbackStatus = ScholarshipCalculationHelper.getExpectedFeedbackStatus(scholarship.expectedFeedbackDate, scholarship.status)
    val nextAction = ScholarshipCalculationHelper.determineNextAction(scholarship, requirements)
    val eligibility = ScholarshipCalculationHelper.checkEligibility(
        studentCgpa = effectiveCgpa,
        studentScale = studentProfile.gradingScale,
        scholarship = scholarship,
        isTarget = isTargetCgpa
    )

    val deadlineColor = when (deadlineCountdown.urgency) {
        UrgencyLevel.CRITICAL -> Color(0xFFDB4437) // Red
        UrgencyLevel.HIGH -> Color(0xFFE65100) // Deep Orange
        UrgencyLevel.MEDIUM -> Color(0xFFF4B400) // Amber
        UrgencyLevel.PASSED -> Color.Gray
        else -> Color(0xFF0F9D58) // Green
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = scholarship.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = scholarship.organization,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showExportDialog = true }, modifier = Modifier.testTag("export_dossier_button")) {
                    Icon(Icons.Default.Share, contentDescription = "Export Dossier", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showEditDialog = true }, modifier = Modifier.testTag("edit_scholarship_button")) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Details")
                }
                IconButton(onClick = { showDeleteConfirmDialog = true }, modifier = Modifier.testTag("delete_scholarship_button")) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. STATUS & DEADLINE HERO CARD
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Status & Change Status row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = scholarship.status,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            // Change Status Button with Dropdown
                            Box {
                                OutlinedButton(
                                    onClick = { showStatusMenu = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Change Status", fontSize = 12.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = showStatusMenu,
                                    onDismissRequest = { showStatusMenu = false }
                                ) {
                                    ScholarshipStatus.ALL.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s) },
                                            onClick = {
                                                viewModel.updateScholarshipStatus(scholarship, s)
                                                showStatusMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Deadline Countdown Banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = deadlineColor.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, deadlineColor.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "APPLICATION DEADLINE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = deadlineColor,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = deadlineCountdown.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = deadlineColor
                                    )
                                    Text(
                                        text = ScholarshipCalculationHelper.formatDate(scholarship.deadlineDate),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (scholarship.deadlineDate != null && deadlineCountdown.urgency != UrgencyLevel.PASSED) {
                                    IconButton(
                                        onClick = {
                                            ScholarshipCalendarHelper.addEventToDeviceCalendar(
                                                context = context,
                                                scholarship = scholarship,
                                                requirements = requirements,
                                                eventTitle = "Application Deadline",
                                                eventDateMillis = scholarship.deadlineDate!!
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.DateRange, contentDescription = "Sync to Calendar", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        // Next Action Suggestion
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Next Action",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = nextAction.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = nextAction.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Eligibility Badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(android.graphics.Color.parseColor(eligibility.colorHex)).copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color(android.graphics.Color.parseColor(eligibility.colorHex)).copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (eligibility.status == EligibilityStatus.ELIGIBLE) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = eligibility.badgeText,
                                    tint = Color(android.graphics.Color.parseColor(eligibility.colorHex)),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Eligibility: ${eligibility.badgeText}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(android.graphics.Color.parseColor(eligibility.colorHex))
                                    )
                                    Text(
                                        text = eligibility.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. OVERVIEW & LINKS (OFFLINE STORED)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Overview & Links", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        // Funding Amount
                        val amountText = if (scholarship.amount > 0) "${scholarship.currency} %,.0f".format(scholarship.amount) else "Unstated"
                        Text(
                            text = "Award Funding: $amountText",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (scholarship.description.isNotBlank()) {
                            Text(
                                text = scholarship.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Stored Application URL Button (Opens external browser via Intent)
                        if (scholarship.applicationUrl.isNotBlank()) {
                            Button(
                                onClick = {
                                    openUrl(context, scholarship.applicationUrl)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("open_portal_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Application Portal Externally")
                            }
                        }

                        // Organization Website & Contact Email
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (scholarship.organizationWebsite.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { openUrl(context, scholarship.organizationWebsite) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Website", fontSize = 12.sp)
                                }
                            }
                            if (scholarship.contactEmail.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:${scholarship.contactEmail}")
                                            putExtra(Intent.EXTRA_SUBJECT, "Inquiry: ${scholarship.name}")
                                        }
                                        try {
                                            context.startActivity(emailIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Email Inquiry", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 3. KEY DATES & TIMELINES
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Milestones & Important Dates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        DateRowItem(
                            label = "Opening Date",
                            dateMillis = scholarship.openingDate,
                            context = context,
                            scholarship = scholarship,
                            requirements = requirements
                        )
                        DateRowItem(
                            label = "Application Deadline",
                            dateMillis = scholarship.deadlineDate,
                            subLabel = deadlineCountdown.label,
                            context = context,
                            scholarship = scholarship,
                            requirements = requirements
                        )
                        DateRowItem(
                            label = "Expected Feedback Date",
                            dateMillis = scholarship.expectedFeedbackDate,
                            subLabel = feedbackStatus.label,
                            context = context,
                            scholarship = scholarship,
                            requirements = requirements
                        )
                        if (scholarship.testDate != null) {
                            DateRowItem(
                                label = "Assessment / Test Date",
                                dateMillis = scholarship.testDate,
                                context = context,
                                scholarship = scholarship,
                                requirements = requirements
                            )
                        }
                        if (scholarship.interviewDate != null) {
                            DateRowItem(
                                label = "Interview Date",
                                dateMillis = scholarship.interviewDate,
                                context = context,
                                scholarship = scholarship,
                                requirements = requirements
                            )
                        }
                        if (scholarship.followUpDate != null) {
                            DateRowItem(
                                label = "Follow-Up Date",
                                dateMillis = scholarship.followUpDate,
                                context = context,
                                scholarship = scholarship,
                                requirements = requirements
                            )
                        }
                    }
                }
            }

            // 4. REQUIREMENTS CHECKLIST
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Requirements Checklist", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("${progress.completed} of ${progress.total} Completed (${progress.percentage.toInt()}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = { showAddRequirementDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp).testTag("add_requirement_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontSize = 12.sp)
                            }
                        }

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { progress.percentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Filter Chips (All, Incomplete, Completed)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = requirementFilter == "All",
                                onClick = { requirementFilter = "All" },
                                label = { Text("All (${requirements.size})", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = requirementFilter == "Incomplete",
                                onClick = { requirementFilter = "Incomplete" },
                                label = { Text("Incomplete (${requirements.size - progress.completed})", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = requirementFilter == "Completed",
                                onClick = { requirementFilter = "Completed" },
                                label = { Text("Done (${progress.completed})", fontSize = 11.sp) }
                            )
                        }

                        // Requirements List
                        val filteredReqs = when (requirementFilter) {
                            "Completed" -> requirements.filter { it.status == RequirementStatus.COMPLETED || it.status == RequirementStatus.SUBMITTED }
                            "Incomplete" -> requirements.filter { it.status != RequirementStatus.COMPLETED && it.status != RequirementStatus.SUBMITTED }
                            else -> requirements
                        }

                        if (filteredReqs.isEmpty()) {
                            Text(
                                text = if (requirements.isEmpty()) "No requirements registered yet. Tap 'Add' to build checklist." else "No requirements match this filter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredReqs.forEach { req ->
                                    RequirementItemCard(
                                        requirement = req,
                                        onToggle = { viewModel.toggleRequirementStatus(req) },
                                        onDelete = { viewModel.deleteRequirement(req) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. TIMELINE & ACTIVITY LOG
            item {
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
                            Text("Activity History & Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            TextButton(
                                onClick = { showTimelineDialog = true },
                                modifier = Modifier.testTag("add_timeline_note_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Note", fontSize = 12.sp)
                            }
                        }

                        if (timelineEvents.isEmpty()) {
                            Text("No timeline history recorded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                timelineEvents.forEach { event ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .size(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (event.isAutomatic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = ScholarshipCalculationHelper.formatDate(event.date),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (event.description.isNotBlank()) {
                                                Text(event.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        if (!event.isAutomatic) {
                                            IconButton(
                                                onClick = { viewModel.deleteTimelineEvent(event) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Clear, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. OUTCOME RECORDING SECTION
            item {
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
                            Text("Outcome & Final Decision", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            OutlinedButton(
                                onClick = { showOutcomeDialog = true },
                                modifier = Modifier.testTag("record_outcome_button")
                            ) {
                                Text(if (scholarship.outcome != null) "Update Outcome" else "Record Outcome", fontSize = 12.sp)
                            }
                        }

                        if (scholarship.outcome != null) {
                            val outcomeColor = if (scholarship.outcome.equals("Awarded", ignoreCase = true)) Color(0xFF0F9D58) else Color(0xFFDB4437)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = outcomeColor.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, outcomeColor.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("DECISION: ${scholarship.outcome?.uppercase()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = outcomeColor)
                                    if ((scholarship.awardAmount ?: 0.0) > 0) {
                                        Text("Awarded Value: ${scholarship.awardCurrency ?: scholarship.currency} %,.0f".format(scholarship.awardAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Text("Recorded Date: ${ScholarshipCalculationHelper.formatDate(scholarship.awardDate)}", style = MaterialTheme.typography.bodySmall)
                                    if (!scholarship.awardNotes.isNullOrBlank()) {
                                        Text("Notes: ${scholarship.awardNotes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        } else {
                            Text("Outcome not yet recorded. Tap 'Record Outcome' once announced.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // Modal Dialogs
    if (showEditDialog) {
        ScholarshipFormDialog(
            scholarship = scholarship,
            onDismiss = { showEditDialog = false },
            onSave = { name, org, desc, amt, curr, appUrl, orgWeb, email, nts, openD, deadD, feedD, testD, intD, folD, st, minC, minS, _ ->
                viewModel.updateScholarship(
                    scholarship.copy(
                        name = name,
                        organization = org,
                        description = desc,
                        amount = amt,
                        currency = curr,
                        applicationUrl = appUrl,
                        organizationWebsite = orgWeb,
                        contactEmail = email,
                        notes = nts,
                        openingDate = openD,
                        deadlineDate = deadD,
                        expectedFeedbackDate = feedD,
                        testDate = testD,
                        interviewDate = intD,
                        followUpDate = folD,
                        status = st,
                        minCgpa = minC,
                        minScale = minS
                    )
                )
                showEditDialog = false
            }
        )
    }

    if (showAddRequirementDialog) {
        RequirementSelectionDialog(
            existingRequirements = requirements,
            onDismiss = { showAddRequirementDialog = false },
            onSave = { title, cat, det, dead, nts ->
                viewModel.addRequirement(
                    scholarshipId = scholarship.id,
                    title = title,
                    category = cat,
                    details = det,
                    deadline = dead,
                    notes = nts
                )
                showAddRequirementDialog = false
            }
        )
    }

    if (showOutcomeDialog) {
        OutcomeDialog(
            scholarship = scholarship,
            onDismiss = { showOutcomeDialog = false },
            onSave = { outcome, awardAmt, awardCurr, awardDate, awardNotes ->
                viewModel.recordScholarshipOutcome(
                    scholarship = scholarship,
                    outcome = outcome,
                    awardAmount = awardAmt,
                    awardCurrency = awardCurr,
                    awardDate = awardDate,
                    awardNotes = awardNotes
                )
                showOutcomeDialog = false
            }
        )
    }

    if (showTimelineDialog) {
        TimelineEventDialog(
            onDismiss = { showTimelineDialog = false },
            onSave = { title, desc, date ->
                viewModel.addManualTimelineEvent(
                    scholarshipId = scholarship.id,
                    title = title,
                    description = desc,
                    date = date
                )
                showTimelineDialog = false
            }
        )
    }

    if (showExportDialog) {
        ScholarshipExportDialog(
            viewModel = viewModel,
            singleScholarship = scholarship,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Scholarship?") },
            text = { Text("Are you sure you want to remove '${scholarship.name}' and all associated requirements and timeline history?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteScholarship(scholarship)
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DateRowItem(
    label: String,
    dateMillis: Long?,
    subLabel: String? = null,
    context: Context,
    scholarship: Scholarship,
    requirements: List<ScholarshipRequirement> = emptyList()
) {
    // Hide calendar button if it's the deadline and it's passed
    val isDeadline = label.contains("Deadline", ignoreCase = true)
    val isPassed = if (isDeadline && dateMillis != null) {
        ScholarshipCalculationHelper.getDeadlineCountdown(dateMillis).urgency == UrgencyLevel.PASSED
    } else false

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ScholarshipCalculationHelper.formatDate(dateMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (dateMillis != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!subLabel.isNullOrBlank()) {
                    Text(" • $subLabel", style = MaterialTheme.typography.bodySmall, color = if (isPassed) Color.Gray else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (dateMillis != null && dateMillis > 0 && !isPassed) {
            IconButton(
                onClick = {
                    ScholarshipCalendarHelper.addEventToDeviceCalendar(
                        context = context,
                        scholarship = scholarship,
                        requirements = requirements,
                        eventTitle = label,
                        eventDateMillis = dateMillis
                    )
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = "Add to Calendar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun RequirementItemCard(
    requirement: ScholarshipRequirement,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = requirement.status == RequirementStatus.COMPLETED || requirement.status == RequirementStatus.SUBMITTED
    val statusColor = when (requirement.status) {
        RequirementStatus.COMPLETED, RequirementStatus.SUBMITTED -> Color(0xFF0F9D58)
        RequirementStatus.IN_PROGRESS -> Color(0xFF1A73E8)
        else -> Color.Gray
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = requirement.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = requirement.status,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(requirement.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (requirement.details.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(requirement.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open URL: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
