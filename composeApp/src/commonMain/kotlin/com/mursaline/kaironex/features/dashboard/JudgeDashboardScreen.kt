package com.mursaline.kaironex.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mursaline.kaironex.agents.genesis.StudentProfile
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * The "Judge View" - A high-density dashboard simulating the Agent's internal model of the user.
 * Visualizes the "Pressure Map" created during the Genesis Interview.
 */
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data class JudgeDashboardScreen(
    val profile: StudentProfile
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        JudgeDashboardContent(
            profile = profile,
            onBack = { navigator.pop() }
        )
    }
}

@Composable
fun JudgeDashboardContent(
    profile: StudentProfile,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KaironexColors.Slate900)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // ... (Rest of content same as before)
        // Copying the previous implementation of JudgeDashboardScreen logic here
        // --- HEADER ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "JUDGE VIEW // AGENT SPACE",
                    style = MaterialTheme.typography.labelSmall,
                    color = KaironexColors.GeminiBlurple,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    "Pressure Map Utilization",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // --- GRID LAYOUT (Simple Column for now) ---
        
        // 1. IDENTITY & GOALS
        Card(
            colors = CardDefaults.cardColors(containerColor = KaironexColors.Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("SUBJECT: ${profile.name.uppercase()}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("AMBITION: ${profile.careerAmbition ?: "UNKNOWN"}", color = KaironexColors.GeminiBlurple)
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = KaironexColors.Slate700)
                Spacer(modifier = Modifier.height(8.dp))
                Text("University: ${profile.university}", color = KaironexColors.Slate300)
                Text("Major: ${profile.major} (Sem: ${profile.semester})", color = KaironexColors.Slate300)
                Text("Target CGPA: ${profile.targetCgpa}", color = KaironexColors.Slate300)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. PRESSURE VECTORS (The "UniFlow" Logic)
        Text("Calculated Pressure Vectors", color = KaironexColors.Slate500, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // FINANCIAL STAKES
            PressureCard(
                title = "FINANCIAL STAKES",
                value = profile.financialStakes ?: "NONE",
                riskLevel = when(profile.financialStakes?.lowercase()) {
                    "visa", "scholarship" -> "HIGH CRITICAL"
                    "self-funded" -> "MODERATE"
                    else -> "LOW"
                },
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AttachMoney
            )
            
            // WORK LOAD
            PressureCard(
                title = "WORK LOAD",
                value = if (profile.hasJob) "${profile.workHoursPerWeek ?: 0.0} hrs/wk" else "FULL-TIME STUDENT",
                riskLevel = if ((profile.workHoursPerWeek ?: 0.0) > 20.0) "HIGH BURN" else "SUSTAINABLE",
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Work
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // BIO-RHYTHM
            PressureCard(
                title = "BIOLOGICAL FUEL",
                value = "${profile.sleepTime} - ${profile.wakeTime}",
                riskLevel = "MONITORING",
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Hotel
            )

            // STRESS RESPONSE
            PressureCard(
                title = "STRESS RESPONSE",
                value = profile.stressResponse ?: "UNKNOWN",
                riskLevel = if (profile.stressResponse?.lowercase() == "freeze") "INTERVENTION REQUIRED" else "ADAPTIVE",
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Psychology
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- PREDICTIVE INTERVENTION LOG ---
        Text("Orchestrator Logic Trace", color = KaironexColors.Slate500, style = MaterialTheme.typography.labelMedium)
        Card(
             colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
             modifier = Modifier.fillMaxWidth().height(200.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("> INGESTING PRESSURE MAP...", color = KaironexColors.SuccessGreen, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                if (profile.financialStakes?.lowercase() == "visa") {
                    Text("> DETECTED HIGH VISA STAKES.", color = KaironexColors.SuccessGreen, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                    Text("> CONFIGURING 'MARATHON AGENT' PRIORITY: CRITICAL", color = KaironexColors.GeminiBlurple, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                }
                if (profile.stressResponse?.lowercase() == "freeze") {
                   Text("> STRESS RESPONSE: FREEZE -> ACTIVATING MICRO-STEPS", color = Color(0xFFF59E0B), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                }
                Text("> SETTING THOUGHT SIGNATURE DEPTH: LEVEL 3", color = Color.Yellow, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun PressureCard(
    title: String,
    value: String,
    riskLevel: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = KaironexColors.Slate800),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = KaironexColors.Slate500)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate500)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = when {
                    riskLevel.contains("HIGH") || riskLevel.contains("CRITICAL") || riskLevel.contains("REQUIRED") -> Color(0xFF7F1D1D)
                    riskLevel.contains("MODERATE") || riskLevel.contains("MONITORING") -> Color(0xFF78350F)
                    else -> Color(0xFF14532D)
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    riskLevel, 
                    color = Color.White, 
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
