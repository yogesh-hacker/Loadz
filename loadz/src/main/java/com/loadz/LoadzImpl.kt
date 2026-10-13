package com.loadz

import com.loadz.database.LoadzDatabase
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import com.loadz.hls.HlsVariant
import com.loadz.hls.M3u8Parser
import java.io.File

internal class LoadzImpl(
    private val configuration: LoadzConfiguration,
    private val database: LoadzDatabase,
    private val dispatcher: DownloadDispatcher,
    private val mainListenerDispatcher: MainListenerDispatcher
) : Loadz {

    private val isClosedAtomic = AtomicBoolean(false)
    override val isClosed: Boolean get() = isClosedAtomic.get()

    override val namespace: String = configuration.namespace

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isFrozen = false

    private inline fun <T> executeAsync(
        crossinline action: suspend () -> T,
        noinline onSuccess: ((T) -> Unit)? = null,
        noinline onError: ((LoadzError) -> Unit)? = null
    ) {
        if (isClosed) {
            onError?.invoke(LoadzError.UNKNOWN.apply {
                throwable = IllegalStateException("Loadz instance is closed.")
            })
            return
        }

        scope.launch {
            runCatching {
                action()
            }.onSuccess { result ->
                if (onSuccess != null) {
                    withContext(Dispatchers.Main.immediate) { onSuccess(result) }
                }
            }.onFailure { exception ->
                val error = LoadzError.UNKNOWN.apply { throwable = exception }
                if (onError != null) {
                    withContext(Dispatchers.Main.immediate) { onError(error) }
                }
            }
        }
    }

    override fun enqueue(
        request: LoadzRequest,
        onSuccess: ((LoadzRequest) -> Unit)?,
        onError: ((LoadzError) -> Unit)?
    ): Loadz = apply {
        executeAsync(
            action = {
                if (database.getDownload(request.id) != null) {
                    throw IllegalStateException("Request with ID ${request.id} already exists in database.")
                }
                
                if (File(request.filePath).exists() || database.getDownloadByPath(request.filePath) != null) {
                    val error = LoadzError.FILE_ALREADY_EXISTS.apply { 
                        throwable = IllegalStateException("File already exists: ${request.filePath}") 
                    }
                    if (onError != null) {
                        withContext(Dispatchers.Main.immediate) { onError(error) }
                    }
                    // Abort enqueueing
                    return@executeAsync request
                }

                val download = Download(
                    url = request.url,
                    filePath = request.filePath,
                    downloaderType = request.downloaderType,
                    id = request.id,
                    namespace = request.namespace
                ).apply {
                    status = Status.QUEUED
                    headers = request.headers
                    networkType = request.networkType
                    priority = request.priority
                    autoRetryMaxAttempts = request.autoRetryMaxAttempts
                }

                database.insert(download)

                if (!isFrozen) {
                    dispatcher.enqueue(download)
                } else {
                    download.status = Status.PAUSED
                    database.update(download)
                }

                mainListenerDispatcher.onQueued(download)
                request
            },
            onSuccess = onSuccess,
            onError = onError
        )
    }

    override fun enqueue(
        requests: List<LoadzRequest>,
        onResult: ((List<Pair<LoadzRequest, LoadzError>>) -> Unit)?
    ): Loadz = apply {
        executeAsync(
            action = {
                val results = mutableListOf<Pair<LoadzRequest, LoadzError>>()
                val downloadsToInsert = mutableListOf<Download>()

                requests.forEach { req ->
                    try {
                        val download = Download(req.url, req.filePath, req.downloaderType, req.id, req.namespace).apply {
                            status = if (isFrozen) Status.PAUSED else Status.QUEUED
                            headers = req.headers
                            networkType = req.networkType
                            priority = req.priority
                            autoRetryMaxAttempts = req.autoRetryMaxAttempts
                        }
                        downloadsToInsert.add(download)
                    } catch (e: Exception) {
                        results.add(Pair(req, LoadzError.UNKNOWN.apply { throwable = e }))
                    }
                }

                database.insertAll(downloadsToInsert)
                if (!isFrozen) {
                    dispatcher.enqueueAll(downloadsToInsert)
                }

                downloadsToInsert.forEach { download ->
                    mainListenerDispatcher.onQueued(download)
                }
                results
            },
            onSuccess = onResult
        )
    }

    override fun fetchHlsQualities(
        url: String,
        headers: Map<String, String>,
        onResult: (List<HlsVariant>?, Exception?) -> Unit
    ): Loadz = apply {
        scope.launch {
            try {
                val parser = M3u8Parser(dispatcher.httpClient)
                val qualities = parser.fetchQualities(url, headers)
                withContext(Dispatchers.Main.immediate) {
                    onResult(qualities, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main.immediate) {
                    onResult(null, e)
                }
            }
        }
    }

    override fun pause(id: Int): Loadz = apply { pause(listOf(id)) }

    override fun pause(ids: List<Int>): Loadz = apply {
        executeAsync(action = {
            val downloadsToUpdate = ids.mapNotNull { database.getDownload(it) }
            downloadsToUpdate.forEach { 
                it.status = Status.PAUSED 
                dispatcher.pause(it.id)
            }
            database.updateAll(downloadsToUpdate)
            downloadsToUpdate.forEach { d -> mainListenerDispatcher.onPaused(d) }
        })
    }

    override fun pauseAll(): Loadz = apply {
        executeAsync(action = {
            val activeDownloads = database.getDownloadsByStatus(listOf(Status.DOWNLOADING, Status.QUEUED))
            activeDownloads.forEach { it.status = Status.PAUSED }
            database.updateAll(activeDownloads)
            dispatcher.pauseAll()
            activeDownloads.forEach { d -> mainListenerDispatcher.onPaused(d) }
        })
    }

    override fun resume(id: Int): Loadz = apply { resume(listOf(id)) }

    override fun resume(ids: List<Int>): Loadz = apply {
        executeAsync(action = {
            val downloadsToUpdate = ids.mapNotNull { database.getDownload(it) }
            downloadsToUpdate.forEach { 
                it.status = Status.QUEUED 
                if (!isFrozen) dispatcher.resume(it.id)
            }
            database.updateAll(downloadsToUpdate)
            downloadsToUpdate.forEach { d -> mainListenerDispatcher.onResumed(d) }
        })
    }

    override fun resumeAll(): Loadz = apply {
        executeAsync(action = {
            val pausedDownloads = database.getDownloadsByStatus(listOf(Status.PAUSED))
            pausedDownloads.forEach { it.status = Status.QUEUED }
            database.updateAll(pausedDownloads)
            if (!isFrozen) dispatcher.resumeAll()
            pausedDownloads.forEach { d -> mainListenerDispatcher.onResumed(d) }
        })
    }

    override fun cancel(id: Int): Loadz = apply { cancel(listOf(id)) }

    override fun cancel(ids: List<Int>): Loadz = apply {
        executeAsync(action = {
            val downloadsToUpdate = ids.mapNotNull { database.getDownload(it) }
            downloadsToUpdate.forEach { 
                it.status = Status.CANCELLED 
                dispatcher.cancel(it.id)
            }
            database.updateAll(downloadsToUpdate)
            downloadsToUpdate.forEach { d -> mainListenerDispatcher.onCancelled(d) }
        })
    }

    override fun cancelAll(): Loadz = apply {
        executeAsync(action = {
            val pendingDownloads = database.getAllDownloads().filter {
                it.status != Status.COMPLETED && it.status != Status.FAILED
            }
            pendingDownloads.forEach { it.status = Status.CANCELLED }
            database.updateAll(pendingDownloads)
            dispatcher.cancelAll()
            pendingDownloads.forEach { d -> mainListenerDispatcher.onCancelled(d) }
        })
    }

    override fun retry(id: Int): Loadz = apply { retry(listOf(id)) }
    override fun retry(ids: List<Int>): Loadz = apply {
        executeAsync(action = {
            val downloadsToUpdate = ids.mapNotNull { database.getDownload(it) }
                .filter { it.status == Status.FAILED || it.status == Status.CANCELLED }
            
            downloadsToUpdate.forEach { 
                it.status = Status.QUEUED
                it.error = null
                if (!isFrozen) dispatcher.enqueue(it)
            }
            database.updateAll(downloadsToUpdate)
        })
    }

    override fun freeze(): Loadz = apply {
        isFrozen = true
        dispatcher.pauseAll()
    }

    override fun unfreeze(): Loadz = apply {
        isFrozen = false
        dispatcher.resumeAll()
    }

    override fun remove(id: Int): Loadz = apply { remove(listOf(id)) }

    override fun remove(ids: List<Int>): Loadz = apply {
        executeAsync(action = {
            ids.forEach {
                dispatcher.cancel(it)
                database.delete(it)
            }
        })
    }

    override fun removeAll(): Loadz = apply {
        executeAsync(action = {
            dispatcher.cancelAll()
            database.deleteAll()
        })
    }

    override fun removeAllWithStatus(status: Status): Loadz = apply { removeAllWithStatus(listOf(status)) }
    
    override fun removeAllWithStatus(statuses: List<Status>): Loadz = apply {
        executeAsync(action = {
            val toRemove = database.getDownloadsByStatus(statuses)
            toRemove.forEach {
                dispatcher.cancel(it.id)
                database.delete(it.id)
            }
        })
    }

    override fun delete(id: Int): Loadz = apply { delete(listOf(id)) }

    override fun delete(ids: List<Int>): Loadz = apply {
        executeAsync(action = {
            ids.forEach {
                val download = database.getDownload(it)
                dispatcher.cancel(it)
                database.delete(it)
                download?.let { d -> dispatcher.deleteFile(d.filePath) }
            }
        })
    }

    override fun deleteAll(): Loadz = apply {
        executeAsync(action = {
            val allDownloads = database.getAllDownloads()
            dispatcher.cancelAll()
            database.deleteAll()
            allDownloads.forEach { dispatcher.deleteFile(it.filePath) }
        })
    }

    override fun deleteAllWithStatus(status: Status): Loadz = apply { deleteAllWithStatus(listOf(status)) }
    
    override fun deleteAllWithStatus(statuses: List<Status>): Loadz = apply {
        executeAsync(action = {
            val toDelete = database.getDownloadsByStatus(statuses)
            toDelete.forEach {
                dispatcher.cancel(it.id)
                database.delete(it.id)
                dispatcher.deleteFile(it.filePath)
            }
        })
    }

    override fun resetAutoRetryAttempts(downloadId: Int, retryDownload: Boolean): Loadz = apply {
        executeAsync(action = {
            database.getDownload(downloadId)?.let {
                it.autoRetryAttempts = 0
                if (retryDownload && (it.status == Status.FAILED || it.status == Status.CANCELLED)) {
                    it.status = Status.QUEUED
                    it.error = null
                    if (!isFrozen) dispatcher.enqueue(it)
                }
                database.update(it)
            }
        })
    }

    override fun renameCompletedDownloadFile(id: Int, newFileName: String): Loadz = apply {
        // Will implement actual OS renaming in the Network Engine logic
    }

    override fun getDownloads(onResult: (List<Download>) -> Unit): Loadz = apply {
        executeAsync(action = { database.getAllDownloads() }, onSuccess = onResult)
    }

    override fun getDownload(id: Int, onResult: (Download?) -> Unit): Loadz = apply {
        executeAsync(action = { database.getDownload(id) }, onSuccess = onResult)
    }

    override fun getDownloads(idList: List<Int>, onResult: (List<Download>) -> Unit): Loadz = apply {
        executeAsync(action = {
            idList.mapNotNull { database.getDownload(it) }
        }, onSuccess = onResult)
    }

    override fun getDownloadsWithStatus(status: Status, onResult: (List<Download>) -> Unit): Loadz = apply {
        getDownloadsWithStatus(listOf(status), onResult)
    }

    override fun getDownloadsWithStatus(statuses: List<Status>, onResult: (List<Download>) -> Unit): Loadz = apply {
        executeAsync(action = { database.getDownloadsByStatus(statuses) }, onSuccess = onResult)
    }

    override fun getDownloadsByTag(tag: String, onResult: (List<Download>) -> Unit): Loadz = apply {
        executeAsync(action = { database.getAllDownloads().filter { it.tag == tag } }, onSuccess = onResult)
    }

    override fun setGlobalNetworkType(networkType: NetworkType): Loadz = apply {
        // Apply logic to dispatcher config if needed
    }

    override fun setDownloadConcurrentLimit(downloadConcurrentLimit: Int): Loadz = apply {
        // Set dynamic limit on dispatcher
    }

    override fun enableLogging(enabled: Boolean): Loadz = apply {
        // Control logger
    }

    override fun addListener(listener: LoadzListener): Loadz = apply {
        if (!isClosed) {
            mainListenerDispatcher.attachListener(listener)
            dispatcher.attachListener(listener)
        }
    }

    override fun removeListener(listener: LoadzListener): Loadz = apply {
        mainListenerDispatcher.detachListener(listener)
        dispatcher.detachListener(listener)
    }

    override fun close() {
        if (isClosedAtomic.compareAndSet(false, true)) {
            scope.cancel()
            mainListenerDispatcher.clear()
            dispatcher.shutdown()
        }
    }
}
