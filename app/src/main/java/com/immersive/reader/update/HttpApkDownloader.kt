package com.immersive.reader.update

import android.content.Context
import com.immersive.reader.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class HttpApkDownloader(
    private val context: Context,
) : ApkDownloader {
    override suspend fun download(url: String, onProgress: (Float) -> Unit) {
        val directory = updatesDir().apply { mkdirs() }
        val partial = File(directory, PARTIAL_NAME)
        val target = File(directory, APK_NAME)
        partial.delete()
        target.delete()

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "ImmersiveReader/${BuildConfig.VERSION_NAME}")
            setRequestProperty("Accept", "application/octet-stream")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw UpdateHttpException(code)
            val total = connection.contentLengthLong
            var read = 0L
            connection.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count <= 0) break
                        output.write(buffer, 0, count)
                        read += count
                        if (total > 0L) {
                            onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            onProgress(1f)
        } catch (error: Exception) {
            partial.delete()
            target.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    override fun deletePartial() {
        val directory = updatesDir()
        File(directory, PARTIAL_NAME).delete()
        File(directory, APK_NAME).delete()
    }

    fun apkFile(): File = File(updatesDir(), APK_NAME)

    private fun updatesDir(): File = File(context.cacheDir, UPDATES_DIR)

    companion object {
        const val UPDATES_DIR = "updates"
        const val APK_NAME = "update.apk"
        const val PARTIAL_NAME = "update.apk.partial"
    }
}
