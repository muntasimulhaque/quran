package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * The one network question a download asks before it spends the reader's
 * data: is the phone on a metered connection?
 *
 * A missing network is not "metered": the transfer will fail at its own door
 * with its own message, and a warning about a network that is not there would
 * be a second, wrong sentence. The answer is true only when there is an
 * active network and it does not carry the unmetered capability (owner
 * decision, 23.1).
 */
object DownloadNetwork {

    fun isMetered(context: Context): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val active = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(active) ?: return false
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
        return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
