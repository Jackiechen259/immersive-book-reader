package com.immersive.reader.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immersive.reader.R
import com.immersive.reader.core.data.BookRepository
import com.immersive.reader.core.model.Book
import com.immersive.reader.core.model.ExitPolicy
import com.immersive.reader.core.model.TimerMode
import com.immersive.reader.focus.FocusCapabilities
import com.immersive.reader.focus.FocusCapabilityDetector
import com.immersive.reader.session.ReadingSessionCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val deepFocus: Boolean = false,
    val starting: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class StartSessionViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val coordinator: ReadingSessionCoordinator,
    private val capabilityDetector: FocusCapabilityDetector,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StartSessionUiState())
    val uiState: StateFlow<StartSessionUiState> = _uiState.asStateFlow()
    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book.asStateFlow()
    val focusCapabilities: FocusCapabilities = capabilityDetector.getCapabilities()

    fun loadBook(bookId: String) {
        viewModelScope.launch {
            val loaded = bookRepository.getBook(bookId)
            _book.value = loaded
            if (loaded == null) {
                _uiState.update { it.copy(errorMessage = context.getString(R.string.error_book_missing)) }
            }
        }
    }

    fun setTimerMode(mode: TimerMode) = _uiState.update {
        it.copy(timerMode = mode, deepFocus = if (mode == TimerMode.OPEN_ENDED) false else it.deepFocus, errorMessage = null)
    }
    fun setHours(hours: Int) = _uiState.update { it.copy(hours = hours.coerceIn(0, 99), errorMessage = null) }
    fun setMinutes(minutes: Int) = _uiState.update { it.copy(minutes = minutes.coerceIn(0, 59), errorMessage = null) }
    fun setLockEnvironment(enabled: Boolean) = _uiState.update { it.copy(lockEnvironment = enabled) }
    fun setDeepFocus(enabled: Boolean) = _uiState.update { it.copy(deepFocus = enabled, lockEnvironment = true) }

    fun start(bookId: String, onStarted: (String) -> Unit) {
        val current = _uiState.value
        val durationMillis = if (current.timerMode == TimerMode.COUNTDOWN) {
            (current.hours * 60L + current.minutes) * 60_000L
        } else {
            null
        }
        if (current.timerMode == TimerMode.COUNTDOWN && (durationMillis == null || durationMillis <= 0L)) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.error_countdown_too_short)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(starting = true, errorMessage = null) }
            runCatching {
                val book = bookRepository.getBook(bookId) ?: error(context.getString(R.string.error_book_missing))
                coordinator.start(
                    bookId = bookId,
                    timerMode = current.timerMode,
                    targetDurationMillis = durationMillis,
                    exitPolicy = when {
                        current.deepFocus -> ExitPolicy.TIME_LOCKED
                        current.lockEnvironment -> ExitPolicy.HOLD_TO_EXIT
                        else -> ExitPolicy.CONFIRM
                    },
                    startLocatorJson = book.lastLocatorJson,
                )
            }.onSuccess { session ->
                onStarted(session.id)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(starting = false, errorMessage = error.message ?: context.getString(R.string.error_start_reading))
                }
            }
        }
    }
}
