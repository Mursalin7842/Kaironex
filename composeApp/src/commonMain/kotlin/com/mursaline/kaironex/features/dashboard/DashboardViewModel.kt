package com.mursaline.kaironex.features.dashboard

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mursaline.kaironex.core.stats.HomeStats
import com.mursaline.kaironex.core.stats.StatsProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: com.mursaline.kaironex.core.stats.AppwriteStatsRepository
) : ScreenModel {

    val homeStats = repository.homeStats
    val isLoading = repository.isLoading

    init {
        screenModelScope.launch {
            // Trigger refresh to replace EMPTY with real data
            repository.refreshHomeStats() 
        }
    }
}
