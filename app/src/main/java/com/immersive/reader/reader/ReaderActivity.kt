package com.immersive.reader.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.fragment.app.commit
import com.immersive.reader.R
import com.immersive.reader.core.model.ThemeMode
import com.immersive.reader.ui.theme.ImmersiveReaderTheme
import androidx.lifecycle.lifecycleScope
import com.immersive.reader.core.datastore.ReaderPreferences
import com.immersive.reader.core.datastore.ReaderPreferencesRepository
import com.immersive.reader.core.data.BookRepository
import com.immersive.reader.focus.FocusController
import com.immersive.reader.core.model.ExitPolicy
import com.immersive.reader.core.model.SessionStatus
import com.immersive.reader.reader.readium.ReadiumPublicationManager
import com.immersive.reader.session.ReadingSessionCoordinator
import com.immersive.reader.session.ReadingSessionState
import com.immersive.reader.ui.reader.ReaderOverlay
import com.immersive.reader.ui.reader.ReaderExitDialogState
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

@AndroidEntryPoint
class ReaderActivity : AppCompatActivity(), ReaderProgressListener {
    @Inject lateinit var bookRepository: BookRepository
    @Inject lateinit var readium: ReadiumPublicationManager
    @Inject lateinit var publicationStore: PublicationStore
    @Inject lateinit var preferencesRepository: ReaderPreferencesRepository
    @Inject lateinit var sessionCoordinator: ReadingSessionCoordinator
    @Inject lateinit var focusController: FocusController

    private var bookTitle by mutableStateOf("")
    private var loading by mutableStateOf(true)
    private var errorMessage by mutableStateOf<String?>(null)
    private var controlsVisible by mutableStateOf(false)
    private var progression by mutableStateOf<Double?>(null)
    private var readerPreferences by mutableStateOf(ReaderPreferences())
    private var sessionElapsedMillis by mutableStateOf<Long?>(null)
    private var sessionRemainingMillis by mutableStateOf<Long?>(null)
    private var exitDialog by mutableStateOf<ReaderExitDialogState?>(null)
    private var summaryLaunched = false
    private var focusEntered = false
    private var hideControlsJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        focusController.attach(this)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = requestExit()
        })
        enableEdgeToEdge()
        bookTitle = getString(R.string.opening_book)
        applyStatusBarAppearance(ThemeMode.LIGHT)

        val bookId = intent.getStringExtra(EXTRA_BOOK_ID).orEmpty()
        if (bookId.isBlank()) {
            finish()
            return
        }

        val navigatorContainerId = View.generateViewId()
        val root = FrameLayout(this)
        root.setBackgroundColor(pageColor(ThemeMode.LIGHT))
        root.addView(
            FrameLayout(this).apply { id = navigatorContainerId },
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
        )
        setContentView(root)
        lifecycleScope.launch {
            preferencesRepository.preferences.collect {
                readerPreferences = it
                root.setBackgroundColor(pageColor(it.theme))
                applyStatusBarAppearance(it.theme)
                if (it.keepScreenAwake) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        lifecycleScope.launch {
            sessionCoordinator.state.collect { state ->
                when (state) {
                    is ReadingSessionState.Active -> {
                        sessionElapsedMillis = state.elapsedMillis
                        sessionRemainingMillis = state.remainingMillis
                    }
                    is ReadingSessionState.Finished -> {
                        sessionElapsedMillis = state.session.accumulatedReadingMillis
                        sessionRemainingMillis = null
                        if (state.session.bookId == bookId && !isFinishing && !summaryLaunched) {
                            summaryLaunched = true
                            lifecycleScope.launch {
                                focusController.exit()
                                startActivity(SessionSummaryActivity.intent(this@ReaderActivity, state.session.id))
                                finish()
                            }
                        }
                    }
                    ReadingSessionState.Idle -> Unit
                }
            }
        }
        val overlay = androidx.compose.ui.platform.ComposeView(this).apply {
            setContent {
                ImmersiveReaderTheme(themeMode = readerPreferences.theme) {
                    ReaderOverlay(
                        title = bookTitle,
                        loading = loading,
                        errorMessage = errorMessage,
                        controlsVisible = controlsVisible,
                        progression = progression,
                        preferences = readerPreferences,
                        sessionElapsedMillis = sessionElapsedMillis,
                        sessionRemainingMillis = sessionRemainingMillis,
                        exitDialog = exitDialog,
                        onOpenSettings = { controlsVisible = true },
                        onRequestExit = ::requestExit,
                        onDismissExit = { exitDialog = null },
                        onRequestEmergencyExit = { exitDialog = exitDialog?.copy(emergency = true) },
                        onEndSession = ::endSession,
                        onLeaveReader = ::leaveReader,
                        onThemeChange = { lifecycleScope.launch { preferencesRepository.setTheme(it) } },
                        onFontSizeChange = { lifecycleScope.launch { preferencesRepository.setFontSize(it) } },
                        onLineHeightChange = { lifecycleScope.launch { preferencesRepository.setLineHeight(it) } },
                        onReadingModeChange = { lifecycleScope.launch { preferencesRepository.setReadingMode(it) } },
                    )
                }
            }
        }
        root.addView(
            overlay,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
        )

        lifecycleScope.launch {
            val restoredSession = intent.getStringExtra(EXTRA_SESSION_ID)?.let { sessionCoordinator.restore() }
            val book = bookRepository.getBook(bookId)
            if (book == null) {
                showError(getString(R.string.error_book_missing))
                return@launch
            }
            bookTitle = book.title
            val publication = readium.open(File(book.filePath)).getOrElse { failure ->
                showError(failure.message ?: getString(R.string.error_open_this_epub))
                return@launch
            }
            publicationStore.put(bookId, publication)
            supportFragmentManager.commit {
                replace(
                    navigatorContainerId,
                    EpubReaderFragment.newInstance(book.id, book.lastLocatorJson),
                    READER_TAG,
                )
            }
            restoredSession?.takeIf { it.bookId == bookId }?.let {
                focusController.enter(it)
                focusEntered = true
            }
            loading = false
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && focusEntered && !isFinishing) {
            lifecycleScope.launch { focusController.restore() }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (!loading && errorMessage == null) {
            controlsVisible = true
            hideControlsJob?.cancel()
            hideControlsJob = lifecycleScope.launch {
                delay(CONTROLS_TIMEOUT_MILLIS)
                controlsVisible = false
            }
        }
    }

    override fun onProgressChanged(progression: Double?) {
        this.progression = progression
    }

    override fun onDestroy() {
        hideControlsJob?.cancel()
        focusController.detach(this)
        super.onDestroy()
    }

    private fun leaveReader() {
        lifecycleScope.launch {
            focusController.exit()
            finish()
        }
    }

    private fun applyStatusBarAppearance(theme: ThemeMode) {
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            theme != ThemeMode.DARK
    }

    private fun requestExit() {
        val active = sessionCoordinator.state.value as? ReadingSessionState.Active
        if (active == null) {
            lifecycleScope.launch {
                focusController.exit()
                finish()
            }
            return
        }
        exitDialog = ReaderExitDialogState(active = active)
    }

    private fun endSession(emergency: Boolean) {
        val active = sessionCoordinator.state.value as? ReadingSessionState.Active ?: return
        summaryLaunched = true
        lifecycleScope.launch {
            val book = bookRepository.getBook(active.session.bookId)
            val finishedSession = sessionCoordinator.finish(
                status = if (emergency) SessionStatus.EMERGENCY_EXIT else SessionStatus.USER_ENDED,
                endLocatorJson = book?.lastLocatorJson,
                emergencyExit = emergency,
            )
            focusController.exit()
            exitDialog = null
            finishedSession?.let { session ->
                startActivity(SessionSummaryActivity.intent(this@ReaderActivity, session.id))
                finish()
            }
        }
    }

    private fun showError(message: String) {
        loading = false
        errorMessage = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val EXTRA_BOOK_ID = "book_id"
        private const val READER_TAG = "epub_reader"
        private const val CONTROLS_TIMEOUT_MILLIS = 4_500L

        private const val EXTRA_SESSION_ID = "session_id"

        fun intent(context: Context, bookId: String, sessionId: String? = null): Intent =
            Intent(context, ReaderActivity::class.java).apply {
                putExtra(EXTRA_BOOK_ID, bookId)
                sessionId?.let { putExtra(EXTRA_SESSION_ID, it) }
            }

        private fun pageColor(theme: ThemeMode): Int = android.graphics.Color.parseColor(
            when (theme) {
                ThemeMode.DARK -> "#141C19"
                ThemeMode.SEPIA -> "#EFE4CF"
                ThemeMode.LIGHT -> "#F6F3EA"
            },
        )
    }
}
