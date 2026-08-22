package com.wormhole.browser.core.downloads

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.withPermit

object WormHoleDownloadEngine {

    private val dispatcher = okhttp3.Dispatcher().apply {
        maxRequests = 12
        maxRequestsPerHost = 6
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .dispatcher(dispatcher)
        .connectionPool(okhttp3.ConnectionPool(12, 5, TimeUnit.MINUTES))
        .build()

    // Caps how many downloads actively transfer bytes at once, independent of how many
    // are queued/enqueued. Keeps memory/CPU bounded and avoids saturating the user's
    // connection when many downloads are requested in a burst (e.g. "download all").
    const val MAX_CONCURRENT_DOWNLOADS = 4
    @PublishedApi
    internal val downloadSemaphore = kotlinx.coroutines.sync.Semaphore(MAX_CONCURRENT_DOWNLOADS)

    suspend inline fun <T> withDownloadSlot(crossinline block: suspend () -> T): T =
        downloadSemaphore.withPermit { block() }

    private val cancelFlags = ConcurrentHashMap<Long, Boolean>()

    fun requestCancel(downloadId: Long) {
        cancelFlags[downloadId] = true
    }

    fun clearCancelFlag(downloadId: Long) {
        cancelFlags.remove(downloadId)
    }

    fun createDestination(context: Context, fileName: String, mimeType: String): DownloadDestination? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType.ifBlank { "application/octet-stream" })
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
                DownloadDestination.MediaStoreDestination(itemUri)
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val target = uniqueFile(downloadsDir, fileName)
                DownloadDestination.FileDestination(target)
            }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun fetch(
        context: Context,
        downloadId: Long,
        url: String,
        userAgent: String?,
        cookie: String?,
        destination: DownloadDestination,
        onProgress: (bytesDownloaded: Long, bytesTotal: Long) -> Unit,
    ): FetchResult {
        cancelFlags[downloadId] = false

        val request = Request.Builder()
            .url(url)
            .apply {
                userAgent?.let { header("User-Agent", it) }
                cookie?.let { if (it.isNotBlank()) header("Cookie", it) }
            }
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return FetchResult.Failure("Server returned ${response.code}")
                }
                val body = response.body ?: return FetchResult.Failure("Empty response body")
                val total = body.contentLength().coerceAtLeast(0)

                val outputStream = when (destination) {
                    is DownloadDestination.MediaStoreDestination ->
                        context.contentResolver.openOutputStream(destination.uri)
                    is DownloadDestination.FileDestination ->
                        FileOutputStream(destination.file)
                } ?: return FetchResult.Failure("Could not open destination for writing")

                var downloaded = 0L
                val buffer = ByteArray(64 * 1024)

                outputStream.use { out ->
                    body.byteStream().use { input ->
                        while (true) {
                            if (cancelFlags[downloadId] == true) {
                                return FetchResult.Cancelled
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            downloaded += read
                            onProgress(downloaded, total)
                        }
                        out.flush()
                    }
                }

                if (destination is DownloadDestination.MediaStoreDestination) {
                    val values = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                    context.contentResolver.update(destination.uri, values, null, null)
                }

                FetchResult.Success(bytesDownloaded = downloaded, bytesTotal = if (total > 0) total else downloaded)
            }
        } catch (e: IOException) {
            FetchResult.Failure(e.message ?: "Network error")
        } catch (e: SecurityException) {
            FetchResult.Failure("Permission denied")
        } catch (e: IllegalStateException) {
            FetchResult.Failure(e.message ?: "Download failed")
        } finally {
            cancelFlags.remove(downloadId)
        }
    }

    fun deletePartialFile(context: Context, destinationUri: String) {
        try {
            val uri = Uri.parse(destinationUri)
            if (uri.scheme == "file") {
                uri.path?.let { File(it).delete() }
            } else {
                context.contentResolver.delete(uri, null, null)
            }
        } catch (_: Exception) {

        }
    }

    private fun uniqueFile(dir: File, fileName: String): File {
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate
        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        var i = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base ($i)$ext")
            i++
        }
        return candidate
    }
}

sealed interface DownloadDestination {
    val uriString: String

    data class MediaStoreDestination(val uri: Uri) : DownloadDestination {
        override val uriString: String get() = uri.toString()
    }

    data class FileDestination(val file: File) : DownloadDestination {
        override val uriString: String get() = Uri.fromFile(file).toString()
    }
}

sealed interface FetchResult {
    data class Success(val bytesDownloaded: Long, val bytesTotal: Long) : FetchResult
    data class Failure(val reason: String) : FetchResult
    data object Cancelled : FetchResult
}
