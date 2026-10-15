package com.deehem.gpawhiz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deehem.gpawhiz.data.Course
import com.deehem.gpawhiz.service.GpaCalcService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEntryDialog(
    course: Course?,
    onDismiss: () -> Unit,
    onSave: (code: String, title: String, units: Int, score: Int, grade: String, carryOver: Boolean, assessmentBreakdown: String?) -> Unit,
    scale: Double
) {
    var code by remember { mutableStateOf(course?.code ?: "") }
    var title by remember { mutableStateOf(course?.title ?: "") }
    var unitsStr by remember { mutableStateOf(course?.units?.toString() ?: "3") }
    var scoreStr by remember { mutableStateOf(course?.score?.toString() ?: "75") }
    
    // Choose Letter Grade directly, no numeric scores shown
    val grades = (if (scale >= 5.0) {
        listOf("A", "B", "C", "D", "E", "F")
    } else {
        listOf("A", "B", "C", "D", "F")
    }) + listOf("Awaiting Grade")
    
    var selectedGrade by remember { mutableStateOf(course?.grade ?: grades.first()) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var isCarryOver by remember { mutableStateOf(course?.isCarryOver ?: false) }

    // Assessment Breakdown State
    var hasBreakdown by remember { mutableStateOf(!course?.assessmentBreakdown.isNullOrBlank()) }
    var components by remember {
        mutableStateOf(
            if (!course?.assessmentBreakdown.isNullOrBlank()) {
                com.deehem.gpawhiz.data.AssessmentComponent.listFromJson(course?.assessmentBreakdown)
            } else {
                listOf(
                    com.deehem.gpawhiz.data.AssessmentComponent("Test 1", 20, null),
                    com.deehem.gpawhiz.data.AssessmentComponent("Assignment", 10, null),
                    com.deehem.gpawhiz.data.AssessmentComponent("Attendance", 10, null),
                    com.deehem.gpawhiz.data.AssessmentComponent("Final Exam", 60, null)
                )
            }
        )
    }

    val dialogTitle = if (course == null) "Register New Lecture Course" else "Modify Course Details"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Course Code (e.g. GST 111, MTH 101)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("course_code_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course Title Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = unitsStr,
                        onValueChange = { unitsStr = it },
                        label = { Text("Units Weight") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("course_units_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = scoreStr,
                        onValueChange = {
                            scoreStr = it
                            val scoreInt = it.toIntOrNull()
                            if (scoreInt != null && scoreInt in 0..100) {
                                val calculatedGrade = GpaCalcService.getGradeForScore(scoreInt, scale)
                                if (selectedGrade != "Awaiting Grade") {
                                    selectedGrade = calculatedGrade
                                }
                            }
                        },
                        label = { Text("Score (0-100)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("course_score_input"),
                        singleLine = true
                    )
                }

                // Grade Direct Dropdown Selector Layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (selectedGrade == "Awaiting Grade") "Awaiting Grade" else "Grade $selectedGrade",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Earned Grade") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("grade_selector_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            grades.forEach { gradeOption ->
                                DropdownMenuItem(
                                    text = { Text(text = gradeOption, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        selectedGrade = gradeOption
                                        dropdownExpanded = false
                                        if (gradeOption == "Awaiting Grade") {
                                            isCarryOver = false
                                        } else {
                                            if (gradeOption == "F") {
                                                isCarryOver = true
                                            }
                                            val suggestedScore = when (gradeOption) {
                                                "A" -> "75"
                                                "B" -> "65"
                                                "C" -> "55"
                                                "D" -> "47"
                                                "E" -> "42"
                                                else -> "0"
                                            }
                                            val currentScoreInt = scoreStr.toIntOrNull()
                                            if (currentScoreInt == null || GpaCalcService.getGradeForScore(currentScoreInt, scale) != gradeOption) {
                                                scoreStr = suggestedScore
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Dynamic live Grade Preview Box
                val isAwaiting = selectedGrade.equals("Awaiting Grade", ignoreCase = true)
                val previewColor = when (selectedGrade) {
                    "A" -> Color(0xFF0F9D58)
                    "B", "C" -> Color(0xFF1A73E8)
                    "D", "E" -> Color(0xFFF4B400)
                    "Awaiting Grade" -> Color.Gray
                    else -> Color(0xFFDB4437)
                }
                
                val unitsInt = unitsStr.toIntOrNull() ?: 1
                val calculatedPoints = if (isAwaiting) 0.0 else GpaCalcService.getPointsForGrade(selectedGrade, scale) * unitsInt
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(previewColor.copy(alpha = 0.08f))
                        .border(1.dp, previewColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PREVIEW QUALITY POINTS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = previewColor,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (isAwaiting) {
                            Text(
                                text = "Awaiting Grade  |  No points accumulated yet",
                                color = previewColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        } else {
                            Text(
                                text = "Grade $selectedGrade  |  %.1f Points Earned".format(calculatedPoints),
                                color = previewColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (selectedGrade != "F" && selectedGrade != "Awaiting Grade") isCarryOver = !isCarryOver },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isCarryOver || selectedGrade == "F",
                        onCheckedChange = { if (selectedGrade != "F" && selectedGrade != "Awaiting Grade") isCarryOver = it },
                        enabled = selectedGrade != "F" && selectedGrade != "Awaiting Grade"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Outstanding Carry-Over course?",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // ==========================================
                // OPTIONAL ASSESSMENT BREAKDOWN & PREDICTOR
                // ==========================================
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { hasBreakdown = !hasBreakdown },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Assessment Breakdown (CA + Exam)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Define CA tests, assignments, and predict needed exam score",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = hasBreakdown,
                        onCheckedChange = { hasBreakdown = it },
                        modifier = Modifier.testTag("toggle_ca_breakdown")
                    )
                }

                if (hasBreakdown) {
                    val totalMax = components.sumOf { it.maxMarks }
                    val isValidTotal = totalMax == 100

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isValidTotal) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "COMPONENTS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isValidTotal) "Total Max: 100/100 (Valid)" else "Total Max: $totalMax/100 (Must equal 100)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isValidTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }

                            components.forEachIndexed { index, component ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = component.name,
                                        onValueChange = { newName ->
                                            components = components.toMutableList().apply {
                                                this[index] = component.copy(name = newName)
                                            }
                                        },
                                        label = { Text("Name", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1.3f),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = component.maxMarks.toString(),
                                        onValueChange = { newMax ->
                                            val m = newMax.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                                            components = components.toMutableList().apply {
                                                this[index] = component.copy(maxMarks = m)
                                            }
                                        },
                                        label = { Text("Max", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.9f),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = component.scoreEarned?.toString() ?: "",
                                        onValueChange = { newScore ->
                                            val s = newScore.filter { c -> c.isDigit() }.toIntOrNull()
                                            components = components.toMutableList().apply {
                                                this[index] = component.copy(scoreEarned = s)
                                            }
                                        },
                                        label = { Text("Score", fontSize = 10.sp) },
                                        placeholder = { Text("-", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.9f),
                                        singleLine = true
                                    )

                                    if (components.size > 1) {
                                        IconButton(
                                            onClick = {
                                                components = components.toMutableList().apply { removeAt(index) }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove component",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        components = components + com.deehem.gpawhiz.data.AssessmentComponent(
                                            name = "Component ${components.size + 1}",
                                            maxMarks = 10,
                                            scoreEarned = null
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Component", fontSize = 12.sp)
                                }

                                val earnedSum = components.mapNotNull { it.scoreEarned }.sum()
                                if (components.any { it.scoreEarned != null }) {
                                    TextButton(
                                        onClick = {
                                            scoreStr = earnedSum.toString()
                                            val calculatedGrade = GpaCalcService.getGradeForScore(earnedSum, scale)
                                            if (selectedGrade != "Awaiting Grade") {
                                                selectedGrade = calculatedGrade
                                            }
                                        }
                                    ) {
                                        Text("Apply Score ($earnedSum)", fontSize = 12.sp)
                                    }
                                }
                            }

                            // TARGET EXAM MARK PREDICTOR
                            val examIndex = components.indexOfLast { it.name.contains("exam", ignoreCase = true) }.takeIf { it >= 0 } ?: (components.size - 1)
                            val examComponent = components.getOrNull(examIndex)
                            val caComponents = components.filterIndexed { idx, _ -> idx != examIndex }
                            val caEarned = caComponents.mapNotNull { it.scoreEarned }.sum()
                            val caMax = caComponents.sumOf { it.maxMarks }
                            val examMax = examComponent?.maxMarks ?: (100 - caMax)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("ca_exam_predictor_card")
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "TARGET EXAM MARK PREDICTOR",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "Current CA: $caEarned / $caMax marks  |  Final Exam Max: $examMax marks",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    val targetGrades = if (scale >= 5.0) {
                                        listOf("A" to 70, "B" to 60, "C" to 50, "D" to 45, "E" to 40)
                                    } else {
                                        listOf("A" to 70, "B" to 60, "C" to 50, "D" to 45)
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    targetGrades.forEach { (targetGrade, minScore) ->
                                        val needed = minScore - caEarned
                                        val (statusText, statusColor) = when {
                                            needed <= 0 -> {
                                                "Grade $targetGrade: Already secured! ($caEarned CA score meets $minScore)" to Color(0xFF0F9D58)
                                            }
                                            needed > examMax -> {
                                                "Grade $targetGrade: Not reachable (Needs $needed/$examMax in exam)" to Color(0xFFDB4437)
                                            }
                                            else -> {
                                                "Grade $targetGrade: You have $caEarned/$caMax in CA. You need at least $needed/$examMax in the exam." to MaterialTheme.colorScheme.primary
                                            }
                                        }

                                        Text(
                                            text = statusText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = statusColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val u = unitsStr.toIntOrNull() ?: 1
                    val s = scoreStr.toIntOrNull() ?: when (selectedGrade) {
                        "A" -> 75
                        "B" -> 65
                        "C" -> 55
                        "D" -> 47
                        "E" -> 42
                        else -> 0
                    }
                    val breakdownJson = if (hasBreakdown && components.isNotEmpty()) {
                        com.deehem.gpawhiz.data.AssessmentComponent.listToJson(components)
                    } else null

                    if (code.isNotEmpty() && u > 0) {
                        onSave(code, title, u, s, selectedGrade, isCarryOver, breakdownJson)
                    }
                },
                modifier = Modifier.testTag("submit_course_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
