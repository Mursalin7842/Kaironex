package com.mursaline.kaironex.features.zones.vitality

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlinx.coroutines.launch

object VitalityDashboardScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<VitalityViewModel>()
        val state by viewModel.uiState.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        
        // Local State
        var showLogExpenseDialog by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        Scaffold(
            containerColor = KaironexColors.CloudGray, // Matches Dashboard/Home background
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Vitality Agent", 
                            fontWeight = FontWeight.Bold, 
                            color = KaironexColors.Slate900 
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = KaironexColors.Slate900)
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            navigator.push(DayZeroSetupScreen(isEditMode = true)) 
                        }) {
                            Icon(Icons.Default.Settings, "Settings", tint = KaironexColors.GeminiBlurple)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = KaironexColors.CloudGray,
                        titleContentColor = KaironexColors.Slate900
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp), // Increased padding to match Home
                    verticalArrangement = Arrangement.spacedBy(24.dp) // Increased spacing
                ) {
                    item { Spacer(Modifier.height(8.dp)) }

                    // 1. HERO CARD (Defcon/Budget)
                    item {
                        DefconHeroCard(
                            defconLevel = state.defconLevel,
                            defconLabel = state.defconLabel,
                            todayBudget = state.todayBudget,
                            voiceMessage = state.voiceMessage
                        )
                    }

                    // 2. AI RECOMMENDATION (NEW)
                    if (state.mealRecommendation != null) {
                        item {
                            Text(
                                "AI Scout Recommendation", 
                                style = MaterialTheme.typography.titleMedium, 
                                color = KaironexColors.Slate900, 
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))
                            
                            val rec = state.mealRecommendation!!
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = KaironexColors.GeminiBlurple,
                                shadowElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if(rec.action == "COOK") "👨‍🍳" else "🛵", fontSize = 28.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                "ACTION: ${rec.action}", 
                                                style = MaterialTheme.typography.labelSmall, 
                                                color = Color.White.copy(alpha=0.8f),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                rec.meal_name, 
                                                style = MaterialTheme.typography.titleLarge, 
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    // Stats Row
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        StatBadge("💲${rec.cost_estimate}", Color.White.copy(alpha=0.2f))
                                        StatBadge("⏱️ ${rec.time_estimate}m", Color.White.copy(alpha=0.2f))
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    // Reasoning Trace
                                    Text(
                                        "\"${rec.reasoning_trace}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha=0.9f),
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }

                    // 3. DAILY FUEL PLAN
                    item {
                        Text(
                            "Daily Fuel Plan", 
                            style = MaterialTheme.typography.titleMedium, 
                            color = KaironexColors.Slate900, 
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        
                        if (state.mealPlan != null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = KaironexColors.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Optimization Badge
                                    if (state.isUsingProactiveData) {
                                        Surface(
                                            color = KaironexColors.SuccessGreen.copy(alpha=0.1f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text(
                                                "DEFCON ${state.defconLevel} OPTIMIZED",
                                                modifier = Modifier.padding(horizontal=8.dp, vertical=4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = KaironexColors.SuccessGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    MealOptionItem("Breakfast", state.mealPlan!!.breakfast_options.firstOrNull()?.name ?: "Coffee", "🍳", state.mealPlan!!.breakfast_options.firstOrNull()?.cost)
                                    MealOptionItem("Lunch", state.mealPlan!!.lunch_options.firstOrNull()?.name ?: "Sandwich", "🍱", state.mealPlan!!.lunch_options.firstOrNull()?.cost)
                                    MealOptionItem("Dinner", state.mealPlan!!.dinner_options.firstOrNull()?.name ?: "Pasta", "🥘", state.mealPlan!!.dinner_options.firstOrNull()?.cost)
                                }
                            }
                        } else {
                            // Empty State - Call to Action
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = KaironexColors.White, // Use faint blue tinge?
                                border = androidx.compose.foundation.BorderStroke(1.dp, KaironexColors.BorderGray),
                                modifier = Modifier.fillMaxWidth().clickable { navigator.push(MealDecisionScreen) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Restaurant, null, tint = KaironexColors.Slate500, modifier = Modifier.size(32.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("No Plan Generated", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Tap to have AI struct your diet", style = MaterialTheme.typography.bodySmall, color = KaironexColors.Slate500)
                                    Spacer(Modifier.height(16.dp))
                                    Button(
                                        onClick = { navigator.push(MealDecisionScreen) },
                                        colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.GeminiBlurple)
                                    ) {
                                        Text("Generate Plan")
                                    }
                                }
                            }
                        }
                    }

                    // 3. QUICK STATS ROW
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FloatingStatCard(
                                title = "Fridge Status",
                                value = "${state.fridgeEstimatedDays} Day Supply",
                                valueColor = KaironexColors.Slate900,
                                modifier = Modifier.weight(1f).height(120.dp),
                                subtext = "Last scanned 2h ago"
                            )
                            FloatingStatCard(
                                title = "Days to Payday",
                                value = "${state.daysToPayday}",
                                valueColor = KaironexColors.ElectricBlue,
                                modifier = Modifier.weight(1f).height(120.dp)
                            )
                        }
                    }

                    // 4. QUICK ACTIONS GRID - Condensed
                    item {
                        Text(
                            "Vitality Actions", 
                            style = MaterialTheme.typography.titleMedium, 
                            color = KaironexColors.Slate900, 
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        // New Grid Design - Hiding Manual Logs for Demo
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                             CompactActionCard(
                                 icon = "💰",
                                 label = "Funds",
                                 onClick = { navigator.push(EmergencyFundScreen) },
                                 modifier = Modifier.weight(1f)
                             )
                             CompactActionCard(
                                 icon = "📸",
                                 label = "Scan Fridge",
                                 onClick = { navigator.push(FridgeScanScreen) },
                                 modifier = Modifier.weight(1f)
                             )
                        }
                    }

                    // 5. VICTORY FEAST (Unlockable)
                    item {
                        val isUnlocked = state.victoryFeastUnlocked
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isUnlocked) Color(0xFFFFF7E6) else KaironexColors.CloudGray, // Gold if unlocked
                            border = if (!isUnlocked) androidx.compose.foundation.BorderStroke(1.dp, KaironexColors.BorderGray) else null,
                            shadowElevation = if (isUnlocked) 2.dp else 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isUnlocked) "🎉" else "🔒", fontSize = 32.sp)
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(
                                        if (isUnlocked) "VICTORY FEAST UNLOCKED" else "VICTORY FEAST LOCKED", 
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = if (isUnlocked) KaironexColors.AttentionOrange else KaironexColors.Slate500, 
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (isUnlocked) "Reward: ${state.favoriteFood.ifEmpty { "Pizza" }}" else "Complete a milestone to unlock reward", 
                                        style = MaterialTheme.typography.bodyLarge, 
                                        color = if (isUnlocked) KaironexColors.Slate900 else KaironexColors.Slate500, 
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    
                    item { Spacer(Modifier.height(32.dp)) }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = KaironexColors.GeminiBlurple
                    )
                }
            }
        }
        
        // Expense Dialog
        if (showLogExpenseDialog) {
            LogExpenseDialog(
                onDismiss = { showLogExpenseDialog = false },
                onConfirm = { amount, desc ->
                    viewModel.logExpense(amount, desc)
                    showLogExpenseDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Expense logged: $$amount")
                    }
                }
            )
        }
    }

    // --- HELPER COMPONENTS ---

    @Composable
    private fun DefconHeroCard(
        defconLevel: Int,
        defconLabel: String,
        todayBudget: Double,
        voiceMessage: String
    ) {
        val gradientColors = when(defconLevel) {
            1 -> listOf(Color(0xFFB71C1C), Color(0xFFD32F2F))
            2 -> listOf(Color(0xFFE65100), Color(0xFFF57C00))
            3 -> listOf(Color(0xFFF9A825), Color(0xFFFBC02D))
            else -> listOf(KaironexColors.SuccessGreen, Color(0xFF00AA88))
        }

        Surface(
            shape = RoundedCornerShape(24.dp), // Slightly rounder for Hero
            shadowElevation = 4.dp, // Higher elevation for Hero
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(gradientColors))
            ) {
                 Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Badge
                    Surface(
                         color = Color.Black.copy(alpha = 0.2f),
                         shape = RoundedCornerShape(50),
                         modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "DEFCON $defconLevel: $defconLabel",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    
                    Text("TODAY'S BUDGET", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f))
                    Text(
                        text = "$${"%.2f".format(todayBudget)}",
                        style = MaterialTheme.typography.displayLarge, // Big Impact
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = "\"$voiceMessage\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }

    @Composable
    private fun FloatingStatCard(
        title: String,
        value: String,
        valueColor: Color,
        modifier: Modifier = Modifier,
        subtext: String? = null
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = KaironexColors.White,
            shadowElevation = 2.dp,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title, 
                    style = MaterialTheme.typography.labelMedium, 
                    color = KaironexColors.Slate500,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    value, 
                    style = MaterialTheme.typography.headlineLarge, // Slightly smaller than displayMedium
                    fontWeight = FontWeight.Bold, 
                    color = valueColor,
                    textAlign = TextAlign.Center
                )
                if (subtext != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        subtext,
                        style = MaterialTheme.typography.labelSmall,
                        color = KaironexColors.Slate500,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    @Composable
    private fun CompactActionCard(icon: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
        Card(
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = KaironexColors.White),
            shape = RoundedCornerShape(12.dp),
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = KaironexColors.Slate900,  maxLines = 1)
            }
        }
    }
    
    @Composable
    private fun LogExpenseDialog(
        onDismiss: () -> Unit,
        onConfirm: (Double, String) -> Unit
    ) {
        var amount by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Log Expense") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amt = amount.toDoubleOrNull()
                        if (amt != null && amt > 0) {
                            onConfirm(amt, description)
                        }
                    },
                    enabled = amount.isNotEmpty()
                ) {
                    Text("Log")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    private fun StatBadge(text: String, background: Color) {
        Surface(
            color = background,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }

    @Composable
    private fun MealOptionItem(label: String, name: String, icon: String, cost: Double? = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate500)
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = KaironexColors.Slate900)
            }
            if (cost != null) {
                Text(
                    "$${cost}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.Slate500
                )
            }
        }
    }
}
