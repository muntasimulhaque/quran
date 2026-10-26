package io.github.muntasimulhaque.quran.data

/**
 * The one host family the app is allowed to read from.
 *
 * The catalog's URLs point at the project's own GitHub Releases. GitHub's
 * download service answers with a redirect to its release storage, which is
 * a different host name, so the check is on the host family rather than on
 * one string. Anything else is refused before a byte is read, including a
 * redirect that would otherwise carry the request to a third party.
 */
object ReleaseHosts {

    private val allowed = setOf(
        "github.com",
        "release-assets.githubusercontent.com",
        "objects.githubusercontent.com",
    )

    fun isAllowed(url: String): Boolean {
        val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (uri.userInfo != null) return false
        if (uri.port != -1 && uri.port != 443) return false
        val host = uri.host?.lowercase() ?: return false
        return host in allowed
    }
}
