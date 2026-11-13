package com.walkingtours.app.ui.persona

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walkingtours.app.ServiceLocator
import com.walkingtours.app.ai.Guide
import com.walkingtours.app.ai.NARRATION_LANGUAGES
import com.walkingtours.app.ui.components.BackSticker
import com.walkingtours.app.ui.components.InkCard
import com.walkingtours.app.ui.components.Kicker
import com.walkingtours.app.ui.components.PaperBackground
import com.walkingtours.app.ui.components.Sparkle
import com.walkingtours.app.ui.components.Sticker
import com.walkingtours.app.ui.theme.CityAccent
import com.walkingtours.app.ui.theme.FootnotesMonoFont
import com.walkingtours.app.ui.theme.FootnotesTitleFont
import com.walkingtours.app.ui.theme.LocalFootnotesPalette
import com.walkingtours.app.ui.theme.footnotesAccents

/**
 * The guide picker, staged as a casting session rather than a settings form.
 *
 * Four writers, four obsessions, one city. Picking a guide is the most consequential choice in the
 * app — it changes what every stop talks about — so it gets a full page and a bit of theatre.
 */
@Composable
fun PersonaScreen(
    tourId: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val personaSettings = ServiceLocator.personaSettings
    val guideController = ServiceLocator.guide
    val aiSettings = ServiceLocator.aiSettings
    val aiState by aiSettings.state.collectAsStateWithLifecycle()
    val persona by personaSettings.state.collectAsStateWithLifecycle()
    val currentGuide = persona.guide(tourId)
    val p = LocalFootnotesPalette.current

    var guides by remember { mutableStateOf<List<Guide>?>(null) }

    LaunchedEffect(tourId, aiState.narrationLanguage) {
        guides = null
        guides = guideController.suggestGuides(tourId)
    }

    PaperBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackSticker(onBack = onBack)
                Spacer(Modifier.width(12.dp))
                Kicker("The casting", color = p.tomato)
                Spacer(Modifier.weight(1f))
                Sticker(
                    text = "4 voices",
                    container = p.mustard,
                    rotation = 3f,
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Who's in\nyour ear?",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FootnotesTitleFont,
                    fontSize = 48.sp,
                    lineHeight = 45.sp,
                ),
                color = p.ink,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Four storytellers for this walk. Each one is obsessed with a different side " +
                    "of the place — pick the obsession that matches yours. They'll be with you at " +
                    "every stop.",
                style = MaterialTheme.typography.bodyLarge,
                color = p.inkSoft,
            )
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SPOKEN IN",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.inkSoft,
                    modifier = Modifier.weight(1f),
                )
                LanguageDropdown(
                    selected = aiState.narrationLanguage,
                    onSelect = { tag -> aiSettings.update { it.copy(narrationLanguage = tag) } },
                )
            }
            Spacer(Modifier.height(18.dp))

            if (guides == null) {
                val accents = footnotesAccents()
                repeat(2) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        repeat(2) { column ->
                            GuideCardSkeleton(
                                modifier = Modifier.weight(1f),
                                accent = accents[(row * 2 + column) % accents.size],
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            } else {
                val accents = footnotesAccents()
                // The guide already walking this tour must be on the casting sheet. Suggestions are
                // regenerated per process and can differ from the set the walker originally chose,
                // which used to make their own guide invisible — and therefore impossible to keep.
                val offered = guides!!.let { generated ->
                    if (currentGuide == null || generated.any { it.id == currentGuide.id }) {
                        generated
                    } else {
                        (listOf(currentGuide) + generated).take(4)
                    }
                }
                offered.chunked(2).forEachIndexed { rowIndex, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        pair.forEachIndexed { columnIndex, guide ->
                            val index = rowIndex * 2 + columnIndex
                            GuideCard(
                                guide = guide,
                                index = index,
                                accent = accents[index % accents.size],
                                current = guide.id == currentGuide?.id,
                                modifier = Modifier.weight(1f),
                                onPick = {
                                    personaSettings.setGuide(tourId, guide)
                                    guideController.ensureVoiceMatchesGuide(tourId)
                                    onDone()
                                },
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Sparkle(Modifier.size(20.dp), color = p.teal)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "You can change your mind later. The guide will pretend not to notice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.inkSoft,
                )
            }
        }
    }
}

@Composable
private fun LanguageDropdown(selected: String, onSelect: (String) -> Unit) {
    val p = LocalFootnotesPalette.current
    var expanded by remember { mutableStateOf(false) }
    val label = NARRATION_LANGUAGES.firstOrNull { it.first == selected }?.second ?: selected
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(50),
            color = p.paperRaised,
            contentColor = p.ink,
            border = BorderStroke(1.5.dp, p.ink),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "Narration language",
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            NARRATION_LANGUAGES.forEach { (tag, text) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (tag == selected) "$text  ✓" else text,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(tag)
                    },
                )
            }
        }
    }
}

@Composable
private fun GuideCard(
    guide: Guide,
    index: Int,
    accent: CityAccent,
    current: Boolean,
    modifier: Modifier,
    onPick: (Guide) -> Unit,
) {
    val p = LocalFootnotesPalette.current
    InkCard(
        modifier = modifier
            .height(258.dp)
            .graphicsLayer { rotationZ = if (index % 2 == 0) -1.1f else 1.1f },
        onClick = { onPick(guide) },
        container = accent.background,
        contentColor = accent.foreground,
        shape = RoundedCornerShape(18.dp),
        shadow = 5.dp,
    ) {
        Box(Modifier.fillMaxSize().padding(13.dp)) {
            Text(
                text = guide.name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FootnotesTitleFont,
                    fontSize = 86.sp,
                    lineHeight = 80.sp,
                ),
                color = accent.foreground.copy(alpha = 0.16f),
                modifier = Modifier.align(Alignment.TopEnd),
            )
            Column(Modifier.fillMaxSize()) {
                Text(
                    text = "VOICE ${"%02d".format(index + 1)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent.foreground.copy(alpha = 0.78f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = guide.name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FootnotesTitleFont,
                        fontSize = 20.sp,
                        lineHeight = 21.sp,
                    ),
                    color = accent.foreground,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                if (guide.angle.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .background(accent.foreground, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = guide.angle.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = accent.background,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (guide.tagline.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = guide.tagline,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        color = accent.foreground.copy(alpha = 0.85f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (current) "CURRENT GUIDE" else "PICK ME",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent.foreground,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(accent.foreground, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (current) {
                                Icons.Filled.Check
                            } else {
                                Icons.AutoMirrored.Filled.ArrowForward
                            },
                            contentDescription = null,
                            tint = accent.background,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideCardSkeleton(modifier: Modifier = Modifier, accent: CityAccent) {
    val transition = rememberInfiniteTransition(label = "guide-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "guide-skeleton-alpha",
    )
    val p = LocalFootnotesPalette.current
    Box(
        modifier = modifier
            .height(258.dp)
            .background(accent.background, RoundedCornerShape(18.dp))
            .padding(13.dp)
            .alpha(alpha),
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .width(54.dp)
                    .height(10.dp)
                    .background(accent.foreground.copy(alpha = 0.4f), RoundedCornerShape(5.dp)),
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .height(20.dp)
                    .background(accent.foreground.copy(alpha = 0.35f), RoundedCornerShape(5.dp)),
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.55f)
                    .height(16.dp)
                    .background(accent.foreground.copy(alpha = 0.28f), RoundedCornerShape(5.dp)),
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(accent.foreground.copy(alpha = 0.22f), RoundedCornerShape(5.dp)),
            )
        }
    }
}
