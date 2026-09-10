package com.immersive.reader.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immersive.reader.achievement.AchievementEvaluator
import com.immersive.reader.achievement.AchievementProgress
import com.immersive.reader.achievement.AchievementRepository
import com.immersive.reader.achievement.AchievementUnlockSync
import com.immersive.reader.core.data.BookRepository
import com.immersive.reader.core.data.ReadingSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    bookRepository: BookRepository,
    sessionRepository: ReadingSessionRepository,
    achievementRepository: AchievementRepository,
) : ViewModel() {
    init {
        viewModelScope.launch {
            combine(bookRepository.books, sessionRepository.sessions) { books, sessions ->
                books to sessions
            }.collect { (books, sessions) ->
                achievementRepository.sync(
                    AchievementEvaluator.evaluate(books, sessions, ZoneId.systemDefault()),
                )
            }
        }
    }

    val items: StateFlow<List<AchievementProgress>> = combine(
        bookRepository.books,
        sessionRepository.sessions,
        achievementRepository.unlocks,
    ) { books, sessions, unlocks ->
        AchievementUnlockSync.present(
            AchievementEvaluator.evaluate(books, sessions, ZoneId.systemDefault()),
            unlocks.associateBy { it.id },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AchievementEvaluator.evaluate(emptyList(), emptyList(), ZoneId.systemDefault()),
    )
}
