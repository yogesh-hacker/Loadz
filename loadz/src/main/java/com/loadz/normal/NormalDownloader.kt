package com.loadz.normal

import com.loadz.*

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream

class NormalDownloader(
    private val client: OkHttpClient,
    private val download: Download,
    private val updateDbAndNotify: (Download, (LoadzListener) -> Unit) -> Unit
) {
    suspend fun execute() = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder().url(download.url)
        download.headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        val file = File(download.filePath)
        file.parentFile?.mkdirs()

        var downloaded = 0L
        if (file.exists()) {
            downloaded = file.length()
            requestBuilder.addHeader("Range", "bytes=$downloaded-")
        }

        val request = requestBuilder.build()
        var response: Response? = null

        try {
            response = client.newCall(request).execute()

            if (!response.isSuccessful && response.code != 206) {
                throw Exception("Server returned HTTP ${response.code}")
            }

            val body = response.body ?: throw Exception("Empty response body")
            val contentLength = body.contentLength()
            download.totalBytes = if (downloaded > 0) downloaded + contentLength else contentLength

            body.byteStream().use { inputStream ->
                FileOutputStream(file, downloaded > 0).use { outputStream ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var lastUpdate = System.currentTimeMillis()
                    var lastDownloaded = downloaded

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (!isActive) throw CancellationException("Download Paused/Cancelled")

                        outputStream.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        download.downloadedBytes = downloaded
                        
                        if (download.totalBytes > 0) {
                            download.progress = ((downloaded * 100) / download.totalBytes).toInt()
                        }

                        val now = System.currentTimeMillis()
                        val timeDiff = now - lastUpdate
                        if (timeDiff >= 1000) { 
                            val bytesDiff = downloaded - lastDownloaded
                            download.bytesPerSecond = (bytesDiff * 1000) / timeDiff
                            lastUpdate = now
                            lastDownloaded = downloaded
                            updateDbAndNotify(download) { it.onProgress(download, download.progress) }
                        }
                    }
                }
            }

            download.status = Status.COMPLETED
            updateDbAndNotify(download) { it.onCompleted(download) }
            
        } finally {
            response?.close()
        }
    }
}