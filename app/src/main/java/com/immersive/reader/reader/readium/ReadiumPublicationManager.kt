package com.immersive.reader.reader.readium

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

data class ReadiumBookMetadata(
    val title: String,
    val author: String?,
)

@Singleton
class ReadiumPublicationManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context = context,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = null,
        ),
    )

    suspend fun open(file: File): Result<Publication> = withContext(Dispatchers.IO) {
        runCatching {
            val asset = assetRetriever.retrieve(file).getOrNull()
                ?: error("Readium could not recognize this EPUB")
            publicationOpener.open(asset, allowUserInteraction = false).getOrNull()
                ?: error("Readium could not open this EPUB")
        }
    }

    suspend fun readMetadata(file: File): Result<ReadiumBookMetadata> = open(file).map { publication ->
        try {
            ReadiumBookMetadata(
                title = publication.metadata.title ?: "",
                author = publication.metadata.authors.firstOrNull()?.name,
            )
        } finally {
            publication.close()
        }
    }
}
