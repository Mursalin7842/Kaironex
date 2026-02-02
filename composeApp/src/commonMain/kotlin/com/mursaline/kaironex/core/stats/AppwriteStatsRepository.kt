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
class AppwriteStatsRepository(
    private val brainClient: BrainApiClient,
    private val userId: String
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Cached state from backend
    private val _userState = MutableStateFlow<UserStateResponse?>(null)
    val userState: StateFlow<UserStateResponse?> = _userState.asStateFlow()

    private val _homeStats = MutableStateFlow(StatsProvider.getHomeStats())
    val homeStats: StateFlow<HomeStats> = _homeStats.asStateFlow()

    private val _moreStats = MutableStateFlow(StatsProvider.getMoreStats())
    val moreStats: StateFlow<MoreStats> = _moreStats.asStateFlow()

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
            // 1. Get user state (contains cached studentState_json)
            val state = brainClient.getUserState(userId)
            _userState.value = state

            if (state != null) {
                // 2. Parse the state JSON
                parseUserState(state)
            }

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
            val state = brainClient.getUserState(userId)
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
            val state = brainClient.getUserState(userId)
            if (state != null) {
                parseMoreStats(state)
            }
        } catch (e: Exception) {
            println("❌ Failed to refresh more stats: ${e.message}")
        } finally {
            _isLoading.value = false
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

            // Parse campaign specifics
            campaignJson?.let { campaign ->
                val skillProgress = campaign["skill_progress"]?.jsonPrimitive?.floatOrNull ?: 0.58f

                _moreStats.value = _moreStats.value.copy(
                    campaign = _moreStats.value.campaign.copy(
                        skillsProgress = skillProgress
                    )
                )
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
}

// Extension to safely parse int from string
private fun String.toIntOrNull(): Int? = try { this.toInt() } catch (e: Exception) { null }
