package com.walkingtours.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walkingtours.app.R

/**
 * Footnotes' palette: a risograph travel zine printed on cream paper.
 *
 * Warm paper, near-black ink, and five loud inks that behave like spot colours. Cards are drawn
 * with a 2 dp ink outline and a hard offset shadow rather than a soft elevation: nothing in this
 * app should look like it was extruded by a design system. The loud colours are used for
 * wayfinding and city identity, never for long stretches of text.
 */
object FootnotesColors {
    // Paper and ink
    val Paper = Color(0xFFF6EFE1)
    val PaperRaised = Color(0xFFFFFAF0)
    val PaperDeep = Color(0xFFEDE3D1)
    val Ink = Color(0xFF17130F)
    val InkSoft = Color(0xFF4D4438)
    val Line = Color(0xFFD8CBB5)

    // Riso inks
    val Tomato = Color(0xFFE4572E)
    val TomatoDeep = Color(0xFFB93B1B)
    val Mustard = Color(0xFFF2B134)
    val MustardDeep = Color(0xFFC9890B)
    val Teal = Color(0xFF1F8A7A)
    val TealDeep = Color(0xFF12685B)
    val Cobalt = Color(0xFF2D5EA8)
    val CobaltDeep = Color(0xFF1C3F77)
    val Plum = Color(0xFF7A4E8C)
    val Rose = Color(0xFFD96C8A)

    // Dark-room versions
    val NightPaper = Color(0xFF15120F)
    val NightRaised = Color(0xFF1F1A15)
    val NightDeep = Color(0xFF2B241C)
    val NightInk = Color(0xFFF5EAD7)
    val NightInkSoft = Color(0xFFCDBEA8)
    val NightLine = Color(0xFF41382D)
}

/**
 * Extra colours Material's scheme has no slot for. City identity and the zine's decorative inks
 * live here, resolved for the current light/dark mode.
 */
data class FootnotesPalette(
    val paper: Color,
    val paperRaised: Color,
    val paperDeep: Color,
    val ink: Color,
    val inkSoft: Color,
    val line: Color,
    val tomato: Color,
    val mustard: Color,
    val teal: Color,
    val cobalt: Color,
    val plum: Color,
    val rose: Color,
    val isDark: Boolean,
)

val LocalFootnotesPalette = staticCompositionLocalOf {
    FootnotesPalette(
        paper = FootnotesColors.Paper,
        paperRaised = FootnotesColors.PaperRaised,
        paperDeep = FootnotesColors.PaperDeep,
        ink = FootnotesColors.Ink,
        inkSoft = FootnotesColors.InkSoft,
        line = FootnotesColors.Line,
        tomato = FootnotesColors.Tomato,
        mustard = FootnotesColors.Mustard,
        teal = FootnotesColors.Teal,
        cobalt = FootnotesColors.Cobalt,
        plum = FootnotesColors.Plum,
        rose = FootnotesColors.Rose,
        isDark = false,
    )
}

private val LightPalette = FootnotesPalette(
    paper = FootnotesColors.Paper,
    paperRaised = FootnotesColors.PaperRaised,
    paperDeep = FootnotesColors.PaperDeep,
    ink = FootnotesColors.Ink,
    inkSoft = FootnotesColors.InkSoft,
    line = FootnotesColors.Line,
    tomato = FootnotesColors.Tomato,
    mustard = FootnotesColors.Mustard,
    teal = FootnotesColors.Teal,
    cobalt = FootnotesColors.Cobalt,
    plum = FootnotesColors.Plum,
    rose = FootnotesColors.Rose,
    isDark = false,
)

private val DarkPalette = FootnotesPalette(
    paper = FootnotesColors.NightPaper,
    paperRaised = FootnotesColors.NightRaised,
    paperDeep = FootnotesColors.NightDeep,
    ink = FootnotesColors.NightInk,
    inkSoft = FootnotesColors.NightInkSoft,
    line = FootnotesColors.NightLine,
    tomato = Color(0xFFFF6B4A),
    mustard = Color(0xFFF7C65D),
    teal = Color(0xFF4CC4B3),
    cobalt = Color(0xFF7FA6E8),
    plum = Color(0xFFC59AD6),
    rose = Color(0xFFF08CA7),
    isDark = true,
)

private val LightColors = lightColorScheme(
    primary = FootnotesColors.Tomato,
    onPrimary = FootnotesColors.PaperRaised,
    primaryContainer = Color(0xFFF8D8CC),
    onPrimaryContainer = Color(0xFF4A1508),
    secondary = FootnotesColors.Teal,
    onSecondary = FootnotesColors.PaperRaised,
    secondaryContainer = Color(0xFFCDE9E2),
    onSecondaryContainer = Color(0xFF06312B),
    tertiary = FootnotesColors.Mustard,
    onTertiary = FootnotesColors.Ink,
    tertiaryContainer = Color(0xFFFAE5B3),
    onTertiaryContainer = Color(0xFF3E2B04),
    background = FootnotesColors.Paper,
    onBackground = FootnotesColors.Ink,
    surface = FootnotesColors.Paper,
    onSurface = FootnotesColors.Ink,
    surfaceVariant = FootnotesColors.PaperDeep,
    onSurfaceVariant = FootnotesColors.InkSoft,
    surfaceContainerLowest = FootnotesColors.PaperRaised,
    surfaceContainerLow = Color(0xFFFBF5E9),
    surfaceContainer = Color(0xFFF2EADA),
    surfaceContainerHigh = Color(0xFFEDE3D1),
    surfaceContainerHighest = Color(0xFFE5D8C2),
    outline = Color(0xFF8D7F6B),
    outlineVariant = FootnotesColors.Line,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    inverseSurface = FootnotesColors.Ink,
    inverseOnSurface = FootnotesColors.Paper,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6B4A),
    onPrimary = Color(0xFF2A0C04),
    primaryContainer = Color(0xFF7A2E1A),
    onPrimaryContainer = Color(0xFFFFDAD0),
    secondary = Color(0xFF4CC4B3),
    onSecondary = Color(0xFF032B25),
    secondaryContainer = Color(0xFF15564D),
    onSecondaryContainer = Color(0xFFCDE9E2),
    tertiary = Color(0xFFF7C65D),
    onTertiary = Color(0xFF2C2105),
    tertiaryContainer = Color(0xFF6A4E0A),
    onTertiaryContainer = Color(0xFFFAE5B3),
    background = FootnotesColors.NightPaper,
    onBackground = FootnotesColors.NightInk,
    surface = FootnotesColors.NightPaper,
    onSurface = FootnotesColors.NightInk,
    surfaceVariant = FootnotesColors.NightDeep,
    onSurfaceVariant = FootnotesColors.NightInkSoft,
    surfaceContainerLowest = Color(0xFF0F0D0B),
    surfaceContainerLow = FootnotesColors.NightRaised,
    surfaceContainer = Color(0xFF241E18),
    surfaceContainerHigh = FootnotesColors.NightDeep,
    surfaceContainerHighest = Color(0xFF372F26),
    outline = Color(0xFF9C8C76),
    outlineVariant = FootnotesColors.NightLine,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = FootnotesColors.NightInk,
    inverseOnSurface = FootnotesColors.NightPaper,
)

private val Grotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

private val Serif = FontFamily(
    Font(R.font.fraunces_bold, FontWeight.Bold),
    Font(R.font.fraunces_black, FontWeight.Black),
    Font(R.font.fraunces_bold_italic, FontWeight.Bold, FontStyle.Italic),
)

private val Mono = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_bold, FontWeight.Bold),
)

/**
 * Type does most of the shouting here. Fraunces is soft, a little wonky and completely over the
 * top at display sizes; Space Grotesk keeps the reading comfortable; Space Mono carries every
 * piece of metadata so the app reads like a field guide with actual field notes in the margins.
 */
private val FootnotesTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Black,
        fontSize = 58.sp,
        lineHeight = 54.sp,
        letterSpacing = (-1.6).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Black,
        fontSize = 46.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.2).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Black,
        fontSize = 38.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 29.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

private val FootnotesShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

val FootnotesTitleFont = Serif
val FootnotesBodyFont = Grotesk
val FootnotesMonoFont = Mono

@Composable
fun FootnotesTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(
        LocalFootnotesPalette provides if (darkTheme) DarkPalette else LightPalette,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = FootnotesTypography,
            shapes = FootnotesShapes,
            content = content,
        )
    }
}

/** Kept as an alias so nothing that still imports the old name breaks while the app is rebuilt. */
@Composable
fun WalkingToursTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit,
) = FootnotesTheme(themeMode = themeMode, content = content)
