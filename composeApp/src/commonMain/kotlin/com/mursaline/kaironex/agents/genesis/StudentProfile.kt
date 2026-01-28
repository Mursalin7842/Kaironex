package com.mursaline.kaironex.agents.genesis

import kotlinx.serialization.Serializable

@Serializable
data class StudentProfile(
    // --- IDENTITY & METADATA ---
    val name: String = "",
    
    // --- ACADEMIC LIFE ---
    val university: String = "",
    val major: String? = null,
    val semester: String? = null,        // Current Semester
    val totalSemesters: String? = null,  // New: Total Semesters
    val currentCgpa: String? = null,     // New: CGPA until now
    val academicResults: String? = null, // "Good", "CGPA 3.5" (Verbal)
    
    val isInternationalStudent: Boolean? = null, 
    val homeCountry: String? = null,             
    val currentCountry: String? = null,  // New: Where they are currently living

    // --- JOB LIFE ---
    val hasJob: Boolean? = null,
    val jobDescription: String? = null,          // "Barista", "Developer"
    val jobSchedule: String? = null,             // "9am to 5pm"
    val jobCommuteTime: String? = null,          // "30 mins"
    val jobWorkDays: String? = null,             // "Mon-Fri"
    val wantsJobHelp: Boolean? = null,           // New: If no job, do they want help?

    // --- STRATEGY ---
    val mainPriority: String? = null,            // "JOB", "CGPA"
    
    // --- PHYSICS (Energy) ---
    val energyPreference: String? = null,
    val dailyFocusCapacity: Int? = null,

    // --- CONSTRAINTS ---
    val learningStyle: String? = null,
    val protectedTime: String? = null,           // "Prayer, Gym"
    val failureCause: String? = null,

    // --- REAL WORLD SCHEDULE ---
    // Key: "Class Name", Value: "Time/Days"
    val classSchedule: Map<String, String> = emptyMap(),
    // Key: "Activity", Value: "Description/Time"
    val customCommitments: Map<String, String> = emptyMap(),
    // Key: "Constraint", Value: "Description" (Protected/Non-Negotiable)
    val nonNegotiables: Map<String, String> = emptyMap(),
    
    // Key: "Route" (e.g. Home-Uni), Value: "Time" (e.g. 30m)
    val commuteMap: Map<String, String> = emptyMap(),

    val routineFile: String? = null, // Path or Content of uploaded routine
    
    // --- INTERNATIONAL CONTEXT ---
    val visaStatus: String? = null,      // "F1", "Student Route"
    val workRestrictions: String? = null ,// "20h limit", "No work"

    // --- LEGACY/COMPATIBILITY FIELDS ---
    val secondaryPriority: String? = null,
    val universityLocation: String? = null,
    val targetCgpa: String? = null,
    val financialStakes: String? = null,
    val careerAmbition: String? = null,
    val sleepTime: String? = null,
    val wakeTime: String? = null,
    val needsJob: Boolean = false,
    val workHoursPerWeek: Double? = null,
    val commuteDuration: String? = null,
    val commuteTime: String? = null,
    val scheduleSource: String? = null,
    val primaryStudyMaterial: String? = null,
    val stressResponse: String? = null
)
