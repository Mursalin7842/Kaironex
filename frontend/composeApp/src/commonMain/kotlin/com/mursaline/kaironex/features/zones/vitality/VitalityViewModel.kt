package com.mursaline.kaironex.features.zones.vitality

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mursaline.kaironex.core.stats.AppwriteStatsRepository
import com.mursaline.kaironex.core.stats.VitalityStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.Clock

class VitalityViewModel(
    private val repository: AppwriteStatsRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(VitalityUiState())
    val uiState: StateFlow<VitalityUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        collectVitalityStats()
        loadMealPlan()
    }

    private fun loadMealPlan() {
         screenModelScope.launch {
             val plan = repository.getTodayMealPlan()
             _uiState.value = _uiState.value.copy(mealPlan = plan)
         }
    }

    private fun collectVitalityStats() {
        screenModelScope.launch {
            repository.moreStats.collect { moreStats ->
                val stats = moreStats.vitality
                
                // Calculate Days to Payday
                val daysToPayday = try {
                    if (stats.nextPayday != null) {
                        val today = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                        val payday = kotlinx.datetime.LocalDate.parse(stats.nextPayday)
                        payday.toEpochDays() - today.toEpochDays()
                    } else 0
                } catch (e: Exception) { 0 }

                // Calculate Daily Budget
                val disposable = stats.totalBalance - stats.monthlyBills
                val daysCalc = if (daysToPayday > 0) daysToPayday else 30
                val dailyBudget = (disposable / daysCalc).coerceAtLeast(0.0)

                // Defcon Logic
                val (defcon, label) = when {
                    stats.budgetRunwayDays < 7 && stats.budgetRunwayDays > 0 -> 1 to "CRITICAL"
                    stats.budgetRunwayDays < 14 && stats.budgetRunwayDays > 0 -> 2 to "DANGER"
                    stats.budgetRunwayDays < 30 && stats.budgetRunwayDays > 0 -> 3 to "WARNING"
                    stats.budgetRunwayDays == 0 -> 4 to "SETUP"
                    else -> 4 to "STABLE"
                }

                _uiState.value = _uiState.value.copy(
                    defconLevel = defcon,
                    defconLabel = label,
                    spentToday = 0.0,
                    leftToday = dailyBudget, 
                    mealsInFridge = stats.mealsInFridge,
                    daysToPayday = daysToPayday.coerceAtLeast(0),
                    recentVictory = stats.victoryMeal,
                    
                    // Setup Data Sync
                    totalBalance = stats.totalBalance,
                    monthlyBills = stats.monthlyBills,
                    paydayDate = try { stats.nextPayday?.let { kotlinx.datetime.LocalDate.parse(it) } } catch(e:Exception){ null },
                    favoriteFood = stats.victoryMeal ?: "",
                    emergencyFund = stats.emergencyFund,
                    isCalibrated = stats.totalBalance > 0 || stats.monthlyBills > 0,
                    
                    // === V2 DEMO DATA ===
                    isUsingProactiveData = stats.dailyRunway > 0,
                    // If V2 data exists, override legacy calcs
                    todayBudget = if (stats.dailyRunway > 0) stats.dailyRunway else dailyBudget,
                    fridgeEstimatedDays = stats.fridgeEstimatedDays,
                    victoryFeastUnlocked = stats.victoryFeastUnlocked,
                    // Prefer server message if available
                    voiceMessage = stats.defconMessage ?: (if (defcon < 3) "Alert: Finances critical. Engage rationing." else "Systems optimal. Maintain course."),
                    // Prefer server meal plan if available
                    mealPlan = stats.todaysMealPlan ?: _uiState.value.mealPlan,
                    mealRecommendation = stats.recommendation
                )
                
                _isLoading.value = false
            }
        }
    }
    
    fun updateSetupData(
        financialInfo: String,
        favoriteFood: String,
        imageBase64: String? = null
    ) {
        screenModelScope.launch {
            _isLoading.value = true
            val result = repository.setupVitalityBrain(
                financialInfo = financialInfo,
                imageBase64 = imageBase64,
                favoriteFood = favoriteFood
            )
            
            if (result != null) {
                _uiState.value = _uiState.value.copy(
                    defconLevel = result.defcon_level,
                    defconLabel = when(result.defcon_level) {
                        5 -> "ABUNDANCE"
                        4 -> "STABLE"
                        3 -> "CAUTION"
                        2 -> "DANGER"
                        1 -> "CRITICAL"
                        else -> "STABLE"
                    },
                    todayBudget = result.daily_runway,
                    voiceMessage = result.message,
                    isCalibrated = true
                )
            }
            _isLoading.value = false
        }
    }

    fun logExpense(amount: Double, description: String) {
        screenModelScope.launch {
            _isLoading.value = true
            val currentStats = repository.moreStats.value.vitality
             val newBills = currentStats.monthlyBills + amount
             val newBalance = currentStats.totalBalance - amount
             
             repository.updateVitalityState(
                 totalBalance = newBalance,
                 monthlyBills = newBills,
                 paydayDate = currentStats.nextPayday,
                 favoriteFood = currentStats.victoryMeal ?: "Pizza"
             )
             _isLoading.value = false
        }
    }

    fun onScanFridge(imageBase64: String) {
        screenModelScope.launch {
            _isLoading.value = true
            val result = repository.scanFridgeBrain(imageBase64)
            
            if (result != null) {
                _uiState.value = _uiState.value.copy(
                    fridgeIngredients = result.ingredients,
                    fridgeDaysRemaining = result.days_remaining,
                    scanMessage = result.message,
                    mealsInFridge = result.ingredients.sumOf { it.servings }
                )
            }
            _isLoading.value = false
        }
    }

    fun makeMealDecision(timeMins: Int, energy: Int) {
        screenModelScope.launch {
            _isLoading.value = true
            val currentBudget = _uiState.value.leftToday
            val decision = repository.makeMealDecisionBrain(timeMins, energy, currentBudget)
            if (decision != null) {
                _uiState.value = _uiState.value.copy(
                    lastDecision = decision,
                    voiceMessage = decision.suggestion
                )
            }
            _isLoading.value = false
        }
    }

    fun logSleep(hours: Double, quality: String) {
        screenModelScope.launch {
            _isLoading.value = true
            val currentStats = repository.moreStats.value.vitality
            
            repository.updateVitalityState(
                totalBalance = currentStats.totalBalance,
                monthlyBills = currentStats.monthlyBills,
                paydayDate = currentStats.nextPayday,
                favoriteFood = currentStats.victoryMeal ?: "Pizza",
                mealsInFridge = currentStats.mealsInFridge,
                sleepLog = mapOf("hours" to hours, "quality" to quality)
            )
            _isLoading.value = false
        }
    }

    fun logActivity(type: String, duration: Int, intensity: String) {
        screenModelScope.launch {
            _isLoading.value = true
            val currentStats = repository.moreStats.value.vitality
            
            repository.updateVitalityState(
                totalBalance = currentStats.totalBalance,
                monthlyBills = currentStats.monthlyBills,
                paydayDate = currentStats.nextPayday,
                favoriteFood = currentStats.victoryMeal ?: "Pizza",
                mealsInFridge = currentStats.mealsInFridge,
                activityLog = mapOf("type" to type, "duration" to duration, "intensity" to intensity)
            )
            _isLoading.value = false
        }
    }

    fun manageEmergencyFund(action: String, amount: Double) {
        screenModelScope.launch {
            _isLoading.value = true
            val currentStats = repository.moreStats.value.vitality
            val currentFund = currentStats.emergencyFund
            
            val newFund = if (action == "deposit") currentFund + amount else (currentFund - amount).coerceAtLeast(0.0)
            
            repository.updateVitalityState(
                totalBalance = currentStats.totalBalance,
                monthlyBills = currentStats.monthlyBills,
                paydayDate = currentStats.nextPayday,
                favoriteFood = currentStats.victoryMeal ?: "Pizza",
                mealsInFridge = currentStats.mealsInFridge,
                emergencyFund = newFund
            )
            _isLoading.value = false
        }
    }
}
