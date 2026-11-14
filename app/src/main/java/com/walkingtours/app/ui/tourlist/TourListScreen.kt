package com.walkingtours.app.ui.tourlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walkingtours.app.ServiceLocator
import com.walkingtours.app.data.db.TourEntity
import com.walkingtours.app.ui.components.AssetPhoto
import com.walkingtours.app.ui.components.BackSticker
import com.walkingtours.app.ui.components.DottedRule
import com.walkingtours.app.ui.components.InkCard
import com.walkingtours.app.ui.components.Kicker
import com.walkingtours.app.ui.components.PaperBackground
import com.walkingtours.app.ui.components.Stamp
import com.walkingtours.app.ui.components.Sticker
import com.walkingtours.app.ui.theme.FootnotesMonoFont
import com.walkingtours.app.ui.theme.FootnotesTitleFont
import com.walkingtours.app.ui.theme.LocalFootnotesPalette
import com.walkingtours.app.ui.theme.cityAccent
import com.walkingtours.app.util.CountryFlags
import com.walkingtours.app.util.Formatters

/**
 * A city's walks, set like the contents page of a small, opinionated guidebook.
 *
 * The city name is the hero here, not the first photograph: the walker has already chosen the
 * place, and now wants to know what there is to do in it.
 */
@Composable
fun TourListScreen(
    onOpenTour: (String) -> Unit,
    /** When set, only this city's tours are listed. */
    city: String? = null,
    onBack: (() -> Unit)? = null,
) {
    val repository = ServiceLocator.repository
    val p = LocalFootnotesPalette.current

    LaunchedEffect(Unit) { repository.ensureContentLoaded() }

    val toursAll by remember { repository.observeTours() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val tours = if (city == null) toursAll else toursAll.filter { it.city == city }
    val stopCounts by remember { repository.observeStopCounts() }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val first = tours.firstOrNull()
    val accent = cityAccent(city ?: first?.city ?: "somewhere")
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    PaperBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 30.dp + navBottom),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBack != null) {
                            BackSticker(onBack = onBack)
                            Spacer(Modifier.width(12.dp))
                        }
                        Kicker("The contents page", color = accent.background)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${tours.size} ${if (tours.size == 1) "WALK" else "WALKS"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.inkSoft,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = CountryFlags.flag(first?.country.orEmpty()).ifEmpty { "◎" },
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = first?.country.orEmpty().uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = p.inkSoft,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = city ?: "All the walks",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FootnotesTitleFont,
                            fontSize = 50.sp,
                            lineHeight = 48.sp,
                        ),
                        color = p.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when {
                            tours.isEmpty() -> "Fetching the good stuff."
                            tours.size == 1 -> "One walk. Put your shoes where your mouth is."
                            else -> "${tours.size} walks. Different moods, same city. Pick a lane."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = p.inkSoft,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accent.background, RoundedCornerShape(4.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "★",
                        style = MaterialTheme.typography.titleSmall,
                        color = accent.foreground,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "SELF-GUIDED · GEOFENCED · NO UMBRELLA HAT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent.foreground,
                    )
                }
            }

            items(tours, key = { it.id }) { tour ->
                val stopCount = stopCounts.firstOrNull { it.tourId == tour.id }?.stopCount ?: 0
                TourPoster(
                    tour = tour,
                    stopCount = stopCount,
                    index = tours.indexOf(tour) + 1,
                    onClick = { onOpenTour(tour.id) },
                )
            }

            if (tours.isEmpty()) {
                item {
                    InkCard(
                        modifier = Modifier.fillMaxWidth(),
                        container = p.mustard,
                        contentColor = androidx.compose.ui.graphics.Color(0xFF221A0E),
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                text = "No walks here. Awkward.",
                                style = MaterialTheme.typography.headlineSmall,
                                color = androidx.compose.ui.graphics.Color(0xFF221A0E),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "The map is still being folded. Try again in a moment, or go " +
                                    "somewhere with more pavement.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.ui.graphics.Color(0xFF221A0E).copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }

            item {
                Column(Modifier.padding(top = 10.dp)) {
                    DottedRule(color = p.ink.copy(alpha = 0.25f))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Pick the walk that fits the shoes you wore.",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FootnotesTitleFont,
                            fontSize = 23.sp,
                        ),
                        color = p.ink,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Every one of them starts wherever you are standing. No queue, no flag to follow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.inkSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun TourPoster(
    tour: TourEntity,
    stopCount: Int,
    index: Int,
    onClick: () -> Unit,
) {
    val p = LocalFootnotesPalette.current
    val accent = cityAccent("${tour.city}/${tour.id}")

    InkCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        shadow = 6.dp,
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(178.dp)) {
                AssetPhoto(
                    assetPath = tour.heroImage,
                    contentDescription = tour.title,
                    modifier = Modifier.fillMaxSize(),
                )
                Sticker(
                    text = "Walk ${"%02d".format(index)}",
                    modifier = Modifier.padding(12.dp),
                    container = accent.background,
                    contentColor = accent.foreground,
                    rotation = -2f,
                )
                Stamp(
                    text = "${tour.distanceKm} km  ·  ${Formatters.duration(tour.totalWalkMinutes)}",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    container = p.paperRaised,
                    rotation = 1.5f,
                )
            }
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = tour.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FootnotesTitleFont,
                        fontSize = 27.sp,
                        lineHeight = 29.sp,
                    ),
                    color = p.ink,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = tour.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.inkSoft,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    QuickStat(Icons.Filled.Place, "$stopCount stops")
                    QuickStat(Icons.AutoMirrored.Filled.DirectionsWalk, "${tour.distanceKm} km")
                    QuickStat(
                        Icons.Filled.Schedule,
                        Formatters.duration(
                            tour.totalWalkMinutes,
                        ),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TAKE THIS WALK",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FootnotesMonoFont,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = accent.background,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(accent.background, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = accent.foreground,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    val p = LocalFootnotesPalette.current
    Row(
        modifier = Modifier
            .background(p.paperDeep, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = p.inkSoft, modifier = Modifier.size(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = p.inkSoft,
            maxLines = 1,
        )
    }
}
