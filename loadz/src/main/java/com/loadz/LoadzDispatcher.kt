package com.loadz

import android.content.Context
import com.loadz.database.LoadzDatabase
import com.loadz.hls.HlsDownloader
import com.loadz.normal.NormalDownloader
import kotlinx.coroutines.*
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class LoadzDispatcher(
    private val context: Context,
    private val database: LoadzDatabase,
    private val mainListenerDispatcher: MainListenerDispatcher
) : DownloadDispatcher {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<Int, Job>()

    // Use OkHttp for better connection pooling, HTTP/2 support, and reliability
    override val httpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher().apply {
            maxRequests = 128
            maxRequestsPerHost = 32
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            // CDNs often block requests without a valid User-Agent
            if (original.header("User-Agent") == null) {
                requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36")
            }
            chain.proceed(requestBuilder.build())
        }
        .build()

    override fun enqueue(download: Download) {
        if (activeJobs.containsKey(download.id)) return
        
        val job = scope.launch {
            try {
                if (download.status != Status.DOWNLOADING) {
                    download.status = Status.DOWNLOADING
                    updateDbAndNotify(download) { it.onProgress(download, download.progress) }
                }

                if (download.downloaderType == DownloaderType.HLS) {
                    val baseTempDir = context.getExternalFilesDir("hls_temp") ?: File(context.filesDir, "hls_temp")
                    HlsDownloader(httpClient, download, baseTempDir, ::updateDbAndNotify).execute()
                } else {
                    NormalDownloader(httpClient, download, ::updateDbAndNotify).execute()
                }
            } catch (e: CancellationException) {
                // Job was cancelled (paused or removed)
            } catch (e: Exception) {
                android.util.Log.e("LoadzDispatcher", "Download Failed: ${e.message}", e)
                download.status = Status.FAILED
                val error = LoadzError.UNKNOWN.apply { throwable = e }
                download.error = error
                updateDbAndNotify(download) {
                    it.onFailed(download, e.message ?: "Unknown Error")
                    it.onError(download, e)
                }
            } finally {
                activeJobs.remove(download.id)
            }
        }
        activeJobs[download.id] = job
    }

    override fun enqueueAll(downloads: List<Download>) {
        downloads.forEach { enqueue(it) }
    }

    override fun pause(id: Int) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
    }

    override fun pauseAll() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }

    override fun resume(id: Int) {
        database.getDownload(id)?.let { enqueue(it) }
    }

    override fun resumeAll() {
        database.getDownloadsByStatus(listOf(Status.QUEUED)).forEach { enqueue(it) }
    }

    override fun cancel(id: Int) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
    }

    override fun cancelAll() {
        pauseAll()
    }

    override fun deleteFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            file.deleteRecursively()
        }
        val baseTempDir = context.getExternalFilesDir("hls_temp") ?: File(context.filesDir, "hls_temp")
        if (baseTempDir.exists()) {
            val tempDirs = baseTempDir.listFiles { _, name -> name.startsWith("hls_temp_") }
            tempDirs?.forEach { it.deleteRecursively() }
        }
    }

    override fun attachListener(listener: LoadzListener) {
        mainListenerDispatcher.attachListener(listener)
    }

    override fun detachListener(listener: LoadzListener) {
        mainListenerDispatcher.detachListener(listener)
    }

    override fun shutdown() {
        scope.cancel()
        activeJobs.clear()
    }

    private fun updateDbAndNotify(download: Download, notifyAction: (LoadzListener) -> Unit) {
        database.update(download)
        mainListenerDispatcher.dispatch(notifyAction)
    }
}
