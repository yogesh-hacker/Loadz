package com.loadz

import android.net.Uri
import java.io.Serializable

/**
 * A snapshot of a specific download managed by Loadz.
 */
data class Download(
    val url: String,
    val filePath: String,
    val downloaderType: DownloaderType = DownloaderType.NORMAL,
    val id: Int = url.hashCode(),
    val namespace: String = "default_loadz"
) : Serializable {
    var group: Int = 0
    var tag: String? = null

    val fileUri: Uri get() = Uri.parse("file://$filePath")

    // --- Network & Constraints ---
    var headers: Map<String, String> = emptyMap()
    var priority: Priority = Priority.NORMAL
    var networkType: NetworkType = NetworkType.ALL

    // --- State & Progress ---
    var status: Status = Status.ADDED
    var error: LoadzError? = null
    var progress: Int = 0
    var downloadedBytes: Long = 0
    var totalBytes: Long = 0

    // --- Speed & Time ---
    var createdAt: Long = System.currentTimeMillis()
    var estimatedTimeInMs: Long = -1
    var bytesPerSecond: Long = 0
    var totalDurationMs: Long = 0

    // --- HLS Specific Metrics ---
    var segmentsTotal: Int = 0
    var segmentsDone: Int = 0

    // --- Retry Logic ---
    var autoRetryMaxAttempts: Int = 0
    var autoRetryAttempts: Int = 0

    fun copy(): Download {
        val copy = Download(url, filePath, downloaderType, id, namespace)
        copy.group = group
        copy.tag = tag
        copy.headers = headers
        copy.priority = priority
        copy.networkType = networkType
        copy.status = status
        copy.error = error
        copy.progress = progress
        copy.downloadedBytes = downloadedBytes
        copy.totalBytes = totalBytes
        copy.createdAt = createdAt
        copy.estimatedTimeInMs = estimatedTimeInMs
        copy.bytesPerSecond = bytesPerSecond
        copy.totalDurationMs = totalDurationMs
        copy.segmentsTotal = segmentsTotal
        copy.segmentsDone = segmentsDone
        copy.autoRetryMaxAttempts = autoRetryMaxAttempts
        copy.autoRetryAttempts = autoRetryAttempts
        return copy
    }
}