package com.loadz.hls

data class HlsVariant(
    val resolution: String,
    val bitrate: Long,
    val url: String
) {
    val displayName: String
        get() {
            if (resolution.isNotBlank()) {
                val parts = resolution.split("x", "X")
                if (parts.size == 2) {
                    return "${parts[1]}p"
                }
                return resolution
            }
            if (bitrate > 0) {
                return "${bitrate / 1000} kbps"
            }
            return "Unknown Quality"
        }
}