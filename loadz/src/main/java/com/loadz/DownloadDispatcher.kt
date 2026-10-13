package com.loadz

import okhttp3.OkHttpClient

interface DownloadDispatcher {
    val httpClient: OkHttpClient

    fun enqueue(download: Download)
    fun enqueueAll(downloads: List<Download>)
    fun pause(id: Int)
    fun pauseAll()
    fun resume(id: Int)
    fun resumeAll()
    fun cancel(id: Int)
    fun cancelAll()
    fun deleteFile(filePath: String)
    fun attachListener(listener: LoadzListener)
    fun detachListener(listener: LoadzListener)
    fun shutdown()
}