package io.github.muntasimulhaque.quran.data

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * One HTTP connection to the project's own release host family.
 *
 * A redirect is not followed by the platform and checked afterward: every
 * hop is checked before the connection that would use it is opened, and the
 * request goes to no host outside [ReleaseHosts]. The caller still owns the
 * response code, the body, and the socket; this only answers which URL was
 * allowed to receive the request (owner decision, security audit).
 */
internal object ReleaseHttp {

    private const val MAX_REDIRECTS = 5

    fun open(url: String, configure: (HttpURLConnection) -> Unit = {}): HttpURLConnection {
        var current = url
        var hops = 0
        while (true) {
            if (!ReleaseHosts.isAllowed(current)) {
                throw IllegalStateException("the download URL is not the project's release host")
            }
            val connection = URL(current).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/octet-stream")
            configure(connection)
            connection.connect()
            val code = connection.responseCode
            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (location.isNullOrBlank() || hops >= MAX_REDIRECTS) {
                    throw IllegalStateException("the download redirected too many times")
                }
                hops++
                current = runCatching { URI(current).resolve(location.trim()).toString() }
                    .getOrElse {
                        throw IllegalStateException("the download redirect could not be read")
                    }
                continue
            }
            return connection
        }
    }
}
