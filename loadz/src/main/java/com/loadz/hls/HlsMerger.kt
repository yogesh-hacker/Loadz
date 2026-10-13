package com.loadz.hls

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CancellationException
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Handles segment concatenation and native MP4/MKV remuxing exclusively using FFmpegKit.
 */
class HlsMerger {

    fun mergeSegments(
        tempDir: File,
        segmentCount: Int,
        targetFile: File,
        totalDurationMs: Long,
        onProgress: (Int) -> Unit,
        isCancelled: () -> Boolean
    ) {
        if (isCancelled()) throw CancellationException("Conversion Cancelled")

        // 1. Always create the list.txt file for FFmpeg concat demuxer
        val listFile = File(tempDir, "list.txt")
        listFile.bufferedWriter().use { writer ->
            for (index in 0 until segmentCount) {
                val tsFile = File(tempDir, "segment_$index.ts")
                writer.write("file '${tsFile.absolutePath}'\n")
            }
        }

        // 2. Build FFmpeg command based on file extension
        val isMp4OrMkv = targetFile.name.endsWith(".mp4", ignoreCase = true) ||
                targetFile.name.endsWith(".mkv", ignoreCase = true)

        val ffmpegCommand = if (isMp4OrMkv) {
            "-y -f concat -safe 0 -i \"${listFile.absolutePath}\" -c copy -bsf:a aac_adtstoasc \"${targetFile.absolutePath}\""
        } else {
            "-y -f concat -safe 0 -i \"${listFile.absolutePath}\" -c copy \"${targetFile.absolutePath}\""
        }

        // 3. Execute Async with Session-Specific Callbacks and Timed Latch
        val latch = CountDownLatch(1)

        val session = FFmpegKit.executeAsync(
            ffmpegCommand,
            { _ -> 
                // completeCallback
                latch.countDown()
            },
            { _ -> 
                // logCallback (Ignored)
            },
            { statistics -> 
                // statisticsCallback
                if (statistics != null && totalDurationMs > 0) {
                    val rawProgress = (statistics.time.toDouble() / totalDurationMs.toDouble()) * 100
                    val estProgress = rawProgress.toInt().coerceIn(0, 99)
                    onProgress(estProgress)
                }
            }
        )

        // 4. Active Monitoring Loop for Cancellation
        while (!latch.await(250, TimeUnit.MILLISECONDS)) {
            if (isCancelled()) {
                FFmpegKit.cancel(session.getSessionId())
                throw CancellationException("Conversion Cancelled")
            }
        }

        // 5. Evaluate Final Result
        if (ReturnCode.isSuccess(session.getReturnCode())) {
            onProgress(100)
        } else if (ReturnCode.isCancel(session.getReturnCode())) {
            throw CancellationException("Conversion Cancelled")
        } else {
            val logs = session.getAllLogsAsString()
            throw Exception("FFmpeg conversion failed: \n$logs")
        }
    }
}