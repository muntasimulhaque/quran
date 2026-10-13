package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.Wash
import io.github.muntasimulhaque.quran.ui.theme.Radii

/**
 * The app's one door to content the reader does not have yet: the sentence
 * that says what is missing, and the one word that adds it.
 *
 * The reading and the ayah card both draw this door, and they used to draw it
 * by hand, twice: two copies that match today drift apart on the next edit,
 * and a reader meets the same door wearing two faces (owner decision, this
 * session). Each surface passes its own sentence and its own action word, so
 * the component owns the shape and the module owns the words.
 */
@Composable
fun AddDoor(
    text: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val press = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            // The door's click is the whole 48 dp, and the shape the eye
            // reads is drawn inside it, so the accessibility node and the
            // touch target are the same box (owner decision, this session).
            .clickable(
                interactionSource = press,
                indication = null,
                onClick = onClick,
            )
            .minimumInteractiveComponentSize()
            .clip(Radii.Card)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = Wash.Door))
            // The ripple draws after the clip, on the card the reader sees.
            // An indication on the click node would flash the whole slab as
            // a rectangle over the shape (the same rule TextButton wears).
            .indication(press, ripple(color = MaterialTheme.colorScheme.primary))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = action,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
