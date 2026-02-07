package com.mursaline.kaironex.features.study

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mursaline.kaironex.features.resources.FileRepository
import com.mursaline.kaironex.features.study.components.Resource
import com.mursaline.kaironex.features.study.components.ResourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudyViewModel(
    private val fileRepository: FileRepository,
    private val scheduleRepository: ScheduleRepository,
    private val brainApiClient: com.mursaline.kaironex.brain.BrainApiClient,
    private val sessionManager: com.mursaline.kaironex.core.KaironexSessionManager
) : ScreenModel {

    private val _resources = MutableStateFlow<List<Resource>>(emptyList())
    val resources = _resources.asStateFlow()
    
    private val _schedule = MutableStateFlow<List<ScheduleRepository.ScheduleTask>>(emptyList())
    val schedule = _schedule.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    init {
        loadResources()
        loadSchedule()
    }
    
    // ... loadResources and loadSchedule remain same ...
    
    fun loadResources() {
        screenModelScope.launch {
            try {
                val fetched = fileRepository.fetchResources()
                _resources.value = fetched.map { 
                     Resource(
                         id = it.id,
                         title = it.title,
                         type = ResourceType.PDF,
                         duration = "Doc"
                     )
                }
            } catch (e: Exception) {
                // Handle error
                println("ViewModel Load Error: ${e.message}")
            }
        }
    }
    
    fun loadSchedule(force: Boolean = false) {
        screenModelScope.launch {
            try {
                _schedule.value = scheduleRepository.fetchSchedule(forceRefresh = force)
            } catch (e: Exception) {
                println("Schedule Load Error: ${e.message}")
            }
        }
    }
    
    fun uploadFiles(files: List<io.github.vinceglb.filekit.core.PlatformFile>) {
        screenModelScope.launch {
            _isUploading.value = true
            try {
                for (file in files) {
                    val success = fileRepository.uploadFile(file)
                    if (success) {
                        println("Uploaded: ${file.name}")
                        // Trigger ingestion event via brain if needed, 
                        // but db_helper logs simple heartbeats. 
                        // The resource_ingestion event actually happens via backend watching OR can be manual triggers.
                        // For now we rely on the backend seeing the new Resource record? 
                        // Wait, study_agent `_handle_resource_ingestion` is triggered how?
                        // Ah, we need to trigger the brain event manually if we want IMMEDIATE analysis.
                        
                        // Let's assume FileRepository merely saves storage/record.
                        // We can optionally trigger brain analysis here:
                        // brainApiClient.trigger("resource_ingestion", ...) 
                        // But for now let's just refresh list.
                    }
                }
                loadResources()
            } catch (e: Exception) {
                println("Upload Error: ${e.message}")
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun generateSchedule(resourceIds: List<String>, days: Int) {
        screenModelScope.launch {
            _isGenerating.value = true
            try {
                val userId = sessionManager.userId.value ?: return@launch
                
                val ids = kotlinx.serialization.json.JsonArray(
                    resourceIds.map { kotlinx.serialization.json.JsonPrimitive(it) }
                )
                
                brainApiClient.triggerBrain(
                    userId = userId,
                    eventType = "schedule_request",
                    data = mapOf(
                        "duration_days" to days,
                        "resource_ids" to ids 
                    )
                )
                
                kotlinx.coroutines.delay(5000) 
                loadSchedule()
                
            } catch (e: Exception) {
                println("Generate Schedule Error: ${e.message}")
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun triggerInitialScheduleGeneration() {
        screenModelScope.launch {
            try {
                val userId = sessionManager.userId.value ?: return@launch
                
                // Trigger Study Agent to generate the semester schedule based on uploaded files
                brainApiClient.triggerBrain(
                    userId = userId,
                    eventType = "schedule_request",
                    data = mapOf(
                        "trigger" to "onboarding_complete",
                        "duration_days" to 120 // Default Semester Length
                    )
                )
                println("Brain Triggered: Initial Schedule Generation initiated.")
                
            } catch (e: Exception) {
                println("Study Trigger Error: ${e.message}")
            }
        }
    }
}
