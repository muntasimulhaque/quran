package io.github.muntasimulhaque.quran.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * What content exists, and what is on this device.
 *
 * The catalog is a small file inside the app: every pack, its type, its
 * language, its size, its SHA-256, and the one URL it can be fetched from.
 * Nothing is fetched at launch and nothing is fetched on its own; the catalog
 * is the menu, and the reader does the ordering.
 *
 * A pack is installed by placing its database at
 * `filesDir/packs/<id>/<id>.db`. Removing it deletes that file. The core pack
 * (the Quran text and its page layout) is shipped inside the app.
 */
class PackCatalog private constructor(private val packs: Map<String, ContentPack>) {

    fun all(): List<ContentPack> = packs.values.sortedWith(
        compareBy({ if (it.shipped) 0 else 1 }, { it.type.ordinal }, { it.name.lowercase() }),
    )

    fun get(id: String): ContentPack? = packs[id]

    fun ofType(type: PackType): List<ContentPack> = all().filter { it.type == type }

    /** The same catalog, marked with what this device already has. */
    fun withInstalled(installed: Set<String>): PackCatalog =
        PackCatalog(packs.mapValues { (id, pack) -> pack.copy(installed = id in installed) })

    companion object {
        const val CORE_ID = "core"

        fun load(context: Context): PackCatalog {
            val json = runCatching {
                context.assets.open("content/catalog.json").bufferedReader().use { it.readText() }
            }.getOrNull() ?: return PackCatalog(emptyMap())
            return parse(json)
        }

        fun parse(json: String): PackCatalog {
            val out = HashMap<String, ContentPack>()
            val root = runCatching { JSONObject(json) }.getOrNull() ?: return PackCatalog(emptyMap())
            val packs = root.optJSONArray("packs") ?: JSONArray()
            for (index in 0 until packs.length()) {
                val pack = packs.optJSONObject(index) ?: continue
                val id = pack.optString("id")
                if (!SafePaths.validId(id)) continue
                val sha256 = pack.optString("sha256")
                if (!SafePaths.validHash(sha256)) continue
                val bytes = pack.optLong("bytes", 0)
                if (bytes < 0) continue
                val shipped = pack.optBoolean("shipped", false)
                if (shipped && id != CORE_ID) continue
                val type = PackType.ofOrNull(pack.optString("type")) ?: continue
                val url = pack.optString("url").takeIf { it.isNotEmpty() }
                if (url != null && !url.startsWith("https://")) continue
                out[id] = ContentPack(
                    id = id,
                    type = type,
                    name = pack.optString("name"),
                    language = pack.optString("language"),
                    credit = pack.optString("credit"),
                    license = pack.optString("license"),
                    creditBn = pack.optString("creditBn"),
                    licenseBn = pack.optString("licenseBn"),
                    version = pack.optString("version"),
                    shipped = shipped,
                    ayahs = pack.optInt("ayahs", 6236),
                    bytes = bytes,
                    sha256 = sha256,
                    url = url,
                )
            }
            return PackCatalog(out)
        }
    }
}

/**
 * The packs on this device, and the one operation that puts them there.
 * Every install is a whole file with a known hash; nothing is ever patched
 * in place, so an interrupted download cannot leave a half pack behind.
 */
class PackStore(private val context: Context) {

    /** The shipped core pack, kept in the app's own storage, named by hash. */
    fun coreFile(expectation: ContentPack): File {
        if (!SafePaths.validHash(expectation.sha256)) {
            throw IllegalStateException("the core pack has no checksum")
        }
        val name = "core-${expectation.sha256.take(8)}.db"
        val directory = File(context.filesDir, "content").apply { mkdirs() }
        val target = File(directory, name)
        if (target.isFile && target.length() == expectation.bytes &&
            sha256(target).equals(expectation.sha256, ignoreCase = true)
        ) {
            return target
        }
        val temporary = File(directory, "$name.part")
        context.assets.open("content/core.db").use { input ->
            temporary.outputStream().buffered().use { output -> input.copyTo(output) }
        }
        if (temporary.length() != expectation.bytes ||
            !sha256(temporary).equals(expectation.sha256, ignoreCase = true)
        ) {
            temporary.delete()
            throw IllegalStateException("the shipped Quran text does not match the catalog")
        }
        if (!temporary.renameTo(target)) {
            temporary.delete()
            throw IllegalStateException("the shipped Quran text could not be placed")
        }
        directory.listFiles { file -> file.name != target.name }?.forEach { it.delete() }
        return target
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Forgets the copied core pack, so the next open copies it from the app again. */
    fun discardCore() {
        File(context.filesDir, "content").deleteRecursively()
    }

    fun fileFor(id: String): File {
        val directory = packDirectory(id)
        return SafePaths.childUnder(directory, "$id.db") ?: File(directory, "__invalid__")
    }

    fun installed(): Set<String> = packRoot().listFiles()
        ?.filter { it.isDirectory && File(it, "${it.name}.db").exists() }
        ?.map { it.name }
        ?.toSet()
        ?: emptySet()

    /**
     * Installs a pack from a file the caller has already verified, or from
     * the app's own assets when the build carried it (development builds and
     * tests work with no network at all). The catalog is the expectation:
     * a file that does not match its length and SHA-256 is not installed,
     * whichever door it arrived through (owner decision, security audit).
     */
    fun install(id: String, verified: File? = null): Boolean {
        val pack = PackCatalog.load(context).get(id) ?: return false
        return install(pack, verified)
    }

    fun install(pack: ContentPack, verified: File? = null): Boolean {
        if (!SafePaths.validId(pack.id) || !SafePaths.validHash(pack.sha256)) return false
        val target = fileFor(pack.id)
        if (target.isFile && (pack.bytes <= 0 || target.length() == pack.bytes) &&
            sha256(target).equals(pack.sha256, ignoreCase = true)
        ) {
            return true
        }
        if (target.exists() && !target.delete()) return false
        val directory = target.parentFile ?: return false
        directory.mkdirs()
        val temporary = SafePaths.childUnder(directory, "${pack.id}.db.part") ?: return false
        if (verified != null) {
            verified.copyTo(temporary, overwrite = true)
        } else {
            val asset = runCatching { context.assets.open("packs/${pack.id}.db") }.getOrNull() ?: return false
            asset.use { input -> temporary.outputStream().buffered().use { output -> input.copyTo(output) } }
        }
        if ((pack.bytes > 0 && temporary.length() != pack.bytes) ||
            !sha256(temporary).equals(pack.sha256, ignoreCase = true)
        ) {
            temporary.delete()
            return false
        }
        if (!temporary.renameTo(target)) {
            temporary.delete()
            return false
        }
        PackVerifier(context).remember(pack, target)
        return true
    }

    fun remove(id: String): Boolean {
        val directory = SafePaths.childUnder(packRoot(), id) ?: return false
        PackVerifier(context).forget(id)
        return directory.deleteRecursively()
    }

    private fun packRoot() = File(context.filesDir, "packs").apply { mkdirs() }

    private fun packDirectory(id: String): File =
        SafePaths.childUnder(packRoot(), id) ?: File(packRoot(), "__invalid__")
}
