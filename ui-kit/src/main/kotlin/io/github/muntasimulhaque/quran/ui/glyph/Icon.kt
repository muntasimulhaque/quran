package io.github.muntasimulhaque.quran.ui.glyph

/**
 * Every glyph the interface needs, in one set, drawn by the app.
 *
 * There is no icon library behind this app and no borrowed mark: each of these
 * is a drawing in [Glyphs], and the one hand is what makes the pill, the bar,
 * the sheets, and the card read as one surface. A set taken from a source would
 * cover seventeen of the eighteen and leave the one that matters most, the
 * tafsir's commentary, drawn in another hand beside them (owner decision, this
 * session).
 *
 * A mark never carries its own size. The job does, through [GlyphSize], and the
 * job is the control the mark stands in, never the screen it is drawn on.
 */
enum class Icon {
    /** The list that opens Browse. */
    Browse,

    /** The magnifier that opens Search. */
    Search,

    /** Two rails with their handles: the sliders that open the reading's display panel. */
    Display,

    /** A ring and its eight teeth: the settings hub. */
    Settings,

    /** The empty ribbon that keeps an ayah. */
    Bookmark,

    /** The ribbon that says the ayah is kept. */
    BookmarkFilled,

    /** One tick: the answer of a chip or of a choice's ring. */
    Check,

    /** The triangle that recites an ayah. */
    Play,

    /** The two bars that hold a recitation. */
    Pause,

    /** The triangle and its post: the ayah after this one. */
    Next,

    /** The triangle and its post, facing back: the ayah before. */
    Previous,

    /** The two strokes that end a thing. */
    Close,

    /** Three ends and their two lines: the ayah leaves the app. */
    Share,

    /** The three dots that say more stands behind this. */
    More,

    /** The gloss in the page's margin. */
    Tafsir,

    /**
     * The one turn the set draws, and it turns to five jobs: a door beside a
     * word, a tail that ends a row, a fold that opens, a way back, and the
     * surah after this one. It is the size of the job, never of the turn.
     */
    Chevron,

    /** The page with its turned corner: what the reader writes. */
    Note,

    /** The face that turns a reminder's time. */
    Clock,

    /** The keys that type the time instead. */
    Keyboard,
}
