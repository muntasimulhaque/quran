package io.github.muntasimulhaque.quran.data

import java.io.File

/**
 * Puts one verified surah's files into the reciter's folder, beside every
 * surah already there.
 *
 * The folder is shared: `filesDir/recitations/<reciter>/<sssaaa>.mp3` is
 * where [RecitationStore] looks for every surah of that reciter, so a commit
 * must never replace the folder itself. The old commit renamed the folder
 * over the reciter's own, which deleted every surah downloaded before it:
 * a reader who fetched Al-Baqarah after Al-Fatiha found only Al-Baqarah, and
 * the next Play on Al-Fatiha offered its download again (owner report, the
 * audit).
 *
 * The files are moved in one at a time, and a failed move removes only what
 * this commit moved. A process that dies halfway leaves a partial surah, not
 * a whole one: [RecitationStore.hasAllAudio] asks for every ayah's file, so a
 * partial set is never counted as a surah, and the next download repairs it
 * by moving the missing files in and replacing the ones already there.
 */
internal object RecitationCommit {

    /**
     * Moves every file under [staged] into `imported/folder`, replacing a
     * file of the same name, and deletes [staged] when it is done.
     *
     * [staged] holds only the package's flat ayah files; the caller has
     * already read them through the archive's own gate. A name that is not a
     * direct child of [staged] is refused rather than followed.
     */
    fun commit(imported: File, folder: String, staged: File) {
        if (!SafePaths.validFolder(folder)) {
            throw IllegalStateException("the surah folder is not a safe name")
        }
        val target = SafePaths.childUnder(imported, folder)
            ?: throw IllegalStateException("the surah folder is not a safe name")
        target.mkdirs()
        val files = staged.listFiles { file -> file.isFile }.orEmpty().sortedBy { it.name }
        if (files.isEmpty()) {
            throw IllegalStateException("the package held no ayah files")
        }
        val moved = ArrayList<File>(files.size)
        try {
            for (file in files) {
                val destination = SafePaths.childUnder(target, file.name)
                    ?: throw IllegalStateException("the package names an unsafe file")
                if (destination.exists() && !destination.delete()) {
                    throw IllegalStateException("the previous ayah file could not be replaced")
                }
                if (!file.renameTo(destination)) {
                    destination.delete()
                    throw IllegalStateException("the ayah file could not be moved into place")
                }
                moved += destination
            }
        } catch (failure: Throwable) {
            // The commit's own files go, and nothing else. A pre-existing
            // surah is not touched by this rollback; it may already have had
            // one file replaced, which leaves a partial surah the whole-surah
            // check refuses and the next download repairs.
            for (destination in moved) destination.delete()
            throw failure
        }
        staged.deleteRecursively()
    }
}
