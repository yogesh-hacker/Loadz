package com.loadz

class LoadzRequest(
    val url: String,
    val filePath: String,
    val id: Int = url.hashCode(),
    val namespace: String = "default_loadz",
    val downloaderType: DownloaderType = DownloaderType.NORMAL,
    val headers: Map<String, String> = emptyMap(),
    val networkType: NetworkType = NetworkType.ALL,
    val priority: Priority = Priority.NORMAL,
    val autoRetryMaxAttempts: Int = 0
)