package com.mursaline.kaironex.features.zones.vitality

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@OptIn(InternalSerializationApi::class)
@Serializable
data class VitalitySetupResult(
    val status: String,
    val defcon_level: Int,
    val daily_runway: Double,
    val message: String,
    val survival_state: JsonObject? = null
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class FridgeScanResult(
    val status: String,
    val ingredients: List<FridgeIngredient> = emptyList(),
    val days_remaining: Int = 0,
    val needs_shopping: Boolean = false,
    val message: String = "",
    val meal_suggestions: List<String> = emptyList()
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class FridgeIngredient(
    val name: String,
    val quantity: String,
    val servings: Int = 0
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class MealPlan(
    val plan_id: String = "",
    val breakfast_options: List<MealOption> = emptyList(),
    val lunch_options: List<MealOption> = emptyList(),
    val dinner_options: List<MealOption> = emptyList(),
    val total_options: Int = 0
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class MealOption(
    val id: String,
    val name: String,
    val cost: Double,
    val type: String // "cook" or "order"
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class MealDecision(
    val action: String, // "cook" or "order"
    val reason: String,
    val suggestion: String,
    val budget_allocation: Double = 0.0
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class MealRecommendation(
    val action: String, // "COOK" or "ORDER"
    val meal_name: String,
    val cost_estimate: Double,
    val time_estimate: Int,
    val reasoning_trace: String
)
