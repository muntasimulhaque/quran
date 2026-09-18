package io.github.muntasimulhaque.quran.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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
                if (id.isEmpty()) continue
                out[id] = ContentPack(
                    id = id,
                    type = PackType.of(pack.optString("type")),
                    name = pack.optString("name"),
                    language = pack.optString("language"),
                    credit = pack.optString("credit"),
                    license = pack.optString("license"),
                    version = pack.optString("version"),
                    shipped = pack.optBoolean("shipped", false),
                    ayahs = pack.optInt("ayahs", 6236),
                    bytes = pack.optLong("bytes", 0),
                    sha256 = pack.optString("sha256"),
                    url = pack.optString("url").takeIf { it.isNotEmpty() },
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
        val name = "core-${expectation.sha256.take(8)}.db"
        val directory = File(context.filesDir, "content").apply { mkdirs() }
        val target = File(directory, name)
        if (target.exists() && target.length() == expectation.bytes) {
            return target
        }
        val temporary = File(directory, "$name.part")
        context.assets.open("content/core.db").use { input ->
            temporary.outputStream().buffered().use { output -> input.copyTo(output) }
        }
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
        directory.listFiles { file -> file.name != target.name }?.forEach { it.delete() }
        return target
    }

    fun fileFor(id: String): File = File(packDirectory(id), "$id.db")

    fun installed(): Set<String> = packRoot().listFiles()
        ?.filter { it.isDirectory && File(it, "${it.name}.db").exists() }
        ?.map { it.name }
        ?.toSet()
        ?: emptySet()

    /**
     * Installs a pack from a file the caller has already verified, or from
     * the app's own assets when the build carried it (development builds and
     * tests work with no network at all).
     */
    fun install(id: String, verified: File? = null): Boolean {
        val target = fileFor(id)
        if (target.exists()) return true
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "$id.db.part")
        if (verified != null) {
            verified.copyTo(temporary, overwrite = true)
        } else {
            val asset = runCatching { context.assets.open("packs/$id.db") }.getOrNull() ?: return false
            asset.use { input -> temporary.outputStream().buffered().use { output -> input.copyTo(output) } }
        }
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
        return true
    }

    fun remove(id: String): Boolean {
        val directory = packDirectory(id)
        val removed = directory.deleteRecursively()
        return removed
    }

    private fun packRoot() = File(context.filesDir, "packs").apply { mkdirs() }

    private fun packDirectory(id: String) = File(packRoot(), id)
}
