package com.loadz.hls

/**
 * Represents a single HLS media segment parsed from an M3U8 playlist.
 */
data class HlsSegment(
    val index: Int,
    val url: String,
    val durationMs: Long = 0L,
    val keyUrl: String? = null,
    val ivHex: String? = null,
    val sequenceNumber: Int = index
)