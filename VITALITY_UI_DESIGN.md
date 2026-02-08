# 🧬 VITALITY AGENT v2.0 - ANDROID UI DESIGN GUIDE
## Survival & Growth Protocol - KMP Implementation

> **For**: Kaironex Android/KMP Study App  
> **Agent**: Vitality Agent v2.0 - Life Logistics Engine  
> **Last Updated**: February 2026

---

## 📱 SCREEN INVENTORY

The Vitality Agent requires these screens:

| Screen | Priority | Purpose |
|--------|----------|---------|
| Day Zero Setup | P0 | Financial onboarding |
| Daily Dashboard | P0 | Main budget & status view |
| Fridge Scan | P1 | Camera-based inventory |
| Meal Decision | P1 | Cook vs Order interface |
| Shopping Alert | P1 | Location-triggered notifications |
| Victory Feast | P2 | Celebration modal |
| History/Analytics | P3 | Spending trends |

---

## 🎨 DESIGN SYSTEM

### Color Palette (Defcon-Based)

```kotlin
// VitalityColors.kt
object VitalityColors {
    // Defcon Level Colors
    val defcon1Survival = Color(0xFFD32F2F)      // Deep Red - Critical
    val defcon2Critical = Color(0xFFF57C00)      // Orange - Warning
    val defcon3Caution = Color(0xFFFBC02D)       // Yellow - Caution
    val defcon4Stable = Color(0xFF388E3C)        // Green - Stable
    val defcon5Abundance = Color(0xFF1976D2)     // Blue - Abundance
    
    // Gradient backgrounds per defcon
    val defcon1Gradient = listOf(Color(0xFFD32F2F), Color(0xFF8B0000))
    val defcon2Gradient = listOf(Color(0xFFF57C00), Color(0xFFE65100))
    val defcon3Gradient = listOf(Color(0xFFFBC02D), Color(0xFFF9A825))
    val defcon4Gradient = listOf(Color(0xFF388E3C), Color(0xFF2E7D32))
    val defcon5Gradient = listOf(Color(0xFF1976D2), Color(0xFF1565C0))
    
    // Meal Decision Colors
    val cookGreen = Color(0xFF4CAF50)
    val orderOrange = Color(0xFFFF9800)
    val emergencyRed = Color(0xFFE53935)
    val convenienceBlue = Color(0xFF2196F3)
    
    // Victory Feast - Celebration Gold
    val victoryGold = Color(0xFFFFD700)
    val victoryGradient = listOf(Color(0xFFFFD700), Color(0xFFFFA000))
}
```

### Typography

```kotlin
// VitalityTypography.kt
object VitalityTypography {
    // Budget Amount - Large, Bold, Mono
    val budgetAmount = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        letterSpacing = (-2).sp
    )
    
    // Defcon Label
    val defconLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 14.sp,
        letterSpacing = 2.sp
    )
    
    // Voice Message (AI personality)
    val voiceMessage = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        fontStyle = FontStyle.Italic,
        lineHeight = 24.sp
    )
}
```

---

## 📲 SCREEN 1: DAY ZERO SETUP (One-Shot Financial Calibration)

### Purpose
First-time user financial onboarding. Collects all data needed to calculate Defcon level.

### Wireframe
```
┌─────────────────────────────────────┐
│  ← Back                    Skip →   │
├─────────────────────────────────────┤
│                                     │
│    🧬 WELCOME TO VITALITY           │
│                                     │
│    Let's calibrate your             │
│    survival systems                 │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  💰 Current Balance         │   │
│  │  $ [___________]            │   │
│  │  (checking + savings)       │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  📅 Monthly Fixed Bills     │   │
│  │  $ [___________]            │   │
│  │  (rent, utilities, subs)    │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  💼 Job Status (Auto-Synced)│   │
│  │  ✓ Has Job (from Profile)   │   │
│  │  ─────────────────────────  │   │
│  │  💸 Other Income Source     │   │
│  │  [Optional: family, etc]    │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  📆 Next Payday/Aid Date    │   │
│  │  [Select Date]              │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  🍕 Favorite Celebration    │   │
│  │  Food (for victories!)      │   │
│  │  [___________]              │   │
│  └─────────────────────────────┘   │
│                                     │
│    ┌───────────────────────────┐   │
│    │    CALIBRATE SYSTEMS      │   │
│    └───────────────────────────┘   │
│                                     │
└─────────────────────────────────────┘
```

> **NOTE:** Employment status is automatically synced from the user's Profile calibration and Campaign agent's Job section. Students have a "Has Job" checkbox in their profile. If no job, they can optionally specify other income sources (family support, scholarships, etc.).

```kotlin
// DayZeroSetupScreen.kt
@Composable
fun DayZeroSetupScreen(
    hasJobFromProfile: Boolean,  // Auto-synced from Profile/Campaign
    onComplete: (SetupData) -> Unit,
    onSkip: () -> Unit
) {
    var balance by remember { mutableStateOf("") }
    var monthlyBills by remember { mutableStateOf("") }
    var otherIncomeSource by remember { mutableStateOf("") }  // Optional
    var paydayDate by remember { mutableStateOf<LocalDate?>(null) }
    var favoriteFood by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { /* back */ }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onSkip) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
                        text = "WELCOME TO VITALITY",
                        style = VitalityTypography.defconLabel,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Let's calibrate your survival systems",
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
                    DatePickerButton(
                        selectedDate = paydayDate,
                        onDateSelected = { paydayDate = it }
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
                        val data = SetupData(
                            totalBalance = balance.toDoubleOrNull() ?: 0.0,
                            monthlyBills = monthlyBills.toDoubleOrNull() ?: 0.0,
                            hasJob = hasJobFromProfile,  // Auto-synced from Profile/Campaign
                            otherIncomeSource = otherIncomeSource.ifEmpty { null },  // Optional
                            paydayDate = paydayDate,
                            favoriteFood = favoriteFood.ifEmpty { "Pizza" }
                        )
                        onComplete(data)
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
                        Text("CALIBRATE SYSTEMS", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
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
```

---

## 📲 SCREEN 2: DAILY DASHBOARD (Main Hub)

### Purpose
The primary Vitality screen showing today's budget, Defcon status, and quick actions.

### Wireframe
```
┌─────────────────────────────────────┐
│  ☰                      Settings ⚙️ │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │  🟢 DEFCON 4: STABLE        │   │
│  │  ━━━━━━━━━━━━━━━━━━━━━━━━━  │   │
│  │                             │   │
│  │     TODAY'S BUDGET          │   │
│  │        $35.00               │   │
│  │                             │   │
│  │   ┌─────┐  ┌─────┐         │   │
│  │   │Spent│  │Left │         │   │
│  │   │$12  │  │$23  │         │   │
│  │   └─────┘  └─────┘         │   │
│  │                             │   │
│  │  "Looking good! You're     │   │
│  │   under budget today."     │   │
│  └─────────────────────────────┘   │
│                                     │
│  📊 QUICK STATS                     │
│  ┌───────────┐ ┌───────────┐       │
│  │ 🍳 5      │ │ 💰 17     │       │
│  │ meals     │ │ days to   │       │
│  │ in fridge │ │ payday    │       │
│  └───────────┘ └───────────┘       │
│                                     │
│  ⚡ QUICK ACTIONS                   │
│  ┌─────────────────────────────┐   │
│  │ 📷 Scan Fridge              │→  │
│  ├─────────────────────────────┤   │
│  │ 🍽️ What Should I Eat?       │→  │
│  ├─────────────────────────────┤   │
│  │ 💳 Log Expense              │→  │
│  ├─────────────────────────────┤   │
│  │ 🛒 Shopping Mode            │→  │
│  └─────────────────────────────┘   │
│                                     │
│  🎉 RECENT VICTORY                  │
│  ┌─────────────────────────────┐   │
│  │ 🎊 Interview Aced!          │   │
│  │ Pizza unlocked! $25 bonus   │   │
│  └─────────────────────────────┘   │
│                                     │
├─────────────────────────────────────┤
│  🏠   📚   🧬   📍   🎯             │
│ Home Study VITAL Radius Campaign   │
└─────────────────────────────────────┘
```

### Compose Implementation

```kotlin
// VitalityDashboardScreen.kt
@Composable
fun VitalityDashboardScreen(
    vitalityState: VitalityUiState,
    onScanFridge: () -> Unit,
    onMealDecision: () -> Unit,
    onLogExpense: () -> Unit,
    onShoppingMode: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val defconColor = remember(vitalityState.defconLevel) {
        when (vitalityState.defconLevel) {
            1 -> VitalityColors.defcon1Survival
            2 -> VitalityColors.defcon2Critical
            3 -> VitalityColors.defcon3Caution
            4 -> VitalityColors.defcon4Stable
            5 -> VitalityColors.defcon5Abundance
            else -> VitalityColors.defcon4Stable
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { /* drawer */ }) {
                        Icon(Icons.Default.Menu, "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Budget Card
            item {
                DefconBudgetCard(
                    defconLevel = vitalityState.defconLevel,
                    defconLabel = vitalityState.defconLabel,
                    todayBudget = vitalityState.todayBudget,
                    spentToday = vitalityState.spentToday,
                    leftToday = vitalityState.leftToday,
                    voiceMessage = vitalityState.voiceMessage,
                    defconColor = defconColor
                )
            }
            
            // Quick Stats Row
            item {
                SectionTitle("📊 QUICK STATS")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        icon = "🍳",
                        value = "${vitalityState.mealsInFridge}",
                        label = "meals\nin fridge"
                    )
                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        icon = "💰",
                        value = "${vitalityState.daysToPayday}",
                        label = "days to\npayday"
                    )
                }
            }
            
            // Quick Actions
            item {
                SectionTitle("⚡ QUICK ACTIONS")
                QuickActionsCard(
                    onScanFridge = onScanFridge,
                    onMealDecision = onMealDecision,
                    onLogExpense = onLogExpense,
                    onShoppingMode = onShoppingMode
                )
            }
            
            // Recent Victory (if any)
            vitalityState.recentVictory?.let { victory ->
                item {
                    SectionTitle("🎉 RECENT VICTORY")
                    VictoryBadgeCard(victory = victory)
                }
            }
        }
    }
}

@Composable
private fun DefconBudgetCard(
    defconLevel: Int,
    defconLabel: String,
    todayBudget: Double,
    spentToday: Double,
    leftToday: Double,
    voiceMessage: String,
    defconColor: Color
) {
    val gradient = when (defconLevel) {
        1 -> VitalityColors.defcon1Gradient
        2 -> VitalityColors.defcon2Gradient
        3 -> VitalityColors.defcon3Gradient
        4 -> VitalityColors.defcon4Gradient
        5 -> VitalityColors.defcon5Gradient
        else -> VitalityColors.defcon4Gradient
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(colors = gradient),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Defcon Badge
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = defconLabel,
                        style = VitalityTypography.defconLabel,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "TODAY'S BUDGET",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Text(
                    text = "$${String.format("%.2f", todayBudget)}",
                    style = VitalityTypography.budgetAmount,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Spent / Left Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            "$${String.format("%.0f", spentToday)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Divider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp),
                        color = Color.White.copy(alpha = 0.3f)
                    )
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Left",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            "$${String.format("%.0f", leftToday)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Voice Message
                Text(
                    text = "\"$voiceMessage\"",
                    style = VitalityTypography.voiceMessage,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun QuickStatCard(
    modifier: Modifier = Modifier,
    icon: String,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    onScanFridge: () -> Unit,
    onMealDecision: () -> Unit,
    onLogExpense: () -> Unit,
    onShoppingMode: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            QuickActionItem("📷", "Scan Fridge", onClick = onScanFridge)
            Divider()
            QuickActionItem("🍽️", "What Should I Eat?", onClick = onMealDecision)
            Divider()
            QuickActionItem("💳", "Log Expense", onClick = onLogExpense)
            Divider()
            QuickActionItem("🛒", "Shopping Mode", onClick = onShoppingMode)
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}
```

---

## 📲 SCREEN 3: FRIDGE SCAN (Smart Vision)

### Purpose
Camera interface for scanning fridge/pantry contents. Gemini Vision analyzes and catalogs ingredients.

### Wireframe
```
┌─────────────────────────────────────┐
│  ← Back        FRIDGE SCAN     📊   │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │                             │   │
│  │                             │   │
│  │     📷 CAMERA VIEWFINDER    │   │
│  │                             │   │
│  │     [Point at your fridge]  │   │
│  │                             │   │
│  │                             │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  💡 Tips:                   │   │
│  │  • Open fridge doors wide   │   │
│  │  • Good lighting helps      │   │
│  │  • Include all shelves      │   │
│  └─────────────────────────────┘   │
│                                     │
│       ┌─────────────────┐          │
│       │   📸 CAPTURE    │          │
│       └─────────────────┘          │
│                                     │
│  ─ OR ─                            │
│                                     │
│       ┌─────────────────┐          │
│       │ 📁 Upload Photo │          │
│       └─────────────────┘          │
│                                     │
└─────────────────────────────────────┘

          ↓ After Scan ↓

┌─────────────────────────────────────┐
│  ← Back      SCAN RESULTS      🔄   │
├─────────────────────────────────────┤
│                                     │
│  🎉 Found 12 Ingredients!           │
│                                     │
│  🥚 EGGS                  ███░░ 60% │
│     ~6 servings remaining           │
│                                     │
│  🥬 SPINACH               █████ 100%│
│     ~4 servings remaining           │
│                                     │
│  🍚 RICE                  █████ 100%│
│     ~10 servings remaining          │
│                                     │
│  🍗 CHICKEN               ██░░░ 40% │
│     ~2 servings remaining           │
│                                     │
│  🥛 MILK                  █░░░░ 20% │
│     ~1 serving remaining            │
│                                     │
│  ... [scroll for more]              │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  📊 ANALYSIS                │   │
│  │  Estimated meals: 5         │   │
│  │  Days until shopping: 3     │   │
│  │  Nutritional balance: Good  │   │
│  └─────────────────────────────┘   │
│                                     │
│    ┌───────────────────────────┐   │
│    │    SAVE INVENTORY         │   │
│    └───────────────────────────┘   │
│                                     │
└─────────────────────────────────────┘
```

### Compose Implementation

```kotlin
// FridgeScanScreen.kt
@Composable
fun FridgeScanScreen(
    viewModel: FridgeScanViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (val state = uiState) {
        is FridgeScanUiState.Camera -> CameraMode(
            onCapture = { bitmap -> viewModel.analyzeFridge(bitmap) },
            onUpload = { uri -> viewModel.analyzeFromUri(uri) },
            onBack = onNavigateBack
        )
        is FridgeScanUiState.Analyzing -> AnalyzingMode()
        is FridgeScanUiState.Results -> ResultsMode(
            results = state.inventory,
            onSave = { viewModel.saveInventory() },
            onRescan = { viewModel.resetToCamera() },
            onBack = onNavigateBack
        )
        is FridgeScanUiState.Error -> ErrorMode(
            message = state.message,
            onRetry = { viewModel.resetToCamera() }
        )
    }
}

@Composable
private fun CameraMode(
    onCapture: (Bitmap) -> Unit,
    onUpload: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val cameraController = remember { LifecycleCameraController(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onUpload(it) } }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FRIDGE SCAN", style = VitalityTypography.defconLabel) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Camera Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                controller = cameraController
                                cameraController.bindToLifecycle(lifecycleOwner)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Overlay guide
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "📷 Point at your fridge",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            
            // Tips Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 Tips:", fontWeight = FontWeight.Bold)
                    Text("• Open fridge doors wide")
                    Text("• Good lighting helps")
                    Text("• Include all shelves")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Capture Button
            Button(
                onClick = {
                    cameraController.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = image.toBitmap()
                                image.close()
                                onCapture(bitmap)
                            }
                        }
                    )
                },
                modifier = Modifier
                    .size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VitalityColors.defcon4Stable
                )
            ) {
                Text("📸", fontSize = 32.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("─ OR ─", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Upload Button
            OutlinedButton(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Photo")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ResultsMode(
    results: FridgeInventoryUi,
    onSave: () -> Unit,
    onRescan: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SCAN RESULTS") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRescan) {
                        Icon(Icons.Default.Refresh, "Rescan")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Success Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VitalityColors.defcon4Stable.copy(alpha = 0.1f))
                    .padding(16.dp)
            ) {
                Text(
                    "🎉 Found ${results.ingredients.size} Ingredients!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Ingredients List
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(results.ingredients) { ingredient ->
                    IngredientCard(ingredient = ingredient)
                }
                
                // Analysis Summary
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    AnalysisSummaryCard(
                        estimatedMeals = results.daysRemaining * 2,
                        daysUntilShopping = results.daysRemaining,
                        needsShopping = results.needsShopping
                    )
                }
            }
            
            // Save Button
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
            ) {
                Text("SAVE INVENTORY", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun IngredientCard(ingredient: IngredientUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(ingredient.emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    ingredient.name.uppercase(),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "~${ingredient.servings} servings remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Quantity Bar
            Column(horizontalAlignment = Alignment.End) {
                LinearProgressIndicator(
                    progress = ingredient.percentRemaining / 100f,
                    modifier = Modifier.width(80.dp),
                    color = when {
                        ingredient.percentRemaining > 60 -> VitalityColors.defcon4Stable
                        ingredient.percentRemaining > 30 -> VitalityColors.defcon3Caution
                        else -> VitalityColors.defcon1Survival
                    }
                )
                Text(
                    "${ingredient.percentRemaining}%",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
```

---

## 📲 SCREEN 4: MEAL DECISION (Cook vs Order Arbitrator)

### Purpose
Help user decide what to eat based on time, energy, budget, and fridge contents.

### Wireframe
```
┌─────────────────────────────────────┐
│  ← Back       MEAL DECISION         │
├─────────────────────────────────────┤
│                                     │
│    🤔 What should I eat?            │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  ⏰ HOW MUCH TIME?          │   │
│  │  ┌────┐ ┌────┐ ┌────┐      │   │
│  │  │15m │ │30m │ │60m+│      │   │
│  │  │min │ │med │ │lots│      │   │
│  │  └────┘ └────┘ └────┘      │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  🔋 HOW'S YOUR ENERGY?      │   │
│  │  😫───────●─────────😊      │   │
│  │      Low     Med     High   │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  📊 CURRENT CONTEXT         │   │
│  │  Budget: $28 left today     │   │
│  │  Defcon: 4 (Stable)         │   │
│  │  Fridge: 5 meals available  │   │
│  │  Schedule: exam_week        │   │
│  └─────────────────────────────┘   │
│                                     │
│    ┌───────────────────────────┐   │
│    │    🎯 DECIDE FOR ME       │   │
│    └───────────────────────────┘   │
│                                     │
└─────────────────────────────────────┘

          ↓ After Decision ↓

┌─────────────────────────────────────┐
│  ← Back       MEAL DECISION         │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │                             │   │
│  │   🍽️ ORDER FOOD            │   │
│  │                             │   │
│  │   Budget Allocated: $15     │   │
│  │                             │   │
│  └─────────────────────────────┘   │
│                                     │
│  📝 REASONING:                      │
│  "Energy levels low during exam    │
│  week. Rest and refuel is the      │
│  priority. Your brain needs fuel,  │
│  not cooking stress right now."    │
│                                     │
│  💡 SUGGESTIONS:                    │
│  ┌─────────────────────────────┐   │
│  │ 🥗 Healthy Bowl (~$12)      │   │
│  │ Good for focus, under budget│   │
│  ├─────────────────────────────┤   │
│  │ 🍜 Pho/Noodle Soup (~$10)   │   │
│  │ Comfort + hydration         │   │
│  ├─────────────────────────────┤   │
│  │ 🌯 Chipotle Burrito (~$14)  │   │
│  │ Protein + long lasting      │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌────────────┐ ┌────────────┐     │
│  │  📍 Find   │ │ 💳 Log     │     │
│  │  Nearby   │ │ Expense    │     │
│  └────────────┘ └────────────┘     │
│                                     │
└─────────────────────────────────────┘
```

### Compose Implementation

```kotlin
// MealDecisionScreen.kt
@Composable
fun MealDecisionScreen(
    viewModel: MealDecisionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onFindNearby: (MealType) -> Unit,
    onLogExpense: (Double) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MEAL DECISION") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is MealDecisionUiState.Input -> InputMode(
                state = state,
                onTimeSelect = { viewModel.setTimeAvailable(it) },
                onEnergyChange = { viewModel.setEnergyLevel(it) },
                onDecide = { viewModel.makeDecision() },
                modifier = Modifier.padding(padding)
            )
            is MealDecisionUiState.Loading -> LoadingMode(modifier = Modifier.padding(padding))
            is MealDecisionUiState.Result -> ResultMode(
                result = state,
                onFindNearby = onFindNearby,
                onLogExpense = onLogExpense,
                onReset = { viewModel.reset() },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun InputMode(
    state: MealDecisionUiState.Input,
    onTimeSelect: (Int) -> Unit,
    onEnergyChange: (Int) -> Unit,
    onDecide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "🤔 What should I eat?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        // Time Selection
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "⏰ HOW MUCH TIME?",
                    style = VitalityTypography.defconLabel,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimeButton(
                        label = "15m",
                        sublabel = "minimal",
                        selected = state.timeAvailable == 15,
                        onClick = { onTimeSelect(15) }
                    )
                    TimeButton(
                        label = "30m",
                        sublabel = "medium",
                        selected = state.timeAvailable == 30,
                        onClick = { onTimeSelect(30) }
                    )
                    TimeButton(
                        label = "60m+",
                        sublabel = "plenty",
                        selected = state.timeAvailable == 60,
                        onClick = { onTimeSelect(60) }
                    )
                }
            }
        }
        
        // Energy Slider
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "🔋 HOW'S YOUR ENERGY?",
                    style = VitalityTypography.defconLabel,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("😫")
                    Slider(
                        value = state.energyLevel / 100f,
                        onValueChange = { onEnergyChange((it * 100).toInt()) },
                        modifier = Modifier.weight(1f)
                    )
                    Text("😊")
                }
                Text(
                    when {
                        state.energyLevel < 30 -> "Low Energy"
                        state.energyLevel < 70 -> "Moderate"
                        else -> "High Energy"
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        // Current Context
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "📊 CURRENT CONTEXT",
                    style = VitalityTypography.defconLabel,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                ContextRow("Budget", "$${state.budgetRemaining} left today")
                ContextRow("Defcon", state.defconLabel)
                ContextRow("Fridge", "${state.mealsAvailable} meals available")
                ContextRow("Schedule", state.scheduleContext)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Decide Button
        Button(
            onClick = onDecide,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("🎯 DECIDE FOR ME", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResultMode(
    result: MealDecisionUiState.Result,
    onFindNearby: (MealType) -> Unit,
    onLogExpense: (Double) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val decisionColor = when (result.decision) {
        MealDecision.COOK -> VitalityColors.cookGreen
        MealDecision.ORDER -> VitalityColors.orderOrange
        MealDecision.EMERGENCY_FUEL -> VitalityColors.emergencyRed
        MealDecision.CONVENIENCE -> VitalityColors.convenienceBlue
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Decision Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = decisionColor)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    result.decisionEmoji,
                    fontSize = 48.sp
                )
                Text(
                    result.decisionLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Budget Allocated: $${String.format("%.2f", result.budgetAllocated)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Reasoning
        Text(
            "📝 REASONING:",
            style = VitalityTypography.defconLabel,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "\"${result.reasoning}\"",
            style = VitalityTypography.voiceMessage,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Suggestions (if ordering)
        if (result.decision in listOf(MealDecision.ORDER, MealDecision.CONVENIENCE)) {
            Text(
                "💡 SUGGESTIONS:",
                style = VitalityTypography.defconLabel,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(result.suggestions) { suggestion ->
                    SuggestionCard(
                        suggestion = suggestion,
                        onClick = { onFindNearby(suggestion.type) }
                    )
                }
            }
        } else {
            // Cooking suggestions
            Text(
                "🍳 RECIPE IDEAS:",
                style = VitalityTypography.defconLabel,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(result.recipes) { recipe ->
                    RecipeCard(recipe = recipe)
                }
            }
        }
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (result.decision != MealDecision.COOK) {
                OutlinedButton(
                    onClick = { onFindNearby(MealType.HEALTHY) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📍 Find Nearby")
                }
                Button(
                    onClick = { onLogExpense(result.budgetAllocated) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("💳 Log Expense")
                }
            } else {
                Button(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✅ Done Cooking")
                }
            }
        }
    }
}
```

---

## 📲 SCREEN 5: SHOPPING ALERT (Location-Triggered)

### Purpose
Push notification + bottom sheet when user is near grocery store and inventory is low.

### Notification Design
```
┌─────────────────────────────────────┐
│  🛒 KAIRONEX VITALITY              │
│                                     │
│  You're near Walmart!               │
│  Your fridge has 2 days left.       │
│                                     │
│  [View List]     [Dismiss]          │
└─────────────────────────────────────┘
```

### Bottom Sheet Wireframe
```
┌─────────────────────────────────────┐
│  ────────────────────────────       │
│                                     │
│  🛒 SHOPPING TIME                   │
│  Near: Walmart Grocery (0.2mi)      │
│                                     │
│  ⚠️ URGENCY: HIGH                   │
│  Your fridge has 2 days of food     │
│                                     │
│  📋 YOUR DEFCON 3 LIST:             │
│  ┌─────────────────────────────┐   │
│  │ □ Protein (chicken/fish)    │   │
│  │ □ Fresh Vegetables          │   │
│  │ □ Whole Grains              │   │
│  │ □ Eggs                      │   │
│  │ □ Fruit                     │   │
│  │ □ Dairy                     │   │
│  └─────────────────────────────┘   │
│                                     │
│  💰 Budget: $100 max                │
│  ⛔ Avoid: expensive brands         │
│                                     │
│  ┌────────────┐ ┌────────────┐     │
│  │  📍 Open   │ │ ✅ Start   │     │
│  │  Maps     │ │ Shopping   │     │
│  └────────────┘ └────────────┘     │
│                                     │
└─────────────────────────────────────┘
```

### Compose Implementation

```kotlin
// ShoppingAlertBottomSheet.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingAlertBottomSheet(
    alert: ShoppingAlertUi,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onOpenMaps: () -> Unit,
    onStartShopping: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛒", fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "SHOPPING TIME",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Near: ${alert.storeName} (${alert.distance})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Urgency Badge
            val urgencyColor = when (alert.urgency) {
                "critical" -> VitalityColors.defcon1Survival
                "high" -> VitalityColors.defcon2Critical
                else -> VitalityColors.defcon3Caution
            }
            
            Surface(
                color = urgencyColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when (alert.urgency) {
                            "critical" -> "🚨"
                            "high" -> "⚠️"
                            else -> "ℹ️"
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "URGENCY: ${alert.urgency.uppercase()}",
                        fontWeight = FontWeight.Bold,
                        color = urgencyColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        alert.message,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Shopping List
            Text(
                "📋 YOUR DEFCON ${alert.defconLevel} LIST:",
                style = VitalityTypography.defconLabel,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    alert.shoppingItems.forEach { item ->
                        ShoppingListItem(item = item)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Budget & Restrictions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "💰 Budget: $${alert.maxBudget} max",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (alert.forbidden.isNotEmpty()) {
                    Text(
                        "⛔ Avoid: ${alert.forbidden.first()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = VitalityColors.defcon1Survival
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenMaps,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Maps")
                }
                Button(
                    onClick = onStartShopping,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Shopping")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ShoppingListItem(item: ShoppingItemUi) {
    var checked by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { checked = !checked }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { checked = it }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (checked) TextDecoration.LineThrough else null
            )
        }
        // Priority indicator
        Surface(
            color = when (item.priority) {
                1 -> VitalityColors.defcon1Survival
                2 -> VitalityColors.defcon3Caution
                else -> Color.Gray
            }.copy(alpha = 0.2f),
            shape = CircleShape
        ) {
            Text(
                "P${item.priority}",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
```

---

## 📲 SCREEN 6: VICTORY FEAST (Celebration Modal)

### Purpose
Full-screen celebration when user achieves career/academic wins. Triggered by Campaign agent.

### Wireframe
```
┌─────────────────────────────────────┐
│                                     │
│          ✨✨✨✨✨✨✨             │
│                                     │
│              🎉                     │
│                                     │
│      VICTORY FEAST UNLOCKED!       │
│                                     │
│  ┌─────────────────────────────┐   │
│  │                             │   │
│  │   🎊 JOB OFFER RECEIVED!    │   │
│  │                             │   │
│  │   Got the UI Engineer       │   │
│  │   position at Google!       │   │
│  │                             │   │
│  └─────────────────────────────┘   │
│                                     │
│        🍕 PIZZA TIME! 🍕           │
│                                     │
│      Budget Bonus: +$50.00         │
│                                     │
│  "Congratulations! You've earned   │
│   this celebration. I've unlocked  │
│   a special treat budget for       │
│   your favorite food!"             │
│                                     │
│    ┌───────────────────────────┐   │
│    │   🍕 ORDER MY FAVORITE    │   │
│    └───────────────────────────┘   │
│                                     │
│    ┌───────────────────────────┐   │
│    │     Maybe Later           │   │
│    └───────────────────────────┘   │
│                                     │
│          ✨✨✨✨✨✨✨             │
│                                     │
└─────────────────────────────────────┘
```

### Compose Implementation

```kotlin
// VictoryFeastScreen.kt
@Composable
fun VictoryFeastScreen(
    victory: VictoryFeastUi,
    onOrderFood: () -> Unit,
    onDismiss: () -> Unit
) {
    // Confetti animation
    val confettiController = rememberConfettiController()
    
    LaunchedEffect(Unit) {
        confettiController.showConfetti()
        // Play celebration sound
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = VitalityColors.victoryGradient + Color.White
                )
            )
    ) {
        // Confetti overlay
        ConfettiAnimation(
            controller = confettiController,
            modifier = Modifier.fillMaxSize()
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Sparkles
            Text(
                "✨✨✨✨✨✨✨",
                fontSize = 24.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Trophy animation
            val scale by rememberInfiniteTransition().animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500),
                    repeatMode = RepeatMode.Reverse
                )
            )
            
            Text(
                "🎉",
                fontSize = 72.sp,
                modifier = Modifier.scale(scale)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "VICTORY FEAST UNLOCKED!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFF5D4037),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Event Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🎊 ${victory.eventType.displayName}!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        victory.eventDetails,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Favorite Food
            Text(
                "${victory.foodEmoji} ${victory.favoriteFood.uppercase()} TIME! ${victory.foodEmoji}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Budget Bonus
            Surface(
                color = VitalityColors.defcon5Abundance.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "Budget Bonus: +$${String.format("%.2f", victory.rewardAmount)}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VitalityColors.defcon5Abundance
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Voice Message
            Text(
                "\"${victory.voiceMessage}\"",
                style = VitalityTypography.voiceMessage,
                textAlign = TextAlign.Center,
                color = Color(0xFF5D4037).copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Action Buttons
            Button(
                onClick = onOrderFood,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5D4037)
                )
            ) {
                Text(
                    "${victory.foodEmoji} ORDER MY FAVORITE",
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(onClick = onDismiss) {
                Text(
                    "Maybe Later",
                    color = Color(0xFF5D4037).copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sparkles
            Text(
                "✨✨✨✨✨✨✨",
                fontSize = 24.sp
            )
        }
    }
}
```

---

## 🎯 VIEW MODELS & DATA CLASSES

### VitalityUiState

```kotlin
// VitalityUiState.kt
data class VitalityUiState(
    val defconLevel: Int = 4,
    val defconLabel: String = "DEFCON 4: STABLE",
    val todayBudget: Double = 0.0,
    val spentToday: Double = 0.0,
    val leftToday: Double = 0.0,
    val voiceMessage: String = "",
    val mealsInFridge: Int = 0,
    val daysToPayday: Int = 0,
    val recentVictory: VictoryFeastUi? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class MealDecisionUiState {
    data class Input(
        val timeAvailable: Int = 30,
        val energyLevel: Int = 50,
        val budgetRemaining: Double = 0.0,
        val defconLabel: String = "",
        val mealsAvailable: Int = 0,
        val scheduleContext: String = ""
    ) : MealDecisionUiState()
    
    object Loading : MealDecisionUiState()
    
    data class Result(
        val decision: MealDecision,
        val decisionLabel: String,
        val decisionEmoji: String,
        val budgetAllocated: Double,
        val reasoning: String,
        val suggestions: List<MealSuggestionUi>,
        val recipes: List<RecipeUi>
    ) : MealDecisionUiState()
}

enum class MealDecision {
    COOK, ORDER, EMERGENCY_FUEL, CONVENIENCE
}

data class VictoryFeastUi(
    val eventType: VictoryEventType,
    val eventDetails: String,
    val favoriteFood: String,
    val foodEmoji: String,
    val rewardAmount: Double,
    val voiceMessage: String
)

enum class VictoryEventType(val displayName: String) {
    JOB_OFFER("Job Offer Received"),
    INTERVIEW_ACED("Interview Aced"),
    EXAM_PASSED("Exam Passed"),
    PROJECT_COMPLETED("Project Completed"),
    CERTIFICATION_EARNED("Certification Earned")
}
```

---

## 📡 API INTEGRATION

### VitalityRepository

```kotlin
// VitalityRepository.kt
class VitalityRepository @Inject constructor(
    private val appwriteClient: AppwriteFunctions
) {
    suspend fun getFinancialSetup(userId: String): Result<FinancialSetup> {
        return appwriteClient.execute(
            functionId = "vitality-brain-v2",
            data = mapOf(
                "intent" to "one_shot_setup",
                "user_id" to userId
            )
        )
    }
    
    suspend fun analyzeFridge(userId: String, imageBase64: String): Result<FridgeInventory> {
        return appwriteClient.execute(
            functionId = "vitality-brain-v2",
            data = mapOf(
                "intent" to "fridge_scan",
                "user_id" to userId,
                "image_base64" to imageBase64
            )
        )
    }
    
    suspend fun makeMealDecision(
        userId: String,
        timeAvailable: Int,
        energyLevel: Int
    ): Result<MealDecision> {
        return appwriteClient.execute(
            functionId = "vitality-brain-v2",
            data = mapOf(
                "intent" to "decision_matrix",
                "user_id" to userId,
                "time_available_minutes" to timeAvailable,
                "energy_level" to energyLevel
            )
        )
    }
    
    suspend fun getDailyBudget(userId: String): Result<DailyBudget> {
        return appwriteClient.execute(
            functionId = "vitality-brain-v2",
            data = mapOf(
                "intent" to "daily_budget_check",
                "user_id" to userId
            )
        )
    }
    
    suspend fun unlockVictoryFeast(
        userId: String,
        eventType: String,
        eventDetails: String
    ): Result<VictoryFeast> {
        return appwriteClient.execute(
            functionId = "vitality-brain-v2",
            data = mapOf(
                "intent" to "unlock_reward",
                "user_id" to userId,
                "event_type" to eventType,
                "event_details" to eventDetails
            )
        )
    }
    
    suspend fun getShoppingList(userId: String): Result<ShoppingList> {
        return appwriteClient.execute(
            functionId = "vitality-brain-v2",
            data = mapOf(
                "intent" to "shopping_alert",
                "user_id" to userId
            )
        )
    }
}
```

---

## 🏗️ NAVIGATION SETUP

```kotlin
// VitalityNavGraph.kt
sealed class VitalityDestination(val route: String) {
    object Dashboard : VitalityDestination("vitality/dashboard")
    object DayZeroSetup : VitalityDestination("vitality/setup")
    object FridgeScan : VitalityDestination("vitality/fridge")
    object MealDecision : VitalityDestination("vitality/meal")
    object VictoryFeast : VitalityDestination("vitality/victory/{eventType}/{details}") {
        fun createRoute(eventType: String, details: String) = 
            "vitality/victory/$eventType/${URLEncoder.encode(details, "UTF-8")}"
    }
}

@Composable
fun VitalityNavGraph(
    navController: NavHostController,
    startDestination: String = VitalityDestination.Dashboard.route
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(VitalityDestination.Dashboard.route) {
            VitalityDashboardScreen(
                onScanFridge = { navController.navigate(VitalityDestination.FridgeScan.route) },
                onMealDecision = { navController.navigate(VitalityDestination.MealDecision.route) },
                // ...
            )
        }
        
        composable(VitalityDestination.DayZeroSetup.route) {
            DayZeroSetupScreen(
                onComplete = { 
                    // Save setup, navigate to dashboard
                    navController.navigate(VitalityDestination.Dashboard.route) {
                        popUpTo(VitalityDestination.DayZeroSetup.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(VitalityDestination.FridgeScan.route) {
            FridgeScanScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        composable(VitalityDestination.MealDecision.route) {
            MealDecisionScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        composable(
            route = VitalityDestination.VictoryFeast.route,
            arguments = listOf(
                navArgument("eventType") { type = NavType.StringType },
                navArgument("details") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Decode and show victory
        }
    }
}
```

---

## 📱 KMP SHARED CONSIDERATIONS

If using Kotlin Multiplatform, shared code should include:

```kotlin
// shared/src/commonMain/kotlin/vitality/
// ├── domain/
// │   ├── models/
// │   │   ├── DefconLevel.kt
// │   │   ├── FinancialState.kt
// │   │   ├── FridgeInventory.kt
// │   │   └── MealDecision.kt
// │   └── usecases/
// │       ├── CalculateDailyBudgetUseCase.kt
// │       └── MakeMealDecisionUseCase.kt
// └── data/
//     └── VitalityRepositoryImpl.kt (expect/actual)
```

Platform-specific:
- **Android**: Camera integration (CameraX), notifications, location services
- **iOS**: Camera (AVFoundation), local notifications, CoreLocation

---

## 🎨 ANIMATION SPECS

```kotlin
// VitalityAnimations.kt
object VitalityAnimations {
    // Budget number counting up
    val budgetCountSpec = tween<Float>(
        durationMillis = 800,
        easing = FastOutSlowInEasing
    )
    
    // Defcon level transition
    val defconTransition = spring<Color>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    // Card entrance
    val cardEnter = slideInVertically(
        initialOffsetY = { it / 2 },
        animationSpec = tween(300)
    ) + fadeIn(tween(300))
    
    // Victory confetti duration
    const val CONFETTI_DURATION_MS = 3000L
}
```

---

## 📝 IMPLEMENTATION CHECKLIST

```
Phase 1: Core Screens
□ Day Zero Setup Screen
□ Daily Dashboard with Defcon card
□ Basic navigation

Phase 2: Vision Features
□ Camera permission handling
□ Fridge Scan camera UI
□ Image upload fallback
□ Results display

Phase 3: Decision Engine
□ Meal Decision input form
□ Result display with suggestions
□ Integration with maps/delivery apps

Phase 4: Notifications
□ Shopping Alert notification service
□ Bottom sheet implementation
□ Location trigger integration

Phase 5: Celebrations
□ Victory Feast full-screen modal
□ Confetti animation
□ Cross-agent trigger from Campaign

Phase 6: Polish
□ Smooth animations
□ Dark mode support
□ Accessibility (TalkBack)
□ Error states and offline mode
```

---

## 🏆 HACKATHON WINNING FEATURES

To stand out in the Gemini 3 Hackathon:

1. **Real-Time Vision** - Fridge scan with instant AI analysis
2. **Proactive Intelligence** - Shopping alerts before user asks
3. **Cross-Agent Harmony** - Campaign victories → Vitality rewards
4. **Emotional Design** - Celebrations that feel rewarding
5. **Financial Empathy** - Defcon system that adapts to struggle

**Demo Flow for Judges:**
1. Show Day Zero setup (30 sec)
2. Scan fridge with live Gemini Vision (20 sec)
3. "What should I eat?" decision (15 sec)
4. Walk near grocery → instant alert (20 sec)
5. Simulate job offer → Victory Feast! (15 sec)

Total demo: **~2 minutes** showcasing full Survival & Growth Protocol.

---

> **Built for Kaironex - The Student OS**  
> **Vitality Agent v2.0: Survival & Growth Protocol**  
> **Powered by Gemini 3 Flash Preview + Thinking**
