package com.immersive.reader.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.immersive.reader.core.data.BookRepository
import com.immersive.reader.core.data.ReadingSessionRepository
import com.immersive.reader.core.datastore.ReaderPreferencesRepository
import com.immersive.reader.core.model.ExitPolicy
import com.immersive.reader.core.model.SessionStatus
import com.immersive.reader.core.model.ThemeMode
import com.immersive.reader.core.model.TimerMode
import com.immersive.reader.session.ReadingSessionCoordinator
import com.immersive.reader.ui.session.SessionSummaryScreen
import com.immersive.reader.ui.theme.ImmersiveReaderTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator

data class SessionSummaryUiState(
    val title: String,
    val author: String?,
    val coverPath: String?,
    val status: SessionStatus,
    val durationMillis: Long,
    val startProgression: Double?,
    val endProgression: Double?,
    val bookId: String,
)

@AndroidEntryPoint
class SessionSummaryActivity : ComponentActivity() {
    @Inject lateinit var sessionRepository: ReadingSessionRepository
    @Inject lateinit var bookRepository: BookRepository
    @Inject lateinit var sessionCoordinator: ReadingSessionCoordinator
    @Inject lateinit var preferencesRepository: ReaderPreferencesRepository

    private var summary by mutableStateOf<SessionSummaryUiState?>(null)
    private var themeMode by mutableStateOf(ThemeMode.LIGHT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID).orEmpty()
        setContent {
            ImmersiveReaderTheme(themeMode = themeMode) {
                SessionSummaryScreen(
                    summary = summary,
                    onBackToLibrary = ::finish,
                    onContinueReading = ::continueReading,
                )
            }
        }
        lifecycleScope.launch {
            preferencesRepository.preferences.collect { themeMode = it.theme }
        }
        lifecycleScope.launch {
            val session = sessionRepository.getById(sessionId) ?: return@launch
            val book = bookRepository.getBook(session.bookId)
            summary = SessionSummaryUiState(
                title = book?.title ?: "Book",
                author = book?.author,
                coverPath = book?.coverPath,
                status = session.status,
                durationMillis = session.accumulatedReadingMillis,
                startProgression = progression(session.startLocatorJson),
                endProgression = progression(session.endLocatorJson) ?: book?.progression,
                bookId = session.bookId,
            )
        }
    }

    private fun continueReading() {
        val current = summary ?: return
        lifecycleScope.launch {
            val book = bookRepository.getBook(current.bookId) ?: return@launch
            val session = sessionCoordinator.start(
                bookId = book.id,
                timerMode = TimerMode.OPEN_ENDED,
                targetDurationMillis = null,
                exitPolicy = ExitPolicy.HOLD_TO_EXIT,
                startLocatorJson = book.lastLocatorJson,
            )
            startActivity(ReaderActivity.intent(this@SessionSummaryActivity, book.id, session.id))
            finish()
        }
    }

    private fun progression(json: String?): Double? = json?.let {
        runCatching {
            val locator = Locator.fromJSON(JSONObject(it))
            locator?.locations?.totalProgression ?: locator?.locations?.progression
        }.getOrNull()
    }

    companion object {
        private const val EXTRA_SESSION_ID = "session_id"

        fun intent(context: Context, sessionId: String): Intent =
            Intent(context, SessionSummaryActivity::class.java).putExtra(EXTRA_SESSION_ID, sessionId)
    }
}
