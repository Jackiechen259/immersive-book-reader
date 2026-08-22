package com.immersive.reader.reader

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import org.readium.r2.shared.publication.Publication

@Singleton
class PublicationStore @Inject constructor() {
    private val publications = ConcurrentHashMap<String, Any>()

    fun put(bookId: String, publication: Any) {
        publications.put(bookId, publication)?.takeUnless { it === publication }?.let(::close)
    }

    fun get(bookId: String): Publication? = publications[bookId] as? Publication

    fun remove(bookId: String) {
        publications.remove(bookId)?.let(::close)
    }

    private fun close(publication: Any) {
        (publication as? Publication)?.close()
    }
}
