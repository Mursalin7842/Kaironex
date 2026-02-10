package com.mursaline.kaironex.features.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop

import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.ui.components.KxCard
import com.mursaline.kaironex.ui.components.KxCardVariant
import com.mursaline.kaironex.features.study.components.*
import cafe.adriel.voyager.koin.koinScreenModel
import kotlinx.coroutines.delay

/**
 * Study Room Screen - Active study session interface
 *
 * Features:
 * - Resource viewer (PDF, video, code)
 * - Timer tracking with session management
 * - AI study tools (summarize, quiz, explain)
 * - Focus tracking and break reminders
 */
data class StudyRoomScreen(
    val task: ScheduleRepository.ScheduleTask? = null
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = koinScreenModel<StudyViewModel>()


        val resources by viewModel.resources.collectAsState()
        val activeSession by viewModel.activeSession.collectAsState()
        
        // New Content Flows
        val studyResources by viewModel.studyResources.collectAsState()
        val flashCards by viewModel.flashCards.collectAsState()
        val gatekeeperQuiz by viewModel.gatekeeperQuiz.collectAsState()
        

        
        val isQuizActive by viewModel.isQuizActive.collectAsState()

        // Timer state
        var elapsedSeconds by remember { mutableStateOf(0L) }
        var isPaused by remember { mutableStateOf(false) }
        
        // Dialogs
        var showFinishDialog by remember { mutableStateOf(false) }
        var showChangeRequestDialog by remember { mutableStateOf(false) }

        // Start session on entry
        LaunchedEffect(Unit) {
            viewModel.startStudySession(task)
        }

        // Timer logic
        LaunchedEffect(isPaused) {
            while (!isPaused) {
                delay(1000)
                elapsedSeconds++
            }
        }

        // Format timer
        val timerText = remember(elapsedSeconds) {
            val hours = elapsedSeconds / 3600
            val minutes = (elapsedSeconds % 3600) / 60
            val seconds = elapsedSeconds % 60
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

        if (showFinishDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showFinishDialog = false },
                title = { Text("Finish Session?", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                text = {
                    Column {
                        Text("Session Duration: $timerText")
                        Spacer(Modifier.height(8.dp))
                        Text("Did you complete this study task?", color = KaironexColors.SlateGray)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.endStudySession(focusScore = 0.8f, markCompleted = true)
                            showFinishDialog = false
                            navigator?.pop()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.SuccessGreen)
                    ) {
                        Text("Yes, Completed!")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            viewModel.endStudySession(focusScore = 0.5f, markCompleted = false)
                            showFinishDialog = false
                            navigator?.pop()
                        }
                    ) {
                        Text("Not Yet")
                    }
                }
            )
        }
        
        // Schedule Change Request Dialog
        if (showChangeRequestDialog) {
            var reason by remember { mutableStateOf("") }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showChangeRequestDialog = false },
                title = { Text("Request Schedule Change") },
                text = {
                    Column {
                        Text("Why do you need to change/skip this task?", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            placeholder = { Text("e.g. I'm sick, Family emergency...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            if (task != null && reason.isNotBlank()) {
                                viewModel.requestScheduleChange(task.id, reason)
                                showChangeRequestDialog = false
                                navigator?.pop() // Exit study room
                            }
                        },
                        enabled = reason.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.ElectricBlue)
                    ) {
                        Text("Submit Request")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChangeRequestDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.White)) {
            val isMobile = this.maxWidth < 800.dp

            Column(modifier = Modifier.fillMaxSize()) {
                // Header with timer
                StudyHeader(
                    onBack = {
                        showFinishDialog = true
                    },
                    title = task?.title ?: activeSession?.title ?: "Study Session",
                    timer = timerText,
                    isMobile = isMobile,
                    onRequestChange = { showChangeRequestDialog = true }
                )

                if (isMobile) {
                    MobileStudyLayout(
                        resources = resources,
                        task = task,

                        studyResources = studyResources,
                        flashCards = flashCards,
                        gatekeeperQuiz = gatekeeperQuiz,
                        viewModel = viewModel
                    )
                } else {
                    DesktopStudyLayout(
                        resources = resources,
                        task = task,

                        studyResources = studyResources,
                        flashCards = flashCards,
                        gatekeeperQuiz = gatekeeperQuiz,
                        viewModel = viewModel
                    )
                }
                
                // Bottom Control Bar
                StudyContextBar(
                    isMobile = isMobile,
                    isPaused = isPaused,
                    onPauseToggle = { isPaused = !isPaused },
                    onFinish = { showFinishDialog = true }
                )
            }
            
            // Quiz Overlay
            if (isQuizActive && gatekeeperQuiz != null) {
                QuizDialog(
                    quiz = gatekeeperQuiz!!,
                    onDismiss = { viewModel.setQuizActive(false) },
                    onComplete = { score, results ->
                        viewModel.setQuizActive(false)
                        val passed = score >= gatekeeperQuiz!!.passThreshold
                        viewModel.submitQuiz(passed, score, results)
                        
                        if (passed) {
                             // Pass!
                             viewModel.endStudySession(focusScore = 1.0f, markCompleted = true)
                             navigator?.pop()
                        }
                    }
                )
            }
        }
    }
}

// Keep backward compatibility with object reference
@Suppress("unused")
object StudyRoomScreenCompat : Screen {
    @Suppress("unused")
    private fun readResolve(): Any = StudyRoomScreenCompat

    @Composable
    override fun Content() {
        StudyRoomScreen(task = null).Content()
    }
}

@Composable
fun StudyHeader(
    onBack: () -> Unit, 
    title: String, 
    timer: String, 
    isMobile: Boolean = false, 
    onRequestChange: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isMobile) 56.dp else 64.dp)
            .background(Color.White)
            .padding(horizontal = if (isMobile) 8.dp else 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = KaironexColors.InkBlack)
            }
            if (!isMobile) Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = if (isMobile) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KaironexColors.InkBlack,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            // Edit Schedule Button
            IconButton(onClick = onRequestChange) {
                 Icon(
                     imageVector = Icons.Default.Menu,
                     contentDescription = "Request Change",
                     tint = KaironexColors.SlateGray,
                     modifier = Modifier.size(18.dp)
                 )
            }
        }
        
        // Timer badge
        Surface(
            color = KaironexColors.ElectricBlue.copy(alpha=0.1f),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = timer,
                modifier = Modifier.padding(horizontal = if (isMobile) 8.dp else 12.dp, vertical = if (isMobile) 4.dp else 6.dp),
                color = KaironexColors.ElectricBlue,
                fontWeight = FontWeight.Bold,
                style = if (isMobile) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun DesktopStudyLayout(
    resources: List<Resource>,
    task: ScheduleRepository.ScheduleTask?,
    studyResources: List<StudyResource>,
    flashCards: List<FlashCard>,
    gatekeeperQuiz: GatekeeperQuiz?,
    viewModel: StudyViewModel
) {
    // Merge AI resources with uploaded resources
    val combinedResources = remember(studyResources) {
        val aiAndManual = studyResources.map { ai ->
            Resource(
                id = ai.id,
                title = ai.name,
                type = ResourceType.ARTICLE,
                duration = "${ai.estimatedReadTime} min read",
                content = ai.content,
                summary = ai.content.take(100)
            )
        }
        aiAndManual
    }

    // State for Tabs
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Resources", "Study", "Tools")

    // Selection State
    var selectedResource by remember(combinedResources) { 
        mutableStateOf<Resource?>(null)
    }
    
    // Layout
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = KaironexColors.CanvasWhite,
            contentColor = KaironexColors.ElectricBlue,
            modifier = Modifier.width(600.dp).align(Alignment.CenterHorizontally)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, style = MaterialTheme.typography.titleMedium) }
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))

        // Content Area
        KxCard(
            modifier = Modifier.fillMaxSize(),
            variant = KxCardVariant.High,
            backgroundColor = Color.White
        ) {
            when (selectedTab) {
                0 -> ResourcesTabContent(task, combinedResources) { resource ->
                    selectedResource = resource
                    selectedTab = 1 // Switch to Study tab
                }
                1 -> StudyTabContent(selectedResource)
                2 -> ToolsTabContent(flashCards, gatekeeperQuiz, viewModel, task?.subject ?: "General")
            }
        }
    }
}

@Composable
fun MobileStudyLayout(
    resources: List<Resource>,
    task: ScheduleRepository.ScheduleTask?,
    studyResources: List<StudyResource>,
    flashCards: List<FlashCard>,
    gatekeeperQuiz: GatekeeperQuiz?,
    viewModel: StudyViewModel
) {
    // Merge AI resources with uploaded resources
    val combinedResources = remember(studyResources) {
        val aiAndManual = studyResources.map { ai ->
            Resource(
                id = ai.id,
                title = ai.name,
                type = ResourceType.ARTICLE,
                duration = "${ai.estimatedReadTime} min read",
                content = ai.content,
                summary = ai.content.take(100)
            )
        }
        aiAndManual
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Resources", "Study", "Tools")
    var selectedResource by remember(combinedResources) { 
        mutableStateOf<Resource?>(null) 
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = KaironexColors.CanvasWhite,
            contentColor = KaironexColors.ElectricBlue
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (selectedTab) {
                0 -> {
                     KxCard(modifier = Modifier.fillMaxSize(), variant = KxCardVariant.Flat, backgroundColor = KaironexColors.CanvasWhite) {
                        ResourcesTabContent(task, combinedResources, isMobile = true) { resource ->
                            selectedResource = resource
                            selectedTab = 1
                        }
                     }
                }
                1 -> {
                    KxCard(modifier = Modifier.fillMaxSize(), variant = KxCardVariant.High, backgroundColor = Color.White) {
                        StudyTabContent(selectedResource, isMobile = true)
                    }
                }
                2 -> {
                    KxCard(modifier = Modifier.fillMaxSize(), variant = KxCardVariant.Flat, backgroundColor = KaironexColors.CanvasWhite) {
                        ToolsTabContent(flashCards, gatekeeperQuiz, viewModel, task?.subject ?: "General", isMobile = true)
                    }
                }
            }
        }
    }
}

@Composable
fun ResourcesTabContent(
    task: ScheduleRepository.ScheduleTask?,
    resources: List<Resource>,
    isMobile: Boolean = false,
    onResourceClick: (Resource) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(if (isMobile) 12.dp else 24.dp)) {
        // Task Details Header
        if (task != null) {
            Text(
                "TASK DETAILS",
                style = MaterialTheme.typography.labelSmall,
                color = KaironexColors.SlateGray,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                task.title,
                style = if (isMobile) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = KaironexColors.InkBlack
            )
            Spacer(Modifier.height(4.dp))
            if (!task.topics.isNullOrBlank()) {
                Text(
                    task.topics,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaironexColors.SlateGray,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, null, modifier = Modifier.size(16.dp), tint = KaironexColors.ElectricBlue)
                Spacer(Modifier.width(4.dp))
                // Basic time display since formatting helpers aren't here
                val sTime = task.startTime.substringAfter("T").take(5)
                val eTime = task.endTime.substringAfter("T").take(5)
                Text(
                    "$sTime - $eTime",
                    style = MaterialTheme.typography.labelMedium,
                    color = KaironexColors.ElectricBlue,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(if (isMobile) 16.dp else 24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(if (isMobile) 16.dp else 24.dp))
        }

        // Resources List
        Text(
            "STUDY RESOURCES",
            style = MaterialTheme.typography.labelSmall,
            color = KaironexColors.SlateGray,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        
        ResourceIndex(
            resources = resources,
            selectedResourceId = null, // No selection in this view
            onResourceSelect = onResourceClick,
            isMobile = isMobile
        )
    }
}

@Composable
fun StudyTabContent(resource: Resource?, isMobile: Boolean = false) {
    if (resource != null) {
        StudyViewer(resource = resource, isMobile = isMobile)
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Menu, // Or Book icon
                    contentDescription = null,
                    tint = KaironexColors.SlateGray,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Select a resource from the Resources tab to begin studying.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaironexColors.SlateGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ToolsTabContent(
    flashCards: List<FlashCard>,
    gatekeeperQuiz: GatekeeperQuiz?,
    viewModel: StudyViewModel,
    subject: String,
    isMobile: Boolean = false
) {
    // Only Flashcards and Gatekeeper
    StudyTools(
        flashCards = flashCards,
        quiz = gatekeeperQuiz,
        isMobile = isMobile,
        onRequestQuiz = { viewModel.setQuizActive(true) }
    )
}

@Composable
fun TaskTopicsViewer(topics: String, isMobile: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KaironexColors.InkBlack)
            .padding(if (isMobile) 16.dp else 24.dp)
    ) {
        Text(
            "📚 Study Guide",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = topics,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5
        )
    }
}

@Composable
fun StudyContextBar(
    isMobile: Boolean = false,
    isPaused: Boolean = false,
    onPauseToggle: () -> Unit = {},
    onFinish: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(if (isMobile) 56.dp else 64.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = if (isMobile) 12.dp else 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Pause/Resume button
            OutlinedButton(
                onClick = onPauseToggle,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isPaused) KaironexColors.SuccessGreen else KaironexColors.SlateGray
                )
            ) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isPaused) "Resume" else "Pause",
                    style = if (isMobile) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
                )
            }
            
            // Finish button
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.ElectricBlue),
                contentPadding = if (isMobile) PaddingValues(horizontal = 16.dp, vertical = 8.dp) else ButtonDefaults.ContentPadding
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isMobile) "Finish" else "Finish Session",
                    style = if (isMobile) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
@Composable
fun QuizDialog(
    quiz: GatekeeperQuiz,
    onDismiss: () -> Unit,
    onComplete: (Float, List<QuestionResult>) -> Unit
) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    
    // Tracking results
    val results = remember { mutableStateListOf<QuestionResult>() }
    var questionStartTime by remember { mutableStateOf(kotlinx.datetime.Clock.System.now().toEpochMilliseconds()) }
    var isReviewing by remember { mutableStateOf(false) }

    val question = quiz.questions.getOrNull(currentQuestionIndex)

    androidx.compose.ui.window.Dialog(onDismissRequest = { if (!isReviewing) onDismiss() else { /* Don't dismiss in review */ } }) {
        KxCard(
            variant = KxCardVariant.Elevated,
            backgroundColor = Color.White,
            modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp, max = 600.dp)
        ) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(
                    if (isReviewing) "Quiz Results" else "Knowledge Gatekeeper",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.ElectricBlue
                )
                Spacer(Modifier.height(16.dp))

                if (isReviewing) {
                    // Review Screen
                    Text(
                        "Score: $score / ${quiz.questions.size}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val passColor = if (score >= (quiz.passThreshold * quiz.questions.size)) KaironexColors.SuccessGreen else KaironexColors.Rose500
                    Text(
                        if (score >= (quiz.passThreshold * quiz.questions.size)) "PASSED" else "FAILED",
                        color = passColor,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                    
                    results.forEachIndexed { index, result ->
                        val q = quiz.questions.find { it.questionId == result.questionId }
                        if (q != null) {
                            Text(
                                "Q${index + 1}: ${q.questionText}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            val selectedText = q.options.find { it.id == result.selectedAnswer }?.text ?: "Unknown"
                            val correctText = q.options.find { it.id == q.correctAnswer }?.text ?: "Unknown"
                            
                            Text(
                                "You selected: $selectedText",
                                color = if (result.isCorrect) KaironexColors.SuccessGreen else KaironexColors.Rose500,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!result.isCorrect) {
                                Text(
                                    "Correct answer: $correctText",
                                    color = KaironexColors.SuccessGreen,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (q.explanation.isNotBlank()) {
                                     Text(
                                        "Explanation: ${q.explanation}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                     )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { 
                            onComplete(score.toFloat() / quiz.questions.size, results)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.ElectricBlue)
                    ) {
                        Text("Finish Review")
                    }

                } else {
                    // Quiz Taking Screen
                    if (question != null) {
                        Text(
                            "Question ${currentQuestionIndex + 1}/${quiz.questions.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = KaironexColors.SlateGray
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            question.questionText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(24.dp))

                        question.options.forEachIndexed { index, option ->
                            OutlinedButton(
                                onClick = { selectedOption = index },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedOption == index) KaironexColors.ElectricBlue.copy(alpha = 0.1f) else Color.Transparent,
                                    contentColor = if (selectedOption == index) KaironexColors.ElectricBlue else KaironexColors.InkBlack
                                )
                            ) {
                                Text(option.text, modifier = Modifier.fillMaxWidth())
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                                // Calculate result for this question
                                val isCorrect = question.options.getOrNull(selectedOption ?: -1)?.id == question.correctAnswer
                                
                                if (isCorrect) {
                                    score++
                                }
                                
                                results.add(
                                    QuestionResult(
                                        questionId = question.questionId,
                                        selectedAnswer = question.options.getOrNull(selectedOption ?: 0)?.id ?: "",
                                        isCorrect = isCorrect,
                                        timeSpentSeconds = ((now - questionStartTime) / 1000).toInt()
                                    )
                                )

                                if (currentQuestionIndex < quiz.questions.size - 1) {
                                    currentQuestionIndex++
                                    selectedOption = null
                                    questionStartTime = now
                                } else {
                                    // Finish -> Go to Review
                                    isReviewing = true
                                }
                            },
                            enabled = selectedOption != null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.ElectricBlue)
                        ) {
                            Text(if (currentQuestionIndex < quiz.questions.size - 1) "Next Question" else "Submit Quiz")
                        }
                    } else {
                        Text("Error: No questions found.")
                        Button(onClick = onDismiss) { Text("Close") }
                    }
                }
            }
        }
    }
}
