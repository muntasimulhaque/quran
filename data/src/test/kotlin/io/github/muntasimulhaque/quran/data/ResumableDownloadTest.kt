package io.github.muntasimulhaque.quran.data

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference

/**
 * The resumable package transfer, against a server the test owns.
 *
 * The recitation and word-audio downloads used a fresh temporary file on every
 * attempt, so a dropped connection on a 186 MB surah threw the whole transfer
 * away. The pack downloader had resumed since 9.3; this is the shared rule
 * both package downloaders now use (owner decision, 23.1).
 *
 * The production opener checks the project's release host. The test hands in
 * its own opener for a loopback server, so the Range, the 206 answer, the 416
 * answer, and the checksum can be measured without a network or a release.
 */
class ResumableDownloadTest {

    private val temporary = Files.createTempDirectory("resumable-download").toFile()
    private val servers = mutableListOf<HttpServer>()

    @After
    fun clean() {
        servers.forEach { it.stop(0) }
        temporary.deleteRecursively()
    }

    @Test
    fun `a part is appended from its own length`() = runBlocking {
        val payload = payload()
        val part = File(temporary, "pack.part").apply {
            writeBytes(payload.copyOfRange(0, 1024))
        }
        val range = AtomicReference<String>()
        val server = served { exchange, start ->
            range.set(exchange.requestHeaders.getFirst("Range"))
            respond(exchange, payload, start)
        }
        val result = ResumableDownload.download(
            part = part,
            spec = spec(server, payload),
            onProgress = { _, _ -> },
            open = opener(),
        )
        assertTrue("the resumed transfer must succeed", result.isSuccess)
        assertArrayEquals("the part must be the whole package", payload, part.readBytes())
        assertEquals("the server must be asked for the rest", "bytes=1024-", range.get())
    }

    @Test
    fun `a server that ignores the range restarts the part in the same attempt`() = runBlocking {
        val payload = payload()
        val part = File(temporary, "pack.part").apply {
            writeBytes(payload.copyOfRange(0, 1024))
        }
        val server = served { exchange, _ ->
            // The request may carry a Range, but this server answers 200 with
            // the whole file: the part must be truncated, not appended to.
            exchange.responseHeaders.add("Content-Type", "application/octet-stream")
            exchange.sendResponseHeaders(200, payload.size.toLong())
            exchange.responseBody.use { it.write(payload) }
        }
        val result = ResumableDownload.download(
            part = part,
            spec = spec(server, payload),
            onProgress = { _, _ -> },
            open = opener(),
        )
        assertTrue("the whole-file answer must succeed", result.isSuccess)
        assertArrayEquals("no old bytes may survive the restart", payload, part.readBytes())
    }

    @Test
    fun `a full part answering 416 is fetched once from the beginning`() = runBlocking {
        val payload = payload()
        val part = File(temporary, "pack.part").apply { writeBytes(payload) }
        val server = served { exchange, _ ->
            if (exchange.requestHeaders.getFirst("Range") != null) {
                exchange.sendResponseHeaders(416, -1)
            } else {
                exchange.responseHeaders.add("Content-Type", "application/octet-stream")
                exchange.sendResponseHeaders(200, payload.size.toLong())
                exchange.responseBody.use { it.write(payload) }
            }
        }
        val result = ResumableDownload.download(
            part = part,
            spec = spec(server, payload),
            onProgress = { _, _ -> },
            open = opener(),
        )
        assertTrue("the 416 must fall back to a whole fetch", result.isSuccess)
        assertArrayEquals(payload, part.readBytes())
    }

    @Test
    fun `a checksum that does not match is a failure and the part is gone`() = runBlocking {
        val payload = payload()
        val part = File(temporary, "pack.part")
        val server = served { exchange, start -> respond(exchange, payload, start) }
        val result = ResumableDownload.download(
            part = part,
            spec = spec(server, payload).copy(sha256 = "0".repeat(64)),
            onProgress = { _, _ -> },
            open = opener(),
        )
        assertFalse("a package that is not its catalog's file must fail", result.isSuccess)
        assertFalse("the wrong part must not be left for a resume", part.exists())
    }

    private fun payload(): ByteArray = ByteArray(16 * 1024) { index -> (index * 31).toByte() }

    private fun spec(server: HttpServer, payload: ByteArray) = ResumableDownload.Spec(
        url = "http://127.0.0.1:${server.address.port}/pack",
        bytes = payload.size.toLong(),
        sha256 = sha256(payload),
        maxMillis = 30_000,
    )

    private fun opener(): (String, (HttpURLConnection) -> Unit) -> HttpURLConnection =
        { url, configure ->
            (URL(url).openConnection() as HttpURLConnection).also { connection ->
                connection.instanceFollowRedirects = false
                configure(connection)
            }
        }

    private fun served(
        handler: (HttpExchange, Int) -> Unit,
    ): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/pack") { exchange ->
            val range = exchange.requestHeaders.getFirst("Range")
            val start = range
                ?.removePrefix("bytes=")
                ?.removeSuffix("-")
                ?.toIntOrNull()
                ?: 0
            handler(exchange, start)
            exchange.close()
        }
        server.start()
        servers += server
        return server
    }

    private fun respond(exchange: HttpExchange, payload: ByteArray, start: Int) {
        val body = payload.copyOfRange(start, payload.size)
        exchange.responseHeaders.add("Content-Type", "application/octet-stream")
        if (start > 0) {
            exchange.responseHeaders.add(
                "Content-Range",
                "bytes $start-${payload.lastIndex}/${payload.size}",
            )
            exchange.sendResponseHeaders(206, body.size.toLong())
        } else {
            exchange.sendResponseHeaders(200, body.size.toLong())
        }
        exchange.responseBody.use { it.write(body) }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
