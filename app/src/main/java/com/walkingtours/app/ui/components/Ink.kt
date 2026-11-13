package com.walkingtours.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.walkingtours.app.ui.theme.FootnotesBodyFont
import com.walkingtours.app.ui.theme.FootnotesMonoFont
import com.walkingtours.app.ui.theme.FootnotesTitleFont
import com.walkingtours.app.ui.theme.LocalFootnotesPalette
import com.walkingtours.app.ui.theme.cityAccent

/**
 * The paper every screen is printed on: warm stock with a faint dot grid, like a Moleskine that
 * has been through a washing machine and come out better.
 */
@Composable
fun PaperBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val p = LocalFootnotesPalette.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(p.paper)
            .drawBehind {
                // One dotted rule per row, not one draw call per dot: the grid is the same, but a
                // screenful of texture costs ~80 lines instead of ~3,600 circles on every redraw.
                val step = 24.dp.toPx()
                val dot = p.ink.copy(alpha = if (p.isDark) 0.09f else 0.06f)
                val effect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    intervals = floatArrayOf(0.1f, step),
                    phase = 0f,
                )
                var y = step
                while (y < size.height) {
                    drawLine(
                        color = dot,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.7.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        pathEffect = effect,
                    )
                    y += step
                }
            },
        content = content,
    )
}

/**
 * A soft fade from the page's paper into a full-bleed photograph or map. It exists for two reasons:
 * dark status-bar icons need a light backdrop in light mode, light ones need a dark backdrop in
 * dark mode, and a photograph does not know what theme it is in.
 */
@Composable
fun TopEdgeFade(
    modifier: Modifier = Modifier,
    height: Dp = 124.dp,
) {
    val p = LocalFootnotesPalette.current
    val top = p.paper.copy(alpha = if (p.isDark) 0.86f else 0.68f)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    0f to top,
                    0.72f to top.copy(alpha = top.alpha * 0.35f),
                    1f to Color.Transparent,
                ),
            ),
    )
}

/**
 * A paper band that only appears once the page underneath has scrolled, so a full-bleed photograph
 * can run behind the status bar at the top of a page while headlines never collide with the clock.
 * The hairline at its bottom is the printed margin.
 */
@Composable
fun StatusBarScrim(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val p = LocalFootnotesPalette.current
    val inset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(androidx.compose.animation.core.tween(140)),
        exit = fadeOut(androidx.compose.animation.core.tween(140)),
    ) {
        Column(Modifier.fillMaxWidth().background(p.paper)) {
            Spacer(Modifier.height(inset))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(p.ink.copy(alpha = 0.18f)),
            )
        }
    }
}

/**
 * A card with a hard ink shadow. It is the loudest object in the design system, so it is reserved
 * for things worth touching: walks, cities, guide choices. Supporting information gets a quieter
 * [NoteCard].
 */
@Composable
fun InkCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color = LocalFootnotesPalette.current.paperRaised,
    contentColor: Color = LocalFootnotesPalette.current.ink,
    shape: Shape = RoundedCornerShape(20.dp),
    shadow: Dp = 5.dp,
    borderWidth: Dp = 2.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val p = LocalFootnotesPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift by animateDpAsState(
        targetValue = if (pressed) shadow else 0.dp,
        animationSpec = tween(90),
        label = "ink-card-press",
    )
    Box(modifier) {
        // The shadow is a solid block of ink, not a blurred elevation. It is the cheapest way to
        // make a card look printed rather than rendered.
        Box(
            Modifier
                .matchParentSize()
                .offset(shadow, shadow)
                .background(p.ink, shape),
        )
        val inner = Modifier
            .fillMaxWidth()
            .offset(lift, lift)
        if (onClick != null) {
            Surface(
                onClick = onClick,
                modifier = inner,
                shape = shape,
                color = container,
                contentColor = contentColor,
                border = BorderStroke(borderWidth, p.ink),
                interactionSource = interaction,
            ) {
                Box(Modifier.padding(contentPadding)) { content() }
            }
        } else {
            Surface(
                modifier = inner,
                shape = shape,
                color = container,
                contentColor = contentColor,
                border = BorderStroke(borderWidth, p.ink),
            ) {
                Box(Modifier.padding(contentPadding)) { content() }
            }
        }
    }
}

/**
 * A quieter printed panel: hairline rule, no shadow, no ink block. Use for things you read rather
 * than press — field notes, visitor information, the small print.
 */
@Composable
fun NoteCard(
    modifier: Modifier = Modifier,
    container: Color = LocalFootnotesPalette.current.paperRaised,
    contentColor: Color = LocalFootnotesPalette.current.ink,
    accent: Color? = null,
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable () -> Unit,
) {
    val p = LocalFootnotesPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(container, shape)
            .clip(shape)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                // The accent is an inset spine rather than a full-height bar: a bar drawn to the
                // edge would square off the card's rounded corner and read as a rendering bug.
                if (accent != null) {
                    val inset = 8.dp.toPx()
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(inset, 12.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(
                            4.dp.toPx(),
                            (size.height - 24.dp.toPx()).coerceAtLeast(1f),
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    )
                }
                // Draw the outline inset by half its stroke, so clipping to the rounded shape
                // leaves a crisp 1.5 dp line instead of shaving it in half.
                drawRoundRect(
                    color = p.ink.copy(alpha = 0.55f),
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = androidx.compose.ui.geometry.Size(
                        (size.width - stroke).coerceAtLeast(1f),
                        (size.height - stroke).coerceAtLeast(1f),
                    ),
                    style = Stroke(stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx() - stroke / 2f),
                )
            }
            .padding(start = if (accent != null) 22.dp else 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * The primary button: thick ink outline, hard shadow, and a press that physically thunks into the
 * page. Fraunces is deliberately not used here; a button should be read, not admired.
 */
@Composable
fun InkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = LocalFootnotesPalette.current.tomato,
    contentColor: Color = LocalFootnotesPalette.current.paperRaised,
    enabled: Boolean = true,
    shadow: Dp = 4.dp,
    height: Dp = 56.dp,
) {
    val p = LocalFootnotesPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift by animateDpAsState(
        targetValue = if (pressed && enabled) shadow else 0.dp,
        animationSpec = tween(80),
        label = "ink-button-press",
    )
    Box(modifier) {
        Box(
            Modifier
                .matchParentSize()
                .offset(shadow, shadow)
                .background(if (enabled) p.ink else p.line, RoundedCornerShape(16.dp)),
        )
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(lift, lift),
            shape = RoundedCornerShape(16.dp),
            color = if (enabled) container else p.paperDeep,
            contentColor = if (enabled) contentColor else p.inkSoft,
            border = BorderStroke(2.dp, p.ink),
            interactionSource = interaction,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FootnotesBodyFont,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Secondary action: paper, ink outline, no shouting. */
@Composable
fun InkOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 52.dp,
    contentColor: Color = LocalFootnotesPalette.current.ink,
) {
    val p = LocalFootnotesPalette.current
    Surface(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(14.dp),
        color = p.paperRaised,
        contentColor = contentColor,
        border = BorderStroke(1.5.dp, p.ink),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * A stamp from the passport control of a made-up country: mono caps, a printed edge and a
 * knowing tilt. Used for metadata, never for the only copy of important information.
 */
@Composable
fun Stamp(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = LocalFootnotesPalette.current.paperDeep,
    contentColor: Color = LocalFootnotesPalette.current.ink,
    rotation: Float = 0f,
) {
    val p = LocalFootnotesPalette.current
    Box(
        modifier = modifier
            .rotate(rotation)
            .background(container, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FootnotesMonoFont),
            color = contentColor,
            maxLines = 1,
        )
    }
}

/** A sticker: rounder than a stamp, usually louder. */
@Composable
fun Sticker(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = LocalFootnotesPalette.current.mustard,
    contentColor: Color = Color(0xFF221A0E),
    rotation: Float = -2f,
    icon: ImageVector? = null,
) {
    val p = LocalFootnotesPalette.current
    Row(
        modifier = modifier
            .rotate(rotation)
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(13.dp))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
        )
    }
}

/** The tiny all-caps label that starts most sections. */
@Composable
fun Kicker(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalFootnotesPalette.current.tomato,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontFamily = FootnotesMonoFont,
            fontWeight = FontWeight.Bold,
        ),
        color = color,
        modifier = modifier,
    )
}

/** Kicker + big serif title, the editorial section header used across the app. */
@Composable
fun SectionHeading(
    kicker: String,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = LocalFootnotesPalette.current.ink,
    kickerColor: Color = LocalFootnotesPalette.current.tomato,
) {
    Column(modifier) {
        Kicker(kicker, color = kickerColor)
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FootnotesTitleFont),
            color = titleColor,
        )
    }
}

/** A dotted rule, because a solid one would look too much like a form. */
@Composable
fun DottedRule(
    modifier: Modifier = Modifier,
    color: Color = LocalFootnotesPalette.current.ink.copy(alpha = 0.35f),
    dotSize: Dp = 2.dp,
    gap: Dp = 6.dp,
) {
    Canvas(modifier.fillMaxWidth().height(dotSize)) {
        val r = dotSize.toPx() / 2f
        val step = dotSize.toPx() + gap.toPx()
        var x = r
        while (x < size.width) {
            drawCircle(color, r, Offset(x, r))
            x += step
        }
    }
}

/** A four-point sparkle, for accents that should feel hand-drawn. */
@Composable
fun Sparkle(
    modifier: Modifier = Modifier,
    color: Color = LocalFootnotesPalette.current.mustard,
    strokeOnly: Boolean = false,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w / 2f, 0f)
            cubicTo(w * 0.58f, h * 0.34f, w * 0.66f, h * 0.42f, w, h / 2f)
            cubicTo(w * 0.66f, h * 0.58f, w * 0.58f, h * 0.66f, w / 2f, h)
            cubicTo(w * 0.42f, h * 0.66f, w * 0.34f, h * 0.58f, 0f, h / 2f)
            cubicTo(w * 0.34f, h * 0.42f, w * 0.42f, h * 0.34f, w / 2f, 0f)
            close()
        }
        if (strokeOnly) {
            drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
        } else {
            drawPath(path, color)
        }
    }
}

/** Chunky rays around a centre: the "you are here" mark of a hand-made map. */
@Composable
fun Sunburst(
    modifier: Modifier = Modifier,
    color: Color = LocalFootnotesPalette.current.tomato,
    rays: Int = 12,
    filled: Boolean = false,
) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f
        val inner = outer * 0.56f
        val paint = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        repeat(rays) { i ->
            val angle = (Math.PI * 2.0 * i / rays) - Math.PI / 2.0
            val start = Offset(
                c.x + (Math.cos(angle) * inner).toFloat(),
                c.y + (Math.sin(angle) * inner).toFloat(),
            )
            val end = Offset(
                c.x + (Math.cos(angle) * outer).toFloat(),
                c.y + (Math.sin(angle) * outer).toFloat(),
            )
            drawLine(color, start, end, strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        if (filled) {
            drawCircle(color, radius = inner * 0.46f, center = c)
        } else {
            drawCircle(color, radius = inner * 0.46f, center = c, style = paint)
        }
    }
}

/** A strip of low-tack tape. Purely decorative, like most tape. */
@Composable
fun Tape(
    modifier: Modifier = Modifier,
    color: Color = LocalFootnotesPalette.current.mustard,
    rotation: Float = 0f,
) {
    Box(
        modifier
            .rotate(rotation)
            .size(width = 76.dp, height = 22.dp)
            .background(color.copy(alpha = 0.72f), RoundedCornerShape(1.dp)),
    )
}

/** A back control that looks stuck on rather than built in. */
@Composable
fun BackSticker(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = LocalFootnotesPalette.current.paperRaised,
) {
    val p = LocalFootnotesPalette.current
    Surface(
        onClick = onBack,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = container,
        contentColor = p.ink,
        border = BorderStroke(1.5.dp, p.ink),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * The bar at the top of secondary screens. Deliberately not Material's TopAppBar: no elevation,
 * no centred title, no 64 dp of grey. A back sticker, an editorial title and whatever action the
 * page needs.
 */
@Composable
fun FootnotesTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: RowScopeContent = {},
) {
    val p = LocalFootnotesPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            BackSticker(onBack = onBack)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FootnotesBodyFont),
                color = p.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = p.inkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            actions()
        }
    }
}

typealias RowScopeContent = @Composable androidx.compose.foundation.layout.RowScope.() -> Unit

/**
 * One fact, set like a ticket stub: a big mono number and a tiny caption. Three of these across a
 * page read as an itinerary rather than a statistics dashboard.
 */
@Composable
fun TicketStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = LocalFootnotesPalette.current.tomato,
    valueColor: Color = LocalFootnotesPalette.current.ink,
) {
    val p = LocalFootnotesPalette.current
    Column(
        modifier = modifier
            .background(p.paperRaised, RoundedCornerShape(12.dp))
            .drawBehind {
                drawRect(accent, size = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx()))
            }
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FootnotesMonoFont,
                fontWeight = FontWeight.Bold,
            ),
            color = valueColor,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
            color = p.inkSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A city tag with a flag, used to say where a walk is without another paragraph. */
@Composable
fun CityStamp(
    city: String,
    country: String,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    val p = LocalFootnotesPalette.current
    val a = accent ?: cityAccent(city).background
    Row(
        modifier = modifier
            .background(p.paperRaised, RoundedCornerShape(50))
            .drawBehind {
                drawRoundRect(
                    color = p.ink.copy(alpha = 0.55f),
                    style = Stroke(1.2.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                )
            }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).background(a, CircleShape))
        Spacer(Modifier.width(7.dp))
        Text(
            text = "$city, $country".uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = p.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A small square icon tile, printed rather than filled. */
@Composable
fun InkIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = LocalFootnotesPalette.current.ink,
    background: Color = LocalFootnotesPalette.current.mustard,
    size: Dp = 44.dp,
) {
    val p = LocalFootnotesPalette.current
    Box(
        modifier = modifier
            .size(size)
            .background(background, RoundedCornerShape(12.dp))
            .drawBehind {
                drawRoundRect(
                    color = p.ink,
                    style = Stroke(1.5.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.48f))
    }
}

/** Tiny clickable text link, in the zine's voice: underlined, mono, no button chrome. */
@Composable
fun InkTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalFootnotesPalette.current.ink,
) {
    Text(
        text = text,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium.copy(
            fontFamily = FootnotesMonoFont,
            fontWeight = FontWeight.Bold,
        ),
        color = color,
        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
    )
}

/** Centres a line of copy like a caption in a picture book. */
@Composable
fun CenteredCaption(text: String, modifier: Modifier = Modifier) {
    val p = LocalFootnotesPalette.current
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = p.inkSoft,
        textAlign = TextAlign.Center,
    )
}
