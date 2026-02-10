package com.mursaline.kaironex.features.zones.radius

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mursaline.kaironex.core.stats.AppwriteStatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 📡 RADIUS VIEW MODEL
 * =====================
 * Provides real data from AppwriteStatsRepository to 
 * the Radius Dashboard screen.
 */
class RadiusViewModel(
    private val repository: AppwriteStatsRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(RadiusUiState())
    val uiState: StateFlow<RadiusUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        collectRadiusStats()
    }

    private fun collectRadiusStats() {
        screenModelScope.launch {
            repository.moreStats.collect { moreStats ->
                val stats = moreStats.radius

                _uiState.value = _uiState.value.copy(
                    // Visa & Admin
                    visaDaysRemaining = stats.visaDaysRemaining ?: 365,
                    adminAlerts = stats.riskAlerts,
                    scamAlerts = stats.scamRiskAlerts,

                    // Housing
                    housingStabilityScore = stats.housingStabilityScore,
                    utilityReadiness = stats.utilityReadiness,

                    // Cultural
                    languageFluencyScore = stats.languageFluencyScore,
                    termsLearned = (stats.languageFluencyScore * 0.5).toInt(), // Approximate

                    // Local
                    safeZoneAwareness = stats.safeZoneAwareness,
                    localKnowledgeScore = stats.localKnowledgeScore,

                    // Social
                    socialInteractionCount = stats.socialInteractionCount,
                    culturalComfort = stats.culturalComfort,

                    // Calibration
                    isCalibrated = stats.housingStabilityScore > 0 || stats.visaDaysRemaining != null
                )

                _isLoading.value = false
            }
        }
    }
}
