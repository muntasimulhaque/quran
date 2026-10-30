package io.github.muntasimulhaque.quran.ui.reading

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import io.github.muntasimulhaque.quran.core.RichText
import io.github.muntasimulhaque.quran.core.TextBlock
import io.github.muntasimulhaque.quran.core.ayahSpan
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.feature.reading.R
import io.github.muntasimulhaque.quran.ui.kit.AddDoor
import io.github.muntasimulhaque.quran.ui.kit.AppSheet
import io.github.muntasimulhaque.quran.ui.kit.SheetTitle
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.contentPackName
import io.github.muntasimulhaque.quran.ui.kit.languageName
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.rememberExpandedSheetState
import io.github.muntasimulhaque.quran.ui.kit.sheetRow
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.rich.RichBlocks
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.TypeLadder
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.title
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class TafsirView(
    /** The passage's surah, named the way the reading names its own. */
    val surah: String,
    /** The ayahs the passage covers, for example 41:46-48. */
    val reference: String,
    /**
     * The passage already parsed into its blocks. The parse is the one heavy
     * step in showing a tafsir, and it is pure work: doing it here, on the
     * read's own worker, leaves the frame that unfolds the panel with only
     * the layout to do (owner report, this session).
     */
    val blocks: List<TextBlock>,
)

/** Every enabled tafsir read once, and whether the read has finished. */
private data class TafsirLoad(
    val views: Map<String, TafsirView> = emptyMap(),
    val done: Boolean = false,
)

/**
 * The ayah card: one screen per ayah that holds everything deeper without
 * asking the reader to learn an interface.
 *
 * The ayah and its translation and meanings are already on the page, so the
 * card does not repeat them: it opens only the tafsirs. The note is not here
 * at all: it belongs to the ayah's own menu, at the number at its head, one
 * tap away rather than a scroll to the foot of a card (owner decision, this
 * session).
 *
 * The sheet is named by the kind of text it holds, and each pack is named
 * once. The reference belongs to the passage and stands with it, because two
 * packs can cover different ayahs of one surah, so a reference at the head of
 * the sheet would be a promise only one pack keeps (owner decision, this
 * session). With one pack there is no choice, so the name is a byline and the
 * passage stands under it; with more, the name is a door and the passage
 * unfolds under it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahCard(
    content: ContentDatabase,
    ayah: Ayah,
    surahName: String,
    tafsirPacks: List<ContentPack>,
    settings: AppSettings,
    /** The pack a search hit opened the card for, when there is one. */
    initialTafsirPack: String? = null,
    onAddContent: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberExpandedSheetState()
    // The pack a search hit named, when there is one: the hit already chose
    // the commentary, so its door stands open and its passage under it. With
    // no hit, no door opens of itself, and one pack has no door at all.
    val wantedPack = initialTafsirPack?.let { id -> tafsirPacks.firstOrNull { it.id == id } }
    // A search hit opens the pack that carried the match first, so its
    // passage stands at the head of the card instead of under another pack's
    // door. The reader's own language follows it: a pack added by hand in
    // another language is the exception and never the first door, so with two
    // Ibn Kathirs the one in the language the reader is reading leads (owner
    // decision, this session). The fallback is the shell's own, the same
    // answer it gives before the first choice.
    val readerLanguage = settings.uiLanguage ?: "en"
    val orderedPacks = remember(tafsirPacks, initialTafsirPack, readerLanguage) {
        tafsirPacks.sortedWith(
            compareBy<ContentPack>(
                { it.id != initialTafsirPack },
                { it.language != readerLanguage },
                { it.language },
            ),
        )
    }
    // The pack whose door is unfolded. One pack has no door at all, so the
    // state is read only where there is a choice to make.
    var door by remember(ayah.number, initialTafsirPack) {
        mutableStateOf(wantedPack)
    }

    // Every enabled tafsir is read as the card opens, not when its door is
    // tapped: the read is one indexed query of a millisecond or two, and
    // paying for it here puts the text in hand before the sheet has finished
    // moving, so the panel opens on prose instead of on "Opening…" (owner
    // report, this session: the wait was the tap-to-read, not the query).
    val load by produceState<TafsirLoad>(
        initialValue = TafsirLoad(),
        content,
        ayah.number,
        tafsirPacks,
    ) {
        val views = withContext(Dispatchers.IO) {
            tafsirPacks.mapNotNull { pack ->
                try {
                    val passage = content.tafsir(ayah.number, pack.id)
                        ?: return@mapNotNull null
                    val name = content.surah(passage.surah)?.nameSimple
                        ?: surahName
                    val reference = ayahSpan(
                        passage.surah,
                        passage.fromAyah,
                        passage.toAyah,
                    )
                    pack.id to TafsirView(
                        surah = name,
                        reference = reference,
                        blocks = RichText.parseHtml(passage.text),
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    // A damaged tafsir pack is a missing passage, not a crash:
                    // the passage says it is not here, and the repair path can
                    // replace the pack.
                    Log.w("AyahCard", "the tafsir pack ${pack.id} could not be read", error)
                    null
                }
            }.toMap()
        }
        value = TafsirLoad(views = views, done = true)
    }
    val passages = load.views

    // The card is the app's one page, which is the glass, and the tafsir
    // inside it stands on the app's one margin like every other line of prose:
    // a ten inch screen reads a longer line and the reader who changes the
    // tafsir size gets more lines, never a narrower column (owner decision,
    // this session).
    AppSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .imePadding()
                // The gate rides with the scroll: a long tafsir scrolled down
                // and then scrolled back up is the reader reading, never a
                // pull that closes the card under them.
                .sheetVerticalScroll(rememberScrollState())
                .padding(bottom = Space.SheetFoot)
                // The sheet's window appears before its content has been read
                // off the database, so a capture that keeps the first settled
                // frame photographs a card missing its translation. The tag
                // says when the card is whole, and the tour waits on it: the
                // reader never sees the gap (the sheet is animating open), but
                // a still frame does (owner report).
                .testTag("ayah-card"),
        ) {
            // The card is the tafsir it was opened for: the reader tapped
            // the ayah pill's Tafsir mark, or arrived from a tafsir hit that
            // named the pack, so there is nothing here to hide behind a
            // switch. The packs are the choice, and the mark that opens
            // this card exists exactly while one of them is on the device
            // (owner decision, this session).
            // The card names itself once, in the app's own voice: a sheet
            // opened from the pill's Tafsir mark is a surface of the app,
            // and its name is not the passage's title. The Book's voice
            // starts at the passage's reference below (owner decision, this
            // session).
            SheetTitle(
                text = stringResource(R.string.card_tafsir_label),
                modifier = Modifier.padding(
                    start = Space.Gutter,
                    end = Space.Gutter,
                    bottom = Space.Block,
                ),
            )
            if (tafsirPacks.isEmpty()) {
                // The card is only the tafsir doors, so a reader with none
                // open gets the door to add one rather than a sheet with
                // nothing in it.
                AddDoor(
                    text = stringResource(R.string.card_add_tafsir),
                    action = stringResource(R.string.reading_action_add),
                    onClick = onAddContent,
                    modifier = Modifier.padding(horizontal = SheetRowGutter),
                )
            } else {
                val single = tafsirPacks.singleOrNull()
                if (single != null) {
                    // One pack is no choice: the reader tapped Tafsir on
                    // the pill, or arrived from a search hit that already
                    // named the pack, so its name and its language are one
                    // byline and its passage stands under them with no door
                    // to fold (owner decision, this session). A byline is
                    // metadata, so it wears the meta line rather than a
                    // a row's 15: the card's own evidence is the passage's
                    // title below it.
                    val packName = contentPackName(single.id, single.name)
                    Text(
                        text = stringResource(
                            R.string.card_pack_byline,
                            packName,
                            languageName(single.language),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = Space.Gutter,
                            end = Space.Gutter,
                            bottom = Space.Block,
                        ),
                    )
                    PassageArea(
                        view = passages[single.id],
                        done = load.done,
                        // The byline above already names the pack: the wait
                        // is never a second copy of the name.
                        opening = null,
                        settings = settings,
                    )
                } else {
                    orderedPacks.forEach { pack ->
                        val open = door?.id == pack.id
                        // The tafsir's own name, in the reader's language.
                        val packName = contentPackName(pack.id, pack.name)
                        DoorRow(
                            title = packName,
                            subtitle = languageName(pack.language),
                            open = open,
                            onClick = { door = if (open) null else pack },
                        )
                        if (open) {
                            PassageArea(
                                view = passages[pack.id],
                                done = load.done,
                                opening = stringResource(
                                    R.string.card_opening_pack,
                                    packName,
                                ),
                                settings = settings,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Space.Line))
        }
    }
}

@Composable
private fun DoorRow(
    title: String,
    subtitle: String,
    open: Boolean?,
    onClick: () -> Unit,
) {
    // A door that unfolds says whether its content is shown; the row that
    // only opens the settings for a missing pack is an action, not a door,
    // and it says nothing about a state it does not have.
    val state = open?.let {
        stringResource(if (it) R.string.card_door_shown else R.string.card_door_hidden)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sheetRow(onClick = onClick, role = Role.Button)
            .semantics {
                if (state != null) stateDescription = state
            }
            // The row's own room is the app's row gap, not the block gap: two
            // doors stand 24 dp apart (12 above and 12 below each), the room
            // the card keeps between two things of one kind, and the sheet's
            // name sits closer to the first door than a section break
            // (owner decision, this session).
            .padding(horizontal = Space.Gutter - SheetRowGutter, vertical = Space.Row),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The value is under its name, the way every row in every sheet
        // reads: it stood in a right-hand column, the one grammar the card
        // still kept from the hub's old shape (owner decision, the theme
        // and consistency session).
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.Tight),
                )
            }
        }
        // The arrow is the door's only sign: a row whose content unfolds
        // under it has to say so, and it has to say it loudly enough to be
        // seen without a tap. It points down while the content is hidden and
        // turns up once the content is under it.
        IconGlyph(
            icon = Icon.Chevron,
            tint = if (open == true) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .size(GlyphSize.ChevronRow)
                .rotate(if (open == true) 180f else 0f),
        )
    }
}

/**
 * The room one passage takes: its text, or the one line that says why it is
 * not here yet.
 *
 * A reader who chose a tafsir and a reader who has only one see the same
 * block. [opening] is null where the name above already said whose passage is
 * on its way, so the wait never repeats the name (owner decision, this
 * session).
 */
@Composable
private fun PassageArea(
    view: TafsirView?,
    done: Boolean,
    opening: String?,
    settings: AppSettings,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.Gutter),
        contentAlignment = Alignment.Center,
    ) {
        when {
            view != null -> TafsirPanel(view, settings)
            done -> Text(
                text = stringResource(R.string.card_tafsir_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            opening != null -> Text(
                text = opening,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The passage under its name: the ayahs it covers, then the prose.
 *
 * The reference is the passage's, read from the pack that returned it, so two
 * tafsirs of one ayah can name the same span or different ones without the
 * sheet ever promising what only one of them keeps (owner decision, this
 * session). It is the passage's title: [TypeLadder.articleTitleSp], which is
 * 18 at the default body and stays between 18 and 22 at every reader step, so
 * it stands above the document's own heads at every step without becoming a
 * page title on a phone. Its line is the script's title leading, not the
 * face's prose box, because a one-line title is not a paragraph. It is a
 * heading to a screen reader, so a reader who cannot see the page can jump to
 * the passage instead of hearing the whole card.
 */
@Composable
private fun TafsirPanel(
    view: TafsirView,
    settings: AppSettings,
) {
    val voice = LocalReadingVoice.current
    val titleSp = TypeLadder.articleTitleSp(settings.tafsirSp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tafsir-panel"),
    ) {
        Text(
            text = stringResource(
                R.string.card_tafsir_reference,
                view.surah,
                localizedDigits(view.reference),
            ),
            style = voice.title(titleSp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .semantics { heading() }
                .padding(bottom = Space.Block),
        )
        RichBlocks(
            blocks = view.blocks,
            sizeSp = settings.tafsirSp,
            lineSp = settings.tafsirLineSp,
            arabicSp = settings.arabicInlineSp,
        )
    }
}
