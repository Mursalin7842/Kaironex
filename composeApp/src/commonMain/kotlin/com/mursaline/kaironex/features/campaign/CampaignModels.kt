package com.mursaline.kaironex.features.campaign

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Represents the full state of the Campaign Zone.
 * Sourced from 'campaign_state' table in Appwrite.
 */
@Serializable
data class CampaignState(
    val skillTree: List<SkillNode> = emptyList(),
    val questBoard: List<Quest> = emptyList(),
    val armory: ArmoryState = ArmoryState(),
    val simulacrum: SimulacrumState = SimulacrumState(),
    val lastAtsAnalysis: AtsAnalysisResult? = null,
    val lastInterviewDesign: InterviewDesign? = null,
    val isCalibrated: Boolean = false
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
    val icon: String = "✨",
    val learningResources: List<LearningResource> = emptyList(),
    val verification: String? = null
)

@Serializable
data class LearningResource(
    val type: String, // "course", "practice", "video", "book"
    val name: String,
    val url: String? = null,
    val count: Int? = null
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
    val blueprints: List<ArmoryItem> = emptyList()
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

// ============================================================================
// ATS RESUME ANALYSIS
// ============================================================================
@Serializable
data class AtsAnalysisResult(
    val atsScore: Int = 0,
    val scoreBreakdown: AtsScoreBreakdown? = null,
    val matchedKeywords: List<String> = emptyList(),
    val missingKeywords: List<String> = emptyList(),
    val criticalImprovements: List<ResumeImprovement> = emptyList(),
    val bulletImprovements: List<BulletImprovement> = emptyList(),
    val checklist: List<AtsChecklistItem> = emptyList(),
    val overallAssessment: String = "",
    val interviewReady: Boolean = false,
    val estimatedPassRate: String = ""
)

@Serializable
data class AtsScoreBreakdown(
    val keywordMatch: Int = 0,
    val experienceFit: Int = 0,
    val formatStructure: Int = 0,
    val quantification: Int = 0,
    val actionVerbs: Int = 0
)

@Serializable
data class ResumeImprovement(
    val issue: String,
    val current: String,
    val suggested: String,
    val priority: String // "HIGH", "MEDIUM", "LOW"
)

@Serializable
data class BulletImprovement(
    val original: String,
    val improved: String,
    val reason: String
)

@Serializable
data class AtsChecklistItem(
    val item: String,
    val status: String, // "pending", "completed"
    val impact: String // "HIGH", "MEDIUM", "LOW"
)

// ============================================================================
// INTERVIEW DESIGN (Simulacrum)
// ============================================================================
@Serializable
data class InterviewDesign(
    val interviewId: String = "",
    val durationMinutes: Int = 25,
    val interviewer: InterviewerPersona = InterviewerPersona(),
    val questionBank: List<InterviewQuestion> = emptyList(),
    val geminiLivePrompt: String = "",
    val difficultyModifiers: DifficultyModifiers = DifficultyModifiers(),
    val candidatePrepNotes: List<String> = emptyList()
)

@Serializable
data class InterviewerPersona(
    val name: String = "Interviewer",
    val role: String = "Technical Recruiter",
    val company: String = "TechCorp",
    val personality: String = "Professional"
)

@Serializable
data class InterviewQuestion(
    val id: String,
    val category: String, // "icebreaker", "technical", "behavioral", "situational", "closing"
    val question: String,
    val followUps: List<String> = emptyList(),
    val goodAnswerCriteria: List<String> = emptyList(),
    val redFlags: List<String> = emptyList()
)

@Serializable
data class DifficultyModifiers(
    val current: String = "medium",
    val instruction: String = ""
)

// ============================================================================
// GENERATED RESUME
// ============================================================================
@Serializable
data class GeneratedResume(
    val professionalSummary: String = "",
    val skillsSection: List<String> = emptyList(),
    val experienceSection: List<ResumeExperience> = emptyList(),
    val projectsSection: List<ResumeProject> = emptyList(),
    val educationSection: ResumeEducation? = null,
    val suggestions: List<ResumeSuggestion> = emptyList(),
    val skillGaps: List<SkillGap> = emptyList(),
    val interviewPrepTopics: List<String> = emptyList(),
    val estimatedAtsScore: Int = 0,
    val docxBase64: String? = null,       // Base64 encoded DOCX file
    val docxFilename: String? = null      // Suggested filename for download
)

@Serializable
data class ResumeExperience(
    val title: String,
    val organization: String,
    val duration: String,
    val bullets: List<String> = emptyList()
)

@Serializable
data class ResumeProject(
    val name: String,
    val technologies: List<String> = emptyList(),
    val description: String,
    val achievements: List<String> = emptyList()
)

@Serializable
data class ResumeEducation(
    val degree: String,
    val institution: String,
    val year: String,
    val relevantCoursework: List<String> = emptyList()
)

@Serializable
data class ResumeSuggestion(
    val category: String,
    val suggestion: String,
    val priority: String
)

@Serializable
data class SkillGap(
    val skill: String,
    val importance: String,
    val learningPath: String
)

// ============================================================================
// SIMULACRUM STATE
// ============================================================================
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
