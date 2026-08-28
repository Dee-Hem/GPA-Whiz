package com.example.ui.screens.scholarships

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.*
import com.example.service.ScholarshipCalculationHelper
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequirementSelectionDialog(
    existingRequirements: List<ScholarshipRequirement>,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, details: String, deadline: Long?, notes: String) -> Unit
) {
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedPredefined by remember { mutableStateOf<PredefinedRequirement?>(null) }
    var isCustomMode by remember { mutableStateOf(false) }

    var customTitle by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf(RequirementCategory.OTHER) }
    var details by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }

    val categoryFilterList = listOf("All") + RequirementCategory.ALL

    val filteredPredefined = remember(searchQuery, selectedCategoryFilter) {
        PredefinedRequirements.LIST.filter { item ->
            val matchesCategory = selectedCategoryFilter == "All" || item.category == selectedCategoryFilter
            val matchesSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true) ||
                    item.defaultDetails.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val currentTitle = if (isCustomMode) customTitle else (selectedPredefined?.title ?: "")
    val currentCategory = if (isCustomMode) customCategory else (selectedPredefined?.category ?: RequirementCategory.OTHER)

    val isDuplicate = existingRequirements.any { it.title.equals(currentTitle.trim(), ignoreCase = true) }

    fun showDatePicker() {
        val calendar = Calendar.getInstance()
        if (deadline != null && deadline!! > 0) {
            calendar.timeInMillis = deadline!!
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
                }
                deadline = selectedCal.timeInMillis
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
                text = "Add Application Requirement",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mode Toggle: Predefined vs Custom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isCustomMode,
                        onClick = { isCustomMode = false },
                        label = { Text("Standard List") },
                        leadingIcon = if (!isCustomMode) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isCustomMode,
                        onClick = {
                            isCustomMode = true
                            selectedPredefined = null
                        },
                        label = { Text("Custom / Other") },
                        leadingIcon = if (isCustomMode) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!isCustomMode) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search requirement (e.g. Transcript, CV, Essay)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Category filter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categoryFilterList) { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }

                    // Predefined items list
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(filteredPredefined) { item ->
                                val isSelected = selectedPredefined?.title == item.title
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                        .clickable {
                                            selectedPredefined = item
                                            if (details.isBlank()) {
                                                details = item.defaultDetails
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = item.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (filteredPredefined.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No matches found. Switch to 'Custom / Other' above.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Custom Requirement Input
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Requirement Title *") },
                        placeholder = { Text("e.g. Portfolio of Design Projects") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_requirement_title_input"),
                        singleLine = true
                    )

                    var catDropdownExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = catDropdownExpanded,
                        onExpandedChange = { catDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = customCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catDropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = catDropdownExpanded,
                            onDismissRequest = { catDropdownExpanded = false }
                        ) {
                            RequirementCategory.ALL.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        customCategory = cat
                                        catDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Duplicate Warning Banner
                if (isDuplicate && currentTitle.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Duplicate",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notice: This requirement has already been added to this scholarship checklist.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Details / Specific Instructions
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Specific Details / Instructions") },
                    placeholder = { Text("e.g. Minimum 500 words, certified copy, stamped by department...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Optional Specific Requirement Due Date
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { showDatePicker() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Requirement Due Date (Optional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (deadline != null) ScholarshipCalculationHelper.formatDate(deadline) else "Same as scholarship deadline",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (deadline != null) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (deadline != null) {
                            IconButton(onClick = { deadline = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear date", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Icon(Icons.Default.DateRange, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Personal Notes") },
                    placeholder = { Text("e.g. Draft submitted to Dr. Adeleke for review") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentTitle.isNotBlank()) {
                        onSave(currentTitle.trim(), currentCategory, details.trim(), deadline, notes.trim())
                    }
                },
                enabled = currentTitle.isNotBlank(),
                modifier = Modifier.testTag("submit_requirement_button")
            ) {
                Text("Add Requirement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
