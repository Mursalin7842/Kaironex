package com.mursaline.kaironex.features.campaign

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.agents.genesis.StudentProfile
import com.mursaline.kaironex.brain.AppwriteBridge
import com.mursaline.kaironex.core.stats.AppwriteStatsRepository
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Campaign Setup: Student Career Calibration Wizard
 * Refactored to 3 Steps: Baseline -> Ambition -> Review
 */
@OptIn(ExperimentalMaterial3Api::class)
class CampaignSetupScreen(private val isEditMode: Boolean = true) : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val appwriteBridge = koinInject<AppwriteBridge>()
        val statsRepo = koinInject<AppwriteStatsRepository>()
        val scope = rememberCoroutineScope()
        
        // Observe User State for Data Sync
        val userState by statsRepo.userState.collectAsState()

        // Form State
        var currentStep by remember { mutableStateOf(0) }
        var isSubmitting by remember { mutableStateOf(false) }
        var hasExistingProfile by remember { mutableStateOf(false) }
        
// --- 1. The Core (Identity & Context) ---
        var university by remember { mutableStateOf("") }
        var major by remember { mutableStateOf("") }
        var currentCgpa by remember { mutableStateOf("") }
        var targetCgpa by remember { mutableStateOf("3.5") }
        var semester by remember { mutableStateOf("") }
        var totalSemesters by remember { mutableStateOf("") }
        
        var isInternationalStudent by remember { mutableStateOf(false) }
        var homeCountry by remember { mutableStateOf("") }
        var currentCountry by remember { mutableStateOf("") }
        var visaStatus by remember { mutableStateOf("") }
        
        // Job Logic
        var hasJob by remember { mutableStateOf(false) }
        var currentJobRole by remember { mutableStateOf("") }
        var currentJobDesc by remember { mutableStateOf("") }
        var currentJobReason by remember { mutableStateOf("") } 
        
        // --- 2. The Ambition (Job Filter) ---
        var targetRole by remember { mutableStateOf("") }
        var targetIndustry by remember { mutableStateOf("") }
        var workType by remember { mutableStateOf("Internship") } 
        var allowedWorkHours by remember { mutableStateOf("20") }
        var remoteInterest by remember { mutableStateOf(false) }
        var workExperience by remember { mutableStateOf("") } 
        var whyNeedJob by remember { mutableStateOf("") } 
        var financialStatus by remember { mutableStateOf("") } 
        
        var valueDrivers = remember { mutableStateListOf<String>() }
        var stabilityPreference by remember { mutableStateOf("") }
        
        // --- 3. The Arsenal (Skills) ---
        var skillsList = remember { mutableStateListOf<String>() } 
        var newSkill by remember { mutableStateOf("") }
        var newSkillConfidence by remember { mutableStateOf("Medium") }
        var softSkills by remember { mutableStateOf("") }
        
        // Initial Refresh
        // Removed redundant refreshAll() here as it's handled by SessionManager/Repository Caching
        // LaunchedEffect(Unit) { statsRepo.refreshAll() }

        // Sync Logic
        LaunchedEffect(userState) {
            val profileJson = userState?.profile
            if (profileJson != null) {
                try {
                    println("🔍 Syncing Profile from Brain: $profileJson")
                    val profile = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<StudentProfile>(profileJson)
                    
                    // Mark as existing profile if critical fields are present
                    if (!profile.university.isNullOrBlank()) {
                        hasExistingProfile = true
                    }
                    
                    if (university.isEmpty()) university = profile.university ?: ""
                    if (major.isEmpty()) major = profile.major ?: ""
                    if (currentCgpa.isEmpty()) currentCgpa = profile.currentCgpa ?: ""
                    if (targetCgpa == "3.5") targetCgpa = profile.targetCgpa ?: "3.5"
                    if (semester.isEmpty()) semester = profile.semester ?: ""
                    if (totalSemesters.isEmpty()) totalSemesters = profile.totalSemesters ?: ""
                    
                    isInternationalStudent = profile.isInternationalStudent == true
                    if (homeCountry.isEmpty()) homeCountry = profile.homeCountry ?: ""
                    if (currentCountry.isEmpty()) currentCountry = profile.currentCountry ?: ""
                    if (visaStatus.isEmpty()) visaStatus = profile.visaStatus ?: ""
                    
                    hasJob = profile.hasJob == true
                    if (currentJobRole.isEmpty()) currentJobRole = profile.jobDescription ?: "" 
                    if (currentJobReason.isEmpty()) currentJobReason = profile.jobImportance ?: ""
                    
                    if (financialStatus.isEmpty()) {
                        println("💰 Loading Financial Status: ${profile.financialStatus}")
                        financialStatus = profile.financialStatus ?: ""
                    }
                    
                    if (targetRole.isEmpty()) targetRole = profile.targetRole ?: ""
                    if (targetIndustry.isEmpty()) targetIndustry = profile.targetIndustry ?: ""
                    allowedWorkHours = profile.allowedWorkHours?.toString() ?: "20"
                    
                    if (skillsList.isEmpty()) {
                        println("⚔️ Loading Skills: ${profile.skills}")
                        skillsList.addAll(profile.skills)
                    }

                    // Unpack Job Description
                    profile.jobDescription?.let { fullDesc ->
                        if (fullDesc.startsWith("[Exp:")) {
                            // Case: No Job, only Experience
                            if (workExperience.isEmpty()) {
                                workExperience = fullDesc.substring(5, fullDesc.length - 1).trim()
                            }
                        } else {
                            // Case: Job + Optional Experience
                            if (currentJobRole.isEmpty()) {
                                // Format: "Role | Desc \n[Exp: ...]"
                                val parts = fullDesc.split("|", limit = 2)
                                currentJobRole = parts[0].trim()
                                
                                if (parts.size > 1) {
                                    val remaining = parts[1]
                                    val expStart = remaining.indexOf("[Exp:")
                                    if (expStart != -1) {
                                        currentJobDesc = remaining.substring(0, expStart).trim()
                                        if (workExperience.isEmpty()) {
                                            workExperience = remaining.substring(expStart + 5, remaining.length - 1).trim()
                                        }
                                    } else {
                                        currentJobDesc = remaining.trim()
                                    }
                                }
                            }
                        }
                    }
                    // Redundant check for safety (if packing logic varied)
                     if (workExperience.isEmpty() && profile.jobDescription?.contains("[Exp:") == true && !profile.jobDescription!!.startsWith("[Exp:")) {
                        val start = profile.jobDescription!!.indexOf("[Exp:")
                        workExperience = profile.jobDescription!!.substring(start + 5, profile.jobDescription!!.length - 1).trim()
                     }

                    // Unpack Soft Skills
                    if (softSkills.isEmpty() && profile.softSkills.isNotEmpty()) {
                        softSkills = profile.softSkills.keys.joinToString(", ")
                    }
                    
                    // Unpack Motivation
                    if (whyNeedJob.isEmpty() && profile.valueDrivers.isNotEmpty()) {
                        whyNeedJob = profile.valueDrivers.first()
                    }
                    
                } catch(e: Exception) { 
                    println("❌ Profile Sync Error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(if (isEditMode) "Recalibrate Strategy" else "Campaign Setup", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = KaironexColors.CanvasWhite)
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = KaironexColors.CloudGray
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Progress Bar (4 Steps)
                LinearProgressIndicator(
                    progress = { (currentStep + 1) / 4f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF5E35B1),
                    trackColor = Color(0xFFE0E0E0),
                )

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { step ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (step) {
                            0 -> StepOrigin(
                                university, { university = it },
                                major, { major = it },
                                currentCgpa, { currentCgpa = it },
                                semester, { semester = it },
                                totalSemesters, { totalSemesters = it },
                                isInternationalStudent, { isInternationalStudent = it },
                                homeCountry, { homeCountry = it },
                                currentCountry, { currentCountry = it },
                                visaStatus, { visaStatus = it },
                                hasJob, { hasJob = it },
                                currentJobRole, { currentJobRole = it },
                                currentJobDesc, { currentJobDesc = it },
                                currentJobReason, { currentJobReason = it }
                            )
                            1 -> StepAmbition( // Job Filter
                                targetRole, { targetRole = it },
                                targetIndustry, { targetIndustry = it },
                                workType, { workType = it },
                                remoteInterest, { remoteInterest = it },
                                allowedWorkHours, { allowedWorkHours = it },
                                workExperience, { workExperience = it },
                                whyNeedJob, { whyNeedJob = it },
                                financialStatus, { financialStatus = it }
                            )
                            2 -> StepArsenal( // Skills
                                skillsList, 
                                newSkill, { newSkill = it }, 
                                newSkillConfidence, { newSkillConfidence = it },
                                { 
                                    if (newSkill.isNotBlank()) {
                                        skillsList.add("$newSkill ($newSkillConfidence)")
                                        newSkill = ""
                                    }
                                }, 
                                { index -> skillsList.removeAt(index) },
                                softSkills, { softSkills = it }
                            )
                            3 -> StepReview(
                                mapOf(
                                    "University" to university,
                                    "Major" to major,
                                    "CGPA" to "$currentCgpa / $targetCgpa",
                                    "Semester" to "$semester / $totalSemesters",
                                    
                                    "Status" to if(isInternationalStudent) "Intel'l Student" else "Domestic",
                                    "Origin" to if(isInternationalStudent) "$homeCountry -> $currentCountry" else homeCountry.ifBlank { "N/A" },
                                    "Visa" to if(isInternationalStudent) visaStatus else "N/A",
                                    
                                    "Job Status" to if(hasJob) "Employed" else "Looking",
                                    "Current Role" to if(hasJob) currentJobRole else "N/A",
                                    "Job Desc" to if(hasJob) currentJobDesc else "N/A",
                                    "Why This Job" to if(hasJob) currentJobReason else "N/A",
                                    
                                    "Target" to if(targetRole.isNotBlank()) "$targetRole @ $targetIndustry" else "N/A",
                                    "Work Type" to workType,
                                    "Max Hours" to allowedWorkHours,
                                    "Remote" to if(remoteInterest) "Yes" else "No",
                                    
                                    "Experience" to workExperience,
                                    "Motivation" to whyNeedJob,
                                    "Finance" to financialStatus,
                                    
                                    "Hard Skills" to skillsList.joinToString(", "),
                                    "Soft Skills" to softSkills
                                ),
                                onBack = { currentStep = 2 },
                                onSubmit = {
                                    scope.launch {
                                        isSubmitting = true
                                        // Update Repo fields
                                        // Construct Full Profile Object
                                        val currentProfile = userState?.profile?.let {
                                            try {
                                                kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<StudentProfile>(it)
                                            } catch (e: Exception) { StudentProfile() }
                                        } ?: StudentProfile()

                                        val newProfile = currentProfile.copy(
                                            university = university,
                                            major = major,
                                            currentCgpa = currentCgpa,
                                            targetCgpa = targetCgpa,
                                            semester = semester,
                                            totalSemesters = totalSemesters,

                                            // International
                                            isInternationalStudent = isInternationalStudent,
                                            homeCountry = if(isInternationalStudent) homeCountry else currentProfile.homeCountry,
                                            currentCountry = if(isInternationalStudent) currentCountry else currentProfile.currentCountry,
                                            visaStatus = if(isInternationalStudent) visaStatus else currentProfile.visaStatus,

                                            // Job
                                            // Job & Experience Packing
                                            hasJob = hasJob,
                                            jobDescription = if(hasJob) {
                                                buildString {
                                                    append(currentJobRole)
                                                    if (currentJobDesc.isNotBlank()) append(" | $currentJobDesc")
                                                    if (workExperience.isNotBlank()) append("\n[Exp: $workExperience]")
                                                }
                                            } else {
                                                // Preserve existing description if it only had experience, or update if we are saving experience
                                                if (workExperience.isNotBlank()) "[Exp: $workExperience]" else null 
                                            },
                                            jobImportance = if(hasJob) currentJobReason else null,

                                            // Preferences
                                            workType = workType,
                                            allowedWorkHours = allowedWorkHours.toIntOrNull(),
                                            workPreference = if(remoteInterest) "Remote" else "On-site",
                                            
                                            // Financial
                                            financialStatus = financialStatus,
                                            
                                            // Ambition
                                            targetRole = if(targetRole.isNotBlank()) targetRole else null,
                                            targetIndustry = if(targetIndustry.isNotBlank()) targetIndustry else null,
                                            valueDrivers = if(whyNeedJob.isNotBlank()) listOf(whyNeedJob) else emptyList(),

                                            // Skills
                                            skills = skillsList,
                                            // Parse comma-separated soft skills
                                            softSkills = if(softSkills.isNotBlank()) {
                                                softSkills.split(",").map { 
                                                    it.trim().replaceFirstChar { char -> char.uppercase() } to 1 
                                                }.toMap()
                                            } else emptyMap()
                                        )
                                        
                                        // 1. Atomic Save
                                        statsRepo.saveUserProfile(newProfile)

                                        // 2. Trigger Agent
                                        val userId = userState?.userId ?: ""
                                        val calibrationData = mapOf(
                                            "university" to university,
                                            "major" to major,
                                            "currentCgpa" to currentCgpa,
                                            "semester" to semester,
                                            "totalSemesters" to totalSemesters,
                                            "isInternationalStudent" to isInternationalStudent.toString(),
                                            "homeCountry" to homeCountry,
                                            "currentCountry" to currentCountry,
                                            "visaStatus" to visaStatus,
                                            
                                            "hasJob" to hasJob.toString(),
                                            "currentJobRole" to currentJobRole,
                                            "currentJobDesc" to currentJobDesc,
                                            "currentJobReason" to currentJobReason,
                                            
                                            "workType" to workType,
                                            "remoteInterest" to remoteInterest.toString(),
                                            "allowedWorkHours" to allowedWorkHours,
                                            "workExperience" to workExperience,
                                            "whyNeedJob" to whyNeedJob,
                                            "financialStatus" to financialStatus,
                                            
                                            "skills" to skillsList.joinToString(", "),
                                            "softSkills" to softSkills,
                                            
                                            "targetRole" to targetRole,
                                            "targetIndustry" to targetIndustry
                                        )
                                        
                                        if (userId.isNotEmpty()) {
                                            // SAFETY: Only trigger full reset if explicitly NOT in edit mode AND no existing profile was found.
                                            // This prevents accidental wiping of campaign data for existing users.
                                            if (!isEditMode && !hasExistingProfile) {
                                                println("🧠 Triggering Campaign Calibration (New Setup)...")
                                                appwriteBridge.triggerBrain(userId, "campaign", "campaign_calibration", calibrationData)
                                            } else {
                                                println("📝 Profile Updated (Skipping Campaign Reset)")
                                                // Optional: Trigger a lighter 'profile_update' event here if needed in future
                                                // appwriteBridge.triggerBrain(userId, "campaign", "campaign_update_context", calibrationData)
                                            }
                                        }
                                        
                                        statsRepo.refreshAll()
                                        navigator.pop()
                                    }
                                },
                                isSubmitting = isSubmitting
                            )
                        }
                    }
                }
                
                // Nav
                if (currentStep < 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (currentStep > 0) {
                            OutlinedButton(onClick = { currentStep-- }) { Text("Back") }
                        } else { Spacer(Modifier.width(16.dp)) }
                        
                        Button(
                            onClick = { 
                                val isValid = when (currentStep) {
                                    0 -> {
                                        var valid = university.isNotBlank() && major.isNotBlank() && currentCgpa.isNotBlank() && semester.isNotBlank() && totalSemesters.isNotBlank()
                                        if (isInternationalStudent) valid = valid && currentCountry.isNotBlank() && homeCountry.isNotBlank() && visaStatus.isNotBlank()
                                        if (hasJob) valid = valid && currentJobRole.isNotBlank() && currentJobDesc.isNotBlank() && currentJobReason.isNotBlank()
                                        if (!valid) scope.launch { snackbarHostState.showSnackbar("Please fill all required fields (*) to proceed.") }
                                        valid
                                    }
                                    1 -> {
                                        val valid = allowedWorkHours.isNotBlank() && workExperience.isNotBlank() && whyNeedJob.isNotBlank() && financialStatus.isNotBlank()
                                        if (!valid) scope.launch { snackbarHostState.showSnackbar("Please fill all required fields (*) to proceed.") }
                                        valid
                                    }
                                    2 -> {
                                        val valid = skillsList.isNotEmpty() && softSkills.isNotBlank()
                                        if (!valid) scope.launch { snackbarHostState.showSnackbar(if (skillsList.isEmpty()) "Please add at least one hard skill (+ button)" else "Please fill soft skills") }
                                        valid
                                    }
                                    else -> true
                                }
                                if (isValid) currentStep++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E35B1))
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepOrigin(
    university: String, onUni: (String) -> Unit,
    major: String, onMajor: (String) -> Unit,
    cgpa: String, onCgpa: (String) -> Unit,
    semester: String, onSem: (String) -> Unit,
    totalSem: String, onTotalSem: (String) -> Unit,
    isIntl: Boolean, onIntl: (Boolean) -> Unit,
    home: String, onHome: (String) -> Unit,
    current: String, onCurrent: (String) -> Unit,
    visa: String, onVisa: (String) -> Unit,
    hasJob: Boolean, onHasJob: (Boolean) -> Unit,
    jobRole: String, onJobRole: (String) -> Unit,
    jobDesc: String, onJobDesc: (String) -> Unit,
    jobReason: String, onJobReason: (String) -> Unit
) {
    Text("Step 1/4: Academic Info", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
    Text("University & Status", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))

    OutlinedTextField(value = university, onValueChange = onUni, label = { Text("Uni Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(value = major, onValueChange = onMajor, label = { Text("Major *") }, modifier = Modifier.fillMaxWidth().padding(top=8.dp), singleLine = true)
    
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = cgpa, onValueChange = onCgpa, label = { Text("CGPA *") }, modifier = Modifier.weight(1f).padding(top=8.dp), singleLine = true)
        OutlinedTextField(value = semester, onValueChange = onSem, label = { Text("Curr Sem *") }, modifier = Modifier.weight(1f).padding(top=8.dp), singleLine = true)
        OutlinedTextField(value = totalSem, onValueChange = onTotalSem, label = { Text("Total Sem *") }, modifier = Modifier.weight(1f).padding(top=8.dp), singleLine = true)
    }
    
    Spacer(Modifier.height(16.dp))
    Text("International Status", style = MaterialTheme.typography.titleMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = isIntl, onCheckedChange = onIntl)
        Text("International Student")
    }
    if (isIntl) {
        OutlinedTextField(value = current, onValueChange = onCurrent, label = { Text("Current Country *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = home, onValueChange = onHome, label = { Text("Home Country *") }, modifier = Modifier.fillMaxWidth().padding(top=8.dp), singleLine = true)
        OutlinedTextField(value = visa, onValueChange = onVisa, label = { Text("Visa Type *") }, modifier = Modifier.fillMaxWidth().padding(top=8.dp), singleLine = true)
    }

    Spacer(Modifier.height(16.dp))
    Text("Current Job", style = MaterialTheme.typography.titleMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = hasJob, onCheckedChange = onHasJob)
        Text("Already have a job?")
    }
    if (hasJob) {
        OutlinedTextField(value = jobRole, onValueChange = onJobRole, label = { Text("Job Role *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = jobDesc, onValueChange = onJobDesc, label = { Text("Description *") }, modifier = Modifier.fillMaxWidth().padding(top=8.dp), singleLine = true)
        OutlinedTextField(value = jobReason, onValueChange = onJobReason, label = { Text("Reason for doing this job *") }, modifier = Modifier.fillMaxWidth().padding(top=8.dp), singleLine = true)
    }
}



@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepAmbition(
    targetRole: String, onTargetRole: (String) -> Unit,
    targetIndustry: String, onTargetIndustry: (String) -> Unit,
    workType: String, onWorkType: (String) -> Unit,
    remote: Boolean, onRemote: (Boolean) -> Unit,
    allowedHours: String, onAllowedHours: (String) -> Unit,
    exp: String, onExp: (String) -> Unit,
    whyNeed: String, onWhyNeed: (String) -> Unit,
    finance: String, onFinance: (String) -> Unit
) {
    Text("Step 2/4: Preferences & Needs", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
    Text("Job Filter", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))

    // Target Role & Industry (Added)
    OutlinedTextField(value = targetRole, onValueChange = onTargetRole, label = { Text("Target Role (e.g. Software Engineer) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(value = targetIndustry, onValueChange = onTargetIndustry, label = { Text("Target Industry (e.g. Fintech) *") }, modifier = Modifier.fillMaxWidth().padding(top=8.dp), singleLine = true)
    Spacer(Modifier.height(16.dp))

    Text("Job Type", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Part-time", "Internship", "Full-time").forEach { t ->
            FilterChip(selected = workType == t, onClick = { onWorkType(t) }, label = { Text(t) })
        }
    }
    if (workType == "Full-time") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, "Warning", tint = Color(0xFFF57C00))
            Spacer(Modifier.width(8.dp))
            Text("Full-time warning", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF57C00))
        }
    }
    
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top=8.dp)) {
        Checkbox(checked = remote, onCheckedChange = onRemote)
        Text("Remote job interested")
    }
    
    OutlinedTextField(value = allowedHours, onValueChange = onAllowedHours, label = { Text("Allowed work hours *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(value = exp, onValueChange = onExp, label = { Text("Work Experience *") }, modifier = Modifier.fillMaxWidth().padding(top=8.dp), minLines = 2)
    OutlinedTextField(value = whyNeed, onValueChange = onWhyNeed, label = { Text("Reason for Job searching *") }, modifier = Modifier.fillMaxWidth().padding(top=8.dp), minLines = 2)
    OutlinedTextField(value = finance, onValueChange = onFinance, label = { Text("Financial condition right now *") }, modifier = Modifier.fillMaxWidth().padding(top=8.dp), singleLine = true)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepArsenal(
    skills: List<String>, 
    newSkill: String, onNewSkill: (String) -> Unit,
    conf: String, onConf: (String) -> Unit,
    onAdd: () -> Unit, onRem: (Int) -> Unit,
    soft: String, onSoft: (String) -> Unit
) {
    Text("Step 3/4: Skill Section", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
    Text("Skills & Confidence", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))

    Text("Hard Skills", style = MaterialTheme.typography.titleMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = newSkill, onValueChange = onNewSkill, label = { Text("Skill Name *") }, modifier = Modifier.weight(1f), singleLine = true)
        Spacer(Modifier.width(8.dp))
        Box {
           Button(onClick = { 
               val levels = listOf("Low", "Medium", "High")
               val idx = levels.indexOf(conf)
               onConf(levels[(idx + 1) % levels.size])
           }) { Text(conf) }
        }
        IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Add") }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        skills.forEachIndexed { i, s ->
            InputChip(selected = true, onClick = { onRem(i) }, label = { Text(s) }, trailingIcon = { Icon(Icons.Default.Close, "X", modifier = Modifier.size(16.dp)) })
        }
    }
    
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(value = soft, onValueChange = onSoft, label = { Text("Soft Skills *") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
}

@Composable
fun StepReview(
    data: Map<String, String>,
    onBack: () -> Unit,
    onSubmit: () -> Unit, isSubmitting: Boolean
) {
    Text("Step 4/4: Confirm / Edit / Starts", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
    Text("Ready to Build Strategy?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(24.dp))
    
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = KaironexColors.CanvasWhite)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            data.forEach { (k, v) -> ReviewRow(k, v) }
        }
    }
    
    Spacer(Modifier.height(24.dp))
    
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(50.dp)) {
            Text("Back")
        }
        
        Button(
            onClick = onSubmit,
            modifier = Modifier.weight(2f).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E35B1)),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Initialize Campaign")
        }
    }
}

@Composable
fun SkillSliderCompact(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("${value.toInt()}", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = 1f..10f, steps = 8, modifier = Modifier.weight(2f))
    }
}

@Composable
fun ReviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
