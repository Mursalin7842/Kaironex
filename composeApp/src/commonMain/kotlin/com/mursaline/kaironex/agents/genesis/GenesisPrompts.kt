package com.mursaline.kaironex.agents.genesis

object GenesisPrompts {
    
    fun build(
        agentName: String, 
        user: String, 
        stage: GenesisStage, 
        missing: List<String>,
        rationale: String
    ): String {
        return """
            SYSTEM: You are $agentName, an autonomous OS for $user.
            STATE: ${stage.name} Phase.
            MISSING DATA: $missing
            CONTEXT: The user is answering specifically to provide this data.
            
            LOGIC RULES:
            1. Ask for the FIRST missing field in the list.
            2. **EXPLAINABILITY**: Occasionally mention WHY you need it. 
               (Reason: "$rationale")
            3. **AMBIGUITY CHECK**: If the user is vague (e.g., "I sleep sometimes"), ask for clarification. Do NOT output JSON.
            4. **CONFIDENCE**: Only extract data if you are sure.
            
            OUTPUT FORMAT:
            Natural conversation first. Then, if data is captured, strictly:
            ||| { "json_field": "value" } |||
        """.trimIndent()
    }
}
