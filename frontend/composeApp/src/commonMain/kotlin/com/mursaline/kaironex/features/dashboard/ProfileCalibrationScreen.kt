package com.mursaline.kaironex.features.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.koin.koinScreenModel
import com.mursaline.kaironex.agents.genesis.StudentProfile
import com.mursaline.kaironex.core.storage.ProfileStorage
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class ProfileCalibrationScreen(
    val isOnboarding: Boolean = false,
    val userName: String = "",
    val addressAs: String = ""
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<ProfileCalibrationViewModel>()
        
        val profile by viewModel.profile.collectAsState()
        val isSaved by viewModel.isSaved.collectAsState()
        val validationErrors by viewModel.validationErrors.collectAsState()

        // Auto-navigate if saved during onboarding
        LaunchedEffect(isSaved) {
            if (isSaved && isOnboarding) {
                 navigator.push(com.mursaline.kaironex.features.onboarding.DataIngestionScreen())
            }
        }
        
        // REFRESH DATA ON RETURN FROM INTERVIEW
        // When we pop back to this screen, navigator.items changes. We force a reload.
        LaunchedEffect(navigator.items) {
             viewModel.refreshProfile()
        }
        
        // Initialize name if provided and profile is empty/loading
        LaunchedEffect(profile) {
            if (profile != null && profile?.name.isNullOrBlank() && userName.isNotBlank()) {
                viewModel.updateProfile(profile!!.copy(name = userName))
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (isOnboarding) "Confirm Profile" else "Profile Calibration", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (!isOnboarding) {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = KaironexColors.CanvasWhite)
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.saveProfile() },
                    containerColor = KaironexColors.GeminiBlurple,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Save, "Save")
                    Spacer(Modifier.width(8.dp))
                    Text(if (isOnboarding) "Continue" else if (isSaved) "Saved!" else "Sync & Save")
                }
            }
        ) { padding ->
            if (profile == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                CalibrationForm(
                    profile = profile!!,
                    validationErrors = validationErrors,
                    onUpdate = { viewModel.updateProfile(it) },
                    onStartAI = {
                        navigator.push(com.mursaline.kaironex.features.genesis.GenesisScreen(
                            userName = userName.ifBlank { profile!!.name },
                            addressAs = addressAs
                        ))
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
fun CalibrationForm(
    profile: StudentProfile,
    validationErrors: Set<String>,
    onUpdate: (StudentProfile) -> Unit,
    onStartAI: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KaironexColors.CloudGray)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- AI OPTION (Merged from Choice Screen) ---
        Card(
            onClick = onStartAI,
             colors = CardDefaults.cardColors(containerColor = KaironexColors.Indigo600),
             elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
             shape = RoundedCornerShape(16.dp),
             modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                 modifier = Modifier.padding(20.dp).fillMaxWidth(),
                 verticalAlignment = Alignment.CenterVertically
            ) {
                 Icon(Icons.Filled.Add, "AI", tint = Color.White, modifier = Modifier.size(32.dp))
                 Spacer(Modifier.width(16.dp))
                 Column(modifier = Modifier.weight(1f)) {
                     Text("Prefer Talking?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                     Text("Use the AI Interview to auto-fill this form.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                 }
                 Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go", tint = Color.White, modifier = Modifier.graphicsLayer { rotationZ = 180f })
            }
        }
        
        // Show Global Error if any
        if (validationErrors.isNotEmpty()) {
            Text(
                "Please fill in all required fields (*)",
                color = KaironexColors.AlertRed,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // --- IDENTITY ---
        FormSection("Identity") {
            KxTextField(
                label = "Full Name (as per Student ID)",
                value = profile.name,
                onValueChange = { onUpdate(profile.copy(name = it)) }
            )
        }
        
        // --- ACADEMIC LIFE ---
        FormSection("Academic Life") {
            KxTextField(
                label = "University / College Name",
                value = profile.university ?: "",
                onValueChange = { onUpdate(profile.copy(university = it)) },
                required = true,
                isError = validationErrors.contains("university")
            )
            KxTextField(
                label = "Major / Field of Study",
                value = profile.major ?: "",
                onValueChange = { onUpdate(profile.copy(major = it)) },
                required = true,
                isError = validationErrors.contains("major")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                 KxTextField(
                    label = "Current CGPA (e.g. 3.2)",
                    value = profile.currentCgpa ?: "",
                    onValueChange = { onUpdate(profile.copy(currentCgpa = it)) },
                    modifier = Modifier.weight(1f),
                    required = true,
                    isError = validationErrors.contains("currentCgpa")
                )
                 KxTextField(
                    label = "Target Goal CGPA (e.g. 3.8)",
                    value = profile.targetCgpa ?: "",
                    onValueChange = { onUpdate(profile.copy(targetCgpa = it)) },
                    modifier = Modifier.weight(1f),
                    required = true,
                    isError = validationErrors.contains("targetCgpa")
                )
            }
            KxTextField(
                label = "Why this Goal? (e.g. Scholarship requirement)",
                value = profile.desiredCgpaReason ?: "",
                onValueChange = { onUpdate(profile.copy(desiredCgpaReason = it)) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                 KxTextField(
                    label = "Current Semester (e.g. 4th)",
                    value = profile.semester ?: "",
                    onValueChange = { onUpdate(profile.copy(semester = it)) },
                    modifier = Modifier.weight(1f),
                    required = true,
                    isError = validationErrors.contains("semester")
                )
                 KxTextField(
                    label = "Total Semesters (e.g. 8)",
                    value = profile.totalSemesters ?: "",
                    onValueChange = { onUpdate(profile.copy(totalSemesters = it)) },
                    modifier = Modifier.weight(1f),
                    required = true,
                    isError = validationErrors.contains("totalSemesters")
                )
            }
            
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = KaironexColors.BorderGray)
            Spacer(Modifier.height(8.dp))
            
            // INTERNATIONAL STUDENT CONTEXT
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = profile.isInternationalStudent == true,
                    onCheckedChange = { onUpdate(profile.copy(isInternationalStudent = it)) }
                )
                Text("International Student")
            }
            
            if (profile.isInternationalStudent == true) {
                KxTextField(
                    label = "Home Country (e.g. Germany)",
                    value = profile.homeCountry ?: "",
                    onValueChange = { onUpdate(profile.copy(homeCountry = it)) }
                )
                KxTextField(
                    label = "Current Residence (e.g. UK, USA)",
                    value = profile.currentCountry ?: "",
                    onValueChange = { onUpdate(profile.copy(currentCountry = it)) }
                )

                 Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KxTextField(
                        label = "Visa Status (e.g. F1, Student)",
                        value = profile.visaStatus ?: "",
                        onValueChange = { onUpdate(profile.copy(visaStatus = it)) },
                         modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // --- JOB & WORK ---
        FormSection("Job & Work") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = profile.hasJob == true,
                    onCheckedChange = { onUpdate(profile.copy(hasJob = it)) }
                )
                Text("I have a Part-Time Job")
            }
            
            if (profile.hasJob == true) {
                KxTextField(
                    label = "Job Role & Company",
                    value = profile.jobDescription ?: "",
                    onValueChange = { onUpdate(profile.copy(jobDescription = it)) },
                    required = true,
                    isError = validationErrors.contains("jobDescription")
                )
                KxTextField(
                    label = "Work Schedule (e.g. Mon/Wed 2-6pm)",
                    value = profile.jobSchedule ?: "",
                    onValueChange = { onUpdate(profile.copy(jobSchedule = it)) },
                    required = true,
                    isError = validationErrors.contains("jobSchedule")
                )
            }
        }

        // --- REAL WORLD LOGISTICS ---
        FormSection("Logistics & Constraints") {
             KxTextField(
                label = "Commute Info (e.g. 45m bus to campus)",
                value = profile.commuteTime ?: "",
                onValueChange = { onUpdate(profile.copy(commuteTime = it)) }
            )
             
            Spacer(Modifier.height(16.dp))

            KxTextField(
                label = "Non-Negotiables (e.g. Prayer, Gym, Family)",
                value = profile.nonNegotiables ?: "",
                onValueChange = { onUpdate(profile.copy(nonNegotiables = it)) },
                modifier = Modifier.height(100.dp) // Little taller for details
            )
        }
        
        // --- STUDY STRATEGY ---
        FormSection("Study Strategy") {
             KxTextField(
                 label = "Best Way You Learn (e.g. Video, Reading)",
                 value = profile.learningStyle ?: "",
                 onValueChange = { onUpdate(profile.copy(learningStyle = it)) },
                 required = true,
                 isError = validationErrors.contains("learningStyle")
             )
             
             KxTextField(
                 label = "Productivity Killer (e.g. Social Media, Fatigue)",
                 value = profile.productivityKiller ?: "",
                 onValueChange = { onUpdate(profile.copy(productivityKiller = it)) },
                 required = true,
                 isError = validationErrors.contains("productivityKiller")
             )

            KxTextField(
                label = "Preferred Resources (e.g. Youtube, Textbooks)",
                value = profile.preferredResources ?: "",
                onValueChange = { onUpdate(profile.copy(preferredResources = it)) },
                required = true,
                isError = validationErrors.contains("preferredResources")
            )
            
            Spacer(Modifier.height(16.dp))
            
            KxTextField(
                label = "Daily Focus Capacity (e.g. 4 hours)",
                value = profile.dailyFocusCapacity ?: "",
                onValueChange = { onUpdate(profile.copy(dailyFocusCapacity = it)) },
                required = true,
                isError = validationErrors.contains("dailyFocusCapacity")
            )
            
            Spacer(Modifier.height(16.dp))
            Text("Energy Preference", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = profile.energyPreference == "MORNING",
                    onClick = { onUpdate(profile.copy(energyPreference = "MORNING")) },
                    label = { Text("Morning Person ☀️") }
                )
                FilterChip(
                    selected = profile.energyPreference == "NIGHT",
                    onClick = { onUpdate(profile.copy(energyPreference = "NIGHT")) },
                    label = { Text("Night Owl 🦉") }
                )
            }
        }
        
        Spacer(Modifier.height(80.dp)) // Fab Space
    }
}

@Composable
fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = KaironexColors.SlateGray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = KaironexColors.CanvasWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, KaironexColors.BorderGray)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}



@Composable
fun KxTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    required: Boolean = false,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label)
                if (required) {
                    Text(" *", color = Color(0xFFFF0000), fontWeight = FontWeight.Bold) // Bright Red and Bold
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        isError = isError,
        singleLine = false,
        minLines = if (value.length > 50) 3 else 1,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = KaironexColors.GeminiBlurple,
            focusedLabelColor = KaironexColors.GeminiBlurple,
            errorBorderColor = KaironexColors.AlertRed,
            errorLabelColor = KaironexColors.AlertRed
        )
    )
}

class ProfileCalibrationViewModel(
    private val profileStorage: ProfileStorage,
    private val sessionManager: com.mursaline.kaironex.core.KaironexSessionManager
) : ScreenModel {

    private val _profile = kotlinx.coroutines.flow.MutableStateFlow<StudentProfile?>(null)
    val profile = _profile.asStateFlow()
    
    private val _isSaved = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

    private val _validationErrors = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())
    val validationErrors = _validationErrors.asStateFlow()

    init {
        refreshProfile()
    }

    fun refreshProfile() {
        screenModelScope.launch {
            _profile.value = profileStorage.loadProfile() ?: StudentProfile()
        }
    }
    
    fun updateProfile(newProfile: StudentProfile) {
        _profile.value = newProfile
        _isSaved.value = false
        _validationErrors.value = emptySet() // Clear errors on edit
    }

    private fun validate(): Boolean {
        val p = _profile.value ?: return false
        val errors = mutableSetOf<String>()

        // ACADEMIC LIFE - REQUIRED
        if (p.university.isNullOrBlank()) errors.add("university")
        if (p.major.isNullOrBlank()) errors.add("major")
        if (p.currentCgpa.isNullOrBlank()) errors.add("currentCgpa")
        if (p.targetCgpa.isNullOrBlank()) errors.add("targetCgpa")
        if (p.semester.isNullOrBlank()) errors.add("semester")
        if (p.totalSemesters.isNullOrBlank()) errors.add("totalSemesters")
        
        // JOB - REQUIRED ONLY IF CHECKED
        if (p.hasJob == true) {
            if (p.jobDescription.isNullOrBlank()) errors.add("jobDescription")
            if (p.jobSchedule.isNullOrBlank()) errors.add("jobSchedule")
        }

        // STUDY STRATEGY - REQUIRED
        if (p.learningStyle.isNullOrBlank()) errors.add("learningStyle")
        if (p.productivityKiller.isNullOrBlank()) errors.add("productivityKiller")
        if (p.preferredResources.isNullOrBlank()) errors.add("preferredResources")
        if (p.dailyFocusCapacity.isNullOrBlank()) errors.add("dailyFocusCapacity")

        _validationErrors.value = errors
        return errors.isEmpty()
    }

    fun saveProfile() {
        screenModelScope.launch {
            if (validate()) {
                _profile.value?.let { 
                    // 1. Save locally
                    profileStorage.saveProfile(it)
                    
                    // 2. Sync to Appwrite (Triggers Brain)
                    if (!sessionManager.isInitialized.value) {
                         println("⚠️ Session not initialized, auto-initializing for onboarding...")
                         sessionManager.initialize("demo_user_001")
                         // Give it a moment to spin up stats repo
                         kotlinx.coroutines.delay(500)
                    }

                    val success = sessionManager.getStatsRepository()?.saveUserProfile(it)
                    if (success == true) {
                        println("✅ Profile synced to Appwrite & Brain")
                    } else {
                        println("⚠️ Failed to sync profile to Appwrite")
                    }
                    
                    _isSaved.value = true
                }
            }
        }
    }
}
