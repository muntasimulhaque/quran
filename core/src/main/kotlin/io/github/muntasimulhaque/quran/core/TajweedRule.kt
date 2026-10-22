package io.github.muntasimulhaque.quran.core

/**
 * The eighteen rules the QUL QPC Hafs tajweed annotation carries, in the
 * order their code characters run in the content database. The code is one
 * printable character per Quran character, `0` for no rule, `1` through `9`
 * and `a` through `i` for the rules. The build writes it and the reading reads
 * it, so the wire form lives here and nowhere else.
 */
enum class TajweedRule(val wire: String, val code: Char) {
    HAM_WASL("ham_wasl", '1'),
    LAAM_SHAMSIYAH("laam_shamsiyah", '2'),
    SLNT("slnt", '3'),
    GHUNNAH("ghunnah", '4'),
    QALAQAH("qalaqah", '5'),
    IKHAFA("ikhafa", '6'),
    IKHAFA_SHAFAWI("ikhafa_shafawi", '7'),
    IQLAB("iqlab", '8'),
    IDGHAM_GHUNNAH("idgham_ghunnah", '9'),
    IDGHAM_WO_GHUNNAH("idgham_wo_ghunnah", 'a'),
    IDGHAM_SHAFAWI("idgham_shafawi", 'b'),
    IDGHAM_MUTAJANISAYN("idgham_mutajanisayn", 'c'),
    IDGHAM_MUTAQARIBAYN("idgham_mutaqaribayn", 'd'),
    MADDA_NORMAL("madda_normal", 'e'),
    MADDA_PERMISSIBLE("madda_permissible", 'f'),
    MADDA_OBLIGATORY_MONFASEL("madda_obligatory_monfasel", 'g'),
    MADDA_OBLIGATORY_MOTTASEL("madda_obligatory_mottasel", 'h'),
    MADDA_NECESSARY("madda_necessary", 'i');

    companion object {
        fun fromWire(wire: String): TajweedRule? = entries.firstOrNull { it.wire == wire }

        fun fromCode(code: Char): TajweedRule? = entries.firstOrNull { it.code == code }
    }
}

/** One run of one rule over a range of the ayah's own text. */
data class TajweedSpan(val start: Int, val end: Int, val rule: TajweedRule)

/**
 * The two shapes the app needs from a tajweed line: the tagged source form
 * the content build reads, and the compact code form the reading draws.
 *
 * No Quran text is created here. [parse] reads the QUL annotation that already
 * sits on the source text and returns that same text with one code per
 * character; an unknown class is an error, not a rule silently dropped.
 */
object TajweedCodec {

    data class Parsed(val text: String, val code: String)

    private val classAttribute = Regex("class\\s*=\\s*['\"]?([A-Za-z_]+)")
    private val entity = Regex("&(?:[a-zA-Z]+|#[0-9]+|#x[0-9A-Fa-f]+);")
    private const val CLOSE = "</rule>"
    private const val NONE = '0'

    fun parse(tagged: String): Parsed {
        val source = entity.replace(tagged, "")
        val text = StringBuilder(source.length)
        val code = StringBuilder(source.length)
        val stack = ArrayDeque<TajweedRule>()
        var index = 0
        while (index < source.length) {
            if (source.startsWith("<rule", index)) {
                val close = source.indexOf('>', index)
                if (close < 0) error("a tajweed tag at $index is not closed")
                val attribute = classAttribute.find(source.substring(index, close + 1))
                    ?: error("a tajweed tag at $index has no class")
                val wire = attribute.groupValues[1]
                val rule = TajweedRule.fromWire(wire)
                    ?: error("unknown tajweed rule: $wire")
                stack.addLast(rule)
                index = close + 1
                continue
            }
            if (source.startsWith(CLOSE, index)) {
                if (stack.isEmpty()) error("a closing tajweed tag has no opening tag")
                stack.removeLast()
                index += CLOSE.length
                continue
            }
            val cp = source.codePointAt(index)
            val count = Character.charCount(cp)
            for (offset in 0 until count) {
                text.append(source[index + offset])
                code.append(stack.lastOrNull()?.code ?: NONE)
            }
            index += count
        }
        return Parsed(text.toString(), code.toString())
    }

    /**
     * The runs of one code line, in text order. Adjacent characters with the
     * same rule become one span; `0` is not a run and is never returned. A
     * code line whose length does not match a text is rejected by the caller
     * before this runs, so a span can never point outside the line.
     */
    fun spans(code: String): List<TajweedSpan> {
        val spans = ArrayList<TajweedSpan>(64)
        var index = 0
        while (index < code.length) {
            val char = code[index]
            if (char == NONE) {
                index++
                continue
            }
            var end = index
            while (end + 1 < code.length && code[end + 1] == char) end++
            val rule = TajweedRule.fromCode(char)
            if (rule != null) spans += TajweedSpan(index, end + 1, rule)
            index = end + 1
        }
        return spans
    }

    /** True when [code] can be drawn: every character is a known code. */
    fun isWellFormed(code: String): Boolean = code.all { it == NONE || TajweedRule.fromCode(it) != null }
}
