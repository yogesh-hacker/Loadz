package com.loadz

interface LoadzListener {
    fun onAdded(download: Download)
    fun onQueued(download: Download)
    fun onProgress(download: Download, progress: Int)
    fun onConversion(download: Download, progress: Int)
    fun onCompleted(download: Download)
    fun onFailed(download: Download, error: String)
    fun onPaused(download: Download)
    fun onResumed(download: Download)
    fun onCancelled(download: Download)
    fun onError(download: Download, exception: Exception)
}
