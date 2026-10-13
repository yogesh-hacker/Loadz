package com.loadz

abstract class AbstractLoadzListener : LoadzListener {
    override fun onAdded(download: Download) {}
    override fun onQueued(download: Download) {}
    override fun onProgress(download: Download, progress: Int) {}
    override fun onConversion(download: Download, progress: Int) {}
    override fun onCompleted(download: Download) {}
    override fun onFailed(download: Download, error: String) {}
    override fun onPaused(download: Download) {}
    override fun onResumed(download: Download) {}
    override fun onCancelled(download: Download) {}
    override fun onError(download: Download, exception: Exception) {}
}
