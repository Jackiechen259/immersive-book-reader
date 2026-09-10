package com.immersive.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immersive.reader.core.datastore.ReaderPreferences
import com.immersive.reader.core.datastore.ReaderPreferencesRepository
import com.immersive.reader.core.model.ReadingMode
import com.immersive.reader.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ReaderPreferencesViewModel @Inject constructor(
    private val repository: ReaderPreferencesRepository,
) : ViewModel() {
    val preferences: StateFlow<ReaderPreferences> = repository.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderPreferences())
    fun setTheme(value: ThemeMode) = viewModelScope.launch { repository.setTheme(value) }
    fun setFontSize(value: Float) = viewModelScope.launch { repository.setFontSize(value) }
    fun setLineHeight(value: Float) = viewModelScope.launch { repository.setLineHeight(value) }
    fun setReadingMode(value: ReadingMode) = viewModelScope.launch { repository.setReadingMode(value) }
    fun setKeepScreenAwake(value: Boolean) = viewModelScope.launch { repository.setKeepScreenAwake(value) }
    fun setUseDnd(value: Boolean) = viewModelScope.launch { repository.setUseDnd(value) }
    fun setAchievementsVisible(value: Boolean) = viewModelScope.launch { repository.setAchievementsVisible(value) }
}
