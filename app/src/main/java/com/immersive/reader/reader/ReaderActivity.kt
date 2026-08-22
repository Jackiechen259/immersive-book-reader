package com.immersive.reader.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.immersive.reader.core.datastore.ReaderPreferences
import com.immersive.reader.core.datastore.ReaderPreferencesRepository
import com.immersive.reader.core.data.BookRepository
import com.immersive.reader.reader.readium.ReadiumPublicationManager
import com.immersive.reader.ui.reader.ReaderOverlay
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

@AndroidEntryPoint
class ReaderActivity : FragmentActivity(), ReaderProgressListener {
    @Inject lateinit var bookRepository: BookRepository
    @Inject lateinit var readium: ReadiumPublicationManager
    @Inject lateinit var publicationStore: PublicationStore
    @Inject lateinit var preferencesRepository: ReaderPreferencesRepository

    private var bookTitle by mutableStateOf("Opening book…")
    private var loading by mutableStateOf(true)
    private var errorMessage by mutableStateOf<String?>(null)
    private var controlsVisible by mutableStateOf(false)
    private var progression by mutableStateOf<Double?>(null)
    private var readerPreferences by mutableStateOf(ReaderPreferences())
    private var hideControlsJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        val bookId = intent.getStringExtra(EXTRA_BOOK_ID).orEmpty()
        if (bookId.isBlank()) {
            finish()
            return
        }

        val navigatorContainerId = View.generateViewId()
        val root = FrameLayout(this)
        root.setBackgroundColor(android.graphics.Color.WHITE)
        root.addView(
            FrameLayout(this).apply { id = navigatorContainerId },
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
        )
        setContentView(root)
        lifecycleScope.launch {
            preferencesRepository.preferences.collect { readerPreferences = it }
        }
        val overlay = androidx.compose.ui.platform.ComposeView(this).apply {
            setContent {
                ReaderOverlay(
                    title = bookTitle,
                    loading = loading,
                    errorMessage = errorMessage,
                    controlsVisible = controlsVisible,
                    progression = progression,
                    preferences = readerPreferences,
                    onClose = ::finish,
                    onOpenSettings = { controlsVisible = true },
                    onThemeChange = { lifecycleScope.launch { preferencesRepository.setTheme(it) } },
                    onFontSizeChange = { lifecycleScope.launch { preferencesRepository.setFontSize(it) } },
                    onLineHeightChange = { lifecycleScope.launch { preferencesRepository.setLineHeight(it) } },
                    onReadingModeChange = { lifecycleScope.launch { preferencesRepository.setReadingMode(it) } },
                )
            }
        }
        root.addView(
            overlay,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
        )

        lifecycleScope.launch {
            val book = bookRepository.getBook(bookId)
            if (book == null) {
                showError("This book is no longer in your library")
                return@launch
            }
            bookTitle = book.title
            val publication = readium.open(File(book.filePath)).getOrElse { failure ->
                showError(failure.message ?: "Unable to open this EPUB")
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
            loading = false
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

    private fun showError(message: String) {
        loading = false
        errorMessage = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val EXTRA_BOOK_ID = "book_id"
        private const val READER_TAG = "epub_reader"
        private const val CONTROLS_TIMEOUT_MILLIS = 4_500L

        fun intent(context: Context, bookId: String): Intent =
            Intent(context, ReaderActivity::class.java).putExtra(EXTRA_BOOK_ID, bookId)
    }
}
