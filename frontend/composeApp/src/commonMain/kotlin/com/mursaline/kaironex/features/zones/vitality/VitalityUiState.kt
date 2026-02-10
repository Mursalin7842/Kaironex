package com.mursaline.kaironex.features.zones.vitality

import kotlinx.datetime.LocalDate

data class VitalityUiState(
    val defconLevel: Int = 4,
    val defconLabel: String = "STABLE",
    val todayBudget: Double = 35.0,
    val spentToday: Double = 0.0,
    val leftToday: Double = 35.0,
    val voiceMessage: String = "Systems optimal. Maintain course.",
    val mealsInFridge: Int = 0,
    val daysToPayday: Int = 0,
    val recentVictory: String? = null,
    
    // Setup Data
    val isCalibrated: Boolean = false,
    val monthlyBills: Double = 0.0,
    val totalBalance: Double = 0.0,
    val paydayDate: LocalDate? = null,
    val favoriteFood: String = "",
    val emergencyFund: Double = 0.0,
    
    // Brain Data
    val mealPlan: MealPlan? = null,
    val fridgeIngredients: List<FridgeIngredient> = emptyList(),
    val fridgeDaysRemaining: Int = 0,
    val scanMessage: String = "",
    val lastDecision: MealDecision? = null,
    
    // New Fields for Demo
    val mealRecommendation: MealRecommendation? = null,
    val fridgeEstimatedDays: Int = 0,
    val victoryFeastUnlocked: Boolean = false,
    val isUsingProactiveData: Boolean = false
)
