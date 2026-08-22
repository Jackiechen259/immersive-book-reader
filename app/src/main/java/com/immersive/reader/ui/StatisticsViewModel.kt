package com.immersive.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immersive.reader.core.data.BookRepository
import com.immersive.reader.core.data.ReadingSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class BookReadingStat(
    val title: String,
    val totalMillis: Long,
)

data class ReadingStatistics(
    val totalMillis: Long = 0L,
    val sessionCount: Int = 0,
    val completedCount: Int = 0,
    val interruptedCount: Int = 0,
    val perBook: List<BookReadingStat> = emptyList(),
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    sessionRepository: ReadingSessionRepository,
    bookRepository: BookRepository,
) : ViewModel() {
    private val counts = combine(
        sessionRepository.totalReadingMillis,
        sessionRepository.sessionCount,
        sessionRepository.completedCount,
        sessionRepository.interruptedCount,
    ) { total, sessions, completed, interrupted ->
        ReadingStatistics(totalMillis = total, sessionCount = sessions, completedCount = completed, interruptedCount = interrupted)
    }

    val statistics: StateFlow<ReadingStatistics> = combine(
        counts,
        sessionRepository.perBookReadingMillis,
        bookRepository.books,
    ) { base, perBook, books ->
        val titles = books.associateBy { it.id }
        base.copy(
            perBook = perBook.mapNotNull { total ->
                titles[total.bookId]?.let { book -> BookReadingStat(book.title, total.totalMillis) }
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingStatistics())
}
