package com.deehem.gpawhiz.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deehem.gpawhiz.service.GpaCalcService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicAssistantScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ACADEMIC ASSISTANT", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Academic Utilities & Converters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Use these standalone tools to estimate and convert your academic metrics. These do not affect your stored records.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // A. GPA Scale Conversion (5.0 <-> 4.0)
            GpaScaleConverterCard()

            // B. Percentage <-> GPA Conversion
            PercentageToGpaConverterCard()
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun GpaScaleConverterCard() {
    var inputValue by remember { mutableStateOf("") }
    var isFrom5To4 by remember { mutableStateOf(true) }
    
    val result = remember(inputValue, isFrom5To4) {
        val valDouble = inputValue.toDoubleOrNull() ?: 0.0
        if (isFrom5To4) {
            GpaCalcService.convertScale(valDouble, 5.0, 4.0)
        } else {
            GpaCalcService.convertScale(valDouble, 4.0, 5.0)
        }
    }

    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text = "GPA SCALE CONVERTER", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isFrom5To4) "5.0 Scale" else "4.0 Scale",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { isFrom5To4 = !isFrom5To4 }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Swap Direction")
                }
                Text(
                    text = if (isFrom5To4) "4.0 Scale" else "5.0 Scale",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                label = { Text("Enter GPA (${if (isFrom5To4) "5.0" else "4.0"} Scale)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("gpa_scale_input"),
                singleLine = true
            )

            if (inputValue.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Converted Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "%.2f".format(result),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "on ${if (isFrom5To4) "4.0" else "5.0"} Scale",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PercentageToGpaConverterCard() {
    var percentageInput by remember { mutableStateOf("") }
    
    val gpa4 = remember(percentageInput) {
        val p = percentageInput.toDoubleOrNull() ?: 0.0
        GpaCalcService.percentageToGpa(p, 4.0)
    }
    
    val gpa5 = remember(percentageInput) {
        val p = percentageInput.toDoubleOrNull() ?: 0.0
        GpaCalcService.percentageToGpa(p, 5.0)
    }

    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(text = "PERCENTAGE TO GPA", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
            }

            OutlinedTextField(
                value = percentageInput,
                onValueChange = { percentageInput = it },
                label = { Text("Enter Percentage Score (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("percentage_input"),
                suffix = { Text("%") },
                singleLine = true
            )

            if (percentageInput.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Estimated Conversions*",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ConversionResultBox(label = "4.0 Scale", value = "%.2f".format(gpa4), modifier = Modifier.weight(1f))
                        ConversionResultBox(label = "5.0 Scale", value = "%.2f".format(gpa5), modifier = Modifier.weight(1f))
                    }
                    
                    Text(
                        text = "*Estimated based on standard institutional mapping. Actual results may vary.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ConversionResultBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
