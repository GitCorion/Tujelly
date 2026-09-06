package com.example.tujelly.ui.screens.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tujelly.data.local.UserPreferencesRepository
import com.example.tujelly.data.model.DEFAULT_SELECTED_PLATFORMS
import com.example.tujelly.data.model.SUPPORTED_PLATFORMS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlatformSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)

    private val _selected = MutableStateFlow<Set<String>>(DEFAULT_SELECTED_PLATFORMS)
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    fun togglePlatform(id: String) {
        val current = _selected.value
        _selected.value = if (id in current) current - id else current + id
    }

    fun selectAll() {
        _selected.value = SUPPORTED_PLATFORMS.map { it.id }.toSet()
    }

    fun deselectAll() {
        _selected.value = emptySet()
    }

    fun onContinue(onDone: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.updateSelectedPlatforms(_selected.value)
            userPreferencesRepository.markOnboardingCompleted()
            onDone()
        }
    }
}
