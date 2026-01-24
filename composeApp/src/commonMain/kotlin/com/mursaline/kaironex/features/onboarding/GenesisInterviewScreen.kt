package com.mursaline.kaironex.features.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.MainShellScreen
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ============================================================
 * GENESIS INTERVIEW SCREEN
 * ============================================================
 *
 * Kaironex conducts a conversational interview to understand the student.
 * The interaction mode (Voice or Chat) is passed from SystemSetupScreen.
 *
 * If Voice mode:
 * - AI speaks questions using Text-to-Speech
 * - User responds via voice (Speech-to-Text)
 * - Conversation feels natural like talking to a person
 *
 * If Chat mode:
 * - Questions appear as chat messages
 * - User types responses
 *
 * After interview completion:
 * - Shows profile summary
 * - Asks to connect Google Drive for study materials
 * - Then launches main app
 *
 * Data Collected:
 * - University/College name
 * - Degree type (BTech, BSc, Masters, PhD)
 * - Major/Field (CSE, Mechanical, Business, etc.)
 * - Current semester/year
 * - Part-time job details
 * - Work schedule
 * - Study goals
 * - Challenges they face
 * - Preferred study times
 * - Extracurricular commitments
 *
 * This data feeds into Gemini to personalize the entire experience.
 */

data class StudentProfile(
    var name: String = "",
    var university: String = "",
    var degreeType: String = "",
    var major: String = "",
    var semester: String = "",
    var hasPartTimeJob: Boolean = false,
    var jobTitle: String = "",
    var workSchedule: String = "",
    var weeklyWorkHours: Int = 0,
    var studyGoals: List<String> = emptyList(),
    var challenges: List<String> = emptyList(),
    var preferredStudyTime: String = "",
    var extracurriculars: List<String> = emptyList(),
    var sleepSchedule: String = "",
    var internationalStudent: Boolean = false,
    var visaType: String = ""
)

data class ChatMessage(
    val id: Int,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// Interview questions flow
val interviewQuestions = listOf(
    "Hey! I'm Kairo, your personal study companion. Let's get to know each other! What's your name?",
    "Nice to meet you, {name}! Which university or college are you studying at?",
    "Great! What degree are you pursuing? (e.g., BTech, BSc, Masters, MBA, PhD)",
    "What's your major or field of study?",
    "Which semester or year are you currently in?",
    "Do you have a part-time job? (Yes/No)",
    "What's your job title and where do you work?",
    "What's your typical work schedule? (e.g., Mon-Fri 4PM-8PM, Weekends only)",
    "About how many hours per week do you work?",
    "What are your main study goals this semester? (e.g., Pass all exams, Get internship, Learn DSA)",
    "What's your biggest challenge right now? (e.g., Time management, Focus, Understanding concepts)",
    "When do you prefer to study? (e.g., Early morning, Late night, Afternoon)",
    "Are you involved in any clubs, sports, or other activities?",
    "Are you an international student? (Yes/No)",
    "What's your typical sleep schedule? (e.g., 11PM-7AM)",
    "Perfect! I now have a complete picture of your life. Let me set up your personalized experience..."
)

/**
 * Screen that takes the interaction mode from SystemSetupScreen
 */
data class GenesisInterviewScreen(
    val interactionMode: InteractionMode
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        var currentQuestionIndex by remember { mutableStateOf(0) }
        var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
        var userInput by remember { mutableStateOf("") }
        var isTyping by remember { mutableStateOf(false) }
        var studentProfile by remember { mutableStateOf(StudentProfile()) }
        var screenState by remember { mutableStateOf(InterviewState.INTERVIEWING) }

        // Voice mode states
        var isListening by remember { mutableStateOf(false) }
        var isSpeaking by remember { mutableStateOf(false) }

        val listState = rememberLazyListState()

        // Start interview immediately
        LaunchedEffect(Unit) {
            delay(500)
            isTyping = true

            // If voice mode, "speak" the question
            if (interactionMode == InteractionMode.VOICE) {
                isSpeaking = true
                // TODO: Implement actual TTS here
                // TextToSpeech.speak(interviewQuestions[0])
            }

            delay(1500)
            isTyping = false
            isSpeaking = false
            messages = messages + ChatMessage(
                id = 0,
                content = interviewQuestions[0],
                isFromUser = false
            )
        }

        // Auto-scroll to bottom
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KaironexColors.CloudGray)
        ) {
            when (screenState) {
                InterviewState.INTERVIEWING -> {
                    // Chat/Voice interface
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        InterviewHeader(
                            questionNumber = currentQuestionIndex + 1,
                            totalQuestions = interviewQuestions.size - 1,
                            mode = interactionMode,
                            isSpeaking = isSpeaking,
                            isListening = isListening
                        )

                        // Messages
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(messages) { message ->
                                ChatBubble(message = message)
                            }

                            // Typing/Speaking indicator
                            if (isTyping || isSpeaking) {
                                item {
                                    TypingIndicator(isVoice = interactionMode == InteractionMode.VOICE)
                                }
                            }
                        }

                        // Input area
                        ChatInputArea(
                            value = userInput,
                            onValueChange = { userInput = it },
                            onSend = {
                                if (userInput.isNotBlank()) {
                                    processUserResponse(
                                        answer = userInput.trim(),
                                        scope = scope,
                                        currentQuestionIndex = currentQuestionIndex,
                                        studentProfile = studentProfile,
                                        messages = messages,
                                        interactionMode = interactionMode,
                                        onMessagesUpdate = { messages = it },
                                        onProfileUpdate = { studentProfile = it },
                                        onQuestionIndexUpdate = { currentQuestionIndex = it },
                                        onTypingUpdate = { isTyping = it },
                                        onSpeakingUpdate = { isSpeaking = it },
                                        onComplete = { screenState = InterviewState.PROFILE_COMPLETE }
                                    )
                                    userInput = ""
                                }
                            },
                            isVoiceMode = interactionMode == InteractionMode.VOICE,
                            isListening = isListening,
                            onVoiceInput = {
                                // Toggle listening state
                                isListening = !isListening
                                if (isListening) {
                                    // TODO: Start speech recognition
                                    // SpeechRecognizer.startListening()
                                    // On result: userInput = recognizedText
                                }
                            }
                        )
                    }
                }

                InterviewState.PROFILE_COMPLETE -> {
                    // Show profile summary then move to Drive connection
                    ProfileCompleteScreen(
                        studentProfile = studentProfile,
                        onContinue = { screenState = InterviewState.DRIVE_CONNECT }
                    )
                }

                InterviewState.DRIVE_CONNECT -> {
                    // Google Drive connection
                    DriveConnectionScreen(
                        onConnect = {
                            // TODO: Implement Google Drive OAuth
                            navigator.replaceAll(MainShellScreen)
                        },
                        onSkip = {
                            navigator.replaceAll(MainShellScreen)
                        }
                    )
                }
            }
        }
    }
}

enum class InterviewState {
    INTERVIEWING,
    PROFILE_COMPLETE,
    DRIVE_CONNECT
}

private fun processUserResponse(
    answer: String,
    scope: kotlinx.coroutines.CoroutineScope,
    currentQuestionIndex: Int,
    studentProfile: StudentProfile,
    messages: List<ChatMessage>,
    interactionMode: InteractionMode,
    onMessagesUpdate: (List<ChatMessage>) -> Unit,
    onProfileUpdate: (StudentProfile) -> Unit,
    onQuestionIndexUpdate: (Int) -> Unit,
    onTypingUpdate: (Boolean) -> Unit,
    onSpeakingUpdate: (Boolean) -> Unit,
    onComplete: () -> Unit
) {
    // Add user message
    val updatedMessages = messages + ChatMessage(
        id = messages.size,
        content = answer,
        isFromUser = true
    )
    onMessagesUpdate(updatedMessages)

    // Process answer and update profile
    val updatedProfile = processAnswer(currentQuestionIndex, answer, studentProfile)
    onProfileUpdate(updatedProfile)

    // Move to next question
    scope.launch {
        delay(500)
        onTypingUpdate(true)
        if (interactionMode == InteractionMode.VOICE) {
            onSpeakingUpdate(true)
        }
        delay(1500)
        onTypingUpdate(false)
        onSpeakingUpdate(false)

        var nextIndex = currentQuestionIndex + 1

        if (nextIndex >= interviewQuestions.size - 1) {
            // Final message
            onMessagesUpdate(updatedMessages + ChatMessage(
                id = updatedMessages.size,
                content = interviewQuestions.last(),
                isFromUser = false
            ))
            delay(2000)
            onComplete()
        } else {
            // Skip job questions if no job
            if (nextIndex == 6 && !updatedProfile.hasPartTimeJob) {
                nextIndex = 9
            } else if (nextIndex == 14 && !updatedProfile.internationalStudent) {
                nextIndex = 15
            }

            onQuestionIndexUpdate(nextIndex)

            val nextQuestion = interviewQuestions[nextIndex]
                .replace("{name}", updatedProfile.name)

            onMessagesUpdate(updatedMessages + ChatMessage(
                id = updatedMessages.size,
                content = nextQuestion,
                isFromUser = false
            ))

            // TODO: If voice mode, speak the question
            // if (interactionMode == InteractionMode.VOICE) {
            //     TextToSpeech.speak(nextQuestion)
            // }
        }
    }
}

@Composable
private fun InterviewHeader(
    questionNumber: Int,
    totalQuestions: Int,
    mode: InteractionMode,
    isSpeaking: Boolean,
    isListening: Boolean
) {
    Surface(
        color = KaironexColors.CanvasWhite,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Animated icon for voice mode
                    if (mode == InteractionMode.VOICE) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val pulse by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = if (isSpeaking || isListening) 1.3f else 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        Icon(
                            if (isListening) Icons.Default.Mic else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isSpeaking || isListening) KaironexColors.SuccessGreen
                                  else KaironexColors.GeminiBlurple,
                            modifier = Modifier.size(20.dp).scale(pulse)
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = KaironexColors.GeminiBlurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (mode == InteractionMode.VOICE)
                            if (isSpeaking) "Kairo is speaking..."
                            else if (isListening) "Listening..."
                            else "Voice Interview"
                        else "Getting to know you",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.InkBlack
                    )
                }
                Text(
                    "$questionNumber / $totalQuestions",
                    style = MaterialTheme.typography.labelMedium,
                    color = KaironexColors.GeminiBlurple
                )
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { questionNumber.toFloat() / totalQuestions },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = KaironexColors.GeminiBlurple,
                trackColor = KaironexColors.CloudGray
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromUser) {
            // Kairo avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KaironexColors.GeminiBlurple),
                contentAlignment = Alignment.Center
            ) {
                Text("K", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 16.dp
            ),
            color = if (message.isFromUser) KaironexColors.GeminiBlurple
                   else KaironexColors.CanvasWhite,
            shadowElevation = if (message.isFromUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isFromUser) Color.White else KaironexColors.InkBlack,
                modifier = Modifier.padding(12.dp)
            )
        }

        if (message.isFromUser) {
            Spacer(Modifier.width(8.dp))
            // User avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KaironexColors.ElectricBlue),
                contentAlignment = Alignment.Center
            ) {
                Text("U", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TypingIndicator(isVoice: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition()
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        )
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(KaironexColors.GeminiBlurple)
                .then(if (isVoice) Modifier.scale(pulse) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (isVoice) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text("K", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = KaironexColors.CanvasWhite,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isVoice) {
                    Text(
                        "Speaking...",
                        style = MaterialTheme.typography.bodySmall,
                        color = KaironexColors.SlateGray
                    )
                } else {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    KaironexColors.GeminiBlurple.copy(
                                        alpha = if (index == 0) dot1
                                               else if (index == 1) 1f - dot1
                                               else dot1 * 0.5f
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputArea(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isVoiceMode: Boolean,
    isListening: Boolean = false,
    onVoiceInput: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val micPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        color = KaironexColors.CanvasWhite,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Voice mode - Large mic button
            if (isVoiceMode) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Animated listening indicator
                    IconButton(
                        onClick = onVoiceInput,
                        modifier = Modifier
                            .size(72.dp)
                            .scale(micPulse)
                            .clip(CircleShape)
                            .background(
                                if (isListening) KaironexColors.SuccessGreen
                                else KaironexColors.GeminiBlurple
                            )
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop Listening" else "Start Speaking",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    if (isListening) "🎤 Listening... Tap to stop"
                    else "Tap the mic to speak your answer",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isListening) KaironexColors.SuccessGreen
                           else KaironexColors.SlateGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Divider
                Text(
                    "or type below",
                    style = MaterialTheme.typography.labelSmall,
                    color = KaironexColors.SlateGray.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
            }

            // Text input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text("Type your answer...", color = KaironexColors.SlateGray) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KaironexColors.GeminiBlurple,
                        unfocusedBorderColor = KaironexColors.SlateGray.copy(alpha = 0.3f),
                        focusedTextColor = KaironexColors.InkBlack,
                        unfocusedTextColor = KaironexColors.InkBlack,
                        cursorColor = KaironexColors.GeminiBlurple,
                        focusedContainerColor = KaironexColors.CloudGray,
                        unfocusedContainerColor = KaironexColors.CloudGray
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = onSend,
                    enabled = value.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (value.isNotBlank()) KaironexColors.GeminiBlurple
                            else KaironexColors.SlateGray.copy(alpha = 0.2f)
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileCompleteScreen(
    studentProfile: StudentProfile,
    onContinue: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KaironexColors.CloudGray)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success animation
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            KaironexColors.SuccessGreen,
                            KaironexColors.SuccessGreen.copy(alpha = 0.7f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Profile Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = KaironexColors.InkBlack
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "I now understand your life, ${studentProfile.name}",
            style = MaterialTheme.typography.bodyLarge,
            color = KaironexColors.SlateGray,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Profile summary
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = KaironexColors.CanvasWhite,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ProfileSummaryItem("🎓", "University", studentProfile.university)
                ProfileSummaryItem("📚", "Studying", "${studentProfile.major} - ${studentProfile.degreeType}")
                ProfileSummaryItem("📅", "Semester", studentProfile.semester)
                if (studentProfile.hasPartTimeJob) {
                    ProfileSummaryItem("💼", "Work", "${studentProfile.jobTitle} (${studentProfile.weeklyWorkHours}h/week)")
                }
                ProfileSummaryItem("🎯", "Goals", studentProfile.studyGoals.firstOrNull() ?: "Not set")
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(0.85f).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KaironexColors.GeminiBlurple
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Continue to Setup", fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun DriveConnectionScreen(
    onConnect: () -> Unit,
    onSkip: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KaironexColors.CloudGray)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Floating cloud icon
        Box(
            modifier = Modifier
                .offset(y = floatOffset.dp)
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            KaironexColors.ElectricBlue,
                            KaironexColors.ElectricBlue.copy(alpha = 0.7f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CloudUpload,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Connect Your Study Materials",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = KaironexColors.InkBlack,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Import your PDFs, notes, and documents from Google Drive so I can help you study smarter",
            style = MaterialTheme.typography.bodyMedium,
            color = KaironexColors.SlateGray,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        // Benefits list
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = KaironexColors.CanvasWhite,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                BenefitItem("📚", "Auto-index your syllabus and notes")
                Spacer(Modifier.height(12.dp))
                BenefitItem("🧠", "Generate flashcards from your PDFs")
                Spacer(Modifier.height(12.dp))
                BenefitItem("📝", "Create mock tests from your materials")
                Spacer(Modifier.height(12.dp))
                BenefitItem("🎯", "Track your learning progress")
            }
        }

        Spacer(Modifier.height(40.dp))

        // Connect button
        Button(
            onClick = onConnect,
            modifier = Modifier.fillMaxWidth(0.85f).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KaironexColors.GeminiBlurple
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Cloud, null)
            Spacer(Modifier.width(8.dp))
            Text("Connect Google Drive", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        // Skip button
        TextButton(onClick = onSkip) {
            Text(
                "I'll upload files manually later",
                color = KaironexColors.SlateGray
            )
        }
    }
}

@Composable
private fun BenefitItem(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = KaironexColors.InkBlack
        )
    }
}

@Composable
private fun ProfileSummaryItem(emoji: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = KaironexColors.SlateGray
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = KaironexColors.InkBlack
            )
        }
    }
}

// Process user answers and update profile
private fun processAnswer(questionIndex: Int, answer: String, profile: StudentProfile): StudentProfile {
    return when (questionIndex) {
        0 -> profile.copy(name = answer)
        1 -> profile.copy(university = answer)
        2 -> profile.copy(degreeType = answer)
        3 -> profile.copy(major = answer)
        4 -> profile.copy(semester = answer)
        5 -> profile.copy(hasPartTimeJob = answer.lowercase().contains("yes"))
        6 -> profile.copy(jobTitle = answer)
        7 -> profile.copy(workSchedule = answer)
        8 -> profile.copy(weeklyWorkHours = answer.filter { it.isDigit() }.toIntOrNull() ?: 0)
        9 -> profile.copy(studyGoals = answer.split(",").map { it.trim() })
        10 -> profile.copy(challenges = answer.split(",").map { it.trim() })
        11 -> profile.copy(preferredStudyTime = answer)
        12 -> profile.copy(extracurriculars = answer.split(",").map { it.trim() })
        13 -> profile.copy(internationalStudent = answer.lowercase().contains("yes"))
        14 -> profile.copy(sleepSchedule = answer)
        else -> profile
    }
}
