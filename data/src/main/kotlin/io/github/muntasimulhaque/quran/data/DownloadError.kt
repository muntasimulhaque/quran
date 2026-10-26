package io.github.muntasimulhaque.quran.data

import android.database.sqlite.SQLiteFullException
import java.io.IOException

/**
 * Why a download stopped, in the three answers the reader can act on.
 *
 * A failed transfer used to be one sentence for every cause ("Check your
 * connection"), which told a reader with a full phone to look at their Wi-Fi.
 * The three answers are the three things the reader can do: try again on the
 * network, free some space, or read the content that is already here.
 */
enum class DownloadError {
    /** The network, the host, or the transfer failed. */
    Network,

    /** The phone could not write the file, usually because it is full. */
    Storage,

    /** Anything the app cannot name, kept honest rather than guessed at. */
    Other,
}

/** The one classification every download path shares. */
fun Throwable.downloadError(): DownloadError {
    var cause: Throwable? = this
    while (cause != null) {
        if (cause is SQLiteFullException) return DownloadError.Storage
        if (cause is IOException) {
            return if (cause.message.orEmpty().isStorageMessage()) {
                DownloadError.Storage
            } else {
                DownloadError.Network
            }
        }
        if (cause is IllegalStateException) {
            val message = cause.message.orEmpty().lowercase()
            if (message.startsWith("http") || "download" in message) {
                return DownloadError.Network
            }
        }
        cause = cause.cause
    }
    return DownloadError.Other
}

private fun String.isStorageMessage(): Boolean {
    val message = lowercase()
    return listOf("no space", "enospc", "disk full", "storage", "not enough space")
        .any { it in message }
}
