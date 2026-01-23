package com.mursaline.kaironex

import androidx.compose.animation.animateColorAsState
//import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// Voyager & Feature Imports
import cafe.adriel.voyager.navigator.Navigator
import com.mursaline.kaironex.features.auth.LoginScreen
import com.mursaline.kaironex.features.dashboard.DashboardScreen
import com.mursaline.kaironex.ui.theme.KaironexColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    sensorStream: Flow<String> = emptyFlow(),
    isAccessibilityEnabled: Boolean = true,
    onOpenSettings: () -> Unit = {},
    onLockTriggered: (Boolean) -> Unit = {}
) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            background = KaironexColors.CloudGray,
            surface = KaironexColors.CanvasWhite,
            primary = KaironexColors.ElectricBlue,
            onBackground = KaironexColors.InkBlack,
            onSurface = KaironexColors.InkBlack
        )
    ) {
        // Entry Point: Login
        Navigator(LoginScreen)
    }
}

// --- LEGACY COMPONENTS (Retained for reference) ---

data class PressureMap(
    val cognitiveLoad: Float,
    val stakes: String,
    val activeTask: String,
    val upcomingDeadlines: Int
) {
    fun getStatusColor(): Color {
        return when {
            cognitiveLoad > 0.8f -> Color(0xFFE53935) // Red
            cognitiveLoad > 0.5f -> Color(0xFFFFB300) // Orange
            else -> Color(0xFF43A047)               // Green
        }
    }
}

@Composable
fun PermissionScreen(onOpenSettings: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("⚙️", fontSize = 60.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Setup Required", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("To detect distractions like Netflix, Kaironex needs Accessibility permissions.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onOpenSettings, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))) {
                Text("Enable in Settings", color = Color.Black)
            }
        }
    }
}

@Composable
fun LockScreen(onUnlockRequest: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFB71C1C)).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚠️", fontSize = 80.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Text("DISTRACTION DETECTED", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Kaironex has locked this device to protect your focus.", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onUnlockRequest, colors = ButtonDefaults.buttonColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(text = "NEGOTIATE UNLOCK", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PressureGauge(pressure: PressureMap) {
    val animatedColor by animateColorAsState(pressure.getStatusColor())
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(150.dp).background(color = animatedColor.copy(alpha = 0.2f), shape = CircleShape).border(4.dp, animatedColor, CircleShape), contentAlignment = Alignment.Center) {
            Text(text = "${(pressure.cognitiveLoad * 100).toInt()}%", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "SYSTEM LOAD", style = MaterialTheme.typography.labelMedium, color = Color.Gray, letterSpacing = 2.sp)
    }
}

@Composable
fun ContextCard(title: String, value: String, icon: String, isWarning: Boolean = false) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (isWarning) Color(0xFF3E2723) else Color(0xFF2C2C2C))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Text(text = value, style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
    }
}