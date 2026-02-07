package com.mursaline.kaironex.features.study

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MonthlyPlansViewModel(
    private val repository: MonthlyPlansRepository
) : ScreenModel {

    private val _plans = MutableStateFlow<List<MonthlyPlansRepository.MonthlyPlan>>(emptyList())
    val plans = _plans.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadPlans()
    }

    fun loadPlans() {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                _plans.value = repository.fetchMonthlyPlans()
            } catch (e: Exception) {
                println("Failed to load monthly plans: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
