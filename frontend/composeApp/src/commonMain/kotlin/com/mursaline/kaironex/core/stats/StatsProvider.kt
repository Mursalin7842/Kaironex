package com.mursaline.kaironex.core.stats

/**
 * Stats Provider - Provides demo/dummy data for the stats system
 *
 * In production, this would connect to:
 * - Local database (SQLDelight)
 * - Gemini AI for predictions
 * - Firebase for sync
 */
object StatsProvider {

    /**
     * Get comprehensive Home stats
     */
    fun getHomeStats(): HomeStats {
        return HomeStats(
            cognitive = CognitiveStats(
                focusScore = 78,
                focusTrend = TrendDirection.UP,
                deepWorkMinutes = 94,
                deepWorkTarget = 120,
                distractionsBlocked = 12,
                cognitiveLoad = CognitiveLoadLevel.BALANCED,
                mentalFatigueIndex = FatigueLevel.LOW,
                retentionStrength = 0.72f,
                learningEfficiency = 0.85f
            ),
            learning = LearningStats(
                conceptMastery = mapOf(
                    "Data Structures" to 0.68f,
                    "Algorithms" to 0.54f,
                    "System Design" to 0.41f,
                    "OOP" to 0.92f,
                    "Databases" to 0.76f
                ),
                overallMastery = 0.66f,
                knowledgeRetention = 0.74f,
                topicsCompletedToday = 2,
                topicsCompletedThisWeek = 7,
                weakAreas = listOf(
                    WeakArea("Recursion", "Algorithms", 0.32f),
                    WeakArea("Heap Memory", "System Design", 0.28f),
                    WeakArea("SQL Joins", "Databases", 0.45f)
                ),
                mockTestAccuracyTrend = TrendDirection.UP
            ),
            habits = HabitStats(
                studyStreak = 5,
                longestStreak = 14,
                sessionsPlanned = 4,
                sessionsCompleted = 3,
                sessionsSkipped = 1,
                pomodoroSuccessRate = 0.82f,
                nextDeadline = Deadline(
                    title = "DSA Final Exam",
                    subject = "Data Structures",
                    daysRemaining = 12,
                    hoursRemaining = 8,
                    urgencyLevel = UrgencyLevel.MEDIUM
                )
            ),
            mentalState = MentalStateStats(
                motivationLevel = 72,
                motivationTrend = TrendDirection.STABLE,
                burnoutRisk = RiskLevel.LOW,
                confidenceTrend = TrendDirection.UP,
                currentMode = StudyMode.DEEP,
                aiInsight = "Your mind is sharp but nearing optimal load — consider a 15-min break soon."
            ),
            pressure = PressureStats(
                pressureIndex = 45,
                pressureTrend = TrendDirection.DOWN,
                distractionRiskLevel = RiskLevel.LOW,
                scheduleStability = 0.78f,
                deadlinePressure = 0.52f,
                burnoutProbability = 0.18f,
                crisisProximity = CrisisProximity(
                    examDays = 12,
                    financialStressDays = null,
                    deadlineDays = 5,
                    interviewDays = 21
                ),
                lifeInterferenceRatio = 0.32f
            ),
            direction = DirectionStats(
                goalAlignment = 0.81f,
                careerPathProgress = 0.34f,
                skillTreeProgress = 0.47f,
                milestoneDistance = 18,
                trajectoryStatus = TrajectoryStatus.RISING,
                futureReadinessScore = 72
            )
        )
    }

    /**
     * Get comprehensive More/Life Support stats
     */
    fun getMoreStats(): MoreStats {
        return MoreStats(
            lifeStability = LifeStabilityScore(
                overallScore = 76,
                campaignContribution = 0.28f,
                vitalityContribution = 0.42f,
                radiusContribution = 0.30f,
                trend = TrendDirection.STABLE
            ),
            campaign = CampaignStats(
                applicationsSent = 24,
                interviewsScheduled = 3,
                interviewSuccessRate = 0.67f,
                skillsProgress = 0.58f,
                codingPracticeMinutes = 320,
                resumeStrengthScore = 78,
                hiringProbability = 0.42f,
                marketFitScore = 0.71f,
                employerResponseRate = 0.18f,
                interviewConfidence = 65,
                faangReadinessScore = 48,
                agentStatus = AgentStatus.ATTENTION,
                riskAlerts = listOf(
                    "System Design practice needed"
                ),
                isCalibrated = false
            ),
            vitality = VitalityStats(
                totalBalance = 0.0,
                monthlyBills = 0.0,
                nextPayday = null,
                victoryMeal = "Pizza",
                budgetRunwayDays = 0,
                monthlyBurnRate = 0f,
                savingsProgress = 0f,
                emergencyFundPercent = 0f,
                mealsInFridge = 0,
                mealsPlanned = 0,
                mealsSkipped = 0,
                nutritionAdequacy = 0.72f,
                groceryEfficiency = 0.68f,
                sleepQualityScore = 71,
                energyLevelToday = 78,
                fatigueIndex = FatigueLevel.LOW,
                focusBodyCorrelation = 0.62f,
                burnoutRisk = RiskLevel.LOW,
                agentStatus = AgentStatus.STABLE,
                riskAlerts = listOf(
                    "Emergency fund below recommended 3 months"
                )
            ),
            radius = RadiusStats(
                visaDaysRemaining = 89,
                housingStabilityScore = 92,
                utilityReadiness = 1.0f,
                socialInteractionCount = 8,
                languageFluencyScore = 78,
                culturalComfort = 0.72f,
                scamRiskAlerts = 0,
                safeZoneAwareness = 0.85f,
                localKnowledgeScore = 64,
                agentStatus = AgentStatus.ATTENTION,
                riskAlerts = listOf(
                    "Visa renewal in 89 days - start paperwork"
                )
            ),
            systemMeta = SystemMetaStats(
                studentOperatingCapacity = 68,
                lifeToStudyInterference = 0.32f,
                studyCapacityRemaining = 0.68f,
                aiAssistEffectiveness = AIEffectiveness(
                    focusImprovement = 0.18f,
                    learningSpeedBoost = 0.22f,
                    stressReduction = 0.41f
                ),
                behaviorConsistencyScore = 74,
                disciplineReliabilityIndex = 81,
                selfControlStrength = 72,
                decisionQualityScore = 78
            )
        )
    }
}
