const fs = require('fs')

// Moves the pill's reciter state up above init{}, where the collector in
// init can read it. Property initializers below init{} have not run when
// init{} does, and the 3.6 capture run died on that read.
const file = 'app/src/main/kotlin/io/github/muntasimulhaque/quran/ui/ReaderViewModel.kt'
let text = fs.readFileSync(file, 'utf8')
const crlf = (s) => s.replace(/\n/g, '\r\n')

const start = text.indexOf(crlf('    /**\n     * The reciters the pill\'s chooser offers,'))
if (start < 0) throw new Error('no start marker')
const end = text.indexOf(crlf('    /**\n     * The reader chose a reciter on the pill'), start)
if (end < 0) throw new Error('no end marker')
text = text.slice(0, start) + text.slice(end)

const block = `    /**
     * The reciters the pill's chooser offers, each with what it would still
     * need for the surah the reader is hearing. It is the same list the
     * offer carries, read once per surah and per reciter rather than per
     * tap: a menu that did its reading as it opened would show the reader
     * an empty list for a frame.
     *
     * This and the ayah below are declared here, with the rest of the state,
     * and not further down where they were written first, because the
     * collector in \`init\` reads them. \`viewModelScope\` runs on the main
     * dispatcher immediately, so that collector's first read happens while
     * the view model is still being built, and a property declared after
     * \`init\` has no backing value yet: reading one is a crash, not a
     * default. The 3.6 capture run found it on all three legs at once.
     */
    var pillReciters by mutableStateOf<List<ListenOption>>(emptyList())
        private set

    /**
     * The ayah the pill is about: the one the reader last asked to hear.
     * The player knows the ayah it is on, but not the one it was asked for
     * while a package is on its way, and that is the one a reciter chosen
     * now has to be asked about.
     */
    private var listenTarget by mutableIntStateOf(0)

    /**
     * What the pill's chooser offers, read for one surah and one reciter. A
     * surah or a reciter the app cannot name is the same answer as a surah
     * with no package published for anyone: nothing to offer, and an empty
     * menu rather than a menu of dead rows.
     */
    private suspend fun readPillReciters(scope: PillScope) {
        val surah = scope.surah ?: surahOf(listenTarget)?.number
        if (scope.recitation == null || surah == null) {
            pillReciters = emptyList()
            return
        }
        pillReciters = listenOptions(surah)
    }

    /** The one reading the chooser's numbers belong to: who, and which surah. */
    private data class PillScope(val recitation: String?, val surah: Int?)

`

const anchor = crlf('    }\r\n\r\n    init {')
const where = text.indexOf(anchor)
if (where < 0) throw new Error('no init anchor')
if (text.indexOf(anchor, where + 1) >= 0) throw new Error('the init anchor is not unique')
const cut = where + crlf('    }\r\n\r\n').length
text = text.slice(0, cut) + crlf(block) + text.slice(cut)

for (const needle of ['var pillReciters', 'private var listenTarget', 'private suspend fun readPillReciters', 'private data class PillScope']) {
  const count = text.split(needle).length - 1
  if (count !== 1) throw new Error(`${needle} appears ${count} times`)
}
if (text.indexOf('var pillReciters') > text.indexOf('    init {')) {
  throw new Error('the state is still below init')
}
if (text.includes('The reciters the pill\'s chooser offers, each with what it would still')) {
  const first = text.indexOf('The reciters the pill\'s chooser offers, each with what it would still')
  if (text.indexOf('The reciters the pill\'s chooser offers, each with what it would still', first + 1) >= 0) {
    throw new Error('the KDoc is duplicated')
  }
}

fs.writeFileSync(file, text)
console.log('moved above init')
