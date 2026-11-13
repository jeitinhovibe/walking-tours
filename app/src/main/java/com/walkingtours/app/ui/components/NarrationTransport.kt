package com.walkingtours.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.walkingtours.app.R
import com.walkingtours.app.audio.NarrationState
import com.walkingtours.app.ui.theme.FootnotesMonoFont
import com.walkingtours.app.ui.theme.LocalFootnotesPalette
import kotlin.math.abs
import kotlin.math.sin

/**
 * The player, as a piece of kit worth looking at.
 *
 * This app is an audio guide, so the transport is not utility furniture: it is the one object the
 * walker touches while standing in front of something extraordinary. It gets a printed panel, a
 * physical play disc, and a live waveform that moves with the voice instead of a stock slider.
 *
 * The row still does everything the old one did — play, pause, rewind, skip, scrub, speed — but the
 * hierarchy is honest: play is the button, everything else is available without competing.
 */
@Composable
fun NarrationTransport(
    state: NarrationState,
    positionMs: Long,
    durationMs: Long,
    rate: Float,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onRateChange: (Float) -> Unit,
    message: String? = null,
    modifier: Modifier = Modifier,
    /**
     * False on pages with no neighbours — a detour is a single chapter, so there is nowhere to
     * step to. The buttons are removed rather than disabled: a dead control invites the tap it
     * cannot honour.
     */
    showPrevNext: Boolean = true,
) {
    val p = LocalFootnotesPalette.current
    val playing = state == NarrationState.PLAYING || state == NarrationState.PREPARING
    val unavailable = state == NarrationState.UNAVAILABLE
    val hasTimeline = durationMs > 0L

    var scrubbing by remember { mutableStateOf<Float?>(null) }
    val liveFraction = if (hasTimeline) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val shownFraction = scrubbing ?: liveFraction

    val transition = rememberInfiniteTransition(label = "waveform")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "waveform-phase",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(p.paperRaised, RoundedCornerShape(18.dp)),
    ) {
        // A strip of ink at the top, like the colour bar on a printed ticket.
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(p.ink, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
        )
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayDisc(
                playing = playing,
                enabled = !unavailable,
                onClick = onPlayPause,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stateLabel(state),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FootnotesMonoFont,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (unavailable) p.inkSoft else p.tomato,
                    )
                    Spacer(Modifier.weight(1f))
                    SpeedStamp(rate = rate, enabled = !unavailable, onRateChange = onRateChange)
                }
                Spacer(Modifier.height(6.dp))
                Waveform(
                    playing = playing,
                    progress = shownFraction,
                    phase = phase,
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = com.walkingtours.app.util.Formatters.clock(positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = p.inkSoft,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (hasTimeline) {
                            com.walkingtours.app.util.Formatters.clock(durationMs)
                        } else {
                            "--:--"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = p.inkSoft,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (showPrevNext) {
                TransportButton(R.drawable.ic_previous, "Previous stop", onClick = onPrevious)
            }
            TransportButton(
                R.drawable.ic_rewind_15,
                "Rewind 15 seconds",
                enabled = !unavailable,
                onClick = onRewind,
            )
            MinimalScrubber(
                fraction = shownFraction,
                enabled = hasTimeline && !unavailable,
                onScrub = { scrubbing = it },
                onCommit = { value ->
                    scrubbing = null
                    onSeekFraction(value)
                },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
            TransportButton(
                R.drawable.ic_forward_15,
                "Forward 15 seconds",
                enabled = !unavailable,
                onClick = onForward,
            )
            if (showPrevNext) {
                TransportButton(R.drawable.ic_next, "Next stop", onClick = onNext)
            }
        }
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
            )
        }
    }
}

private fun stateLabel(state: NarrationState): String = when (state) {
    NarrationState.PREPARING -> "WARMING UP"
    NarrationState.PLAYING -> "PLAYING"
    NarrationState.PAUSED -> "PAUSED"
    NarrationState.COMPLETED -> "FINISHED"
    NarrationState.UNAVAILABLE -> "NO AUDIO"
    NarrationState.IDLE -> "READY"
}

/** A physical-looking play button: a coloured disc with an ink rim and a hard little shadow. */
@Composable
private fun PlayDisc(
    playing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val p = LocalFootnotesPalette.current
    Box(Modifier.size(58.dp)) {
        Box(
            Modifier
                .size(56.dp)
                .padding(start = 2.dp, top = 2.dp)
                .background(p.ink, CircleShape),
        )
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(54.dp),
            shape = CircleShape,
            color = if (enabled) p.tomato else p.paperDeep,
            contentColor = if (enabled) p.paperRaised else p.inkSoft,
            border = BorderStroke(1.5.dp, p.ink),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/**
 * A waveform that is decorative in the sense that it is not sampled from the audio, and useful in
 * the sense that it shows exactly how far through the chapter the walker is. The bars breathe only
 * while the narrator is actually talking.
 */
@Composable
private fun Waveform(
    playing: Boolean,
    progress: Float,
    phase: Float,
    modifier: Modifier = Modifier,
) {
    val p = LocalFootnotesPalette.current
    Canvas(modifier) {
        val barCount = 42
        val gap = 2.dp.toPx()
        val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(1f)
        val mid = size.height / 2f
        val played = progress.coerceIn(0f, 1f)
        for (i in 0 until barCount) {
            // A deterministic shape, so the waveform is the same every time the chapter opens. The
            // sine makes it move; the two other waves stop it looking like a bar chart.
            val base = 0.28f +
                0.42f * abs(sin(i * 0.63f)) +
                0.30f * abs(sin(i * 0.21f + 1.3f))
            val motion = if (playing) 0.18f * sin(phase + i * 0.7f) else 0f
            val height = (size.height * (base + motion).coerceIn(0.16f, 1f))
            val x = i * (barWidth + gap)
            val color = if ((i + 0.5f) / barCount <= played) {
                p.tomato
            } else {
                p.ink.copy(alpha = 0.16f)
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, mid - height / 2f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@Composable
private fun SpeedStamp(
    rate: Float,
    enabled: Boolean,
    onRateChange: (Float) -> Unit,
) {
    val p = LocalFootnotesPalette.current
    Surface(
        onClick = {
            val options = listOf(0.75f, 1.0f, 1.25f, 1.5f)
            val next = options[(options.indexOfFirst { abs(it - rate) < 0.01f } + 1)
                .mod(options.size)]
            onRateChange(next)
        },
        enabled = enabled,
        shape = RoundedCornerShape(5.dp),
        color = p.paperDeep,
        contentColor = p.ink,
        border = BorderStroke(1.dp, p.ink.copy(alpha = 0.5f)),
    ) {
        Text(
            text = if (abs(rate - 1.0f) < 0.01f) "1×" else "${rate}×",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

/**
 * A transport icon sized to the row rather than to Material's 48dp touch target. It still has an
 * accessible label, and the row is horizontal so mis-taps land on a neighbour rather than nowhere.
 */
@Composable
private fun TransportButton(
    iconRes: Int,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val p = LocalFootnotesPalette.current
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = if (enabled) p.ink else p.ink.copy(alpha = 0.3f),
            modifier = Modifier.size(21.dp),
        )
    }
}

/**
 * A deliberately quiet scrubber: a hairline with a small dot, in place of Material's slider with its
 * thick track and haloed thumb.
 *
 * This page is mostly writing and photographs, and a heavy control in the middle of it pulls the eye
 * away from the thing the walker came for. Progress still has to be visible and seekable, so it stays
 * — it is just no longer the loudest element on the screen.
 */
@Composable
private fun MinimalScrubber(
    fraction: Float,
    enabled: Boolean,
    onScrub: (Float) -> Unit,
    onCommit: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalFootnotesPalette.current
    val trackColor = p.ink.copy(alpha = 0.16f)
    val progressColor = p.tomato
    val thumbColor = p.tomato

    var lastFraction by remember { mutableStateOf(fraction) }

    Canvas(
        modifier = modifier
            .height(22.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val target = if (size.width <= 0) 0f else (offset.x / size.width).coerceIn(0f, 1f)
                    lastFraction = target
                    onScrub(target)
                    onCommit(target)
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val target = if (size.width <= 0) 0f else (offset.x / size.width).coerceIn(0f, 1f)
                        lastFraction = target
                        onScrub(target)
                    },
                    onDragEnd = { onCommit(lastFraction) },
                    onDragCancel = { onCommit(lastFraction) },
                    onHorizontalDrag = { change, _ ->
                        val target = if (size.width <= 0) {
                            0f
                        } else {
                            (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                        lastFraction = target
                        onScrub(target)
                    },
                )
            },
    ) {
        val stroke = 3.dp.toPx()
        val inset = stroke / 2f
        val usable = (size.width - stroke).coerceAtLeast(1f)
        val centreY = size.height / 2f
        val shown = fraction.coerceIn(0f, 1f)

        drawLine(
            color = trackColor,
            start = Offset(inset, centreY),
            end = Offset(inset + usable, centreY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = progressColor,
            start = Offset(inset, centreY),
            end = Offset(inset + usable * shown, centreY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = thumbColor,
            radius = 4.5.dp.toPx(),
            center = Offset(inset + usable * shown, centreY),
        )
    }
}
