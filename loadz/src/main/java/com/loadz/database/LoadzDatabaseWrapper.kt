package com.loadz.database

import com.loadz.*

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject

class LoadzDatabaseWrapper(context: Context, val namespace: String) : LoadzDatabase {
    private val dbHelper = LoadzSQLiteHelper(context, "loadz_$namespace.db")

    override fun insert(download: Download) {
        dbHelper.writableDatabase.insert("downloads", null, download.toContentValues())
    }

    override fun insertAll(downloads: List<Download>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            downloads.forEach { db.insert("downloads", null, it.toContentValues()) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun update(download: Download) {
        dbHelper.writableDatabase.update("downloads", download.toContentValues(), "id = ?", arrayOf(download.id.toString()))
    }

    override fun updateAll(downloads: List<Download>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            downloads.forEach { update(it) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun delete(id: Int) {
        dbHelper.writableDatabase.delete("downloads", "id = ?", arrayOf(id.toString()))
    }

    override fun deleteAll() {
        dbHelper.writableDatabase.delete("downloads", null, null)
    }

    override fun getDownload(id: Int): Download? {
        val cursor = dbHelper.readableDatabase.query("downloads", null, "id = ?", arrayOf(id.toString()), null, null, null)
        return cursor.use { if (it.moveToFirst()) it.toDownload() else null }
    }

    override fun getDownloadByPath(filePath: String): Download? {
        val cursor = dbHelper.readableDatabase.query("downloads", null, "filePath = ?", arrayOf(filePath), null, null, null)
        return cursor.use { if (it.moveToFirst()) it.toDownload() else null }
    }

    override fun getAllDownloads(): List<Download> {
        val cursor = dbHelper.readableDatabase.query("downloads", null, null, null, null, null, "createdAt DESC")
        val list = mutableListOf<Download>()
        cursor.use { while (it.moveToNext()) list.add(it.toDownload()) }
        return list
    }

    override fun getDownloadsByStatus(statuses: List<Status>): List<Download> {
        if (statuses.isEmpty()) return emptyList()
        val placeholders = statuses.joinToString(",") { "?" }
        val args = statuses.map { it.value.toString() }.toTypedArray()
        val cursor = dbHelper.readableDatabase.query("downloads", null, "status IN ($placeholders)", args, null, null, "createdAt DESC")
        val list = mutableListOf<Download>()
        cursor.use { while (it.moveToNext()) list.add(it.toDownload()) }
        return list
    }

    private fun Download.toContentValues(): ContentValues {
        val cv = ContentValues()
        cv.put("id", id)
        cv.put("url", url)
        cv.put("filePath", filePath)
        cv.put("downloaderType", downloaderType.value)
        cv.put("namespace", namespace)
        cv.put("downloadGroup", group)
        cv.put("tag", tag)
        cv.put("headers", JSONObject(headers as Map<*, *>).toString())
        cv.put("priority", priority.value)
        cv.put("networkType", networkType.value)
        cv.put("status", status.value)
        cv.put("error", error?.value ?: LoadzError.NONE.value)
        cv.put("progress", progress)
        cv.put("downloadedBytes", downloadedBytes)
        cv.put("totalBytes", totalBytes)
        cv.put("createdAt", createdAt)
        cv.put("estimatedTimeInMs", estimatedTimeInMs)
        cv.put("bytesPerSecond", bytesPerSecond)
        cv.put("totalDurationMs", totalDurationMs)
        cv.put("segmentsTotal", segmentsTotal)
        cv.put("segmentsDone", segmentsDone)
        cv.put("autoRetryMaxAttempts", autoRetryMaxAttempts)
        cv.put("autoRetryAttempts", autoRetryAttempts)
        return cv
    }

    private fun Cursor.toDownload(): Download {
        val url = getString(getColumnIndexOrThrow("url"))
        val filePath = getString(getColumnIndexOrThrow("filePath"))
        val downloaderType = DownloaderType.valueOf(getInt(getColumnIndexOrThrow("downloaderType")))
        val id = getInt(getColumnIndexOrThrow("id"))
        val namespace = getString(getColumnIndexOrThrow("namespace"))

        val download = Download(url, filePath, downloaderType, id, namespace)
        download.group = getInt(getColumnIndexOrThrow("downloadGroup"))
        download.tag = getString(getColumnIndexOrThrow("tag"))

        val headersStr = getString(getColumnIndexOrThrow("headers"))
        val headersMap = mutableMapOf<String, String>()
        if (!headersStr.isNullOrEmpty()) {
            val json = JSONObject(headersStr)
            json.keys().forEach { headersMap[it] = json.getString(it) }
        }
        download.headers = headersMap

        download.priority = Priority.valueOf(getInt(getColumnIndexOrThrow("priority")))
        download.networkType = NetworkType.valueOf(getInt(getColumnIndexOrThrow("networkType")))
        download.status = Status.valueOf(getInt(getColumnIndexOrThrow("status")))

        val errVal = getInt(getColumnIndexOrThrow("error"))
        if (errVal != LoadzError.NONE.value) {
            download.error = LoadzError.valueOf(errVal)
        }

        download.progress = getInt(getColumnIndexOrThrow("progress"))
        download.downloadedBytes = getLong(getColumnIndexOrThrow("downloadedBytes"))
        download.totalBytes = getLong(getColumnIndexOrThrow("totalBytes"))
        download.createdAt = getLong(getColumnIndexOrThrow("createdAt"))
        download.estimatedTimeInMs = getLong(getColumnIndexOrThrow("estimatedTimeInMs"))
        download.bytesPerSecond = getLong(getColumnIndexOrThrow("bytesPerSecond"))
        download.totalDurationMs = getLong(getColumnIndexOrThrow("totalDurationMs"))
        download.segmentsTotal = getInt(getColumnIndexOrThrow("segmentsTotal"))
        download.segmentsDone = getInt(getColumnIndexOrThrow("segmentsDone"))
        download.autoRetryMaxAttempts = getInt(getColumnIndexOrThrow("autoRetryMaxAttempts"))
        download.autoRetryAttempts = getInt(getColumnIndexOrThrow("autoRetryAttempts"))
        return download
    }
}

class LoadzSQLiteHelper(context: Context, dbName: String) : SQLiteOpenHelper(context, dbName, null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE downloads (
                id INTEGER PRIMARY KEY,
                url TEXT NOT NULL,
                filePath TEXT NOT NULL,
                downloaderType INTEGER NOT NULL,
                namespace TEXT NOT NULL,
                downloadGroup INTEGER NOT NULL,
                tag TEXT,
                headers TEXT,
                priority INTEGER NOT NULL,
                networkType INTEGER NOT NULL,
                status INTEGER NOT NULL,
                error INTEGER NOT NULL,
                progress INTEGER NOT NULL,
                downloadedBytes INTEGER NOT NULL,
                totalBytes INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                estimatedTimeInMs INTEGER NOT NULL,
                bytesPerSecond INTEGER NOT NULL,
                totalDurationMs INTEGER NOT NULL DEFAULT 0,
                segmentsTotal INTEGER NOT NULL,
                segmentsDone INTEGER NOT NULL,
                autoRetryMaxAttempts INTEGER NOT NULL,
                autoRetryAttempts INTEGER NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE downloads ADD COLUMN totalDurationMs INTEGER NOT NULL DEFAULT 0")
            } catch (e: Exception) {
                // If it already exists or fails, re-create table
                db.execSQL("DROP TABLE IF EXISTS downloads")
                onCreate(db)
            }
        }
    }
}