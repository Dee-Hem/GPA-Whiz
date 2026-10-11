package com.deehem.gpawhiz.ui.screens.scholarships

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deehem.gpawhiz.data.EssayDraft
import com.deehem.gpawhiz.data.Scholarship
import com.deehem.gpawhiz.ui.viewmodel.GpaViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScholarshipDraftsSection(
    viewModel: GpaViewModel,
    scholarships: List<Scholarship>,
    modifier: Modifier = Modifier
) {
    val drafts by viewModel.essayDrafts.collectAsState()
    var editingDraft by remember { mutableStateOf<EssayDraft?>(null) }
    var draftToDelete by remember { mutableStateOf<EssayDraft?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Universal confirm-before-delete dialog
    if (draftToDelete != null) {
        AlertDialog(
            onDismissRequest = { draftToDelete = null },
            title = { Text("Delete Essay Draft?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${draftToDelete!!.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDraft(draftToDelete!!)
                        if (editingDraft?.id == draftToDelete?.id) {
                            editingDraft = null
                        }
                        draftToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { draftToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (editingDraft != null) {
        DraftEditor(
            draft = editingDraft!!,
            scholarships = scholarships,
            onUpdateDraft = { updated ->
                viewModel.updateDraft(updated)
                editingDraft = updated
            },
            onDelete = { draftToDelete = it },
            onClose = { editingDraft = null }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .testTag("drafts_vault_section"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // HEADER & ACTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SOP & ESSAY VAULT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Offline Statements, Diversity Essays & Application Drafts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        viewModel.addDraft(
                            title = "New Essay Draft",
                            body = "",
                            onCreated = { newDraft ->
                                editingDraft = newDraft
                            }
                        )
                    },
                    modifier = Modifier.testTag("add_draft_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Draft")
                }
            }

            // SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search drafts by title, content, or scholarship...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            val filteredDrafts = remember(drafts, searchQuery, scholarships) {
                drafts.filter { draft ->
                    val linkedScholarshipName = scholarships.find { it.id == draft.scholarshipId }?.name ?: ""
                    searchQuery.isBlank() ||
                    draft.title.contains(searchQuery, ignoreCase = true) ||
                    draft.body.contains(searchQuery, ignoreCase = true) ||
                    linkedScholarshipName.contains(searchQuery, ignoreCase = true)
                }
            }

            if (filteredDrafts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "No Essay Drafts Yet" else "No Matching Drafts Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (searchQuery.isBlank())
                                "Draft statements of purpose, scholarship essays, and personal statements offline with live word counts."
                            else "Try searching with a different term.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredDrafts, key = { it.id }) { draft ->
                        val linkedScholarship = scholarships.find { it.id == draft.scholarshipId }
                        DraftCardItem(
                            draft = draft,
                            linkedScholarship = linkedScholarship,
                            onClick = { editingDraft = draft },
                            onDelete = { draftToDelete = draft }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DraftCardItem(
    draft: EssayDraft,
    linkedScholarship: Scholarship?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val words = remember(draft.body) {
        if (draft.body.isBlank()) 0 else draft.body.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
    }
    val chars = draft.body.length
    val dateStr = remember(draft.lastModified) {
        SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(draft.lastModified))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("draft_item_${draft.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = draft.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (linkedScholarship != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = linkedScholarship.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (draft.body.isNotBlank()) {
                Text(
                    text = draft.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "(Empty body - tap to write)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // STATS & ACTIONS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "$words words",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (draft.wordLimit != null && words > draft.wordLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    if (draft.wordLimit != null) {
                        Text(
                            text = "/ ${draft.wordLimit} max",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "• $dateStr",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText(draft.title, draft.body))
                            Toast.makeText(context, "Draft text copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Copy text", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, draft.title)
                                putExtra(Intent.EXTRA_TEXT, draft.body)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Draft"))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Share draft", modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete draft", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftEditor(
    draft: EssayDraft,
    scholarships: List<Scholarship>,
    onUpdateDraft: (EssayDraft) -> Unit,
    onDelete: (EssayDraft) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(draft.title) }
    var body by remember { mutableStateOf(draft.body) }
    var selectedScholarshipId by remember { mutableStateOf(draft.scholarshipId) }
    var wordLimitStr by remember { mutableStateOf(draft.wordLimit?.toString() ?: "") }
    var scholarshipDropdownExpanded by remember { mutableStateOf(false) }

    // Live word & character counters
    val words = remember(body) {
        if (body.isBlank()) 0 else body.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
    }
    val chars = body.length
    val limitInt = wordLimitStr.toIntOrNull()
    val isOverLimit = limitInt != null && limitInt > 0 && words > limitInt

    // Autosave when values change
    LaunchedEffect(title, body, selectedScholarshipId, wordLimitStr) {
        onUpdateDraft(
            draft.copy(
                title = title.ifBlank { "Untitled Draft" },
                body = body,
                scholarshipId = selectedScholarshipId,
                wordLimit = limitInt,
                lastModified = System.currentTimeMillis()
            )
        )
    }

    BackHandler {
        onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "EDIT DRAFT",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("close_draft_editor_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText(title, body))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_draft_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Copy to clipboard")
                    }
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, title)
                                putExtra(Intent.EXTRA_TEXT, body)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Essay Draft"))
                        },
                        modifier = Modifier.testTag("share_draft_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Share text")
                    }
                    IconButton(
                        onClick = { onDelete(draft) },
                        modifier = Modifier.testTag("delete_draft_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete draft", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // TITLE FIELD
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Draft Title (e.g. Chevening Leadership Essay)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("draft_title_input"),
                singleLine = true
            )

            // LINK SCHOLARSHIP & WORD LIMIT ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Scholarship Link Dropdown
                Box(modifier = Modifier.weight(1.5f)) {
                    val linkedScholarshipName = scholarships.find { it.id == selectedScholarshipId }?.name ?: "None (General Essay)"
                    ExposedDropdownMenuBox(
                        expanded = scholarshipDropdownExpanded,
                        onExpandedChange = { scholarshipDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = linkedScholarshipName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Linked Scholarship") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scholarshipDropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = scholarshipDropdownExpanded,
                            onDismissRequest = { scholarshipDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (General Essay)") },
                                onClick = {
                                    selectedScholarshipId = null
                                    scholarshipDropdownExpanded = false
                                }
                            )
                            scholarships.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        selectedScholarshipId = s.id
                                        scholarshipDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Word Limit
                OutlinedTextField(
                    value = wordLimitStr,
                    onValueChange = { wordLimitStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Word Limit") },
                    placeholder = { Text("e.g. 500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("draft_word_limit_input"),
                    singleLine = true
                )
            }

            // LIVE WORD COUNT & LIMIT STATUS BANNER
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isOverLimit) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$words words  •  $chars characters",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverLimit) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                    )

                    if (limitInt != null && limitInt > 0) {
                        if (isOverLimit) {
                            Text(
                                text = "Warning: Over limit by ${words - limitInt} words!",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = "${limitInt - words} words left",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = "Autosaved",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // BODY TEXT INPUT (EXPANDABLE)
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Essay / SOP Content") },
                placeholder = { Text("Type or paste your statement of purpose, essay answers, or scholarship drafts here. It is automatically saved offline...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("draft_body_input")
            )
        }
    }
}
