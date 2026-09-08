package com.immersive.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immersive.reader.core.data.BookRepository
import com.immersive.reader.core.model.TimerMode
import com.immersive.reader.session.ReadingSessionCoordinator
import com.immersive.reader.session.ReadingSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SessionRecoveryUi(
    val bookId: String,
    val sessionId: String,
    val bookTitle: String,
    val elapsedMillis: Long,
    val remainingMillis: Long?,
    val timed: Boolean,
)

@HiltViewModel
class SessionStateViewModel @Inject constructor(
    coordinator: ReadingSessionCoordinator,
    bookRepository: BookRepository,
) : ViewModel() {
    val state: StateFlow<ReadingSessionState> = coordinator.state

    val recovery: StateFlow<SessionRecoveryUi?> = combine(coordinator.state, bookRepository.books) { sessionState, books ->
        val active = sessionState as? ReadingSessionState.Active ?: return@combine null
        val title = books.firstOrNull { it.id == active.session.bookId }?.title ?: "your book"
        SessionRecoveryUi(
            bookId = active.session.bookId,
            sessionId = active.session.id,
            bookTitle = title,
            elapsedMillis = active.elapsedMillis,
            remainingMillis = active.remainingMillis,
            timed = active.session.timerMode == TimerMode.COUNTDOWN,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
