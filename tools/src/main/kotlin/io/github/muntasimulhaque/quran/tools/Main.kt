package io.github.muntasimulhaque.quran.tools

import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val root = File(System.getProperty("user.dir"))
    val command = args.firstOrNull() ?: "help"
    val exit = when (command) {
        "verify" -> Verify(root).run()
        "audit" -> Audit(root).run()
        "search" -> SearchCheck(root).run()
        "audio" -> Audio(root).run(args.drop(1))
        "build" -> Build(root).run()
        "fonts" -> Fonts(root).run()
        "packs" -> when (args.getOrNull(1)) {
            "publish" -> Packs(root).publish()
            else -> Packs(root).run()
        }
        "fetch" -> Fetch(root).run()
        "checkdb" -> CheckDb(root).run()
        "help", "--help" -> {
            println("usage: tools <verify|audit|search|audio|build|fonts|fetch|checkdb|packs>")
        println("       tools packs [build|publish]")
            0
        }
        else -> {
            println("unknown command: $command")
            2
        }
    }
    exitProcess(exit)
}

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(1 shl 16)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

/** True when the file starts with the ZIP local file header. */
fun isZip(file: File): Boolean {
    if (file.length() < 4) return false
    val head = ByteArray(4)
    file.inputStream().use { it.read(head) }
    return head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte() &&
        head[2] == 3.toByte() && head[3] == 4.toByte()
}

/**
 * Extracts a ZIP into [destination], recreating it. Returns the destination
 * directory. Files that are not ZIPs are copied as they are.
 */
fun extract(file: File, destination: File): File {
    destination.deleteRecursively()
    destination.mkdirs()
    val root = destination.canonicalFile
    if (!isZip(file)) {
        val target = SafeFiles.childUnder(root, file.name)
            ?: throw IllegalStateException("the file name is not safe: ${file.name}")
        file.copyTo(target, overwrite = true)
        return destination
    }
    var entries = 0
    var totalBytes = 0L
    ZipInputStream(file.inputStream().buffered()).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            entries++
            if (entries > MAX_ENTRIES) throw IllegalStateException("the archive holds too many files")
            val name = entry.name.trimEnd('/')
            if (name.isEmpty()) {
                zip.closeEntry()
                continue
            }
            val target = SafeFiles.resolveUnder(root, name)
                ?: throw IllegalStateException("the archive names an unsafe path: ${entry.name}")
            if (entry.isDirectory) {
                target.mkdirs()
            } else {
                target.parentFile?.mkdirs()
                target.outputStream().buffered().use { out ->
                    val buffer = ByteArray(1 shl 16)
                    var fileBytes = 0L
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        fileBytes += count
                        totalBytes += count
                        if (fileBytes > MAX_FILE_BYTES || totalBytes > MAX_TOTAL_BYTES) {
                            throw IllegalStateException("the archive is larger than its kind allows")
                        }
                        out.write(buffer, 0, count)
                    }
                }
            }
            zip.closeEntry()
        }
    }
    return destination
}

private const val MAX_ENTRIES = 10_000
private const val MAX_FILE_BYTES = 256L * 1024 * 1024
private const val MAX_TOTAL_BYTES = 512L * 1024 * 1024

fun File.firstWithExtension(vararg extensions: String): File? =
    walkTopDown().firstOrNull { f -> extensions.any { f.name.endsWith(it, ignoreCase = true) } }
