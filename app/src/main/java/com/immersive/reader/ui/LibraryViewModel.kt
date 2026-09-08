package com.immersive.reader.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immersive.reader.core.data.BookRepository
import com.immersive.reader.core.model.Book
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: BookRepository,
) : ViewModel() {
    val books: StateFlow<List<Book>> = repository.books.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    fun importBook(contentResolver: ContentResolver, uri: Uri, onResult: (Result<Book>) -> Unit) {
        viewModelScope.launch {
            _importing.value = true
            try {
                onResult(repository.importBook(contentResolver, uri))
            } finally {
                _importing.value = false
            }
        }
    }

    fun delete(book: Book) {
        viewModelScope.launch { repository.delete(book) }
    }
}
