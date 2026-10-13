package com.loadz

enum class Status(val value: Int) {
    ADDED(0),
    QUEUED(1),
    DOWNLOADING(2),
    PAUSED(3),
    CONVERTING(4),
    COMPLETED(5),
    CANCELLED(6),
    FAILED(7);

    companion object {
        @JvmStatic
        fun valueOf(value: Int): Status {
            return entries.find { it.value == value } ?: ADDED
        }
    }
}
