package com.mursaline.kaironex.features.study.gatekeeper

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * ============================================================
 * KNOWLEDGE GATEKEEPER MODAL
 * ============================================================
 *
 * Non-dismissible modal that appears when:
 * - Student tries to end a study session early
 * - Student tries to access blocked content (Netflix, etc.)
 * - Session break time requires knowledge verification
 *
 * Features:
 * - Auto-generated questions from session content
 * - Progress tracking
 * - Instant grading with animations
 * - Pass/Fail states with appropriate feedback
 * - Remediation suggestions on failure
 * - "Extend block" controls
 *
 * This is NON-DISMISSIBLE - student must pass to proceed
 */

@Composable
fun GatekeeperModal(
    questions: List<GatekeeperQuestion>,
    onPass: () -> Unit,
    onFail: (remediation: String) -> Unit,
    onExtendSession: (minutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var answeredQuestions by remember { mutableStateOf(listOf<AnsweredQuestion>()) }
    var gatekeeperState by remember { mutableStateOf<GatekeeperState>(GatekeeperState.Questioning) }

    val currentQuestion = questions.getOrNull(currentQuestionIndex)
    val progress = (currentQuestionIndex.toFloat() / questions.size.coerceAtLeast(1))

    // Full screen modal overlay (non-dismissible)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        KaironexColors.InkBlack,
                        KaironexColors.InkBlack.copy(alpha = 0.95f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        when (gatekeeperState) {
            is GatekeeperState.Questioning -> {
                QuestioningContent(
                    currentQuestion = currentQuestion,
                    currentIndex = currentQuestionIndex,
                    totalQuestions = questions.size,
                    progress = progress,
                    selectedAnswer = selectedAnswer,
                    onAnswerSelect = { selectedAnswer = it },
                    onSubmit = {
                        if (selectedAnswer != null && currentQuestion != null) {
                            val isCorrect = selectedAnswer == currentQuestion.correctAnswerIndex
                            answeredQuestions = answeredQuestions + AnsweredQuestion(
                                question = currentQuestion,
                                selectedAnswer = selectedAnswer!!,
                                isCorrect = isCorrect
                            )

                            if (currentQuestionIndex < questions.size - 1) {
                                currentQuestionIndex++
                                selectedAnswer = null
                            } else {
                                // All questions answered - calculate result
                                val correctCount = answeredQuestions.count { it.isCorrect }
                                val passThreshold = (questions.size * 0.7).toInt() // 70% to pass

                                gatekeeperState = if (correctCount >= passThreshold) {
                                    GatekeeperState.Passed(correctCount, questions.size)
                                } else {
                                    GatekeeperState.Failed(correctCount, questions.size)
                                }
                            }
                        }
                    }
                )
            }

            is GatekeeperState.Passed -> {
                val state = gatekeeperState as GatekeeperState.Passed
                PassedContent(
                    correctCount = state.correctCount,
                    totalCount = state.totalCount,
                    onContinue = onPass
                )
            }

            is GatekeeperState.Failed -> {
                val state = gatekeeperState as GatekeeperState.Failed
                FailedContent(
                    correctCount = state.correctCount,
                    totalCount = state.totalCount,
                    onRetry = {
                        currentQuestionIndex = 0
                        selectedAnswer = null
                        answeredQuestions = emptyList()
                        gatekeeperState = GatekeeperState.Questioning
                    },
                    onExtendSession = onExtendSession,
                    onAcceptRemediation = {
                        onFail("Review the concepts you missed and try again.")
                    }
                )
            }
        }
    }
}

@Composable
private fun QuestioningContent(
    currentQuestion: GatekeeperQuestion?,
    currentIndex: Int,
    totalQuestions: Int,
    progress: Float,
    selectedAnswer: Int?,
    onAnswerSelect: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔐 Knowledge Gatekeeper",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${currentIndex + 1}/$totalQuestions",
                style = MaterialTheme.typography.labelLarge,
                color = KaironexColors.ElectricBlue
            )
        }

        Spacer(Modifier.height(16.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = KaironexColors.ElectricBlue,
            trackColor = Color.White.copy(alpha = 0.2f)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Answer correctly to unlock",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(32.dp))

        // Question Card
        if (currentQuestion != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = currentQuestion.question,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(24.dp))

                    // Answer options
                    currentQuestion.options.forEachIndexed { index, option ->
                        AnswerOption(
                            text = option,
                            isSelected = selectedAnswer == index,
                            onClick = { onAnswerSelect(index) }
                        )
                        if (index < currentQuestion.options.size - 1) {
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Submit button
        Button(
            onClick = onSubmit,
            enabled = selectedAnswer != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = KaironexColors.ElectricBlue,
                disabledContainerColor = Color.White.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (currentIndex < totalQuestions - 1) "Next Question" else "Submit",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun AnswerOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) KaironexColors.ElectricBlue.copy(alpha = 0.3f)
               else Color.White.copy(alpha = 0.05f),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, KaironexColors.ElectricBlue)
        } else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) KaironexColors.ElectricBlue
                        else Color.White.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun PassedContent(
    correctCount: Int,
    totalCount: Int,
    onContinue: () -> Unit
) {
    val scale by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success icon with animation
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            KaironexColors.SuccessGreen,
                            KaironexColors.SuccessGreen.copy(alpha = 0.5f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Passed",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "🎉 Knowledge Verified!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "You got $correctCount out of $totalCount correct",
            style = MaterialTheme.typography.titleMedium,
            color = KaironexColors.SuccessGreen
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Your understanding is solid. Access granted!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(
                containerColor = KaironexColors.SuccessGreen
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                text = "Continue",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun FailedContent(
    correctCount: Int,
    totalCount: Int,
    onRetry: () -> Unit,
    onExtendSession: (Int) -> Unit,
    onAcceptRemediation: () -> Unit
) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fail icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            KaironexColors.AttentionOrange,
                            KaironexColors.AttentionOrange.copy(alpha = 0.5f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Retry",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Almost There!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "You got $correctCount out of $totalCount correct",
            style = MaterialTheme.typography.titleMedium,
            color = KaironexColors.AttentionOrange
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "You need 70% to pass. Let's strengthen those concepts!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Remediation options
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📚 Remediation Options",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                RemediationOption(
                    icon = Icons.Default.Refresh,
                    title = "Try Again",
                    description = "Answer the questions again",
                    onClick = onRetry
                )

                Spacer(Modifier.height(8.dp))

                RemediationOption(
                    icon = Icons.Default.Timer,
                    title = "Extend Study (+15 min)",
                    description = "Review the material first",
                    onClick = { onExtendSession(15) }
                )

                Spacer(Modifier.height(8.dp))

                RemediationOption(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "View Missed Concepts",
                    description = "See what you need to review",
                    onClick = onAcceptRemediation
                )
            }
        }
    }
}

@Composable
private fun RemediationOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.05f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = KaironexColors.ElectricBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

// Data classes
data class GatekeeperQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String = ""
)

data class AnsweredQuestion(
    val question: GatekeeperQuestion,
    val selectedAnswer: Int,
    val isCorrect: Boolean
)

sealed class GatekeeperState {
    object Questioning : GatekeeperState()
    data class Passed(val correctCount: Int, val totalCount: Int) : GatekeeperState()
    data class Failed(val correctCount: Int, val totalCount: Int) : GatekeeperState()
}
