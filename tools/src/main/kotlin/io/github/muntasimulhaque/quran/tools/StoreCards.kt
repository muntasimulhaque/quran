package io.github.muntasimulhaque.quran.tools

import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import javax.imageio.ImageIO

/**
 * Composes the store cards from the committed captures.
 *
 * The frames the screenshot test writes are the card's only photograph, so
 * this command refuses a frame that is absent or is not the size its form
 * asks for, rather than quietly scaling one the test did not take at the
 * store's canvas. It writes one card per frame, beside the frames under
 * `play-store/screenshots/`, and `--check` names every card the committed set
 * does not already hold, so a commit can prove the store and the source agree
 * (owner decision, 22.4).
 */
class StoreCards(private val root: File) {

    fun run(checkOnly: Boolean): Int {
        val framesRoot = File(root, "play-store/frames")
        val cardsRoot = File(root, "play-store/screenshots")
        val type = StoreCardPaint.typefaces(root)
        var composed = 0
        var stale = 0
        for (layout in StoreCardSheet.layouts) {
            val form = layout.form
            for (card in StoreCardSheet.cards) {
                val frameName = "play-store/frames/${form.folder}/${card.frame}.png"
                val frame = File(root, frameName)
                if (!frame.isFile) {
                    println("storecards: play-store/frames/${form.folder} is missing ${card.frame}.png")
                    return 1
                }
                val capture = ImageIO.read(frame)
                if (capture == null) {
                    println("storecards: $frameName is not an image this reader can read")
                    return 1
                }
                if (capture.width != form.width || capture.height != form.height) {
                    println(
                        "storecards: $frameName is ${capture.width}x${capture.height}, " +
                            "expected ${form.width}x${form.height}",
                    )
                    return 1
                }
                val image = StoreCardPaint.paint(
                    capture = capture,
                    card = card,
                    face = StoreCardSheet.faceOf(card),
                    layout = layout,
                    type = type,
                )
                val cardName = "play-store/screenshots/${form.folder}/${card.frame}.png"
                val target = File(root, cardName)
                if (checkOnly) {
                    val current = if (target.isFile) target.readBytes() else null
                    if (current == null) {
                        println("storecards: $cardName is missing")
                        stale++
                    } else if (digest(current) != digest(encode(image))) {
                        println("storecards: $cardName is not current")
                        stale++
                    }
                } else {
                    target.parentFile?.mkdirs()
                    target.writeBytes(encode(image))
                    composed++
                }
            }
        }
        if (checkOnly) {
            if (stale > 0) return 1
            println("storecards: the committed cards are current")
            return 0
        }
        println("storecards: composed $composed cards")
        return 0
    }

    /** The bytes [run] writes and `--check` compares, from one encoder. */
    private fun encode(image: java.awt.image.BufferedImage): ByteArray {
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        return out.toByteArray()
    }

    private fun digest(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
