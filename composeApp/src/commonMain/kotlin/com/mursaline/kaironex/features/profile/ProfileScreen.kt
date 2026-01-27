package com.mursaline.kaironex.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.ui.components.KxCard
import com.mursaline.kaironex.ui.components.KxCardVariant

/**
 * Profile Screen - Dedicated Settings & Account Page
 *
 * This replaces the floating profile dropdown with a full navigation destination.
 * Contains: Account info, App settings, Study statistics, System configuration.
 */
@Suppress("unused")
object ProfileScreen : Screen {
    @Suppress("unused")
    private fun readResolve(): Any = ProfileScreen

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KaironexColors.CloudGray)
        ) {
            // Top App Bar
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KaironexColors.CanvasWhite
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Profile Header Card
                KxCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = KxCardVariant.Elevated
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Surface(
                            shape = CircleShape,
                            color = KaironexColors.GeminiBlurple,
                            modifier = Modifier.size(100.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "S",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            "Student User",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = KaironexColors.InkBlack
                        )

                        Text(
                            "student@university.edu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KaironexColors.SlateGray
                        )

                        Spacer(Modifier.height(16.dp))

                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStatItem("5", "Day Streak", "🔥")
                            ProfileStatItem("78%", "Focus Score", "🎯")
                            ProfileStatItem("42h", "Study Time", "📚")
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Account Section
                ProfileSection("Account") {
                    SettingsRow(
                        icon = Icons.Filled.Email,
                        label = "Email",
                        value = "student@university.edu",
                        onClick = {}
                    )
                    HorizontalDivider(color = KaironexColors.BorderGray)
                    SettingsRow(
                        icon = Icons.Filled.Lock,
                        label = "Password",
                        value = "••••••••",
                        onClick = {}
                    )
                    HorizontalDivider(color = KaironexColors.BorderGray)
                    SettingsRow(
                        icon = Icons.Filled.Tune, // Changed icon to Tune
                        label = "Profile Calibration",
                        value = "Review Data",
                        onClick = { navigator.push(com.mursaline.kaironex.features.dashboard.ProfileCalibrationScreen()) }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Preferences Section
                ProfileSection("Preferences") {
                    var darkMode by remember { mutableStateOf(false) }
                    var notifications by remember { mutableStateOf(true) }

                    SettingsToggleRow(
                        icon = Icons.Filled.DarkMode,
                        label = "Dark Mode",
                        checked = darkMode,
                        onCheckedChange = { darkMode = it }
                    )
                    HorizontalDivider(color = KaironexColors.BorderGray)
                    SettingsToggleRow(
                        icon = Icons.Filled.Notifications,
                        label = "Notifications",
                        checked = notifications,
                        onCheckedChange = { notifications = it }
                    )
                    HorizontalDivider(color = KaironexColors.BorderGray)
                    SettingsRow(
                        icon = Icons.Filled.Language,
                        label = "Language",
                        value = "English",
                        onClick = {}
                    )
                }

                Spacer(Modifier.height(16.dp))

                // System Configuration Section
                ProfileSection("System Configuration") {
                    SettingsRow(
                        icon = Icons.Filled.Storage,
                        label = "Data & Storage",
                        value = "2.4 GB used",
                        onClick = {}
                    )
                    HorizontalDivider(color = KaironexColors.BorderGray)
                    SettingsRow(
                        icon = Icons.Filled.Security,
                        label = "Privacy & Security",
                        value = "",
                        onClick = {}
                    )
                    HorizontalDivider(color = KaironexColors.BorderGray)
                    SettingsRow(
                        icon = Icons.Filled.Backup,
                        label = "Backup & Sync",
                        value = "Last: Today",
                        onClick = {}
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Support Section
                ProfileSection("Support") {
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.Help,
                        label = "Help Center",
                        value = "",
                        onClick = {}
                    )
                    HorizontalDivider(color = KaironexColors.BorderGray)
                    SettingsRow(
                        icon = Icons.Filled.Feedback,
                        label = "Send Feedback",
                        value = "",
                        onClick = {}
                    )
                    HorizontalDivider(color = KaironexColors.BorderGray)
                    SettingsRow(
                        icon = Icons.Filled.Info,
                        label = "About Kaironex",
                        value = "v1.0.0",
                        onClick = {}
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Sign Out Button
                Button(
                    onClick = { /* TODO: Sign out */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KaironexColors.AlertRed.copy(alpha = 0.1f),
                        contentColor = KaironexColors.AlertRed
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, "Sign Out")
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ProfileStatItem(value: String, label: String, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = KaironexColors.InkBlack
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = KaironexColors.SlateGray
        )
    }
}

@Composable
fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = KaironexColors.SlateGray,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    KxCard(
        modifier = Modifier.fillMaxWidth(),
        variant = KxCardVariant.Flat
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = KaironexColors.SlateGray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = KaironexColors.InkBlack,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = KaironexColors.SlateGray
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Navigate",
            tint = KaironexColors.SlateGray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = KaironexColors.SlateGray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = KaironexColors.InkBlack,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = KaironexColors.ElectricBlue,
                checkedTrackColor = KaironexColors.ElectricBlue.copy(alpha = 0.3f)
            )
        )
    }
}
