package com.mursaline.kaironex.brain

import com.mursaline.kaironex.core.CurrentUser
import com.mursaline.kaironex.core.stats.AppwriteStatsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*

/**
 * 🤖 AGENT CALL SERVICE
 * =====================
 * Enables autonomous agent-to-user communication.
 *
 * Agents proactively reach out to users based on:
 * - Backend brain decisions (polled via AppwriteBridge)
 * - Schedule-aware triggers (upcoming deadlines, study reminders)
 * - Vitality checks (break reminders, wellness)
 * - Campaign milestones (goal progress updates)
 *
 * This is the core differentiator: agents act WITHOUT human supervision.
 */
class AgentCallService(
    private val appwriteBridge: AppwriteBridge,
    private val brainClient: BrainApiClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null
    private var statsRepository: AppwriteStatsRepository? = null

    // =========================================================================
    // STATE
    // =========================================================================

    private val _incomingCall = MutableStateFlow<AgentCall?>(null)
    val incomingCall: StateFlow<AgentCall?> = _incomingCall.asStateFlow()

    private val _callHistory = MutableStateFlow<List<AgentCall>>(emptyList())
    val callHistory: StateFlow<List<AgentCall>> = _callHistory.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    // Track what we've already notified to avoid duplicates
    private val notifiedCallIds = mutableSetOf<String>()

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    fun start(statsRepo: AppwriteStatsRepository?) {
        if (_isActive.value) return
        _isActive.value = true
        statsRepository = statsRepo

        // Start the autonomous polling loop
        pollingJob = scope.launch {
            println("🤖 AgentCallService: Started autonomous monitoring")

            // HACKATHON DEMO: Trigger a call after 15 seconds for demo purposes
            // delay(15_000L)
            // triggerDemoCall()

            while (isActive) {
                try {
                    checkForAgentCalls()
                } catch (e: Exception) {
                    println("⚠️ AgentCallService poll error: ${e.message}")
                }
                delay(POLL_INTERVAL_MS)
            }
        }

        // Start schedule-aware triggers
        scope.launch {
            while (isActive) {
                try {
                    checkScheduleTriggers()
                } catch (_: Exception) { }
                delay(SCHEDULE_CHECK_INTERVAL_MS)
            }
        }
    }

    /**
     * Demo call for hackathon - fires 15 seconds after app start.
     * This shows judges the agent calling feature immediately.
     */
    private fun triggerDemoCall() {
        val callId = "demo_vitality_${System.currentTimeMillis()}"
        if (!notifiedCallIds.contains("demo_call_triggered")) {
            notifiedCallIds.add("demo_call_triggered")
            emitCall(
                AgentCall(
                    id = callId,
                    agent = AgentType.VITALITY,
                    reason = "Hey! I noticed you just started your session. Let me help you plan your study goals for today.",
                    priority = AgentCallPriority.NORMAL,
                    source = AgentCallSource.WELLNESS_CHECK,
                    suggestedAction = "Start planning session"
                )
            )
        }
    }

    fun stop() {
        _isActive.value = false
        pollingJob?.cancel()
        pollingJob = null
        println("🤖 AgentCallService: Stopped")
    }

    // =========================================================================
    // CALL MANAGEMENT
    // =========================================================================

    /**
     * Accept an incoming agent call - opens the Orb/voice session.
     */
    fun acceptCall(callId: String) {
        val call = _incomingCall.value
        if (call?.id == callId) {
            _incomingCall.value = call.copy(status = AgentCallStatus.ACCEPTED)
            _callHistory.value = _callHistory.value + call.copy(status = AgentCallStatus.ACCEPTED)
            println("✅ Agent call accepted: ${call.agent} - ${call.reason}")

            // After a brief delay, clear the incoming call so the UI transitions
            scope.launch {
                delay(500)
                _incomingCall.value = null
            }
        }
    }

    /**
     * Dismiss/snooze an incoming agent call.
     */
    fun dismissCall(callId: String) {
        val call = _incomingCall.value
        if (call?.id == callId) {
            _incomingCall.value = null
            _callHistory.value = _callHistory.value + call.copy(status = AgentCallStatus.DISMISSED)
            println("❌ Agent call dismissed: ${call.agent}")
        }
    }

    // =========================================================================
    // BRAIN POLLING (Backend-Initiated Calls)
    // =========================================================================

    /**
     * Poll the backend brain for any pending agent-initiated actions.
     * The backend may have decided the student needs attention based on:
     * - Marathon agent completing a task
     * - Schedule conflict detected
     * - Deadline approaching with no activity
     * - Wellness check trigger
     */
    private suspend fun checkForAgentCalls() {
        val userId = CurrentUser.userId.ifBlank { return }

        // Poll thought stream for intervention-type thoughts
        val repo = statsRepository ?: return
        val latestThought = repo.getLatestThought()

        if (latestThought != null && !notifiedCallIds.contains(latestThought.agent + latestThought.thought.hashCode())) {
            // Check if this thought is actionable (agent wants to communicate)
            val isIntervention = latestThought.thought.let { t ->
                t.contains("remind", ignoreCase = true) ||
                t.contains("alert", ignoreCase = true) ||
                t.contains("check in", ignoreCase = true) ||
                t.contains("deadline", ignoreCase = true) ||
                t.contains("break", ignoreCase = true) ||
                t.contains("help", ignoreCase = true) ||
                t.contains("suggest", ignoreCase = true) ||
                t.contains("concern", ignoreCase = true) ||
                t.contains("schedule", ignoreCase = true) ||
                latestThought.confidence > 0.8f
            }

            if (isIntervention) {
                val callId = "${latestThought.agent}_${latestThought.thought.hashCode()}"
                notifiedCallIds.add(callId)

                val agentType = mapAgentToType(latestThought.agent)
                emitCall(
                    AgentCall(
                        id = callId,
                        agent = agentType,
                        reason = latestThought.thought.take(120),
                        priority = if (latestThought.confidence > 0.9f) AgentCallPriority.URGENT else AgentCallPriority.NORMAL,
                        source = AgentCallSource.BRAIN_DECISION,
                        context = mapOf(
                            "agent" to latestThought.agent,
                            "confidence" to latestThought.confidence.toString()
                        )
                    )
                )
            }
        }

        // Also check brain events for intervention type
        // The brainClient.brainEvents flow emits intervention events when connected
    }

    // =========================================================================
    // SCHEDULE-AWARE TRIGGERS (Local Intelligence)
    // =========================================================================

    /**
     * Smart local triggers that don't require backend connectivity.
     * These make agents feel "always on" even offline.
     */
    private suspend fun checkScheduleTriggers() {
        val userId = CurrentUser.userId.ifBlank { return }
        val repo = statsRepository ?: return

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = now.hour

        // Vitality Agent: Morning check-in (8-9 AM)
        if (hour in 8..9) {
            val callId = "vitality_morning_${now.date}"
            if (!notifiedCallIds.contains(callId)) {
                notifiedCallIds.add(callId)
                emitCall(
                    AgentCall(
                        id = callId,
                        agent = AgentType.VITALITY,
                        reason = "Good morning! Let's set up your day for success. How are you feeling?",
                        priority = AgentCallPriority.LOW,
                        source = AgentCallSource.SCHEDULE_TRIGGER,
                        suggestedAction = "Start daily planning session"
                    )
                )
            }
        }

        // Study Agent: Study session reminder (based on schedule)
        if (hour in 14..15) {
            val callId = "study_afternoon_${now.date}"
            if (!notifiedCallIds.contains(callId)) {
                notifiedCallIds.add(callId)
                emitCall(
                    AgentCall(
                        id = callId,
                        agent = AgentType.STUDY,
                        reason = "Your study window is open. Ready to dive into your next topic?",
                        priority = AgentCallPriority.NORMAL,
                        source = AgentCallSource.SCHEDULE_TRIGGER,
                        suggestedAction = "Open study room"
                    )
                )
            }
        }

        // Campaign Agent: Evening reflection (8-9 PM)
        if (hour in 20..21) {
            val callId = "campaign_evening_${now.date}"
            if (!notifiedCallIds.contains(callId)) {
                notifiedCallIds.add(callId)
                emitCall(
                    AgentCall(
                        id = callId,
                        agent = AgentType.CAMPAIGN,
                        reason = "Let's review today's progress and plan for tomorrow.",
                        priority = AgentCallPriority.LOW,
                        source = AgentCallSource.SCHEDULE_TRIGGER,
                        suggestedAction = "Review daily progress"
                    )
                )
            }
        }

        // Vitality Agent: Break reminder (every 90 min during study hours)
        if (hour in 9..22) {
            val blockId = hour / 2 // 2-hour blocks
            val callId = "vitality_break_${now.date}_b$blockId"
            if (!notifiedCallIds.contains(callId)) {
                notifiedCallIds.add(callId)
                // Only trigger if user has been "active" (we can check via stats)
                emitCall(
                    AgentCall(
                        id = callId,
                        agent = AgentType.VITALITY,
                        reason = "You've been at it for a while. A 5-minute break can boost focus by 30%.",
                        priority = AgentCallPriority.LOW,
                        source = AgentCallSource.WELLNESS_CHECK,
                        suggestedAction = "Take a guided break"
                    )
                )
            }
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private fun emitCall(call: AgentCall) {
        // Only emit if there's no active call (don't stack calls)
        if (_incomingCall.value == null || _incomingCall.value?.status == AgentCallStatus.DISMISSED) {
            _incomingCall.value = call
            println("📞 Agent calling user: [${call.agent.label}] ${call.reason}")
        } else {
            // Queue it in history for later
            _callHistory.value = _callHistory.value + call.copy(status = AgentCallStatus.QUEUED)
        }
    }

    private fun mapAgentToType(agentName: String): AgentType {
        return when {
            agentName.contains("study", true) -> AgentType.STUDY
            agentName.contains("vital", true) -> AgentType.VITALITY
            agentName.contains("campaign", true) -> AgentType.CAMPAIGN
            agentName.contains("radius", true) -> AgentType.RADIUS
            agentName.contains("super", true) -> AgentType.SUPERVISOR
            else -> AgentType.SUPERVISOR
        }
    }

    /**
     * Manually trigger an agent call (e.g., from marathon completion).
     */
    fun triggerAgentCall(agent: AgentType, reason: String, priority: AgentCallPriority = AgentCallPriority.NORMAL) {
        val callId = "${agent.name}_manual_${System.currentTimeMillis()}"
        emitCall(
            AgentCall(
                id = callId,
                agent = agent,
                reason = reason,
                priority = priority,
                source = AgentCallSource.BRAIN_DECISION
            )
        )
    }

    companion object {
        /** Poll backend every 30 seconds for agent decisions */
        const val POLL_INTERVAL_MS = 30_000L
        /** Check schedule triggers every 5 minutes */
        const val SCHEDULE_CHECK_INTERVAL_MS = 300_000L
    }
}

// =========================================================================
// DATA MODELS
// =========================================================================

data class AgentCall(
    val id: String,
    val agent: AgentType,
    val reason: String,
    val priority: AgentCallPriority = AgentCallPriority.NORMAL,
    val source: AgentCallSource = AgentCallSource.BRAIN_DECISION,
    val status: AgentCallStatus = AgentCallStatus.RINGING,
    val suggestedAction: String? = null,
    val context: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class AgentType(val label: String, val emoji: String, val description: String) {
    STUDY("Study Agent", "📚", "Academic scheduling, resources, and study optimization"),
    VITALITY("Vitality Agent", "💚", "Wellness, breaks, sleep, and energy management"),
    CAMPAIGN("Campaign Agent", "🎯", "Goals, long-term planning, and milestone tracking"),
    RADIUS("Radius Agent", "🌐", "Social connections, networking, and collaboration"),
    SUPERVISOR("Supervisor", "🧠", "Orchestrates all agents and handles complex requests")
}

enum class AgentCallPriority {
    LOW,      // Can be snoozed
    NORMAL,   // Standard notification
    URGENT    // Immediate attention (deadline, crisis)
}

enum class AgentCallSource {
    BRAIN_DECISION,    // Backend brain decided to call
    SCHEDULE_TRIGGER,  // Schedule-based automatic trigger
    WELLNESS_CHECK,    // Vitality agent wellness check
    MARATHON_UPDATE,   // Marathon agent progress/completion
    USER_REQUEST       // User explicitly asked agent to call back
}

enum class AgentCallStatus {
    RINGING,    // Awaiting user response
    ACCEPTED,   // User picked up
    DISMISSED,  // User snoozed/declined
    QUEUED,     // Waiting (another call is active)
    COMPLETED   // Call finished
}
