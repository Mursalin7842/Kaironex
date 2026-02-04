package com.mursaline.kaironex.features.campaign

import kotlinx.serialization.Serializable

/**
 * Represents the full state of the Campaign Zone.
 * Sourced from 'campaign_state' table in Appwrite.
 */
@Serializable
data class CampaignState(
    val skillTree: List<SkillNode> = emptyList(),
    val questBoard: List<Quest> = emptyList(),
    val armory: ArmoryState = ArmoryState(),
    val simulacrum: SimulacrumState = SimulacrumState()
)

// ============================================================================
// SKILL TREE
// ============================================================================
@Serializable
data class SkillNode(
    val id: String,
    val name: String,
    val level: Int,
    val status: String, // "locked", "unlocked", "mastered"
    val description: String,
    val parent: String? = null,
    val xpCost: Int = 0,
    val icon: String = "✨" // Generic default
)

// ============================================================================
// QUEST BOARD
// ============================================================================
@Serializable
data class Quest(
    val id: String,
    val title: String,
    val type: String, // "APPLICATION", "SKILL", "NETWORKING", "RESEARCH"
    val description: String,
    val xp: Int,
    val status: String, // "active", "completed", "expired"
    val deadline: String? = null, // ISO Date
    val tags: List<String> = emptyList()
)

// ============================================================================
// THE ARMORY
// ============================================================================
@Serializable
data class ArmoryState(
    val inventory: List<ArmoryItem> = emptyList(),
    val blueprints: List<ArmoryItem> = emptyList() // Items available to forge
)

@Serializable
data class ArmoryItem(
    val id: String,
    val name: String,
    val type: String, // "TEMPLATE", "TOOL", "GUIDE"
    val status: String, // "equipped", "locked", "owned"
    val costXp: Int = 0,
    val downloadUrl: String? = null
)

@Serializable
data class SimulacrumState(
    val active: Boolean = false,
    val type: String = "behavioral",
    val company: String = "",
    val role: String = "",
    val difficulty: String = "medium",
    val questions_asked: Int = 0,
    val last_question: String = "",
    val last_user_response: String = ""
)
