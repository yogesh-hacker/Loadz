package com.loadz

import com.loadz.database.LoadzDatabaseWrapper
import com.loadz.hls.HlsVariant

/**
 * A lightweight, advanced file and HLS stream download manager.
 * Features: Background downloading, Pause & Resume, HLS (.m3u8) parsing,
 * FFmpeg integration, and network-specific constraints.
 */
interface Loadz {

    /** Returns true if this instance of Loadz is closed and cannot be reused. */
    val isClosed: Boolean

    /** The namespace which this instance of Loadz operates in. */
    val namespace: String

    /**
     * Queues a single request for downloading.
     * @param request The LoadzRequest to queue.
     * @param onSuccess Callback invoked if successfully enqueued.
     * @param onError Callback invoked if enqueuing fails (e.g., already exists, no space).
     */
    fun enqueue(
        request: LoadzRequest,
        onSuccess: ((LoadzRequest) -> Unit)? = null,
        onError: ((LoadzError) -> Unit)? = null
    ): Loadz

    /**
     * Queues a batch of requests for downloading.
     */
    fun enqueue(
        requests: List<LoadzRequest>,
        onResult: ((List<Pair<LoadzRequest, LoadzError>>) -> Unit)? = null
    ): Loadz

    /**
     * Fetches available HLS qualities (variants) from a Master Playlist.
     * If the URL points directly to a Media Playlist, the returned list will be empty.
     */
    fun fetchHlsQualities(
        url: String,
        headers: Map<String, String> = emptyMap(),
        onResult: (List<HlsVariant>?, Exception?) -> Unit
    ): Loadz

    // --- State Controls ---

    fun pause(id: Int): Loadz
    fun pause(ids: List<Int>): Loadz
    fun pauseAll(): Loadz

    fun resume(id: Int): Loadz
    fun resume(ids: List<Int>): Loadz
    fun resumeAll(): Loadz

    fun cancel(id: Int): Loadz
    fun cancel(ids: List<Int>): Loadz
    fun cancelAll(): Loadz

    fun retry(id: Int): Loadz
    fun retry(ids: List<Int>): Loadz

    /**
     * Pauses all currently downloading items, and pauses all download processing operations.
     * Use this method when you do not want Loadz to keep processing downloads
     * but do not want to release the instance of Loadz.
     */
    fun freeze(): Loadz

    /** Allow Loadz to resume operations after freeze has been called. */
    fun unfreeze(): Loadz

    // --- Removal & Deletion ---

    /** Removes the download from Loadz management. The downloaded file on the disk is NOT deleted. */
    fun remove(id: Int): Loadz
    fun remove(ids: List<Int>): Loadz
    fun removeAll(): Loadz
    fun removeAllWithStatus(status: Status): Loadz
    fun removeAllWithStatus(statuses: List<Status>): Loadz

    /** Deletes the download from Loadz management AND deletes the file on the disk. */
    fun delete(id: Int): Loadz
    fun delete(ids: List<Int>): Loadz
    fun deleteAll(): Loadz
    fun deleteAllWithStatus(status: Status): Loadz
    fun deleteAllWithStatus(statuses: List<Status>): Loadz

    // --- Advanced Modifiers ---

    /**
     * Resets the autoRetryAttempts value for a download back to 0.
     * @param retryDownload Retry the download if its status is Status.FAILED. True by default.
     */
    fun resetAutoRetryAttempts(downloadId: Int, retryDownload: Boolean = true): Loadz

    /**
     * Renames the file for a completed download.
     */
    fun renameCompletedDownloadFile(id: Int, newFileName: String): Loadz

    // --- Queries ---

    fun getDownloads(onResult: (List<Download>) -> Unit): Loadz
    fun getDownload(id: Int, onResult: (Download?) -> Unit): Loadz
    fun getDownloads(idList: List<Int>, onResult: (List<Download>) -> Unit): Loadz
    fun getDownloadsWithStatus(status: Status, onResult: (List<Download>) -> Unit): Loadz
    fun getDownloadsWithStatus(statuses: List<Status>, onResult: (List<Download>) -> Unit): Loadz
    fun getDownloadsByTag(tag: String, onResult: (List<Download>) -> Unit): Loadz

    // --- Dynamic Configuration Overrides ---

    /** Overrides each download's specific network type preference and uses a global network type preference instead. */
    fun setGlobalNetworkType(networkType: NetworkType): Loadz

    /** Sets the number of parallel downloads Loadz should perform at any given time dynamically. */
    fun setDownloadConcurrentLimit(downloadConcurrentLimit: Int): Loadz

    /** Enable or disable logging on the fly. */
    fun enableLogging(enabled: Boolean): Loadz

    // --- Listeners ---

    fun addListener(listener: LoadzListener): Loadz
    fun removeListener(listener: LoadzListener): Loadz

    /** Releases held resources. Once closed, this instance cannot be reused. */
    fun close()

    // --- Initialization & Singleton Engine ---

    companion object {
        private val lock = Any()

        @Volatile
        private var defaultConfiguration: LoadzConfiguration? = null

        @Volatile
        private var defaultInstance: Loadz? = null

        /**
         * Sets the global configuration for the default Loadz instance.
         */
        fun setDefaultConfiguration(configuration: LoadzConfiguration) {
            synchronized(lock) {
                defaultConfiguration = configuration
            }
        }

        /**
         * Get the default Loadz Configuration set with setDefaultConfiguration.
         */
        fun getDefaultConfiguration(): LoadzConfiguration? {
            return synchronized(lock) {
                defaultConfiguration
            }
        }

        /**
         * Gets the default singleton instance of Loadz.
         * Throws an exception if setDefaultConfiguration has not been called yet.
         */
        fun getDefaultInstance(): Loadz {
            return synchronized(lock) {
                val config = defaultConfiguration
                    ?: throw RuntimeException("LoadzConfiguration not set. Call setDefaultConfiguration first.")

                var instance = defaultInstance
                if (instance == null || instance.isClosed) {
                    instance = getInstance(config)
                    defaultInstance = instance
                }
                instance
            }
        }

        /**
         * Creates a custom, separate Instance of Loadz based on a specific configuration.
         */
        fun getInstance(configuration: LoadzConfiguration): Loadz {
            val database = LoadzDatabaseWrapper(configuration.appContext, configuration.namespace)
            val mainListenerDispatcher = MainListenerDispatcher()
            val dispatcher = LoadzDispatcher(configuration.appContext, database, mainListenerDispatcher)

            return LoadzImpl(configuration, database, dispatcher, mainListenerDispatcher)
        }
    }
}
