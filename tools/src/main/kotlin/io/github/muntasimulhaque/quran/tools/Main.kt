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
        "help", "--help" -> {
            println("usage: tools <verify|audit>")
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
    if (!isZip(file)) {
        file.copyTo(File(destination, file.name), overwrite = true)
        return destination
    }
    ZipInputStream(file.inputStream().buffered()).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            val target = File(destination, entry.name)
            if (entry.isDirectory) {
                target.mkdirs()
            } else {
                target.parentFile?.mkdirs()
                target.outputStream().buffered().use { out -> zip.copyTo(out) }
            }
            zip.closeEntry()
        }
    }
    return destination
}

fun File.firstWithExtension(vararg extensions: String): File? =
    walkTopDown().firstOrNull { f -> extensions.any { f.name.endsWith(it, ignoreCase = true) } }
