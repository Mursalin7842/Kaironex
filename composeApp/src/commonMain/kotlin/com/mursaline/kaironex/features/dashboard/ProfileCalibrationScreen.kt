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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.agents.genesis.StudentProfile
import com.mursaline.kaironex.core.storage.ProfileStorage
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class ProfileCalibrationScreen(val isOnboarding: Boolean = false) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getScreenModel<ProfileCalibrationViewModel>()
        
        val profile by viewModel.profile.collectAsState()
        val isSaved by viewModel.isSaved.collectAsState()

        // Auto-navigate if saved during onboarding
        LaunchedEffect(isSaved) {
            if (isSaved && isOnboarding) {
                 navigator.push(com.mursaline.kaironex.features.onboarding.GoogleDriveLinkScreen())
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
                    Text(if (isOnboarding) "Confirm & Continue" else if (isSaved) "Saved!" else "Sync & Save")
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
                    onUpdate = { viewModel.updateProfile(it) },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
fun CalibrationForm(
    profile: StudentProfile,
    onUpdate: (StudentProfile) -> Unit,
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
        // --- IDENTITY ---
        FormSection("Identity") {
            KxTextField(
                label = "Name",
                value = profile.name,
                onValueChange = { onUpdate(profile.copy(name = it)) }
            )
        }
        
        // --- ACADEMIC LIFE & ROUTINE ---
        FormSection("Academic Life") {
            KxTextField(
                label = "University",
                value = profile.university,
                onValueChange = { onUpdate(profile.copy(university = it)) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KxTextField(
                    label = "Major",
                    value = profile.major ?: "",
                    onValueChange = { onUpdate(profile.copy(major = it)) },
                    modifier = Modifier.weight(1f)
                )
                 KxTextField(
                    label = "CGPA (Current)",
                    value = profile.currentCgpa ?: "",
                    onValueChange = { onUpdate(profile.copy(currentCgpa = it)) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                 KxTextField(
                    label = "Current Semester",
                    value = profile.semester ?: "",
                    onValueChange = { onUpdate(profile.copy(semester = it)) },
                    modifier = Modifier.weight(1f)
                )
                 KxTextField(
                    label = "Total Semesters",
                    value = profile.totalSemesters ?: "",
                    onValueChange = { onUpdate(profile.copy(totalSemesters = it)) },
                    modifier = Modifier.weight(1f)
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
                    label = "Home Country",
                    value = profile.homeCountry ?: "",
                    onValueChange = { onUpdate(profile.copy(homeCountry = it)) }
                )
                KxTextField(
                    label = "Current Country",
                    value = profile.currentCountry ?: "",
                    onValueChange = { onUpdate(profile.copy(currentCountry = it)) }
                )

                 Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KxTextField(
                        label = "Visa Status",
                        value = profile.visaStatus ?: "",
                        onValueChange = { onUpdate(profile.copy(visaStatus = it)) },
                         modifier = Modifier.weight(1f)
                    )
                    KxTextField(
                        label = "Work Restrictions",
                        value = profile.workRestrictions ?: "",
                        onValueChange = { onUpdate(profile.copy(workRestrictions = it)) },
                         modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = KaironexColors.BorderGray)
            Spacer(Modifier.height(8.dp))

            Text("Class Schedule & Routine", style = MaterialTheme.typography.labelLarge, color = KaironexColors.SlateGray)
            
            // Map Editor for Class Schedule
            KeyValEditor(
                items = profile.classSchedule,
                keyLabel = "Class (e.g. Chem 101)",
                valLabel = "Time (e.g. Mon 10-12)",
                onUpdate = { onUpdate(profile.copy(classSchedule = it)) }
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Routine Upload Placeholder
            OutlinedCard(
                 onClick = { },
                 border = BorderStroke(1.dp, KaironexColors.GeminiBlurple),
                 colors = CardDefaults.outlinedCardColors(containerColor = KaironexColors.CanvasWhite)
            ) {
                 Row(
                     modifier = Modifier.padding(16.dp).fillMaxWidth(),
                     verticalAlignment = Alignment.CenterVertically,
                     horizontalArrangement = Arrangement.Center
                 ) {
                     Icon(Icons.Filled.UploadFile, null, tint = KaironexColors.GeminiBlurple)
                     Spacer(Modifier.width(8.dp))
                     Text("Upload Routine (PDF/Image)", color = KaironexColors.GeminiBlurple)
                 }
            }
            if (profile.routineFile != null) {
                Text("Uploaded: ${profile.routineFile}", style = MaterialTheme.typography.bodySmall, color = KaironexColors.SuccessGreen)
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
                    label = "Job Description",
                    value = profile.jobDescription ?: "",
                    onValueChange = { onUpdate(profile.copy(jobDescription = it)) }
                )
                KxTextField(
                    label = "Job Schedule (e.g. 2pm-7pm)",
                    value = profile.jobSchedule ?: "",
                    onValueChange = { onUpdate(profile.copy(jobSchedule = it)) }
                )
            }
        }

        // --- COMMUTE & LOGISTICS ---
        FormSection("Commute & Logistics") {
            Text("Travel Times (Minutes)", style = MaterialTheme.typography.labelLarge, color = KaironexColors.SlateGray)
            
             KeyValEditor(
                items = profile.commuteMap,
                keyLabel = "Route (e.g. Home->Uni)",
                valLabel = "Time (e.g. 45m)",
                onUpdate = { onUpdate(profile.copy(commuteMap = it)) }
            )
        }

        // --- REAL WORLD COMMITMENTS ---
        FormSection("Real World Constraints") {
            Text("Non-Negotiables (Family, Prayer, etc.)", style = MaterialTheme.typography.labelLarge, color = KaironexColors.SlateGray)
            KeyValEditor(
                items = profile.nonNegotiables,
                keyLabel = "Activity",
                valLabel = "Time/Note",
                // FIX: Pass new map properly
                onUpdate = { onUpdate(profile.copy(nonNegotiables = it)) } 
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text("Other Commitments (Gym, Dates, etc.)", style = MaterialTheme.typography.labelLarge, color = KaironexColors.SlateGray)
            KeyValEditor(
                items = profile.customCommitments,
                keyLabel = "Commitment",
                valLabel = "Details",
                onUpdate = { onUpdate(profile.copy(customCommitments = it)) }
            )
        }
        
        // --- STUDY STRATEGY ---
        FormSection("Study Strategy") {
             Text("How do you learn best?", style = MaterialTheme.typography.labelMedium)
             Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = profile.learningStyle == "VIDEO",
                    onClick = { onUpdate(profile.copy(learningStyle = "VIDEO")) },
                    label = { Text("Video 🎥") }
                )
                FilterChip(
                    selected = profile.learningStyle == "READING",
                    onClick = { onUpdate(profile.copy(learningStyle = "READING")) },
                    label = { Text("Reading 📖") }
                )
             }
             
             Spacer(Modifier.height(8.dp))
             Text("What distracts you most?", style = MaterialTheme.typography.labelMedium)
             Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = profile.failureCause == "DISTRACTION",
                    onClick = { onUpdate(profile.copy(failureCause = "DISTRACTION")) },
                    label = { Text("Socials/Distraction 📱") }
                )
                FilterChip(
                    selected = profile.failureCause == "FATIGUE",
                    onClick = { onUpdate(profile.copy(failureCause = "FATIGUE")) },
                    label = { Text("Fatigue 😴") }
                )
                 FilterChip(
                    selected = profile.failureCause == "CONFUSION",
                    onClick = { onUpdate(profile.copy(failureCause = "CONFUSION")) },
                    label = { Text("Confusion ❓") }
                )
             }
        }

        // --- GOALS & ENERGY ---
        FormSection("Goals & Energy") {
             Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KxTextField(
                    label = "Main Priority",
                    value = profile.mainPriority ?: "",
                    onValueChange = { onUpdate(profile.copy(mainPriority = it)) },
                    modifier = Modifier.weight(1f)
                )
                KxTextField(
                    label = "Secondary Goal",
                    value = profile.secondaryPriority ?: "",
                    onValueChange = { onUpdate(profile.copy(secondaryPriority = it)) },
                    modifier = Modifier.weight(1f)
                )
             }
             
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = KaironexColors.BorderGray)
            Spacer(Modifier.height(8.dp))
            
            Text("Deep Work Capacity: ${profile.dailyFocusCapacity ?: 4} Hours", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = (profile.dailyFocusCapacity ?: 4).toFloat(),
                onValueChange = { onUpdate(profile.copy(dailyFocusCapacity = it.toInt())) },
                valueRange = 1f..12f,
                steps = 11,
                colors = SliderDefaults.colors(thumbColor = KaironexColors.GeminiBlurple, activeTrackColor = KaironexColors.GeminiBlurple)
            )
            
            Spacer(Modifier.height(8.dp))
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
fun KeyValEditor(
    items: Map<String, String>,
    keyLabel: String,
    valLabel: String,
    onUpdate: (Map<String, String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // List existing
        items.forEach { (key, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KaironexColors.CloudGray, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(key, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(value, style = MaterialTheme.typography.bodySmall, color = KaironexColors.SlateGray)
                }
                IconButton(onClick = { 
                    val newMap = items.toMutableMap()
                    newMap.remove(key)
                    onUpdate(newMap)
                }) {
                    Icon(Icons.Filled.Close, "Remove", tint = KaironexColors.AlertRed)
                }
            }
        }
        
        // Add New
        var newKey by remember { mutableStateOf("") }
        var newVal by remember { mutableStateOf("") }
        
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newKey,
                onValueChange = { newKey = it },
                label = { Text(keyLabel) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = newVal,
                onValueChange = { newVal = it },
                label = { Text(valLabel) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            // Bug Fix: Using dedicated handler and logging if needed for debugging
            FilledIconButton(
                onClick = {
                    if (newKey.isNotBlank() && newVal.isNotBlank()) {
                         // Use immutable map addition to guarantee a new instance
                        onUpdate(items + (newKey.trim() to newVal.trim()))
                        newKey = ""
                        newVal = ""
                    }
                },
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Icon(Icons.Filled.Add, "Add")
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
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = KaironexColors.GeminiBlurple,
            focusedLabelColor = KaironexColors.GeminiBlurple
        )
    )
}

class ProfileCalibrationViewModel(
    private val profileStorage: ProfileStorage
) : ScreenModel {

    private val _profile = kotlinx.coroutines.flow.MutableStateFlow<StudentProfile?>(null)
    val profile = _profile.asStateFlow()
    
    private val _isSaved = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        screenModelScope.launch {
            _profile.value = profileStorage.loadProfile() ?: StudentProfile()
        }
    }
    
    fun updateProfile(newProfile: StudentProfile) {
        _profile.value = newProfile
        _isSaved.value = false
    }

    fun saveProfile() {
        screenModelScope.launch {
            _profile.value?.let { 
                profileStorage.saveProfile(it) 
                _isSaved.value = true
            }
        }
    }
}
