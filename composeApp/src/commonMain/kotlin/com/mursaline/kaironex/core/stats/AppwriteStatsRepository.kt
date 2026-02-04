package com.mursaline.kaironex.core.stats

import com.mursaline.kaironex.brain.BrainApiClient
import com.mursaline.kaironex.brain.UserStateResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.decodeFromString
import com.mursaline.kaironex.features.campaign.*

/**
 * 📊 APPWRITE STATS REPOSITORY
 * ============================
 * Replaces mock StatsProvider with real data from Appwrite via Brain API.
 *
 * Data Sources:
 * - users.studentState_json → God mode cached state
 * - vitality_state → Bio-fuel, regen, resources
 * - campaign_state → Skills, quests, armory
 * - radius_state → Safehouse, local scan, social
 * - agent_memory → Brain status, pressure index
 * - schedule → Today's tasks
 * - study_logs → Session history
 * - concept_mastery → Learning progress
 *
 * Usage:
 *   val repo = AppwriteStatsRepository(brainClient, userId)
 *   repo.refreshAll()
 *   val homeStats = repo.homeStats.value
 */
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import com.mursaline.kaironex.core.AppConfig

import com.mursaline.kaironex.agents.genesis.StudentProfile
import kotlinx.serialization.encodeToString
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.ktor.client.request.parameter
import com.mursaline.kaironex.brain.ThoughtStreamItem

import com.mursaline.kaironex.features.campaign.CampaignState

class AppwriteStatsRepository(
    private val brainClient: BrainApiClient,
    private val httpClient: HttpClient,
    private val userId: String
) {


    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    /**
     * Fetch the latest thought directly from Appwrite Database.
     */
    suspend fun getLatestThought(): ThoughtStreamItem? {
        val url = "${AppConfig.Appwrite.ENDPOINT}/databases/${AppConfig.Appwrite.DATABASE_ID}/collections/${AppConfig.Collections.THOUGHT_SIGNATURES}/documents"
        return try {
            val response = httpClient.get(url) {
                header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                if (AppConfig.Appwrite.API_KEY.isNotBlank()) {
                    header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
                }
                // Appwrite REST API query format (v1.4+)
                parameter("queries[0]", """{"method":"equal","attribute":"userId","values":["$userId"]}""")
                parameter("queries[1]", """{"method":"orderDesc","attribute":"${'$'}createdAt"}""")
                parameter("queries[2]", """{"method":"limit","values":[1]}""")
            }

            if (response.status.value == 200) {
                val bodyText = response.bodyAsText()
                val root = json.parseToJsonElement(bodyText).jsonObject
                val documents = root["documents"]?.jsonArray
                
                documents?.firstOrNull()?.let { doc ->
                    val obj = doc.jsonObject
                    
                    // Try to extract thought text
                    var thoughtText = "Thinking..."
                    val reasoning = obj["reasoning_trace"]?.jsonPrimitive?.contentOrNull
                    if (!reasoning.isNullOrBlank()) {
                        // Sometimes it's a JSON list string, sometimes plain text
                        thoughtText = if (reasoning.startsWith("[")) {
                             try {
                                 val list = json.parseToJsonElement(reasoning).jsonArray
                                 list.lastOrNull()?.jsonPrimitive?.content ?: "..."
                             } catch(e:Exception) { reasoning.take(150) }
                        } else {
                             reasoning.take(150)
                        }
                    }

                    ThoughtStreamItem(
                        agent = obj["agentType"]?.jsonPrimitive?.contentOrNull ?: "System",
                        thought = thoughtText,
                        confidence = obj["confidence"]?.jsonPrimitive?.floatOrNull ?: 0.5f
                    )
                }
            } else {
                null
            }
        } catch (e: Exception) {
            println("❌ Get Latest Thought Error: ${e.message}")
            null
        }
    }

    // Cached state from backend
    private val _userState = MutableStateFlow<UserStateResponse?>(null)
    val userState: StateFlow<UserStateResponse?> = _userState.asStateFlow()

    private val _homeStats = MutableStateFlow(StatsProvider.getHomeStats())
    val homeStats: StateFlow<HomeStats> = _homeStats.asStateFlow()

    private val _moreStats = MutableStateFlow(StatsProvider.getMoreStats())
    val moreStats: StateFlow<MoreStats> = _moreStats.asStateFlow()

    // NEW: Campaign State
    private val _campaignState = MutableStateFlow(CampaignState())
    val campaignState: StateFlow<CampaignState> = _campaignState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long>(0)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    /**
     * Refresh all stats from backend.
     */
    suspend fun refreshAll() {
        _isLoading.value = true

        try {
            // 1. Fetch User State
            val state = fetchUserState()
            _userState.value = state

            if (state != null) {
                parseUserState(state)
            }
            
            // 2. Fetch Campaign State (Separate Collection)
            fetchCampaignState()

            _lastSyncTime.value = System.currentTimeMillis()
        } catch (e: Exception) {
            println("❌ Failed to refresh stats: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Refresh just home stats.
     */
    suspend fun refreshHomeStats() {
        _isLoading.value = true

        try {
            val state = fetchUserState()
            if (state != null) {
                parseHomeStats(state)
            }
        } catch (e: Exception) {
            println("❌ Failed to refresh home stats: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Refresh just more/life stats.
     */
    suspend fun refreshMoreStats() {
        _isLoading.value = true

        try {
            val state = fetchUserState()
            if (state != null) {
                parseMoreStats(state)
            }
        } catch (e: Exception) {
            println("❌ Failed to refresh more stats: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Save/Update user profile to Appwrite.
     * This triggers the Brain function if configured.
     */
    /**
     * Save/Update user profile to Appwrite.
     * This triggers the Brain function if configured.
     * Implements UPSERT: Tries to Update, if 404, Create.
     */
    suspend fun saveUserProfile(profile: StudentProfile): Boolean {
        val profileJsonString = json.encodeToString(profile)
        val dataPayload = buildJsonObject {
            put("data", buildJsonObject {
                put("studentprofile_json", profileJsonString)
                put("userId", userId) // Ensure userId is also in the data
            })
        }

        return try {
            // 1. Try UPDATE (Patch)
            val updateUrl = "${AppConfig.Appwrite.ENDPOINT}/databases/${AppConfig.Appwrite.DATABASE_ID}/collections/${AppConfig.Collections.USERS}/documents/$userId"
            
            val updateResponse = httpClient.patch(updateUrl) {
                header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                if (AppConfig.Appwrite.API_KEY.isNotBlank()) {
                    header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
                }
                header("Content-Type", "application/json")
                setBody(dataPayload)
            }

            if (updateResponse.status.value in 200..299) {
                refreshAll()
                return true
            } else if (updateResponse.status.value == 404) {
                // 2. Document not found -> CREATE (Post)
                println("⚠️ User doc not found (404), creating new document for $userId...")
                
                val createUrl = "${AppConfig.Appwrite.ENDPOINT}/databases/${AppConfig.Appwrite.DATABASE_ID}/collections/${AppConfig.Collections.USERS}/documents"
                
                val createPayload = buildJsonObject {
                    put("documentId", userId)
                    put("data", buildJsonObject {
                         put("studentprofile_json", profileJsonString)
                         put("userId", userId)
                         // Initialize empty state
                         put("studentState_json", "{}") 
                    })
                }

                val createResponse = httpClient.post(createUrl) {
                    header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                    if (AppConfig.Appwrite.API_KEY.isNotBlank()) {
                        header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
                    }
                    header("Content-Type", "application/json")
                    setBody(createPayload)
                }

                if (createResponse.status.value in 200..299) {
                    println("✅ Created new user document for $userId")
                    refreshAll()
                    return true
                } else {
                    println("❌ Failed to create user profile: ${createResponse.status} - ${createResponse.bodyAsText()}")
                    return false
                }
            } else {
                println("❌ Failed to update profile appwrite: ${updateResponse.status} - ${updateResponse.bodyAsText()}")
                return false
            }
        } catch (e: Exception) {
            println("❌ Error saving profile to Appwrite: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Update a specific field in the profile (for Voice Agents).
     */
    /**
     * Update a specific field in the profile (for Voice Agents).
     */
    suspend fun updateProfileField(field: String, value: String): Boolean {
        return updateProfileGeneric { current ->
            when(field) {
                "name" -> current.copy(name = value)
                "university" -> current.copy(university = value)
                "major" -> current.copy(major = value)
                "currentCgpa" -> current.copy(currentCgpa = value)
                "targetCgpa" -> current.copy(targetCgpa = value)
                "semester" -> current.copy(semester = value)
                "totalSemesters" -> current.copy(totalSemesters = value)
                
                // International
                "isInternationalStudent" -> current.copy(isInternationalStudent = value.toBoolean())
                "homeCountry" -> current.copy(homeCountry = value)
                "currentCountry" -> current.copy(currentCountry = value)
                "visaStatus" -> current.copy(visaStatus = value)
                
                // Work/Finances
                "hasJob" -> current.copy(hasJob = value.toBoolean())
                "jobDescription" -> current.copy(jobDescription = value)
                "jobSchedule" -> current.copy(jobSchedule = value)
                "jobImportance" -> current.copy(jobImportance = value)
                "financialStatus" -> current.copy(financialStatus = value)
                
                // Psych
                "learningStyle" -> current.copy(learningStyle = value)
                "productivityKiller" -> current.copy(productivityKiller = value)
                "preferredResources" -> current.copy(preferredResources = value)
                "dailyFocusCapacity" -> current.copy(dailyFocusCapacity = value)
                "energyPreference" -> current.copy(energyPreference = value) // chronotype
                "workPreference" -> current.copy(workPreference = value)
                "environmentType" -> current.copy(environmentType = value)
                
                // Ambition
                "targetRole" -> current.copy(targetRole = value)
                "targetIndustry" -> current.copy(targetIndustry = value)
                "stabilityPreference" -> current.copy(stabilityPreference = value)
                "allowedWorkHours" -> current.copy(allowedWorkHours = value.toIntOrNull())
                
                else -> current
            }
        }
    }

    /**
     * Update a list field in the profile.
     */
    suspend fun updateProfileListField(field: String, list: List<String>): Boolean {
        return updateProfileGeneric { current ->
            when(field) {
                "skills" -> current.copy(skills = list)
                "valueDrivers" -> current.copy(valueDrivers = list)
                else -> current
            }
        }
    }

    private suspend fun updateProfileGeneric(transform: (StudentProfile) -> StudentProfile): Boolean {
        val state = fetchUserState() ?: return false
        val profileString = state.profile ?: "{}"
        
        try {
            val currentProfile = try {
                json.decodeFromString<StudentProfile>(profileString)
            } catch (e: Exception) {
                StudentProfile()
            }
            
            val updatedProfile = transform(currentProfile)
            return saveUserProfile(updatedProfile)
        } catch (e: Exception) {
            println("❌ Error updating profile: ${e.message}")
            return false
        }
    }

    private suspend fun fetchUserState(): UserStateResponse? {
        return try {
            val url = "${AppConfig.Appwrite.ENDPOINT}/databases/${AppConfig.Appwrite.DATABASE_ID}/collections/${AppConfig.Collections.USERS}/documents/$userId"
            
            val response = httpClient.get(url) {
                header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                if (AppConfig.Appwrite.API_KEY.isNotBlank()) {
                    header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
                }
            }
            
            val docJson = json.parseToJsonElement(response.bodyAsText()).jsonObject
            
            val stateString = docJson["studentState_json"]?.jsonPrimitive?.contentOrNull
            val profileString = docJson["studentprofile_json"]?.jsonPrimitive?.contentOrNull
            
            val stateMap: Map<String, String> = if (stateString != null) {
                try {
                    val parsed = json.parseToJsonElement(stateString).jsonObject
                    parsed.mapValues { it.value.toString() }
                } catch (e: Exception) { emptyMap<String, String>() }
            } else {
                emptyMap<String, String>()
            }

            UserStateResponse(
                userId = userId,
                state = stateMap,
                profile = profileString,
                lastUpdated = docJson["\$updatedAt"]?.jsonPrimitive?.contentOrNull
            )
        } catch (e: Exception) {
            println("⚠️ Error fetching state from Appwrite: ${e.message}")
            if (AppConfig.useMockData) {
                // Fallback to mock
                null
            } else {
                println("⚠️ Request Failed") 
                null
            }
        } catch (e: Exception) {
            println("❌ Get Latest Thought Error: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun parseUserState(state: UserStateResponse) {
        parseHomeStats(state)
        parseMoreStats(state)
    }

    private fun parseHomeStats(state: UserStateResponse) {
        try {
            val stateJson = state.state

            // Extract pressure index from state
            val pressureIndex = stateJson["pressure_index"]?.toIntOrNull() ?: 45

            // Parse vitality for mental state
            val vitalityJson = stateJson["vitality"]?.let {
                try { json.parseToJsonElement(it).jsonObject } catch (e: Exception) { null }
            }

            // Parse campaign for learning
            val campaignJson = stateJson["campaign"]?.let {
                try { json.parseToJsonElement(it).jsonObject } catch (e: Exception) { null }
            }

            // Build updated home stats
            val current = _homeStats.value

            _homeStats.value = current.copy(
                pressure = current.pressure.copy(
                    pressureIndex = pressureIndex,
                    burnoutProbability = pressureIndex / 100f * 0.5f
                ),
                mentalState = current.mentalState.copy(
                    burnoutRisk = when {
                        pressureIndex < 40 -> RiskLevel.LOW
                        pressureIndex < 70 -> RiskLevel.MEDIUM
                        else -> RiskLevel.HIGH
                    }
                )
            )

            // Parse concept mastery if available
            campaignJson?.get("quest_board")?.let { quests ->
                // Update habits based on active quests
            }

        } catch (e: Exception) {
            println("⚠️ Error parsing home stats: ${e.message}")
        }
    }

    private fun parseMoreStats(state: UserStateResponse) {
        try {
            val stateJson = state.state

            // Extract contributions
            val pressureIndex = stateJson["pressure_index"]?.toIntOrNull() ?: 45

            // Parse each zone state
            val vitalityJson = stateJson["vitality"]?.let {
                try { json.parseToJsonElement(it).jsonObject } catch (e: Exception) { null }
            }

            val campaignJson = stateJson["campaign"]?.let {
                try { json.parseToJsonElement(it).jsonObject } catch (e: Exception) { null }
            }

            val radiusJson = stateJson["radius"]?.let {
                try { json.parseToJsonElement(it).jsonObject } catch (e: Exception) { null }
            }

            val current = _moreStats.value

            // Calculate life stability from pressure
            val overallScore = 100 - pressureIndex

            _moreStats.value = current.copy(
                lifeStability = current.lifeStability.copy(
                    overallScore = overallScore,
                    trend = if (pressureIndex < 50) TrendDirection.UP else TrendDirection.DOWN
                )
            )

            // Parse vitality specifics
            vitalityJson?.let { vitality ->
                val bioFuel = vitality["bio_fuel"]?.jsonObject
                val energyLevel = bioFuel?.get("energy_level")?.jsonPrimitive?.intOrNull ?: 75

                _moreStats.value = _moreStats.value.copy(
                    vitality = _moreStats.value.vitality.copy(
                        energyLevelToday = energyLevel,
                        agentStatus = if (energyLevel > 60) AgentStatus.STABLE else AgentStatus.ATTENTION
                    )
                )
            }

            // DERIVE CALIBRATION FROM PROFILE (Client-Side Immediate Unlock)
            // We check if the user has a valid profile (Uni + Skills/Job) to consider them "Calibrated"
            var isCalibrated = false
            val profileStr = state.profile
            if (profileStr != null) {
                try {
                    val p = json.decodeFromString<StudentProfile>(profileStr)
                    // Calibration Condition: Must have University set AND (Skills added OR Job set)
                    isCalibrated = !p.university.isNullOrBlank() && (p.skills.isNotEmpty() || p.hasJob == true)
                } catch(e: Exception) { 
                     // Fallback to checking campaign flag if profile parse fails
                     isCalibrated = campaignJson?.get("is_calibrated")?.jsonPrimitive?.booleanOrNull ?: false
                }
            } else {
                 isCalibrated = campaignJson?.get("is_calibrated")?.jsonPrimitive?.booleanOrNull ?: false
            }

            // Parse campaign specifics
            campaignJson?.let { campaign ->
                val skillProgress = campaign["skill_progress"]?.jsonPrimitive?.floatOrNull ?: 0.58f
                
                _moreStats.value = _moreStats.value.copy(
                    campaign = _moreStats.value.campaign.copy(
                        skillsProgress = skillProgress,
                        isCalibrated = isCalibrated
                    )
                )
            } ?: run {
                // If campaignJson is null BUT we have calibration (from profile), update stats
                if (isCalibrated) {
                    _moreStats.value = _moreStats.value.copy(
                        campaign = _moreStats.value.campaign.copy(
                            isCalibrated = true
                        )
                    )
                }
            }

            // Parse radius specifics
            radiusJson?.let { radius ->
                val visaDays = radius["visa_days_remaining"]?.jsonPrimitive?.intOrNull ?: 89

                _moreStats.value = _moreStats.value.copy(
                    radius = _moreStats.value.radius.copy(
                        visaDaysRemaining = visaDays
                    )
                )
            }

        } catch (e: Exception) {
            println("⚠️ Error parsing more stats: ${e.message}")
        }
    }

    /**
     * Get the user's wake word from profile.
     */
    fun getWakeWord(): String {
        val profile = _userState.value?.profile
        if (profile != null) {
            try {
                val profileJson = json.parseToJsonElement(profile).jsonObject
                return profileJson["wake_word"]?.jsonPrimitive?.content ?: "kaironex"
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        return "kaironex"
    }

    /**
     * Get the user's display name from profile.
     */
    fun getUserName(): String {
        val profile = _userState.value?.profile
        if (profile != null) {
            try {
                val profileJson = json.parseToJsonElement(profile).jsonObject
                return profileJson["name"]?.jsonPrimitive?.content ?: "Student"
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        return "Student"
    }

    /**
     * Get the agent name from profile.
     */
    fun getAgentName(): String {
        val profile = _userState.value?.profile
        if (profile != null) {
            try {
                val profileJson = json.parseToJsonElement(profile).jsonObject
                return profileJson["agent_name"]?.jsonPrimitive?.content ?: "Kairo"
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        return "Kairo"
    }

    // Extension to safely parse int from string
    private fun String.toIntOrNull(): Int? = try { this.toInt() } catch (e: Exception) { null }
    
    /**
     * Fetch dedicated Campaign State (Skill Tree, Quest Board, Armory).
     */
    private suspend fun fetchCampaignState() {
        try {
            val collectionId = "campaign_state"
            
            // First try to get by document ID (if userId is used as document ID)
            val directUrl = "${AppConfig.Appwrite.ENDPOINT}/databases/${AppConfig.Appwrite.DATABASE_ID}/collections/$collectionId/documents/$userId"

            val directResponse = httpClient.get(directUrl) {
                header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                if (AppConfig.Appwrite.API_KEY.isNotBlank()) {
                    header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
                }
            }

            if (directResponse.status.value == 200) {
                parseCampaignDocument(directResponse.bodyAsText())
                return
            }

            // Fallback: Query by userId field using proper Appwrite query format
            val url = "${AppConfig.Appwrite.ENDPOINT}/databases/${AppConfig.Appwrite.DATABASE_ID}/collections/$collectionId/documents"
            
            val response = httpClient.get(url) {
                header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                if (AppConfig.Appwrite.API_KEY.isNotBlank()) {
                    header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
                }
                // Appwrite REST API query format (v1.4+)
                parameter("queries[0]", """{"method":"equal","attribute":"userId","values":["$userId"]}""")
                parameter("queries[1]", """{"method":"limit","values":[1]}""")
            }

            if (response.status.value == 200) {
                val docJson = json.parseToJsonElement(response.bodyAsText()).jsonObject
                val documents = docJson["documents"]?.jsonArray
                
                documents?.firstOrNull()?.let { doc ->
                    parseCampaignDocument(doc.toString())
                }
            } else {
                val errorBody = response.bodyAsText()
                println("⚠️ Campaign State Error (${response.status}): $errorBody")
            }
        } catch (e: Exception) {
            println("❌ Fetch Campaign State Failed: ${e.message}")
        }
    }

    private fun parseCampaignDocument(documentJson: String) {
        try {
            val obj = json.parseToJsonElement(documentJson).jsonObject

            // Decode internal JSON strings
            val skillTreeStr = obj["skill_tree_json"]?.jsonPrimitive?.contentOrNull
            val questBoardStr = obj["quest_board_json"]?.jsonPrimitive?.contentOrNull
            val armoryStr = obj["the_armory_json"]?.jsonPrimitive?.contentOrNull
            val simulacrumStr = obj["simulacrum_data_json"]?.jsonPrimitive?.contentOrNull

            // Parse into data classes
            val skills = if (skillTreeStr != null) {
                try { json.decodeFromString<List<com.mursaline.kaironex.features.campaign.SkillNode>>(skillTreeStr) } catch(e:Exception) { emptyList() }
            } else emptyList()

            val quests = if (questBoardStr != null) {
                try { json.decodeFromString<List<com.mursaline.kaironex.features.campaign.Quest>>(questBoardStr) } catch(e:Exception) { emptyList() }
            } else emptyList()

            val armory = if (armoryStr != null) {
                try { json.decodeFromString<com.mursaline.kaironex.features.campaign.ArmoryState>(armoryStr) } catch(e:Exception) { com.mursaline.kaironex.features.campaign.ArmoryState() }
            } else com.mursaline.kaironex.features.campaign.ArmoryState()

            val simulacrum = if (simulacrumStr != null) {
                try { json.decodeFromString<com.mursaline.kaironex.features.campaign.SimulacrumState>(simulacrumStr) } catch(e:Exception) { com.mursaline.kaironex.features.campaign.SimulacrumState() }
            } else com.mursaline.kaironex.features.campaign.SimulacrumState()

            _campaignState.value = CampaignState(
                skillTree = skills,
                questBoard = quests,
                armory = armory,
                simulacrum = simulacrum
            )
        } catch (e: Exception) {
            println("❌ Parse Campaign Document Failed: ${e.message}")
        }
    }

    /**
     * Verify and Initialize all user state tables.
     * This implements the "Get or Create" pattern.
     */
    suspend fun initializeUserTables() {
        // 1. Campaign State
        ensureDocument(
            collectionId = "campaign_state", // AppConfig.Collections.CAMPAIGN_STATE
            defaultData = buildJsonObject {
                put("userId", userId)
                put("skill_tree_json", "[]")
                put("quest_board_json", "[]")
                put("the_armory_json", "{}")
                put("simulacrum_data_json", "{}")
            }
        )
        
        // 2. Vitality State - Using CORRECT schema columns
        ensureDocument(
            collectionId = "vitality_state", // AppConfig.Collections.VITALITY_STATE
            defaultData = buildJsonObject {
                put("userId", userId)
                // Correct columns from schema: bio_fuel_json, regen_mode_json, resource_monitor_json, bill_splitter_json
                put("bio_fuel_json", "{\"energy_level\":75,\"hydration\":\"good\",\"caffeine_intake\":0}")
                put("regen_mode_json", "{\"status\":\"active\",\"next_break_in\":45}")
                put("resource_monitor_json", "{}")
                put("bill_splitter_json", "{}")
            }
        )
        
        // 3. Radius State - Using CORRECT schema columns
        ensureDocument(
            collectionId = "radius_state", // AppConfig.Collections.RADIUS_STATE
            defaultData = buildJsonObject {
                put("userId", userId)
                put("user_location", "Unknown") // Required by Schema (string, not coords)
                // Correct columns: safehouse_json, local_scan_json, admin_protocol_json, signal_decoder_json, social_graph_json
                put("safehouse_json", "[]")
                put("local_scan_json", "{}")
                put("admin_protocol_json", "{}")
                put("signal_decoder_json", "{}")
                put("social_graph_json", "[]")
            }
        )
    }

    /**
     * Check if document exists, create if missing (Idempotent).
     */
    private suspend fun ensureDocument(collectionId: String, defaultData: kotlinx.serialization.json.JsonObject) {
        val url = "${AppConfig.Appwrite.ENDPOINT}/databases/${AppConfig.Appwrite.DATABASE_ID}/collections/$collectionId/documents/$userId"
        
        try {
            // 1. Check Existence (GET)
            val check = httpClient.get(url) {
                header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                if (AppConfig.Appwrite.API_KEY.isNotBlank()) header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
            }
            
            if (check.status.value == 200) {
                // Exists - Do nothing (Preserve data)
                return
            }
            
            // 2. Not Found - Create (POST)
            if (check.status.value == 404) {
                println("✨ Initializing missing table: $collectionId")
                
                val createUrl = "${AppConfig.Appwrite.ENDPOINT}/databases/${AppConfig.Appwrite.DATABASE_ID}/collections/$collectionId/documents"
                
                val payload = buildJsonObject {
                    put("documentId", userId) 
                    put("data", defaultData)
                }
                
                val create = httpClient.post(createUrl) {
                    header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                    if (AppConfig.Appwrite.API_KEY.isNotBlank()) header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
                    header("Content-Type", "application/json")
                    setBody(payload)
                }
                
                if (create.status.value !in 200..299) {
                     println("❌ Failed to init $collectionId: ${create.bodyAsText()}")
                }
            } else {
                 println("⚠️ Error checking $collectionId: ${check.status} - ${check.bodyAsText()}")
            }
        } catch (e: Exception) {
            println("❌ Error ensuring table $collectionId: ${e.message}")
        }
    }
}
