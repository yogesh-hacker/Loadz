package com.loadz

/**
 * Enumeration which contains specific errors that can occur during a Loadz download.
 */
enum class LoadzError(
    val value: Int,

    /**
     * A throwable will only be present at the time the error occurs and will not be saved
     * in the database. This allows detailed stack traces to reach the LoadzListener.
     */
    var throwable: Throwable? = null
) {
    UNKNOWN(-1),
    NONE(0),
    FILE_NOT_CREATED(1),
    CONNECTION_TIMED_OUT(2),
    UNKNOWN_HOST(3),
    HTTP_NOT_FOUND(4),
    WRITE_PERMISSION_DENIED(5),
    NO_STORAGE_SPACE(6),
    NO_NETWORK_CONNECTION(7),
    EMPTY_RESPONSE_FROM_SERVER(8),
    REQUEST_ALREADY_EXIST(9),
    DOWNLOAD_NOT_FOUND(10),
    FETCH_DATABASE_ERROR(11),
    REQUEST_WITH_ID_ALREADY_EXIST(13),
    REQUEST_WITH_FILE_PATH_ALREADY_EXIST(14),
    REQUEST_NOT_SUCCESSFUL(15),
    UNKNOWN_IO_ERROR(16),
    FILE_NOT_FOUND(17),
    FETCH_FILE_SERVER_URL_INVALID(19),
    INVALID_CONTENT_HASH(20),
    FAILED_TO_UPDATE_REQUEST(21),
    FAILED_TO_ADD_COMPLETED_DOWNLOAD(22),
    FETCH_FILE_SERVER_INVALID_RESPONSE(23),
    REQUEST_DOES_NOT_EXIST(24),
    ENQUEUE_NOT_SUCCESSFUL(25),
    COMPLETED_NOT_ADDED_SUCCESSFULLY(26),
    ENQUEUED_REQUESTS_ARE_NOT_DISTINCT(27),
    FAILED_TO_RENAME_INCOMPLETE_DOWNLOAD_FILE(28),
    FAILED_TO_RENAME_FILE(29),
    FILE_ALLOCATION_FAILED(30),
    HTTP_CONNECTION_NOT_ALLOWED(31),
    FILE_ALREADY_EXISTS(32);

    companion object {
        @JvmStatic
        fun valueOf(value: Int): LoadzError {
            return when (value) {
                -1 -> UNKNOWN
                0 -> NONE
                1 -> FILE_NOT_CREATED
                2 -> CONNECTION_TIMED_OUT
                3 -> UNKNOWN_HOST
                4 -> HTTP_NOT_FOUND
                5 -> WRITE_PERMISSION_DENIED
                6 -> NO_STORAGE_SPACE
                7 -> NO_NETWORK_CONNECTION
                8 -> EMPTY_RESPONSE_FROM_SERVER
                9 -> REQUEST_ALREADY_EXIST
                10 -> DOWNLOAD_NOT_FOUND
                11 -> FETCH_DATABASE_ERROR
                13 -> REQUEST_WITH_ID_ALREADY_EXIST
                14 -> REQUEST_WITH_FILE_PATH_ALREADY_EXIST
                15 -> REQUEST_NOT_SUCCESSFUL
                16 -> UNKNOWN_IO_ERROR
                17 -> FILE_NOT_FOUND
                19 -> FETCH_FILE_SERVER_URL_INVALID
                20 -> INVALID_CONTENT_HASH
                21 -> FAILED_TO_UPDATE_REQUEST
                22 -> FAILED_TO_ADD_COMPLETED_DOWNLOAD
                23 -> FETCH_FILE_SERVER_INVALID_RESPONSE
                24 -> REQUEST_DOES_NOT_EXIST
                25 -> ENQUEUE_NOT_SUCCESSFUL
                26 -> COMPLETED_NOT_ADDED_SUCCESSFULLY
                27 -> ENQUEUED_REQUESTS_ARE_NOT_DISTINCT
                28 -> FAILED_TO_RENAME_INCOMPLETE_DOWNLOAD_FILE
                29 -> FAILED_TO_RENAME_FILE
                30 -> FILE_ALLOCATION_FAILED
                31 -> HTTP_CONNECTION_NOT_ALLOWED
                32 -> FILE_ALREADY_EXISTS
                else -> UNKNOWN
            }
        }
    }
}
