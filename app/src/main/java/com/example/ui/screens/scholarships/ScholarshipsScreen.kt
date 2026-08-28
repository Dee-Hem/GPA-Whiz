package com.example.ui.screens.scholarships

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
fun ScholarshipsScreen(
    viewModel: GpaViewModel,
    onSelectScholarship: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val scholarships by viewModel.scholarships.collectAsState()
    val allRequirements by viewModel.scholarshipRequirements.collectAsState()
    val studentProfile by viewModel.studentProfile.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val courses by viewModel.courses.collectAsState()

    val currentCgpa = remember(semesters, courses) {
        GpaCalcService.calculateCgpa(semesters, courses)
    }
    val effectiveCgpa = if (currentCgpa > 0.0) currentCgpa else studentProfile.targetCgpa
    val isTargetCgpa = currentCgpa <= 0.0 && studentProfile.targetCgpa > 0.0

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var selectedSortOrder by remember { mutableStateOf("Deadline") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Deadlines, 2: Pipeline

    var showAddDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val stats = remember(scholarships, allRequirements) {
        ScholarshipCalculationHelper.calculateStatistics(scholarships, allRequirements)
    }

    // Filter & Sort
    val filteredScholarships = remember(scholarships, searchQuery, selectedStatusFilter, selectedSortOrder) {
        scholarships.filter { s ->
            val matchesSearch = searchQuery.isBlank() ||
                    s.name.contains(searchQuery, ignoreCase = true) ||
                    s.organization.contains(searchQuery, ignoreCase = true) ||
                    s.notes.contains(searchQuery, ignoreCase = true)
            val matchesStatus = when (selectedStatusFilter) {
                "All" -> true
                "Active" -> s.status in ScholarshipStatus.ACTIVE
                else -> s.status.equals(selectedStatusFilter, ignoreCase = true)
            }
            matchesSearch && matchesStatus
        }.sortedWith { a, b ->
            when (selectedSortOrder) {
                "Name" -> a.name.compareTo(b.name, ignoreCase = true)
                "Amount" -> b.amount.compareTo(a.amount)
                "Added" -> b.dateAdded.compareTo(a.dateAdded)
                else -> { // Deadline
                    val aDead = a.deadlineDate ?: Long.MAX_VALUE
                    val bDead = b.deadlineDate ?: Long.MAX_VALUE
                    aDead.compareTo(bDead)
                }
            }
        }
    }

    val statusFilters = listOf("All", "Active", ScholarshipStatus.NOT_STARTED, ScholarshipStatus.IN_PROGRESS, ScholarshipStatus.SUBMITTED, ScholarshipStatus.AWARDED, ScholarshipStatus.REJECTED)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // TOP APP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SCHOLARSHIP TRACKER",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "100% Offline Application & Requirement Manager",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.testTag("export_scholarships_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export Reports", tint = MaterialTheme.colorScheme.primary)
                }

                Button(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp).testTag("add_scholarship_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // KPI SUMMARY TILES
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                KpiCard(
                    title = "Active Applications",
                    value = stats.activeApplications.toString(),
                    subtext = "of ${stats.totalApplications} total tracked",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                KpiCard(
                    title = "Deadlines in 30d",
                    value = stats.upcomingDeadlinesCount.toString(),
                    subtext = "require urgent action",
                    color = if (stats.upcomingDeadlinesCount > 0) Color(0xFFDB4437) else Color(0xFF0F9D58)
                )
            }
            item {
                KpiCard(
                    title = "Awaiting Feedback",
                    value = stats.awaitingResultsCount.toString(),
                    subtext = "submitted applications",
                    color = Color(0xFF1A73E8)
                )
            }
            item {
                val totalAwardedFormatted = if (stats.totalAwardedFunds.isNotEmpty()) {
                    stats.totalAwardedFunds.entries.joinToString(", ") { "${it.key}%,.0f".format(it.value) }
                } else "0"
                KpiCard(
                    title = "Awarded / Success",
                    value = "${stats.awardedCount} (${stats.successRate.toInt()}%)",
                    subtext = "Funds: $totalAwardedFormatted",
                    color = Color(0xFF0F9D58)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // VIEW TABS: All, Upcoming Deadlines, Pipeline
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("All (${scholarships.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Deadlines Roadmap", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Status Pipeline", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SEARCH & FILTER BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search scholarships, providers, notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                } else null,
                modifier = Modifier.weight(1f).height(50.dp),
                singleLine = true
            )

            // Sort Dropdown
            Box {
                OutlinedButton(
                    onClick = { sortMenuExpanded = true },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    DropdownMenuItem(text = { Text("Sort by Deadline") }, onClick = { selectedSortOrder = "Deadline"; sortMenuExpanded = false })
                    DropdownMenuItem(text = { Text("Sort by Name") }, onClick = { selectedSortOrder = "Name"; sortMenuExpanded = false })
                    DropdownMenuItem(text = { Text("Sort by Amount") }, onClick = { selectedSortOrder = "Amount"; sortMenuExpanded = false })
                    DropdownMenuItem(text = { Text("Sort by Date Added") }, onClick = { selectedSortOrder = "Added"; sortMenuExpanded = false })
                }
            }
        }

        // Status Filter Chips
        if (selectedTab == 0) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(statusFilters) { filter ->
                    FilterChip(
                        selected = selectedStatusFilter == filter,
                        onClick = { selectedStatusFilter = filter },
                        label = { Text(filter, fontSize = 11.sp) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // TAB CONTENT
        when (selectedTab) {
            0 -> {
                // ALL APPLICATIONS LIST
                if (filteredScholarships.isEmpty()) {
                    EmptyScholarshipsState(onAddClick = { showAddDialog = true })
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredScholarships, key = { it.id }) { item ->
                            val reqs = allRequirements.filter { it.scholarshipId == item.id }
                            ScholarshipCard(
                                scholarship = item,
                                requirements = reqs,
                                currentCgpa = effectiveCgpa,
                                gradingScale = studentProfile.gradingScale,
                                isTarget = isTargetCgpa,
                                onClick = { onSelectScholarship(item.id) },
                                onOpenUrl = { openUrl(context, item.applicationUrl) }
                            )
                        }
                    }
                }
            }
            1 -> {
                // DEADLINES ROADMAP VIEW
                DeadlinesRoadmapView(
                    scholarships = scholarships,
                    allRequirements = allRequirements,
                    onSelectScholarship = onSelectScholarship,
                    onSyncCalendar = { s, reqs, title, date ->
                        ScholarshipCalendarHelper.addEventToDeviceCalendar(context, s, reqs, title, date)
                    }
                )
            }
            2 -> {
                // STATUS PIPELINE VIEW
                PipelineStatusView(
                    scholarships = scholarships,
                    allRequirements = allRequirements,
                    onSelectScholarship = onSelectScholarship
                )
            }
        }
    }

    // Modal Dialogs
    if (showAddDialog) {
        ScholarshipFormDialog(
            scholarship = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, org, desc, amt, curr, appUrl, orgWeb, email, nts, openD, deadD, feedD, testD, intD, folD, st, minC, minS, bundledReqs ->
                viewModel.addScholarship(
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
                    minScale = minS,
                    initialRequirements = bundledReqs
                )
                showAddDialog = false
            }
        )
    }

    if (showExportDialog) {
        ScholarshipExportDialog(
            viewModel = viewModel,
            singleScholarship = null,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtext: String,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = Modifier.width(160.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ScholarshipCard(
    scholarship: Scholarship,
    requirements: List<ScholarshipRequirement>,
    currentCgpa: Double,
    gradingScale: Double,
    isTarget: Boolean = false,
    onClick: () -> Unit,
    onOpenUrl: () -> Unit
) {
    val progress = ScholarshipCalculationHelper.getRequirementsProgress(requirements)
    val deadlineCountdown = ScholarshipCalculationHelper.getDeadlineCountdown(scholarship.deadlineDate)
    val nextAction = ScholarshipCalculationHelper.determineNextAction(scholarship, requirements)
    val eligibility = ScholarshipCalculationHelper.checkEligibility(currentCgpa, gradingScale, scholarship, isTarget)

    val deadlineColor = when (deadlineCountdown.urgency) {
        UrgencyLevel.CRITICAL -> Color(0xFFDB4437)
        UrgencyLevel.HIGH -> Color(0xFFE65100)
        UrgencyLevel.MEDIUM -> Color(0xFFF4B400)
        UrgencyLevel.PASSED -> Color.Gray
        else -> Color(0xFF0F9D58)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("scholarship_card_${scholarship.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header row: Name & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = scholarship.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Funding & Deadline Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val amtStr = if (scholarship.amount > 0) "${scholarship.currency} %,.0f".format(scholarship.amount) else "Unstated"
                Text(
                    text = "Funding: $amtStr",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = deadlineColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = deadlineCountdown.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = deadlineColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Requirements Progress
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Requirements: ${progress.completed}/${progress.total} (${progress.percentage.toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (eligibility.status != EligibilityStatus.UNKNOWN && scholarship.minCgpa != null) {
                        Text(
                            text = eligibility.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(android.graphics.Color.parseColor(eligibility.colorHex))
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { progress.percentage / 100f },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Next Action suggestion badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${nextAction.title}: ${nextAction.description}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Stored URL quick launch button if present
            if (scholarship.applicationUrl.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onOpenUrl,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Portal", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DeadlinesRoadmapView(
    scholarships: List<Scholarship>,
    allRequirements: List<ScholarshipRequirement>,
    onSelectScholarship: (Int) -> Unit,
    onSyncCalendar: (Scholarship, List<ScholarshipRequirement>, String, Long) -> Unit
) {
    val withDeadlines = scholarships
        .filter { it.deadlineDate != null }
        .sortedBy { it.deadlineDate!! }

    if (withDeadlines.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No application deadlines scheduled yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val now = System.currentTimeMillis()
    val urgent = withDeadlines.filter { it.deadlineDate!! >= now && it.deadlineDate!! - now <= 3L * 24 * 60 * 60 * 1000 }
    val upcoming = withDeadlines.filter { it.deadlineDate!! >= now && it.deadlineDate!! - now > 3L * 24 * 60 * 60 * 1000 && it.deadlineDate!! - now <= 30L * 24 * 60 * 60 * 1000 }
    val future = withDeadlines.filter { it.deadlineDate!! - now > 30L * 24 * 60 * 60 * 1000 }
    val passed = withDeadlines.filter { it.deadlineDate!! < now }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (urgent.isNotEmpty()) {
            item {
                RoadmapSectionHeader(title = "URGENT (< 3 DAYS)", color = Color(0xFFDB4437))
            }
            items(urgent) { item ->
                val reqs = allRequirements.filter { it.scholarshipId == item.id }
                RoadmapItemCard(item, reqs, onSelectScholarship, onSyncCalendar)
            }
        }

        if (upcoming.isNotEmpty()) {
            item {
                RoadmapSectionHeader(title = "UPCOMING (NEXT 30 DAYS)", color = Color(0xFFE65100))
            }
            items(upcoming) { item ->
                val reqs = allRequirements.filter { it.scholarshipId == item.id }
                RoadmapItemCard(item, reqs, onSelectScholarship, onSyncCalendar)
            }
        }

        if (future.isNotEmpty()) {
            item {
                RoadmapSectionHeader(title = "FUTURE DEADLINES", color = Color(0xFF0F9D58))
            }
            items(future) { item ->
                val reqs = allRequirements.filter { it.scholarshipId == item.id }
                RoadmapItemCard(item, reqs, onSelectScholarship, onSyncCalendar)
            }
        }

        if (passed.isNotEmpty()) {
            item {
                RoadmapSectionHeader(title = "PAST DEADLINES", color = Color.Gray)
            }
            items(passed) { item ->
                val reqs = allRequirements.filter { it.scholarshipId == item.id }
                RoadmapItemCard(item, reqs, onSelectScholarship, onSyncCalendar)
            }
        }
    }
}

@Composable
fun RoadmapSectionHeader(title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
fun RoadmapItemCard(
    scholarship: Scholarship,
    requirements: List<ScholarshipRequirement>,
    onSelectScholarship: (Int) -> Unit,
    onSyncCalendar: (Scholarship, List<ScholarshipRequirement>, String, Long) -> Unit
) {
    val countdown = ScholarshipCalculationHelper.getDeadlineCountdown(scholarship.deadlineDate)
    val deadlineColor = when (countdown.urgency) {
        UrgencyLevel.CRITICAL -> Color(0xFFDB4437)
        UrgencyLevel.HIGH -> Color(0xFFE65100)
        UrgencyLevel.MEDIUM -> Color(0xFFF4B400)
        UrgencyLevel.PASSED -> Color.Gray
        else -> Color(0xFF0F9D58)
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onSelectScholarship(scholarship.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(scholarship.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("${scholarship.organization} • ${ScholarshipCalculationHelper.formatDate(scholarship.deadlineDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(countdown.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = deadlineColor)
            }

            if (scholarship.deadlineDate != null && countdown.urgency != UrgencyLevel.PASSED) {
                IconButton(onClick = { onSyncCalendar(scholarship, requirements, "Deadline", scholarship.deadlineDate!!) }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Add to Calendar", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun PipelineStatusView(
    scholarships: List<Scholarship>,
    allRequirements: List<ScholarshipRequirement>,
    onSelectScholarship: (Int) -> Unit
) {
    val preparing = scholarships.filter { it.status == ScholarshipStatus.NOT_STARTED || it.status == ScholarshipStatus.PREPARING }
    val inProgress = scholarships.filter { it.status == ScholarshipStatus.IN_PROGRESS || it.status == ScholarshipStatus.READY_TO_SUBMIT }
    val submitted = scholarships.filter { it.status == ScholarshipStatus.SUBMITTED || it.status == ScholarshipStatus.ASSESSMENT || it.status == ScholarshipStatus.INTERVIEW || it.status == ScholarshipStatus.AWAITING_RESULT }
    val decided = scholarships.filter { it.status == ScholarshipStatus.AWARDED || it.status == ScholarshipStatus.REJECTED || it.status == ScholarshipStatus.WITHDRAWN || it.status == ScholarshipStatus.EXPIRED }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PipelineSection("1. Planning & Preparing", preparing, allRequirements, onSelectScholarship, Color(0xFF757575)) }
        item { PipelineSection("2. In Progress & Ready", inProgress, allRequirements, onSelectScholarship, Color(0xFF1A73E8)) }
        item { PipelineSection("3. Submitted & In Evaluation", submitted, allRequirements, onSelectScholarship, Color(0xFFF4B400)) }
        item { PipelineSection("4. Decisions & Awards", decided, allRequirements, onSelectScholarship, Color(0xFF0F9D58)) }
    }
}

@Composable
fun PipelineSection(
    stageTitle: String,
    items: List<Scholarship>,
    allRequirements: List<ScholarshipRequirement>,
    onSelectScholarship: (Int) -> Unit,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
            Spacer(modifier = Modifier.width(6.dp))
            Text("$stageTitle (${items.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
        }

        if (items.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text("No applications currently in this stage.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { s ->
                    val reqs = allRequirements.filter { it.scholarshipId == s.id }
                    val progress = ScholarshipCalculationHelper.getRequirementsProgress(reqs)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onSelectScholarship(s.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("${s.organization} • ${s.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${progress.completed}/${progress.total} Reqs", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyScholarshipsState(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "No Scholarship Applications Tracked",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track scholarships, organize prerequisite checklists, manage upcoming deadlines, and export dossiers offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Button(
                onClick = onAddClick,
                modifier = Modifier.testTag("add_first_scholarship_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add First Scholarship")
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
