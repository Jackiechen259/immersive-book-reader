package com.immersive.reader.core.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.immersive.reader.core.database.BookDao
import com.immersive.reader.core.database.BookEntity
import com.immersive.reader.core.database.ReadingSessionDao
import com.immersive.reader.core.database.ReadingSessionEntity
import com.immersive.reader.core.model.Book
import com.immersive.reader.core.model.ReadingSession
import com.immersive.reader.reader.readium.ReadiumPublicationManager
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val filesDir: File,
    private val readium: ReadiumPublicationManager,
) {
    val books: Flow<List<Book>> = bookDao.observeBooks().map { books -> books.map(BookEntity::toModel) }

    suspend fun getBook(bookId: String): Book? = bookDao.getById(bookId)?.toModel()

    suspend fun importBook(contentResolver: ContentResolver, source: Uri): Result<Book> = runCatching {
        val id = UUID.randomUUID().toString()
        val targetDir = File(filesDir, "books").apply { mkdirs() }
        val target = File(targetDir, "$id.epub")
        contentResolver.openInputStream(source)?.use { input -> target.outputStream().use(input::copyTo) }
            ?: error("Unable to open the selected EPUB")

        val displayName = contentResolver.query(source, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).substringBeforeLast('.') else null
            }
            ?.takeIf(String::isNotBlank)
            ?: "Untitled book"
        val metadata = readium.readMetadata(target).getOrElse { error ->
            target.delete()
            throw error
        }

        val book = BookEntity(
            id = id,
            title = metadata.title.ifBlank { displayName },
            author = metadata.author,
            filePath = target.absolutePath,
            coverPath = null,
            lastLocatorJson = null,
            progression = null,
            addedAt = System.currentTimeMillis(),
            lastOpenedAt = null,
        )
        bookDao.insert(book)
        book.toModel()
    }

    suspend fun updateProgress(book: Book, locatorJson: String?, progression: Double?) {
        bookDao.update(
            book.toEntity().copy(
                lastLocatorJson = locatorJson,
                progression = progression,
                lastOpenedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun delete(book: Book) {
        bookDao.delete(book.toEntity())
        File(book.filePath).delete()
        book.coverPath?.let(::File)?.delete()
    }
}

@Singleton
class ReadingSessionRepository @Inject constructor(
    private val dao: ReadingSessionDao,
) {
    suspend fun getActive(): ReadingSession? = dao.getActive()?.toModel()
    suspend fun getById(id: String): ReadingSession? = dao.getById(id)?.toModel()
    val sessions: Flow<List<ReadingSession>> = dao.observeAll().map { it.map(ReadingSessionEntity::toModel) }
    val totalReadingMillis: Flow<Long> = dao.observeTotalReadingMillis()
    val sessionCount: Flow<Int> = dao.observeSessionCount()
    val completedCount: Flow<Int> = dao.observeCompletedCount()
    val interruptedCount: Flow<Int> = dao.observeInterruptedCount()
    suspend fun insert(session: ReadingSession) = dao.insert(session.toEntity())
    suspend fun update(session: ReadingSession) = dao.update(session.toEntity())
}

private fun BookEntity.toModel() = Book(id, title, author, filePath, coverPath, lastLocatorJson, progression, addedAt, lastOpenedAt)
private fun Book.toEntity() = BookEntity(id, title, author, filePath, coverPath, lastLocatorJson, progression, addedAt, lastOpenedAt)
private fun ReadingSessionEntity.toModel() = ReadingSession(id, bookId, timerMode, targetDurationMillis, exitPolicy, startEpochMillis, plannedEndEpochMillis, actualEndEpochMillis, accumulatedReadingMillis, startLocatorJson, endLocatorJson, status, emergencyExit, createdAt, updatedAt)
private fun ReadingSession.toEntity() = ReadingSessionEntity(id, bookId, timerMode, targetDurationMillis, exitPolicy, startEpochMillis, plannedEndEpochMillis, actualEndEpochMillis, accumulatedReadingMillis, startLocatorJson, endLocatorJson, status, emergencyExit, createdAt, updatedAt)
