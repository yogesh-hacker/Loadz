package com.loadz

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Main Thread dispatcher responsible for routing all [LoadzListener] callbacks
 * to the Main (UI) Thread using [Dispatchers.Main] before they reach the consumer.
 */
class MainListenerDispatcher {

    private val mainScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val listeners = CopyOnWriteArraySet<LoadzListener>()

    fun attachListener(listener: LoadzListener) {
        listeners.add(listener)
    }

    fun detachListener(listener: LoadzListener) {
        listeners.remove(listener)
    }

    fun clear() {
        listeners.clear()
    }

    /**
     * Dispatches a callback action to all attached listeners on the Main (UI) Thread.
     */
    fun dispatch(action: (LoadzListener) -> Unit) {
        mainScope.launch {
            listeners.forEach { listener ->
                action(listener)
            }
        }
    }

    fun onAdded(download: Download) = dispatch { it.onAdded(download) }
    fun onQueued(download: Download) = dispatch { it.onQueued(download) }
    fun onProgress(download: Download, progress: Int) = dispatch { it.onProgress(download, progress) }
    fun onConversion(download: Download, progress: Int) = dispatch { it.onConversion(download, progress) }
    fun onCompleted(download: Download) = dispatch { it.onCompleted(download) }
    fun onFailed(download: Download, error: String) = dispatch { it.onFailed(download, error) }
    fun onPaused(download: Download) = dispatch { it.onPaused(download) }
    fun onResumed(download: Download) = dispatch { it.onResumed(download) }
    fun onCancelled(download: Download) = dispatch { it.onCancelled(download) }
    fun onError(download: Download, exception: Exception) = dispatch { it.onError(download, exception) }
}
