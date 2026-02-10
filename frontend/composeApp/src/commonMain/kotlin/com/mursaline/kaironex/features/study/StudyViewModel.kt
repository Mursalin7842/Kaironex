package com.mursaline.kaironex.features.study

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mursaline.kaironex.core.CurrentUser
import com.mursaline.kaironex.features.resources.FileRepository
import com.mursaline.kaironex.features.study.components.Resource
import com.mursaline.kaironex.features.study.components.ResourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.*


/**
 * StudyViewModel - Manages study sessions, schedule, and resources
 *
 * Connects to Kaironex-Brain backend via:
 * - schedule_request → Generates semester strategy + daily tasks
 * - session_start/end → Tracks active study sessions
 * - content_request → Gets JIT content for study room
 */
class StudyViewModel(
    private val fileRepository: FileRepository,
    private val scheduleRepository: ScheduleRepository,
    private val brainApiClient: com.mursaline.kaironex.brain.BrainApiClient
) : ScreenModel {

    // User ID - always use demo_user_001 for hackathon
    private val userId: String
        get() = CurrentUser.userId.ifBlank { "demo_user_001" }

    // Resources (uploaded academic files)
    private val _resources = MutableStateFlow<List<Resource>>(emptyList())
    val resources = _resources.asStateFlow()
    
    // Schedule (tasks from backend)
    private val _schedule = MutableStateFlow<List<ScheduleRepository.ScheduleTask>>(emptyList())
    val schedule = _schedule.asStateFlow()
    
    // Active session state
    private val _activeSession = MutableStateFlow<ActiveStudySession?>(null)
    val activeSession = _activeSession.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Today's tasks (filtered from schedule)
    private val _todayTasks = MutableStateFlow<List<ScheduleRepository.ScheduleTask>>(emptyList())
    val todayTasks = _todayTasks.asStateFlow()

    // Current Study Content (Rich AI Content - NEW)
    private val _studyResources = MutableStateFlow<List<StudyResource>>(emptyList())
    val studyResources = _studyResources.asStateFlow()
    
    private val _flashCards = MutableStateFlow<List<FlashCard>>(emptyList())
    val flashCards = _flashCards.asStateFlow()
    
    private val _gatekeeperQuiz = MutableStateFlow<GatekeeperQuiz?>(null)
    val gatekeeperQuiz = _gatekeeperQuiz.asStateFlow()
    

    
    // Quiz State
    private val _isQuizActive = MutableStateFlow(false)
    val isQuizActive = _isQuizActive.asStateFlow()

    init {
        loadResources()
        loadSchedule()
    }

    fun loadResources() {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                val repoFiles = fileRepository.fetchResources()
                _resources.value = repoFiles.map { model ->
                    Resource(
                         id = model.id,
                         title = model.title,
                         type = when (model.type) {
                             "PDF" -> ResourceType.PDF
                             "VIDEO" -> ResourceType.VIDEO
                             "LINK" -> ResourceType.LINK
                             else -> ResourceType.PDF
                         },
                         url = model.driveLink,
                         summary = model.summary,
                         dateAdded = 0L // Repo doesn't return date yet
                    )
                }
            } catch (e: Exception) {
                println("Error loading resources: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSchedule(force: Boolean = false) {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                _schedule.value = scheduleRepository.fetchSchedule(forceRefresh = force)
            } catch (e: Exception) {
                println("Error loading schedule: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadFiles(files: List<io.github.vinceglb.filekit.core.PlatformFile>) {
        screenModelScope.launch {
            _isUploading.value = true
            try {
                files.forEach { file ->
                    fileRepository.uploadFile(file)
                }
                loadResources()
            } catch (e: Exception) {
                _error.value = "Failed to upload files: ${e.message}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun triggerInitialScheduleGeneration() {
        screenModelScope.launch {
            _isGenerating.value = true
            try {
                brainApiClient.triggerBrain(
                    userId = userId,
                    eventType = "schedule_request",
                    data = mapOf("mode" to "initial_setup")
                )
                // Poll for updates or wait for push
                loadSchedule(force = true)
            } catch (e: Exception) {
                _error.value = "Failed to generate schedule: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    // ... items 111-186 omitted ...

    /**
     * Start a study session
     * Triggers: Study Agent → session_start event
     */
    /**
     * Start a study session
     * Triggers: Study Agent → session_start event
     * 
     * Requirement:
     * 1. Only access TODAY's tasks (filtering out future/past if not explicitly selected)
     * 2. If current task has no content (resources/flashcards/quiz), fallback to the LAST available task that had data.
     */
    fun startStudySession(task: ScheduleRepository.ScheduleTask? = null) {
        screenModelScope.launch {
            try {
                val sessionId = "session_${System.currentTimeMillis()}"

                // 1. Task Selection Logic
                // If a task is explicitly provided, verify it is for TODAY.
                // Otherwise, find the best candidate from TODAY'S schedule.
                var currentTask = task

                // Get Today's Date String (matches ScheduleRepository format)
                val today = kotlinx.datetime.Clock.System.now()
                    .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                    .date.toString()

                if (currentTask != null) {
                    if (!currentTask.startTime.startsWith(today)) {
                        println("⚠️ Requested task ${currentTask.id} is not for TODAY ($today). Ignoring request.")
                        currentTask = null
                    }
                }

                if (currentTask == null) {
                    println("📋 No valid task provided. Looking for available tasks in TODAY's schedule...")
                    
                    // Allow a refresh if schedule is empty
                    val currentSchedule = _schedule.value.ifEmpty {
                         scheduleRepository.fetchSchedule(forceRefresh = true).also {
                             _schedule.value = it
                         }
                    }

                    // Filter: Only Today's tasks that are not completed
                    val todaysTasks = currentSchedule.filter { 
                        it.startTime.startsWith(today) && it.status.lowercase() != "completed" 
                    }

                    // Sort by time just in case
                    currentTask = todaysTasks.minByOrNull { it.startTime }

                    if (currentTask != null) {
                        println("✅ Auto-selected Today's task: ${currentTask.title} (${currentTask.id})")
                    } else {
                        println("⚠️ No available tasks found for TODAY.")
                        // Optional: Could fall back to ANY pending task if strict mode is relaxed, 
                        // but user asked for "only today's task".
                    }
                } else {
                     // Verify the manually provided task is also for today? 
                     // User said "show or accisable by only todays task".
                     // If the user clicked a task from "Upcoming" tab (if it exists), we might want to block it?
                     // For now, assuming navigation handles access control, but we will respect the "Current Focus" flow.
                }

                // 2. Content Loading & Fallback Logic
                // We need resources, flashcards, and quizzes.
                // If the valid 'currentTask' lacks these, we look backwards in history for the "last available data".

                _studyResources.value = emptyList()
                _flashCards.value = emptyList()
                _gatekeeperQuiz.value = null
                _isQuizActive.value = false
                
                // Helper to check if task has ANY content
                fun hasContent(t: ScheduleRepository.ScheduleTask?): Boolean {
                    if (t == null) return false
                    return !t.justInTimeResources.isNullOrBlank() || 
                           !t.flashCards.isNullOrBlank() || 
                           !t.macroQuizes.isNullOrBlank()
                }

                // A. Check if current task has content
                var contentSourceTask = currentTask
                var isFallback = false

                if (!hasContent(currentTask)) {
                    // Try fetching fresh data first
                    if (currentTask != null) {
                        println("🔄 Content missing. Checking fresh data for ${currentTask.id}...")
                        val fresh = scheduleRepository.fetchTask(currentTask.id)
                        if (hasContent(fresh)) {
                            currentTask = fresh // Update main task ref if we got fresh content
                            contentSourceTask = fresh
                        } else {
                            // B. Fallback Search
                            // Look for the most recent previous task that has content
                            println("🕵️‍♀️ Current task empty. Searching backwards for fallback content...")
                            val allTasks = _schedule.value.sortedByDescending { it.startTime } // Newest first
                            
                            // Find first task with content that isn't the current one (already checked)
                            val fallback = allTasks.firstOrNull { hasContent(it) && it.id != currentTask?.id }
                            
                            if (fallback != null) {
                                println("🔙 Found fallback content from: ${fallback.title} (${fallback.id})")
                                contentSourceTask = fallback
                                isFallback = true
                            } else {
                                println("⚠️ No fallback content found in recent history.")
                            }
                        }
                    }
                }

                // Set active session with CURRENT TASK info (title, timer, etc.)
                // But use CONTENT from contentSourceTask
                _activeSession.value = ActiveStudySession(
                    sessionId = sessionId,
                    taskId = currentTask?.id,
                    subject = currentTask?.subject ?: "General Study",
                    title = currentTask?.title ?: "Study Session",
                    startTime = System.currentTimeMillis(),
                    topics = currentTask?.topics
                )

                // Load Content from Source
                if (contentSourceTask != null) {
                    println("🧠 Loading Content from: ${if(isFallback) "FALLBACK" else "CURRENT"} (${contentSourceTask.title})")
                    
                    // 1. Resources
                    val loadedResources = scheduleRepository.parseResources(contentSourceTask)
                    // If fallback, maybe add a disclaimer resource?
                    _studyResources.value = if (isFallback) {
                        val notice = StudyResource(
                            id = "fallback_notice",
                            name = "Using content from: ${contentSourceTask.title}",
                            type = "note",
                            content = "Content for the current task was not available, so we loaded materials from a previous session to keep you studying.",
                            estimatedReadTime = 1
                        )
                        listOf(notice) + loadedResources
                    } else {
                        loadedResources
                    }
                    
                    // 2. Flashcards
                    _flashCards.value = scheduleRepository.parseFlashCards(contentSourceTask)
                    
                    // 3. Quiz
                    _gatekeeperQuiz.value = scheduleRepository.parseGatekeeperQuiz(contentSourceTask)
                    
                    println("📊 Content Status: ${_studyResources.value.size} res, ${_flashCards.value.size} cards, Quiz=${_gatekeeperQuiz.value != null}")
                }

                // Mark CURRENT task as in progress (even if using fallback content)
                if (currentTask != null) {
                    scheduleRepository.startTask(currentTask.id)
                }

                // Notify backend
                brainApiClient.triggerBrain(
                    userId = userId,
                    eventType = "session_start",
                    data = mapOf(
                        "session_id" to sessionId,
                        "task_id" to (currentTask?.id ?: ""),
                        "subject" to (currentTask?.subject ?: "General"),
                        "content_mode" to (currentTask?.contentMode ?: "deep_dive"),
                        "using_fallback_content" to isFallback
                    )
                )

                println("📖 Study session started: $sessionId")
            } catch (e: Exception) {
                println("❌ Start Session Error: ${e.message}")
            }
        }
    }

    /**
     * Submit Gatekeeper Quiz Result
     */
    fun submitQuiz(passed: Boolean, score: Float, questionResults: List<QuestionResult>) {
        screenModelScope.launch {
            val session = _activeSession.value ?: return@launch
            val taskId = session.taskId ?: return@launch
            
            // 1. Save result to backend
            val result = QuizResult(
                attemptNumber = 1, // TODO: Track attempts
                timestamp = kotlinx.datetime.Clock.System.now().toString(),
                passed = passed,
                scorePercent = score,
                pointsEarned = (score * (_gatekeeperQuiz.value?.totalPoints ?: 0)).toInt(),
                totalPoints = _gatekeeperQuiz.value?.totalPoints ?: 0,
                timeTakenSeconds = 0, // TODO: Track time
                questionResults = questionResults,
                weakTopics = emptyList() // TODO: Analyze weak topics
            )
            
            scheduleRepository.saveQuizResult(taskId, result)
            
            // 2. Determine next steps
            if (passed) {
                // Allow proceeding to next component or day
                println("✅ Quiz Passed! Unlocking next content.")
            } else {
                println("❌ Quiz Failed. Remediation needed.")
            }
        }
    }
    
    // ... items 329-483 omitted ...

    /**
     * End the current study session
     * Triggers: Study Agent → session_end event
     */
    fun endStudySession(focusScore: Float = 0.7f, markCompleted: Boolean = true) {
        screenModelScope.launch {
            val session = _activeSession.value ?: return@launch

            try {
                val durationMinutes = ((System.currentTimeMillis() - session.startTime) / 60000).toInt()

                // Mark task as completed if applicable
                if (markCompleted && session.taskId != null) {
                    scheduleRepository.completeTask(session.taskId)
                }

                // Notify backend
                brainApiClient.triggerBrain(
                    userId = userId,
                    eventType = "session_end",
                    data = mapOf(
                        "session_id" to session.sessionId,
                        "task_id" to (session.taskId ?: ""),
                        "duration_minutes" to durationMinutes,
                        "focus_score" to focusScore,
                        "completed" to markCompleted
                    )
                )

                println("✅ Study session ended: ${session.sessionId} (${durationMinutes}min)")

                _activeSession.value = null

                // Refresh schedule to update task status
                loadSchedule(force = true)

            } catch (e: Exception) {
                println("❌ End Session Error: ${e.message}")
            }
        }
    }





    /**
     * Request quiz for mastery check
     */
    fun requestQuiz(topic: String, numQuestions: Int = 3) {
        screenModelScope.launch {
            try {
                brainApiClient.triggerBrain(
                    userId = userId,
                    eventType = "quiz_request",
                    data = mapOf(
                        "topic" to topic,
                        "num_questions" to numQuestions,
                        "difficulty" to "adaptive"
                    )
                )
                println("📝 Quiz requested for topic: $topic")
            } catch (e: Exception) {
                println("❌ Quiz Request Error: ${e.message}")
            }
        }
    }

    /**
     * Request a schedule change
     */
    fun requestScheduleChange(taskId: String, reason: String) {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                println("📩 Requesting schedule change for task $taskId: $reason")
                
                brainApiClient.triggerBrain(
                    userId = userId,
                    eventType = "schedule_change_request",
                    data = mapOf(
                        "taskId" to taskId,
                        "reason" to reason
                    )
                )
                
                // Optimistic UI update or just wait for refresh?
                // For now, let's just log success. The brain will handle the rest.
                println("✅ Change request sent.")
                
            } catch (e: Exception) {
                println("❌ Schedule Change Request Error: ${e.message}")
                _error.value = "Failed to send request"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setQuizActive(active: Boolean) {
        _isQuizActive.value = active
    }


}

/**
 * Active study session data
 */
data class ActiveStudySession(
    val sessionId: String,
    val taskId: String?,
    val subject: String,
    val title: String,
    val startTime: Long,
    val topics: String? = null
)
