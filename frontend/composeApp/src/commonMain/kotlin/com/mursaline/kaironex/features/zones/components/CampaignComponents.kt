package com.mursaline.kaironex.features.zones.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * 🎯 CAMPAIGN ZONE COMPONENTS
 * ============================
 * Skill Tree, Quest Board, Armory, Simulacrum
 *
 * Gamified goal tracking and career development
 */

// =========================================================================
// SKILL TREE COMPONENT
// =========================================================================

data class SkillNode(
    val id: String,
    val name: String,
    val emoji: String,
    val progress: Float,
    val isUnlocked: Boolean,
    val isCurrent: Boolean = false,
    val children: List<String> = emptyList()
)

@Composable
fun SkillTreeCard(
    skills: List<SkillNode>,
    currentSkill: String?,
    onSkillClick: (SkillNode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌳", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Skill Tree",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your learning journey",
                        style = MaterialTheme.typography.bodySmall,
                        color = KaironexColors.SlateGray
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Skill nodes (simplified horizontal layout)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(skills) { skill ->
                    SkillNodeItem(
                        skill = skill,
                        isCurrent = skill.id == currentSkill,
                        onClick = { onSkillClick(skill) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Current focus
            currentSkill?.let { skillId ->
                skills.find { it.id == skillId }?.let { skill ->
                    Surface(
                        color = KaironexColors.GeminiBlurple.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(skill.emoji, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Currently Learning",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KaironexColors.SlateGray
                                )
                                Text(
                                    skill.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { skill.progress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = KaironexColors.GeminiBlurple,
                                    trackColor = KaironexColors.CloudGray
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "${(skill.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = KaironexColors.GeminiBlurple
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillNodeItem(
    skill: SkillNode,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isCurrent -> KaironexColors.GeminiBlurple
        skill.isUnlocked -> Color(0xFF4CAF50)
        else -> KaironexColors.SlateGray.copy(alpha = 0.3f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = skill.isUnlocked, onClick = onClick)
            .padding(4.dp)
    ) {
        Box {
            Surface(
                color = if (skill.isUnlocked) Color.White else KaironexColors.CloudGray,
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .border(3.dp, borderColor, CircleShape),
                shadowElevation = if (isCurrent) 8.dp else 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (skill.isUnlocked) {
                        Text(skill.emoji, style = MaterialTheme.typography.headlineMedium)
                    } else {
                        Icon(
                            Icons.Default.Lock,
                            null,
                            tint = KaironexColors.SlateGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Progress ring or completion check
            if (skill.isUnlocked && skill.progress >= 1f) {
                Surface(
                    color = Color(0xFF4CAF50),
                    shape = CircleShape,
                    modifier = Modifier.size(20.dp).align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Default.Check,
                        null,
                        tint = Color.White,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            skill.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (skill.isUnlocked) KaironexColors.InkBlack else KaironexColors.SlateGray
        )
    }
}

// =========================================================================
// QUEST BOARD COMPONENT
// =========================================================================

data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val progress: Float,
    val deadline: String?,
    val rewards: List<String>,
    val difficulty: QuestDifficulty,
    val isActive: Boolean = false
)

enum class QuestDifficulty(val label: String, val color: Color) {
    EASY("Easy", Color(0xFF4CAF50)),
    MEDIUM("Medium", Color(0xFFFF9800)),
    HARD("Hard", Color(0xFFF44336)),
    EPIC("Epic", Color(0xFF9C27B0))
}

@Composable
fun QuestBoardCard(
    activeQuests: List<Quest>,
    completedCount: Int,
    onQuestClick: (Quest) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📋", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Quest Board",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$completedCount quests completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = KaironexColors.SlateGray
                        )
                    }
                }

                Surface(
                    color = KaironexColors.ElectricBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${activeQuests.size} Active",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = KaironexColors.ElectricBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Quest list
            activeQuests.forEach { quest ->
                QuestItem(quest = quest, onClick = { onQuestClick(quest) })
                if (quest != activeQuests.last()) {
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (activeQuests.isEmpty()) {
                Surface(
                    color = KaironexColors.CloudGray,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎯", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("No active quests", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Set a new goal to get started!",
                            style = MaterialTheme.typography.bodySmall,
                            color = KaironexColors.SlateGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestItem(
    quest: Quest,
    onClick: () -> Unit
) {
    Surface(
        color = KaironexColors.CloudGray,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = quest.difficulty.color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            quest.difficulty.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = quest.difficulty.color,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    quest.deadline?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "⏰ $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = KaironexColors.SlateGray
                        )
                    }
                }

                Text(
                    "${(quest.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.GeminiBlurple
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                quest.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                quest.description,
                style = MaterialTheme.typography.bodySmall,
                color = KaironexColors.SlateGray
            )

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { quest.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = KaironexColors.GeminiBlurple,
                trackColor = Color.White
            )

            if (quest.rewards.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row {
                    Text("Rewards: ", style = MaterialTheme.typography.labelSmall, color = KaironexColors.SlateGray)
                    quest.rewards.forEach { reward ->
                        Text("$reward ", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// =========================================================================
// ARMORY COMPONENT
// =========================================================================

data class ArmoryItem(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val isUnlocked: Boolean,
    val unlockProgress: Float = 0f
)

@Composable
fun ArmoryCard(
    unlockedItems: List<ArmoryItem>,
    nextUnlock: ArmoryItem?,
    totalXp: Int,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "The Armory",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Your unlocked abilities",
                            style = MaterialTheme.typography.bodySmall,
                            color = KaironexColors.SlateGray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$totalXp XP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Unlocked items
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(unlockedItems) { item ->
                    ArmoryItemChip(item = item)
                }
            }

            // Next unlock
            nextUnlock?.let { item ->
                Spacer(Modifier.height(16.dp))

                Surface(
                    color = KaironexColors.CloudGray,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.emoji, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Next Unlock", style = MaterialTheme.typography.labelSmall, color = KaironexColors.SlateGray)
                            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { item.unlockProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFFFFD700),
                                trackColor = Color.White
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${(item.unlockProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArmoryItemChip(item: ArmoryItem) {
    Surface(
        color = KaironexColors.GeminiBlurple.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).width(80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(item.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                item.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
        }
    }
}
