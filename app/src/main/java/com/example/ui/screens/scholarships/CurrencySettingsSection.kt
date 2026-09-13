package com.example.ui.screens.scholarships

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.ExchangeRate
import com.example.service.CurrencyConverter
import com.example.ui.viewmodel.GpaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySettingsSection(
    viewModel: GpaViewModel,
    modifier: Modifier = Modifier
) {
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val exchangeRates by viewModel.exchangeRates.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var rateToEdit by remember { mutableStateOf<ExchangeRate?>(null) }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3C7))
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Column {
                    Text(
                        text = "Scholarship Currency & Exchange Rates",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "100% Offline-First Multi-Currency Engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Each scholarship keeps its authentic original currency. The base currency normalizes total portfolio values and reports using your local offline rate table.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 1. Base Currency Selector
            Text(
                text = "APPLICATION DEFAULT / BASE CURRENCY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            ExposedDropdownMenuBox(
                expanded = currencyDropdownExpanded,
                onExpandedChange = { currencyDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = "$defaultCurrency (${CurrencyConverter.getCurrencySymbol(defaultCurrency)} - ${CurrencyConverter.getCurrencyName(defaultCurrency)})",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Default Reporting Currency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("base_currency_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = currencyDropdownExpanded,
                    onDismissRequest = { currencyDropdownExpanded = false }
                ) {
                    CurrencyConverter.POPULAR_CURRENCIES.forEach { curr ->
                        DropdownMenuItem(
                            text = {
                                Text("${curr.code} (${curr.symbol} - ${curr.name})")
                            },
                            onClick = {
                                viewModel.setDefaultCurrency(curr.code)
                                currencyDropdownExpanded = false
                            },
                            leadingIcon = {
                                Text(curr.symbol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // 2. Exchange-Rate Table Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LOCAL EXCHANGE-RATE TABLE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Approximate conversions labeled with ≈",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        rateToEdit = null
                        showEditDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_exchange_rate_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Rate", fontSize = 12.sp)
                }
            }

            if (exchangeRates.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No offline exchange rates configured yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                // Seed standard offline benchmarks for popular pairs to baseCurrency
                                viewModel.saveExchangeRate("USD", defaultCurrency, if (defaultCurrency == "NGN") 1500.0 else 1.0, "Initial offline benchmark")
                                viewModel.saveExchangeRate("GBP", defaultCurrency, if (defaultCurrency == "NGN") 2000.0 else 1.3, "Initial offline benchmark")
                                viewModel.saveExchangeRate("EUR", defaultCurrency, if (defaultCurrency == "NGN") 1750.0 else 1.1, "Initial offline benchmark")
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("seed_initial_rates_button")
                        ) {
                            Text("Initialize Standard Rates (USD, GBP, EUR → $defaultCurrency)", fontSize = 11.sp)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    exchangeRates.forEach { rateEntry ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "1 ${rateEntry.fromCurrency} = ${CurrencyConverter.getCurrencySymbol(rateEntry.toCurrency)}${CurrencyConverter.formatRateValue(rateEntry.rate)} ${rateEntry.toCurrency}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    val metaLine = buildString {
                                        if (rateEntry.sourceDescription.isNotBlank()) {
                                            append("Source: ${rateEntry.sourceDescription} • ")
                                        }
                                        append("Updated: ${CurrencyConverter.formatDate(rateEntry.lastUpdated)}")
                                    }
                                    Text(
                                        text = metaLine,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            rateToEdit = rateEntry
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.size(36.dp).testTag("edit_rate_${rateEntry.fromCurrency}_${rateEntry.toCurrency}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Rate", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteExchangeRate(rateEntry)
                                        },
                                        modifier = Modifier.size(36.dp).testTag("delete_rate_${rateEntry.fromCurrency}_${rateEntry.toCurrency}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Rate", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        ExchangeRateDialog(
            existingRate = rateToEdit,
            defaultBase = defaultCurrency,
            onDismiss = { showEditDialog = false },
            onSave = { from, to, rate, source ->
                viewModel.saveExchangeRate(from, to, rate, source)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeRateDialog(
    existingRate: ExchangeRate?,
    defaultBase: String,
    onDismiss: () -> Unit,
    onSave: (from: String, to: String, rate: Double, source: String) -> Unit
) {
    var fromCurrency by remember { mutableStateOf(existingRate?.fromCurrency ?: if (defaultBase == "USD") "NGN" else "USD") }
    var toCurrency by remember { mutableStateOf(existingRate?.toCurrency ?: defaultBase) }
    var rateStr by remember { mutableStateOf(existingRate?.let { CurrencyConverter.formatRateValue(it.rate) } ?: "") }
    var source by remember { mutableStateOf(existingRate?.sourceDescription ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var fromDropdownExpanded by remember { mutableStateOf(false) }
    var toDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingRate == null) "Add Offline Exchange Rate" else "Edit Exchange Rate",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Specify conversion rate for offline calculations. Rate must be greater than zero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // From Currency
                ExposedDropdownMenuBox(
                    expanded = fromDropdownExpanded,
                    onExpandedChange = { fromDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "$fromCurrency (${CurrencyConverter.getCurrencyName(fromCurrency)})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From Currency (Source)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("rate_from_currency")
                    )
                    ExposedDropdownMenu(
                        expanded = fromDropdownExpanded,
                        onDismissRequest = { fromDropdownExpanded = false }
                    ) {
                        CurrencyConverter.POPULAR_CURRENCIES.forEach { curr ->
                            DropdownMenuItem(
                                text = { Text("${curr.code} - ${curr.name} (${curr.symbol})") },
                                onClick = {
                                    fromCurrency = curr.code
                                    fromDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // To Currency
                ExposedDropdownMenuBox(
                    expanded = toDropdownExpanded,
                    onExpandedChange = { toDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "$toCurrency (${CurrencyConverter.getCurrencyName(toCurrency)})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To Currency (Target)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("rate_to_currency")
                    )
                    ExposedDropdownMenu(
                        expanded = toDropdownExpanded,
                        onDismissRequest = { toDropdownExpanded = false }
                    ) {
                        CurrencyConverter.POPULAR_CURRENCIES.forEach { curr ->
                            DropdownMenuItem(
                                text = { Text("${curr.code} - ${curr.name} (${curr.symbol})") },
                                onClick = {
                                    toCurrency = curr.code
                                    toDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Rate Input
                OutlinedTextField(
                    value = rateStr,
                    onValueChange = {
                        rateStr = it
                        errorMessage = null
                    },
                    label = { Text("Exchange Rate (1 $fromCurrency = ? $toCurrency) *") },
                    placeholder = { Text("e.g. 1500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("rate_value_input"),
                    singleLine = true,
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Source description
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Source / Benchmark (Optional)") },
                    placeholder = { Text("e.g. Central Bank rate, Bureau de Change") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Preview
                val rateVal = rateStr.toDoubleOrNull()
                if (rateVal != null && rateVal > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Preview: 1 $fromCurrency ≈ ${CurrencyConverter.getCurrencySymbol(toCurrency)}${CurrencyConverter.formatRateValue(rateVal)} $toCurrency",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = rateStr.toDoubleOrNull()
                    if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
                        errorMessage = "Source and Target currencies cannot be the same."
                        return@Button
                    }
                    if (rate == null || rate <= 0.0) {
                        errorMessage = "Please enter a valid positive numeric rate greater than 0."
                        return@Button
                    }
                    onSave(fromCurrency, toCurrency, rate, source)
                },
                modifier = Modifier.testTag("save_exchange_rate_dialog_button")
            ) {
                Text("Save Rate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
