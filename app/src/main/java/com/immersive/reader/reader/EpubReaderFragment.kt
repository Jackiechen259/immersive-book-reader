package com.immersive.reader.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.immersive.reader.R
import com.immersive.reader.core.data.BookRepository
import com.immersive.reader.core.datastore.ReaderPreferences
import com.immersive.reader.core.datastore.ReaderPreferencesRepository
import com.immersive.reader.core.model.ReadingMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.publication.Locator

@AndroidEntryPoint
class EpubReaderFragment : Fragment() {
    @Inject lateinit var publicationStore: PublicationStore
    @Inject lateinit var bookRepository: BookRepository
    @Inject lateinit var preferencesRepository: ReaderPreferencesRepository

    private val bookId: String
        get() = requireArguments().getString(ARG_BOOK_ID).orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        val publication = publicationStore.get(bookId)
            ?: error("Publication is not ready for book $bookId")
        val initialLocator = requireArguments().getString(ARG_LOCATOR_JSON)?.let(::parseLocator)

        childFragmentManager.fragmentFactory = EpubNavigatorFactory(
            publication = publication,
            configuration = EpubNavigatorFactory.Configuration(),
        ).createFragmentFactory(initialLocator = initialLocator)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_epub_reader, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (childFragmentManager.findFragmentByTag(NAVIGATOR_TAG) == null) {
            childFragmentManager.commitNow {
                add(R.id.reader_navigator_container, EpubNavigatorFragment::class.java, Bundle(), NAVIGATOR_TAG)
            }
        }

        val navigator = childFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as? EpubNavigatorFragment
            ?: error("Readium navigator was not created")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    navigator.currentLocator.collect { locator ->
                        (activity as? ReaderProgressListener)?.onProgressChanged(
                            locator.locations.totalProgression ?: locator.locations.progression,
                        )
                        val book = bookRepository.getBook(bookId) ?: return@collect
                        bookRepository.updateProgress(
                            book = book,
                            locatorJson = locator.toJSON().toString(),
                            progression = locator.locations.totalProgression ?: locator.locations.progression,
                        )
                    }
                }
                launch {
                    preferencesRepository.preferences.collect { preferences ->
                        navigator.submitPreferences(preferences.toEpubPreferences())
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (activity?.isFinishing == true) publicationStore.remove(bookId)
        super.onDestroy()
    }

    private fun parseLocator(json: String): Locator? = runCatching {
        Locator.fromJSON(JSONObject(json))
    }.getOrNull()

    companion object {
        private const val ARG_BOOK_ID = "book_id"
        private const val ARG_LOCATOR_JSON = "locator_json"
        private const val NAVIGATOR_TAG = "readium_epub_navigator"

        fun newInstance(bookId: String, locatorJson: String?): EpubReaderFragment =
            EpubReaderFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_BOOK_ID, bookId)
                    putString(ARG_LOCATOR_JSON, locatorJson)
                }
            }
    }

    @OptIn(ExperimentalReadiumApi::class)
    private fun ReaderPreferences.toEpubPreferences() = EpubPreferences(
        fontSize = fontSize.toDouble(),
        lineHeight = lineHeight.toDouble(),
        publisherStyles = false,
        scroll = readingMode == ReadingMode.SCROLLING,
        theme = Theme.valueOf(theme.name),
    )
}

interface ReaderProgressListener {
    fun onProgressChanged(progression: Double?)
}
