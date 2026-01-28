package com.mursaline.kaironex.features.genesis

import com.mursaline.kaironex.agents.genesis.StudentProfile

object GenesisFlow {

    data class InterviewStep(
        val fieldId: String,
        val instruction: String,
        val isTerminal: Boolean = false
    )

    fun getNextStep(profile: StudentProfile): InterviewStep {
        
        // --- 1. ACADEMIC BASE (The Foundation) ---
        if (profile.university.isBlank()) 
            return ask("university", "First, which university are you currently attending?")
        
        if (profile.major.isNullOrBlank()) 
            return ask("major", "Nice. And what is your major or department?")
        
        if (profile.totalSemesters.isNullOrBlank()) 
            return ask("totalSemesters", "How many total semesters are in your program?")
            
        if (profile.semester.isNullOrBlank()) 
            return ask("semester", "Which semester are you currently in?")

        if (profile.currentCgpa.isNullOrBlank()) 
             return ask("currentCgpa", "What is your current CGPA until now?")

        // --- 2. INTERNATIONAL STATUS (Crucial Branch) ---
        if (profile.isInternationalStudent == null) 
            return ask("isInternationalStudent", "Are you studying as an international student?")

        if (profile.isInternationalStudent == true) {
            if (profile.homeCountry.isNullOrBlank()) 
                return ask("homeCountry", "Where are you originally from?")
            if (profile.currentCountry.isNullOrBlank()) 
                return ask("currentCountry", "And which country are you studying in right now?")
            if (profile.visaStatus.isNullOrBlank()) 
                return ask("visaStatus", "What is your visa status? (for example F1, Student Route)")
            if (profile.workRestrictions.isNullOrBlank()) 
                return ask("workRestrictions", "Do you have any strict work hour limits I should know about?")
        }

        // --- 3. JOB & CAREER (Complex Branch) ---
        if (profile.hasJob == null) 
            return ask("hasJob", "Do you currently have a part-time job or internship alongside your studies?")

        if (profile.hasJob == true) {
            // Employed Logic
            if (profile.jobDescription.isNullOrBlank()) 
                return ask("jobDescription", "That is hardworking. What is your role? (for example Barista, Developer)")
            
            if (profile.jobSchedule.isNullOrBlank()) 
                return ask("jobSchedule", "What are your usual work timings? (for example 9am to 5pm)")

             if (profile.jobWorkDays.isNullOrBlank()) 
                return ask("jobWorkDays", "And which days of the week do you work?")
            
            if (profile.jobCommuteTime.isNullOrBlank()) 
                return ask("jobCommuteTime", "How long does it take to get from Uni to your Job?")
        } else {
            // Unemployed Logic
            if (profile.wantsJobHelp == null) 
                return ask("wantsJobHelp", "I see. Would you like Kaironex to help you find a suitable job?")
        }

        // --- 4. COMMUTE (If not captured in Job) ---
        // Note: Logic above captures Uni-Job, here we want general Uni commute
        if (profile.commuteDuration.isNullOrBlank()) 
            return ask("commuteDuration", "Roughly how long is your daily commute to university?")

        // --- 5. STRATEGY & GOALS ---
        val priorityPrompt = if (profile.hasJob == true) "Improving Job Performance" else "Getting a Job"
        if (profile.mainPriority.isNullOrBlank()) 
            return ask("mainPriority", "Moving on. What is your #1 priority right now: $priorityPrompt or Maximizing CGPA?")
        
        // Removed targetCgpa/financialStakes for brevity as per user "Minimise Load" request,
        // but adding Energy/Biology as they are key features
        
        // --- 6. BIOLOGY ---
        if (profile.energyPreference.isNullOrBlank()) 
            return ask("energyPreference", "Let's check your biology. Are you a Morning Bird or a Night Owl?")
            
         if (profile.dailyFocusCapacity == null)
            return ask("dailyFocusCapacity", "Realistically, how many hours of deep focus can you manage per day?")

        // --- 7. CONSTRAINTS ---
        if (profile.learningStyle.isNullOrBlank()) 
            return ask("learningStyle", "Do you learn better by Watching Videos or Reading Documentation?")

        if (profile.failureCause.isNullOrBlank()) 
            return ask("failureCause", "Last one: What usually stops you from studying? Is it Phone Distraction, Fatigue, or Confusion?")

        // --- COMPLETE ---
        return InterviewStep(
            fieldId = "DONE",
            instruction = "Perfect! Profile Calibrated. 🚀 I have everything I need. Your engine is ready. Just say Hey Kaironex whenever you need me.",
            isTerminal = true
        )
    }

    private fun ask(id: String, text: String) = InterviewStep(id, text)
}
