package com.mursaline.kaironex.features.agents

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mursaline.kaironex.core.stats.MoreStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class AgentsViewModel : ScreenModel {

    private val _stats = MutableStateFlow<MoreStats?>(null)
    val stats: StateFlow<MoreStats?> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAgentStats()
    }

    fun loadAgentStats() {
        screenModelScope.launch {
            _isLoading.value = true
            // TODO: Connect to Appwrite Backend to fetch real agent states (Vitality, Campaign, Radius).
            // For now, we simulate a network call that returns NULL or partial real data.
            // As per user request: "show loading until original data comes".
            // Since we don't have the backend Aggregator API yet, we will stay in Loading or Empty state
            // rather than showing fake numbers.
            
            delay(2000) // Simulate network latency
            
            // In a real implementation:
            // val result = repository.fetchAgentStats()
            // _stats.value = result
            
            // For now, allow UI to handle "No Data" gracefully or keep loading if strictly required.
            // We'll set loading to false but stats to null to test "Error/Empty" state, 
            // OR keep loading true if we strictly want "Spinning until data comes".
            // The user said: "show loading or speaning until original data comes".
            // So if original data never comes (backend not ready), we spin.
            
            // UNCOMMENT BELOW when Backend is ready.
            // _isLoading.value = false
        }
    }
}
