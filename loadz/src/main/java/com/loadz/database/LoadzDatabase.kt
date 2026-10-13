package com.loadz.database

import com.loadz.Download
import com.loadz.Status

interface LoadzDatabase {
    fun insert(download: Download)
    fun insertAll(downloads: List<Download>)
    fun update(download: Download)
    fun updateAll(downloads: List<Download>)
    fun delete(id: Int)
    fun deleteAll()
    fun getDownload(id: Int): Download?
    fun getDownloadByPath(filePath: String): Download?
    fun getAllDownloads(): List<Download>
    fun getDownloadsByStatus(statuses: List<Status>): List<Download>
}
