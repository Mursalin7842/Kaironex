package com.mursaline.kaironex.features.study.mocktest

/**
 * Mock Test Data Models and Widget
 */
data class MockTest(
    val id: String,
    val title: String,
    val subject: String,
    val questions: List<MockTestQuestion>,
    val timeLimitSeconds: Int = 1800
)

data class MockTestQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String = ""
)

data class MockTestResult(
    val testId: String,
    val correctCount: Int,
    val totalCount: Int,
    val percentage: Int,
    val passed: Boolean,
    val timeTaken: Int
)
