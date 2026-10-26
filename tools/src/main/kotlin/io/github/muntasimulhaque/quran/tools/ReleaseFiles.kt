package io.github.muntasimulhaque.quran.tools

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * The one host family the content tools may read from. A redirect is checked
 * before the connection that would use it is opened, so a release page can
 * never carry the build to another host (owner decision, security audit).
 */
object ReleaseFiles {

    private val allowed = setOf(
        "github.com",
        "release-assets.githubusercontent.com",
        "objects.githubusercontent.com",
    )

    private const val MAX_REDIRECTS = 5

    fun open(url: String): HttpURLConnection {
        var current = url
        var hops = 0
        while (true) {
            if (!allowed(current)) {
                throw IllegalStateException("the download URL is not the project's release host")
            }
            val connection = URL(current).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 20_000
            connection.readTimeout = 120_000
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

    fun allowed(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (uri.userInfo != null) return false
        if (uri.port != -1 && uri.port != 443) return false
        val host = uri.host?.lowercase() ?: return false
        return host in allowed
    }
}
