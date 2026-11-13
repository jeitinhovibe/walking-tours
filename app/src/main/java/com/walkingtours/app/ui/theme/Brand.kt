package com.walkingtours.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The app's name and voice. One place, so a future owner can rename it without hunting through
 * screens — and so the microcopy sounds like one person wrote it.
 */
object FootnotesBrand {
    const val NAME = "Footnotes"
    const val TAGLINE = "Audio walks with opinions."
    const val SUBTITLE = "Put something interesting in your ears and go outside."
    const val SIGN_OFF = "Walk far. Listen closely."
}

/**
 * A city's ink. Deterministic from its name, so the same city is the same colour on every screen,
 * every launch, and in both themes. The colours are loud on purpose; the ship's cat is not
 * consulted.
 */
data class CityAccent(
    val background: Color,
    val foreground: Color,
    val tint: Color,
    val inkName: String,
)

@Composable
fun footnotesAccents(): List<CityAccent> {
    val p = LocalFootnotesPalette.current
    val lightOnDark = if (p.isDark) Color(0xFF26160F) else p.paperRaised
    val darkOnLight = Color(0xFF221A0E)
    return listOf(
        CityAccent(p.tomato, lightOnDark, p.tomato.copy(alpha = 0.15f), "tomato"),
        CityAccent(p.teal, lightOnDark, p.teal.copy(alpha = 0.15f), "teal"),
        CityAccent(p.cobalt, lightOnDark, p.cobalt.copy(alpha = 0.15f), "cobalt"),
        CityAccent(p.mustard, darkOnLight, p.mustard.copy(alpha = 0.22f), "mustard"),
        CityAccent(p.plum, lightOnDark, p.plum.copy(alpha = 0.15f), "plum"),
        CityAccent(p.rose, darkOnLight, p.rose.copy(alpha = 0.18f), "rose"),
    )
}

@Composable
fun cityAccent(seed: String): CityAccent {
    val accents = footnotesAccents()
    // Stable across processes, unlike String.hashCode in some alternative universes. It is stable
    // here too, but being explicit about overflow makes the intent obvious.
    val index = ((seed.hashCode().toLong() and 0x7fffffffL) % accents.size).toInt()
    return accents[index]
}

/** The colour a tour inherits from its city, with the tour id mixed in so two walks in one city differ. */
@Composable
fun tourAccent(city: String, tourId: String): CityAccent = cityAccent("$city/$tourId")
