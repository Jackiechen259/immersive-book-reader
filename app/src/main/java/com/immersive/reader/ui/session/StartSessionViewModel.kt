package com.immersive.reader.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immersive.reader.core.data.BookRepository
import com.immersive.reader.core.model.ExitPolicy
import com.immersive.reader.core.model.TimerMode
import com.immersive.reader.session.ReadingSessionCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StartSessionUiState(
    val timerMode: TimerMode = TimerMode.OPEN_ENDED,
    val hours: Int = 0,
    val minutes: Int = 30,
    val lockEnvironment: Boolean = true,
    val starting: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class StartSessionViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val coordinator: ReadingSessionCoordinator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StartSessionUiState())
    val uiState: StateFlow<StartSessionUiState> = _uiState.asStateFlow()

    fun setTimerMode(mode: TimerMode) = _uiState.update { it.copy(timerMode = mode, errorMessage = null) }
    fun setHours(hours: Int) = _uiState.update { it.copy(hours = hours.coerceIn(0, 99), errorMessage = null) }
    fun setMinutes(minutes: Int) = _uiState.update { it.copy(minutes = minutes.coerceIn(0, 59), errorMessage = null) }
    fun setLockEnvironment(enabled: Boolean) = _uiState.update { it.copy(lockEnvironment = enabled) }

    fun start(bookId: String, onStarted: (String) -> Unit) {
        val current = _uiState.value
        val durationMillis = if (current.timerMode == TimerMode.COUNTDOWN) {
            (current.hours * 60L + current.minutes) * 60_000L
        } else {
            null
        }
        if (current.timerMode == TimerMode.COUNTDOWN && (durationMillis == null || durationMillis <= 0L)) {
            _uiState.update { it.copy(errorMessage = "Choose a countdown longer than one minute") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(starting = true, errorMessage = null) }
            runCatching {
                val book = bookRepository.getBook(bookId) ?: error("This book is no longer in your library")
                coordinator.start(
                    bookId = bookId,
                    timerMode = current.timerMode,
                    targetDurationMillis = durationMillis,
                    exitPolicy = if (current.lockEnvironment) ExitPolicy.HOLD_TO_EXIT else ExitPolicy.CONFIRM,
                    startLocatorJson = book.lastLocatorJson,
                )
            }.onSuccess { session ->
                onStarted(session.id)
            }.onFailure { error ->
                _uiState.update { it.copy(starting = false, errorMessage = error.message ?: "Unable to start reading") }
            }
        }
    }
}
