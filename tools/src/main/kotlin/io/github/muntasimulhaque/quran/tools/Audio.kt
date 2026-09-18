package io.github.muntasimulhaque.quran.tools

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

/**
 * Downloads the development audio sample: a spread of ayahs for every
 * recitation, enough to exercise the player, the notification, and the word
 * segments without pulling gigabytes. The result is gitignored, and only the
 * debug build bundles it.
 */
class Audio(private val root: File) {

    private val sampleAyahs = listOf(
        "1:1", "1:2", "1:3", "1:4", "1:5", "1:6", "1:7",
        "2:1", "2:2", "2:255",
        "18:1", "36:1", "55:1", "78:1",
        "112:1", "112:2", "112:3", "112:4",
        "114:1", "114:2", "114:3", "114:4", "114:5", "114:6",
    )

    fun sample(): Int {
        val database = File(root, "content/quran.db")
        if (!database.exists()) {
            println("audio: content/quran.db is missing; run build first")
            return 1
        }
        val target = File(root, "content/work/audio-dev")
        val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()
        val semaphore = Semaphore(6)
        val downloaded = AtomicInteger()
        val skipped = AtomicInteger()
        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())
        val jobs = mutableListOf<CompletableFuture<*>>()

        openSqlite(database).use { connection ->
            connection.each(
                "SELECT ra.audio_path FROM recitation_ayah ra " +
                    "JOIN ayah a ON a.number = ra.ayah_number " +
                    "WHERE a.verse_key IN (${sampleAyahs.joinToString(",") { "'$it'" }}) " +
                    "ORDER BY ra.recitation, a.number",
            ) { rs ->
                val audioPath = rs.getString(1) ?: return@each
                val file = File(target, audioPath)
                if (file.length() > 0) {
                    skipped.incrementAndGet()
                    return@each
                }
                semaphore.acquire()
                jobs += client.sendAsync(
                    HttpRequest.newBuilder(URI.create("https://audio-cdn.tarteel.ai/quran/$audioPath")).build(),
                    HttpResponse.BodyHandlers.ofByteArray(),
                ).thenAccept { response ->
                    if (response.statusCode() == 200 && response.body().isNotEmpty()) {
                        file.parentFile?.mkdirs()
                        Files.write(file.toPath(), response.body())
                        downloaded.incrementAndGet()
                    } else {
                        failures += "$audioPath (HTTP ${response.statusCode()})"
                    }
                }.whenComplete { _, _ -> semaphore.release() }
            }
        }
        CompletableFuture.allOf(*jobs.toTypedArray()).join()

        val bytes = target.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        println("audio: sample at ${target.relativeTo(root)}")
        println("audio: downloaded ${downloaded.get()}, already present ${skipped.get()}, ${bytes / 1024} KB total")
        if (failures.isEmpty()) return 0
        println("AUDIO FAILURES (${failures.size})")
        failures.take(10).forEach { println("  - $it") }
        return 1
    }
}
