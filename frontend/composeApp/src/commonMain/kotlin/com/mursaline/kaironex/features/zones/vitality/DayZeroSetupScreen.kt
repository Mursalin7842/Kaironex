package com.mursaline.kaironex.features.zones.vitality

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.theme.VitalityColors
import com.mursaline.kaironex.ui.theme.VitalityTypography
import kotlinx.datetime.LocalDate
import com.mursaline.kaironex.ui.components.KxTextField // Assuming this exists or use OutlinedTextField
import cafe.adriel.voyager.koin.koinScreenModel
import androidx.compose.material.icons.automirrored.filled.ArrowBack

data class DayZeroSetupScreen(
    val isEditMode: Boolean = false
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<VitalityViewModel>()
        val state by viewModel.uiState.collectAsState()

        // Local state for form fields
        var balance by remember { mutableStateOf(if (isEditMode) state.totalBalance.toString() else "") }
        var monthlyBills by remember { mutableStateOf(if (isEditMode) state.monthlyBills.toString() else "") }
        var otherIncomeSource by remember { mutableStateOf("") } // Not in state yet, placeholder
        var paydayDate by remember { mutableStateOf<LocalDate?>(state.paydayDate) }
        var favoriteFood by remember { mutableStateOf(if (isEditMode) state.favoriteFood else "") }
        var isProcessing by remember { mutableStateOf(false) }

        // Mock "Has Job" from campaign/profile (Usually would come from CampaignState)
        val hasJobFromProfile = true 
        var showSuccessBriefing by remember { mutableStateOf(false) }

        if (showSuccessBriefing && state.isCalibrated) {
            // Show Success Screen with Brain Message
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🛡️ CALIBRATION COMPLETE", style = VitalityTypography.defconLabel, color = VitalityColors.defcon4Stable)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(state.voiceMessage, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) {
                        Text("ENTER COMMAND CENTER")
                    }
                }
            }
            return
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        if (isEditMode) Text("Vitality Settings") 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        if (!isEditMode) {
                            TextButton(onClick = { navigator.pop() }) {
                                Text("Skip", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    // Header with animation
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🧬",
                            fontSize = 64.sp
                        )
                        Text(
                            text = if (isEditMode) "RECALIBRATE SYSTEMS" else "WELCOME TO VITALITY",
                            style = VitalityTypography.defconLabel,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isEditMode) "Update your survival parameters" else "Let's calibrate your survival systems",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                // Balance Input
                item {
                    SetupInputCard(
                        icon = "💰",
                        label = "Current Balance",
                        hint = "(checking + savings combined)"
                    ) {
                        OutlinedTextField(
                            value = balance,
                            onValueChange = { balance = it.filter { c -> c.isDigit() || c == '.' } },
                            prefix = { Text("$") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Monthly Bills
                item {
                    SetupInputCard(
                        icon = "📅",
                        label = "Monthly Fixed Bills",
                        hint = "(rent, utilities, subscriptions)"
                    ) {
                        OutlinedTextField(
                            value = monthlyBills,
                            onValueChange = { monthlyBills = it.filter { c -> c.isDigit() || c == '.' } },
                            prefix = { Text("$") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Job Status (Auto-Synced from Profile)
                item {
                    SetupInputCard(
                        icon = "💼",
                        label = "Job Status",
                        hint = "(auto-synced from your profile)"
                    ) {
                        Column {
                            // Display auto-synced job status
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (hasJobFromProfile) VitalityColors.defcon4Stable.copy(alpha = 0.1f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Icon(
                                    if (hasJobFromProfile) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (hasJobFromProfile) VitalityColors.defcon4Stable 
                                           else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (hasJobFromProfile) "Has Job (from Campaign)"
                                    else "Student (no job listed)",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            // Optional: Other income source (if no job)
                            if (!hasJobFromProfile) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "💸 Other Income Source (Optional)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = otherIncomeSource,
                                    onValueChange = { otherIncomeSource = it },
                                    placeholder = { Text("Family support, scholarships, etc.") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
                
                // Payday Date
                item {
                    SetupInputCard(
                        icon = "📆",
                        label = "Next Payday",
                        hint = "(or expected income date)"
                    ) {
                        // TODO: Implement proper DatePicker
                        OutlinedTextField(
                            value = "2026-02-28", // Placeholder
                            onValueChange = { },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Coming Soon") }
                        )
                    }
                }
                
                // Favorite Food
                item {
                    SetupInputCard(
                        icon = "🍕",
                        label = "Victory Celebration Food",
                        hint = "(for when you achieve goals!)"
                    ) {
                        OutlinedTextField(
                            value = favoriteFood,
                            onValueChange = { favoriteFood = it },
                            placeholder = { Text("e.g., Sushi, Pizza, Steak...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Calibrate Button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isProcessing = true
                            
                            // Aggregate SITREP for the Brain
                            val sitrep = """
                                Financial Sitrep:
                                - Total Balance: $balance
                                - Monthly Bills: $monthlyBills
                                - Job Status: ${if (hasJobFromProfile) "Employed/Student" else "Unemployed"}
                                - Payday: 2026-02-28
                                - Favorite Food: $favoriteFood
                            """.trimIndent()
                            
                            viewModel.updateSetupData(
                                financialInfo = sitrep,
                                favoriteFood = favoriteFood.ifEmpty { "Pizza" }
                            )
                            
                            // Don't pop immediately, wait for calibration result
                            showSuccessBriefing = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = balance.isNotEmpty() && monthlyBills.isNotEmpty() && !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (isEditMode) "UPDATE SYSTEMS" else "CALIBRATE SYSTEMS", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SetupInputCard(
    icon: String,
    label: String,
    hint: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(label, fontWeight = FontWeight.Medium)
                    if (hint.isNotEmpty()) {
                        Text(
                            hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
