package com.loadz

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Class used to hold the configuration settings for a Loadz instance.
 * @see Builder
 */
data class LoadzConfiguration internal constructor(
    val appContext: Context,
    val namespace: String,
    val concurrentLimit: Int,
    val progressReportingIntervalMillis: Long,
    val loggingEnabled: Boolean,
    val globalNetworkType: NetworkType,
    val autoStart: Boolean,
    val retryOnNetworkGain: Boolean,
    val backgroundHandler: Handler?,
    val activeDownloadsCheckInterval: Long,
    val maxAutoRetryAttempts: Int,
    val preAllocateFileOnCreation: Boolean,
    val createTempFilesOnEnqueue: Boolean
) {

    /**
     * Creates a new Instance of Loadz with this object's configuration settings.
     */
    fun getNewLoadzInstance(): Loadz {
        return Loadz.Companion.getInstance(this)
    }

    /** Used to create an instance of LoadzConfiguration. */
    class Builder(context: Context) {

        private val appContext: Context = context.applicationContext
        private var namespace: String = "default_loadz"
        private var concurrentLimit: Int = 1
        private var progressReportingIntervalMillis: Long = 1000L // 1 second default for smooth UI
        private var loggingEnabled: Boolean = false
        private var globalNetworkType: NetworkType = NetworkType.ALL
        private var autoStart: Boolean = true
        private var retryOnNetworkGain: Boolean = true
        private var backgroundHandler: Handler? = null
        private var activeDownloadCheckInterval: Long = 300000L // 5 minutes
        private var maxAutoRetryAttempts: Int = 0
        private var preAllocateFileOnCreation: Boolean = true
        private var createTempFilesOnEnqueue: Boolean = true

        /** Sets the namespace which Loadz operates in (used for isolating databases). */
        fun setNamespace(namespace: String?) = apply {
            this.namespace = if (namespace.isNullOrEmpty()) "default_loadz" else namespace
        }

        /** Sets how often progress listeners are triggered (in milliseconds). */
        fun setProgressReportingInterval(intervalMillis: Long) = apply {
            require(intervalMillis >= 0) { "progressReportingIntervalMillis cannot be less than 0" }
            this.progressReportingIntervalMillis = intervalMillis
        }

        /** Sets the number of parallel downloads Loadz should perform at any given time. */
        fun setDownloadConcurrentLimit(limit: Int) = apply {
            require(limit >= 0) { "Concurrent limit cannot be less than 0" }
            this.concurrentLimit = limit
        }

        /** Overrides each download's specified network type preference and uses a global one. */
        fun setGlobalNetworkType(networkType: NetworkType) = apply {
            this.globalNetworkType = networkType
        }

        /** Enable or disable internal logging. */
        fun enableLogging(enabled: Boolean) = apply {
            this.loggingEnabled = enabled
        }

        /** Allows Loadz to start processing old requests instantly. */
        fun enableAutoStart(enabled: Boolean) = apply {
            this.autoStart = enabled
        }

        /** Resumes downloads automatically when network connection is regained. */
        fun enableRetryOnNetworkGain(enabled: Boolean) = apply {
            this.retryOnNetworkGain = enabled
        }

        /**
         * Sets the handler (worker thread) Loadz uses to perform background operations.
         * @throws IllegalArgumentException if handler uses main thread.
         */
        fun setBackgroundHandler(handler: Handler) = apply {
            require(handler.looper.thread != Looper.getMainLooper().thread) {
                "The background handler cannot use the main/ui thread"
            }
            this.backgroundHandler = handler
        }

        /** Global maximum number of times Loadz will auto-retry a failed download. */
        fun setAutoRetryMaxAttempts(attempts: Int) = apply {
            require(attempts >= 0) { "AutoRetryMaxAttempts has to be greater than or equal to 0" }
            this.maxAutoRetryAttempts = attempts
        }

        /** Pre-allocate the download file size on local storage to prevent fragmentation. */
        fun preAllocateFileOnCreation(preAllocate: Boolean) = apply {
            this.preAllocateFileOnCreation = preAllocate
        }

        /** Create temporary .ts files or temp directories as soon as enqueued. */
        fun createTempFilesOnEnqueue(create: Boolean) = apply {
            this.createTempFilesOnEnqueue = create
        }

        /** Builds the final LoadzConfiguration instance. */
        fun build(): LoadzConfiguration {
            return LoadzConfiguration(
                appContext = appContext,
                namespace = namespace,
                concurrentLimit = concurrentLimit,
                progressReportingIntervalMillis = progressReportingIntervalMillis,
                loggingEnabled = loggingEnabled,
                globalNetworkType = globalNetworkType,
                autoStart = autoStart,
                retryOnNetworkGain = retryOnNetworkGain,
                backgroundHandler = backgroundHandler,
                activeDownloadsCheckInterval = activeDownloadCheckInterval,
                maxAutoRetryAttempts = maxAutoRetryAttempts,
                preAllocateFileOnCreation = preAllocateFileOnCreation,
                createTempFilesOnEnqueue = createTempFilesOnEnqueue
            )
        }
    }
}
