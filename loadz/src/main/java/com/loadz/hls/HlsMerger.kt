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
        segments: List<HlsSegment>,
        targetFile: File,
        totalDurationMs: Long,
        onProgress: (Int) -> Unit,
        isCancelled: () -> Boolean
    ) {
        if (isCancelled()) throw CancellationException("Conversion Cancelled")

        // Generate Local M3U8 Playlist
        val localM3u8File = File(tempDir, "local.m3u8")
        localM3u8File.bufferedWriter().use { writer ->
            writer.write("#EXTM3U\n")
            writer.write("#EXT-X-VERSION:7\n")

            // Calculate target duration (max duration across segments, min 1)
            val maxDuration = segments.maxOfOrNull { it.durationMs } ?: 0L
            var targetDurationSec = (maxDuration / 1000.0).toLong()
            if (targetDurationSec <= 0) targetDurationSec = 10
            writer.write("#EXT-X-TARGETDURATION:$targetDurationSec\n")

            for (segment in segments) {
                if (segment.durationMs == 0L) {
                    // It's an fMP4 map initialization segment
                    writer.write("#EXT-X-MAP:URI=\"segment_${segment.index}.ts\"\n")
                } else {
                    val durationSec = segment.durationMs / 1000.0
                    writer.write("#EXTINF:$durationSec,\n")
                    writer.write("segment_${segment.index}.ts\n")
                }
            }
            writer.write("#EXT-X-ENDLIST\n")
        }

        // Build FFmpeg command to read local playlist
        val ffmpegCommand =
            "-allowed_extensions ALL -y -i \"${localM3u8File.absolutePath}\" -c copy \"${targetFile.absolutePath}\""

        // Execute Async with Session-Specific Callbacks and Timed Latch
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
                    val rawProgress =
                        (statistics.time.toDouble() / totalDurationMs.toDouble()) * 100
                    val estProgress = rawProgress.toInt().coerceIn(0, 99)
                    onProgress(estProgress)
                }
            }
        )

        // Active Monitoring Loop for Cancellation
        while (!latch.await(250, TimeUnit.MILLISECONDS)) {
            if (isCancelled()) {
                FFmpegKit.cancel(session.getSessionId())
                throw CancellationException("Conversion Cancelled")
            }
        }

        // Evaluate Final Result
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