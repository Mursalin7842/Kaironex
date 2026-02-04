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
    private val fileRepository: FileRepository
) : ScreenModel {

    private val _resources = MutableStateFlow<List<Resource>>(emptyList())
    val resources = _resources.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadResources()
    }

    fun loadResources() {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                val fetched = fileRepository.fetchResources()
                _resources.value = fetched.map { 
                     // Determine type based on mime type or extension if available, 
                     // but FileRepository.ResourceModel currently defaults type="PDF"
                     // We can improve this mapping later.
                     Resource(
                         id = it.id,
                         title = it.title,
                         type = ResourceType.PDF, // Default for now
                         duration = "Doc" // Placeholder
                     )
                }
            } catch (e: Exception) {
                // Handle error
                println("ViewModel Load Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
