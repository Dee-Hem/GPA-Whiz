package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
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
import com.example.data.Course
import com.example.service.GpaCalcService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEntryDialog(
    course: Course?,
    onDismiss: () -> Unit,
    onSave: (code: String, title: String, units: Int, score: Int, grade: String, carryOver: Boolean) -> Unit,
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
                                .menuAnchor()
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
                                            // Suggest a standard default score for the chosen grade
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
                    if (code.isNotEmpty() && u > 0) {
                        onSave(code, title, u, s, selectedGrade, isCarryOver)
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
