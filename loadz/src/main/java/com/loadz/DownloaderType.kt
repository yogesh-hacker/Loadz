package com.loadz

enum class DownloaderType(val value: Int) {
    HLS(0),
    NORMAL(1);

    companion object {
        @JvmStatic
        fun valueOf(value: Int): DownloaderType {
            return entries.find { it.value == value } ?: NORMAL
        }
    }
}
