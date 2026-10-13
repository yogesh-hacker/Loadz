package com.loadz

enum class NetworkType(val value: Int) {
    ALL(0),
    WIFI_ONLY(1),
    UNMETERED(2);

    companion object {
        @JvmStatic
        fun valueOf(value: Int): NetworkType {
            return entries.find { it.value == value } ?: ALL
        }
    }
}
