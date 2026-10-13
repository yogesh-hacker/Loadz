package com.loadz.hls

import com.loadz.Download
import com.loadz.LoadzListener
import com.loadz.Status
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Orchestrates downloading, AES-128 decryption, and merging of HLS streams.
 */
class HlsDownloader(
    private val client: OkHttpClient,
    private val download: Download,
    private val baseTempDir: File,
    private val updateDbAndNotify: (Download, (LoadzListener) -> Unit) -> Unit
) {
    private val parser = M3u8Parser(client)
    private val decrypter = HlsDecrypter(client)
    private val merger = HlsMerger()

    suspend fun execute() = coroutineScope {
        val targetFile = File(download.filePath)
        targetFile.parentFile?.mkdirs()

        val tempDir = File(baseTempDir, "hls_temp_${download.id}")
        if (!tempDir.exists()) tempDir.mkdirs()

        // FIX: Clean up any orphan .tmp files from a previous force-close/crash
        tempDir.listFiles { _, name -> name.endsWith(".tmp") }?.forEach { it.delete() }

        // Parse Playlist
        val segments = parser.parsePlaylist(download.url, download.headers)

        if (segments.isEmpty()) {
            throw Exception("No TS segments found in M3U8")
        }

        download.segmentsTotal = segments.size
        download.totalDurationMs = segments.sumOf { it.durationMs }

        val existingFiles = tempDir.listFiles { _, name -> name.endsWith(".ts") } ?: emptyArray()
        val downloadedBytesCounter = AtomicLong(0L)
        var segmentsDone = 0

        for (file in existingFiles) {
            downloadedBytesCounter.addAndGet(file.length())
            segmentsDone++
        }

        val segmentsDoneCounter = AtomicInteger(segmentsDone)

        synchronized(this@HlsDownloader) {
            download.segmentsDone = segmentsDone
            download.downloadedBytes = downloadedBytesCounter.get()

            if (segmentsDone > 0) {
                download.totalBytes =
                    (download.downloadedBytes / segmentsDone) * download.segmentsTotal
            }
            download.progress =
                if (download.segmentsTotal > 0) (segmentsDone * 100) / download.segmentsTotal else 0
            updateDbAndNotify(download) { it.onProgress(download, download.progress) }
        }

        try {
            // Download & Decrypt segments in parallel
            val parallelism = 10
            val channel = kotlinx.coroutines.channels.Channel<HlsSegment>(segments.size)
            segments.forEach { channel.trySend(it) }
            channel.close()

            val lastUpdate = AtomicLong(System.currentTimeMillis())
            val lastDownloadedBytes = AtomicLong(downloadedBytesCounter.get())

            // FIX: Thread-safe cache to prevent downloading the same AES key 10 times simultaneously
            val keyCache = ConcurrentHashMap<String, ByteArray>()

            val downloadJobs = List(parallelism) {
                launch(Dispatchers.IO) {
                    for (segment in channel) {
                        if (!isActive) throw CancellationException("Download Paused")
                        val tsFile = File(tempDir, "segment_${segment.index}.ts")

                        if (!tsFile.exists() || tsFile.length() == 0L) {
                            val tsFileTmp = File(tempDir, "segment_${segment.index}.tmp")

                            var success = false
                            var attempts = 0
                            var lastException: Exception? = null
                            var finalBytes: ByteArray = ByteArray(0)

                            // FIX: Per-segment retry logic (3 attempts) to survive flaky mobile networks
                            while (attempts < 3 && !success) {
                                try {
                                    if (!isActive) throw CancellationException("Download Paused")

                                    val request = Request.Builder().url(segment.url).apply {
                                        download.headers.forEach { (k, v) -> addHeader(k, v) }
                                    }.build()

                                    val rawBytes =
                                        client.newCall(request).execute().use { response ->
                                            if (!response.isSuccessful) throw Exception("HTTP ${response.code}: ${response.message}")
                                            response.body?.bytes()
                                                ?: throw Exception("Empty segment body")
                                        }

                                    finalBytes = if (!segment.keyUrl.isNullOrEmpty()) {
                                        // Use the cache! If it's not there, fetch it and put it in.
                                        val keyBytes = keyCache.getOrPut(segment.keyUrl) {
                                            decrypter.fetchKey(segment.keyUrl, download.headers)
                                        }
                                        decrypter.decryptSegment(
                                            rawBytes,
                                            keyBytes,
                                            segment.ivHex,
                                            segment.sequenceNumber
                                        )
                                    } else {
                                        rawBytes
                                    }

                                    success = true
                                } catch (e: CancellationException) {
                                    throw e // Don't retry if the user intentionally paused
                                } catch (e: Exception) {
                                    lastException = e
                                    attempts++
                                    if (attempts < 3) delay(1000L * attempts) // Exponential backoff (1s, 2s)
                                }
                            }

                            if (!success) {
                                throw Exception("Segment ${segment.index} failed after 3 attempts: ${lastException?.message}")
                            }

                            // Write safely
                            FileOutputStream(tsFileTmp).use { out -> out.write(finalBytes) }
                            tsFileTmp.renameTo(tsFile) // Atomic write

                            val currentBytes =
                                downloadedBytesCounter.addAndGet(finalBytes.size.toLong())
                            val sDone = segmentsDoneCounter.incrementAndGet()

                            val now = System.currentTimeMillis()
                            val previousUpdate = lastUpdate.get()
                            val timeDiff = now - previousUpdate

                            if (timeDiff >= 1000) {
                                if (lastUpdate.compareAndSet(previousUpdate, now)) {
                                    val previousBytes = lastDownloadedBytes.getAndSet(currentBytes)
                                    val bytesDiff = currentBytes - previousBytes

                                    download.bytesPerSecond = (bytesDiff * 1000) / timeDiff
                                    download.segmentsDone = sDone
                                    download.downloadedBytes = currentBytes
                                    if (sDone > 0) {
                                        download.totalBytes =
                                            (currentBytes / sDone) * download.segmentsTotal
                                    }
                                    download.progress = ((sDone * 100) / download.segmentsTotal)
                                    updateDbAndNotify(download) {
                                        it.onProgress(
                                            download,
                                            download.progress
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            downloadJobs.joinAll()

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e
        }

        // Merge via Local Playlist mapping
        download.status = Status.CONVERTING
        updateDbAndNotify(download) { it.onConversion(download, 0) }

        try {
            merger.mergeSegments(
                tempDir = tempDir,
                segments = segments, // Passing the actual list of segments to generate local.m3u8
                targetFile = targetFile,
                totalDurationMs = download.totalDurationMs,
                onProgress = { conversionProgress ->
                    download.progress = conversionProgress
                    updateDbAndNotify(download) { it.onConversion(download, conversionProgress) }
                },
                isCancelled = { !isActive }
            )

            download.status = Status.COMPLETED
            updateDbAndNotify(download) { it.onCompleted(download) }

            // Clean up temp data directory
            tempDir.deleteRecursively()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            targetFile.delete()
            throw Exception("Failed to process HLS stream: ${e.message}")
        }
    }
}