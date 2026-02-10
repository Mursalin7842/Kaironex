package com.mursaline.kaironex.agents.genesis

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GenesisAgent {
    
    // Helper to simulate "Agent Memory"
    private val _profile = MutableStateFlow(StudentProfile())
    val profileProto: StateFlow<StudentProfile> = _profile.asStateFlow()
    
    // Direct Access for ViewModel
    var profile: StudentProfile
        get() = _profile.value
        private set(value) {
            _profile.value = value
        }

    fun manualUpdate(name: String) {
        profile = profile.copy(name = name)
    }

    fun updateProfile(newProfile: StudentProfile) {
        profile = newProfile
    }

    fun restoreProfile(savedProfile: StudentProfile) {
        profile = savedProfile
    }
}
