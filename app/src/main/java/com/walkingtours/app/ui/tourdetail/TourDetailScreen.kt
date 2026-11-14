package com.walkingtours.app.ui.tourdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walkingtours.app.ServiceLocator
import com.walkingtours.app.data.db.StopEntity
import com.walkingtours.app.data.db.TourEntity
import com.walkingtours.app.ui.chat.ChatBottomSheet
import com.walkingtours.app.ui.components.AssetPhoto
import com.walkingtours.app.ui.components.BackSticker
import com.walkingtours.app.ui.components.CityStamp
import com.walkingtours.app.ui.components.DottedRule
import com.walkingtours.app.ui.components.InkButton
import com.walkingtours.app.ui.components.InkCard
import com.walkingtours.app.ui.components.InkIconTile
import com.walkingtours.app.ui.components.InkOutlinedButton
import com.walkingtours.app.ui.components.InkTextLink
import com.walkingtours.app.ui.components.Kicker
import com.walkingtours.app.ui.components.NoteCard
import com.walkingtours.app.ui.components.PaperBackground
import com.walkingtours.app.ui.components.SectionHeading
import com.walkingtours.app.ui.components.Sparkle
import com.walkingtours.app.ui.components.Stamp
import com.walkingtours.app.ui.components.StatusBarScrim
import com.walkingtours.app.ui.components.Sticker
import com.walkingtours.app.ui.components.TicketStat
import com.walkingtours.app.ui.components.TopEdgeFade
import com.walkingtours.app.ui.components.TourMap
import com.walkingtours.app.ui.detour.DetourTopicsSheet
import com.walkingtours.app.ui.theme.FootnotesMonoFont
import com.walkingtours.app.ui.theme.FootnotesTitleFont
import com.walkingtours.app.ui.theme.LocalFootnotesPalette
import com.walkingtours.app.ui.theme.cityAccent
import com.walkingtours.app.util.Formatters
import java.util.Locale

/**
 * Height of the overview map. It is the top of the poster: the walker sees the walk before they
 * read a word about it.
 */
private val MAP_HEIGHT = 268.dp

/**
 * Everything a walker needs to decide whether to set off, laid out like the inside spread of a
 * travel zine: the route, the pitch, the cast, the small print, then the itinerary as a hand-drawn
 * line with stamps on it.
 */
@Composable
fun TourDetailScreen(
    tourId: String,
    onBack: () -> Unit,
    /** Begins the walk: the introduction plays, then the geofences take over. */
    onStartTour: () -> Unit,
    /** Continues a part-finished walk at the first stop still to see. */
    onResumeTour: () -> Unit,
    /** Wipes completed stops and begins again. */
    onStartOver: () -> Unit,
    /** Starts the walk at this stop and shows it. */
    onOpenStop: (String) -> Unit,
    /** Opens a detour deep-dive on the chosen topic. */
    onOpenDetour: (String) -> Unit,
    /** Opens the persona flow to choose or change the guide. */
    onOpenPersona: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val repository = ServiceLocator.repository
    val p = LocalFootnotesPalette.current

    val tour by remember(tourId) { repository.observeTour(tourId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val stops by remember(tourId) { repository.observeStops(tourId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val progress by remember(tourId) { repository.observeStopProgress(tourId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val persona by ServiceLocator.personaSettings.state.collectAsStateWithLifecycle()
    val guide = persona.guide(tourId)

    var showDetours by remember { mutableStateOf(false) }
    if (showDetours) {
        DetourTopicsSheet(
            tourId = tourId,
            onPick = { topic ->
                showDetours = false
                onOpenDetour(topic.id)
            },
            onOpenSettings = onOpenSettings,
            onDismiss = { showDetours = false },
        )
    }

    val locationTracker = ServiceLocator.locationTracker
    val trackerFix by locationTracker.lastLocation.collectAsStateWithLifecycle(initialValue = null)
    val headingProvider = ServiceLocator.headingProvider
    val compassHeading by headingProvider.headingDegrees.collectAsStateWithLifecycle()
    DisposableEffect(Unit) {
        locationTracker.acquire()
        headingProvider.start()
        onDispose {
            locationTracker.release()
            headingProvider.stop()
        }
    }
    val mapLat = trackerFix?.latitude
    val mapLng = trackerFix?.longitude
    val mapAccuracy = trackerFix?.accuracy

    var showChat by remember { mutableStateOf(false) }
    if (showChat) {
        ChatBottomSheet(
            tourId = tourId,
            stopId = null,
            onDismiss = { showChat = false },
            onOpenSettings = onOpenSettings,
        )
    }

    val tourEntity = tour
    val visited = progress.map { it.stopId }.toSet()
    val totalStopMinutes = stops.sumOf { it.suggestedMinutes }
    val allDone = stops.isNotEmpty() && stops.all { it.id in visited }
    val startAction = when {
        allDone -> onStartOver
        visited.isEmpty() -> onStartTour
        else -> onResumeTour
    }
    val startLabel = when {
        allDone -> "Walk it again"
        visited.isEmpty() -> "Let's walk"
        else -> "Keep walking"
    }
    val accent = cityAccent(tourEntity?.city ?: tourId)

    PaperBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                SurfaceBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        InkOutlinedButton(
                            text = "Ask",
                            onClick = { showChat = true },
                            modifier = Modifier.weight(0.9f),
                            icon = Icons.Filled.AutoAwesome,
                            height = 56.dp,
                        )
                        InkButton(
                            text = startLabel,
                            onClick = startAction,
                            modifier = Modifier.weight(1.4f),
                            icon = Icons.Filled.Explore,
                            height = 56.dp,
                        )
                    }
                }
            },
        ) { innerPadding ->
            val scrollState = rememberScrollState()
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                // ---- The map, dressed as a poster -----------------------------------------
                Box(Modifier.fillMaxWidth().height(MAP_HEIGHT)) {
                    TourMap(
                        stops = stops,
                        visitedIds = visited,
                        selectedStopId = null,
                        userLat = mapLat,
                        userLng = mapLng,
                        userHeading = compassHeading,
                        userAccuracyMeters = mapAccuracy,
                        modifier = Modifier.fillMaxSize(),
                    )
                    TopEdgeFade()
                    BackSticker(
                        onBack = onBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 14.dp, top = 8.dp),
                    )
                    Sticker(
                        text = "The route",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(end = 14.dp, top = 12.dp),
                        container = p.mustard,
                        rotation = 3f,
                    )
                    if (stops.isNotEmpty()) {
                        Stamp(
                            text = "${stops.size} stops  ·  ${tourEntity?.distanceKm ?: 0.0} km",
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp),
                            container = p.paperRaised,
                            rotation = -1.5f,
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(2.dp).background(p.ink))

                if (tourEntity == null) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = "Folding the map…",
                            style = MaterialTheme.typography.headlineSmall,
                            color = p.ink,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "The good bits are on their way.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = p.inkSoft,
                        )
                    }
                    return@Column
                }

                // ---- The pitch -------------------------------------------------------------
                Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CityStamp(city = tourEntity.city, country = tourEntity.country, accent = accent.background)
                        Spacer(Modifier.weight(1f))
                        Sparkle(Modifier.size(22.dp), color = accent.background)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = tourEntity.title,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FootnotesTitleFont,
                            fontSize = 39.sp,
                            lineHeight = 39.sp,
                        ),
                        color = p.ink,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = tourEntity.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = p.inkSoft,
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatTicket(
                            value = stops.size.toString(),
                            label = "stops",
                            accent = p.tomato,
                            modifier = Modifier.weight(1f),
                        )
                        StatTicket(
                            value = "${tourEntity.distanceKm}",
                            label = "km",
                            accent = p.teal,
                            modifier = Modifier.weight(1f),
                        )
                        StatTicket(
                            value = Formatters.duration(tourEntity.totalWalkMinutes),
                            label = "walking",
                            accent = p.mustard,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Budget ${Formatters.totalExperience(tourEntity.totalWalkMinutes, totalStopMinutes)} " +
                            "if you actually go inside things.",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.inkSoft,
                    )

                    Spacer(Modifier.height(22.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Kicker("The cast", color = accent.background)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "LOCAL KNOWLEDGE, QUESTIONABLE JOKES",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.inkSoft,
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    NoteCard(accent = p.teal) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InkIconTile(
                                icon = Icons.Filled.RecordVoiceOver,
                                background = p.teal,
                                tint = p.paperRaised,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = if (guide == null) "Nobody in your ear yet" else "In your ear",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = p.ink,
                                )
                                Text(
                                    text = guide?.let { g ->
                                        listOfNotNull(
                                            g.name.takeIf { it.isNotBlank() },
                                            g.tagline.takeIf { it.isNotBlank() },
                                        ).joinToString(" · ")
                                    } ?: "Pick one of four storytellers before you set off.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = p.inkSoft,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            InkTextLink(
                                text = if (guide == null) "CHOOSE" else "CHANGE",
                                onClick = onOpenPersona,
                                color = p.teal,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    NoteCard(accent = p.mustard) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InkIconTile(
                                icon = Icons.Filled.Signpost,
                                background = p.mustard,
                                tint = Color(0xFF221A0E),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Take a detour",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = p.ink,
                                )
                                Text(
                                    text = "The history, politics and culture behind this walk. No walking required.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = p.inkSoft,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            InkTextLink(
                                text = "EXPLORE",
                                onClick = { showDetours = true },
                                color = p.ink,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    NoteCard(accent = p.tomato) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InkIconTile(
                                icon = Icons.Filled.ConfirmationNumber,
                                background = p.tomato,
                                tint = p.paperRaised,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Tickets and entrances",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = p.ink,
                                )
                                ticketSummary(stops)?.let { summary ->
                                    Text(
                                        text = summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = p.inkSoft,
                                    )
                                }
                            }
                        }
                    }
                }

                // ---- Field notes -----------------------------------------------------------
                val difficulty = tourEntity.difficulty.takeIf { it.isNotBlank() }
                val bestTime = tourEntity.bestTimeOfDay.takeIf { it.isNotBlank() }
                if (difficulty != null || bestTime != null) {
                    Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 28.dp)) {
                        DottedRule(color = p.ink.copy(alpha = 0.25f))
                        Spacer(Modifier.height(18.dp))
                        SectionHeading(
                            kicker = "Field notes",
                            title = "Know before you go",
                            kickerColor = p.cobalt,
                        )
                        Spacer(Modifier.height(12.dp))
                        if (difficulty != null) {
                            FieldNote(
                                icon = Icons.Filled.Terrain,
                                label = "How hard",
                                value = firstSentence(difficulty),
                                accent = p.cobalt,
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        if (bestTime != null) {
                            FieldNote(
                                icon = Icons.Filled.WbSunny,
                                label = "When to go",
                                value = firstSentence(bestTime),
                                accent = p.mustard,
                            )
                        }
                    }
                }

                // ---- The route -------------------------------------------------------------
                Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 30.dp)) {
                    DottedRule(color = p.ink.copy(alpha = 0.25f))
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Kicker("The route", color = p.tomato)
                            Text(
                                text = "Stop by stop",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FootnotesTitleFont,
                                ),
                                color = p.ink,
                            )
                        }
                        Text(
                            text = "TAP ANY TO JUMP IN",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.inkSoft,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "The walk opens with a briefing. Then the city does the rest.",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.inkSoft,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Column(Modifier.padding(horizontal = 18.dp)) {
                    if (tourEntity.overviewText.isNotBlank()) {
                        IntroTimelineCard(
                            accent = accent,
                            onClick = onStartTour,
                            isLast = stops.isEmpty(),
                        )
                        if (stops.isNotEmpty()) Spacer(Modifier.height(10.dp))
                    }
                    stops.forEachIndexed { index, stop ->
                        StopTimelineRow(
                            stop = stop,
                            visited = stop.id in visited,
                            accent = accent,
                            isLast = index == stops.lastIndex,
                            onClick = { onOpenStop(stop.id) },
                        )
                        if (index != stops.lastIndex) Spacer(Modifier.height(10.dp))
                    }
                }

                // ---- Sign-off --------------------------------------------------------------
                Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 30.dp, bottom = 26.dp)) {
                    DottedRule(color = p.ink.copy(alpha = 0.25f))
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Shoes on. Ears open. Phone away.",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FootnotesTitleFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                            ),
                            color = p.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Sparkle(Modifier.size(20.dp), color = accent.background)
                    }
                }
            }
            StatusBarScrim(
                visible = scrollState.value > 24,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            }
        }
    }
}

/** The printed action bar at the bottom of the page. Solid paper, not frosted glass. */
@Composable
private fun SurfaceBar(content: @Composable () -> Unit) {
    val p = LocalFootnotesPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(p.paperRaised)
            .padding(top = 2.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(p.ink))
        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun StatTicket(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val p = LocalFootnotesPalette.current
    Column(
        modifier = modifier
            .background(p.paperRaised, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(accent, RoundedCornerShape(2.dp)))
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FootnotesMonoFont,
                fontWeight = FontWeight.Bold,
            ),
            color = p.ink,
            maxLines = 1,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = p.inkSoft,
            maxLines = 1,
        )
    }
}

@Composable
private fun FieldNote(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color,
) {
    val p = LocalFootnotesPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(p.paperRaised, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        InkIconTile(
            icon = icon,
            background = accent,
            tint = if (accent == p.mustard) Color(0xFF221A0E) else p.paperRaised,
            size = 38.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = p.inkSoft,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = p.ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun IntroTimelineCard(
    accent: com.walkingtours.app.ui.theme.CityAccent,
    onClick: () -> Unit,
    isLast: Boolean,
) {
    val p = LocalFootnotesPalette.current
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        TimelineRail(
            isLast = isLast,
            accent = accent.background,
            passed = false,
            content = {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(accent.background, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = accent.foreground,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
        )
        InkCard(
            modifier = Modifier.weight(1f),
            onClick = onClick,
            container = accent.background,
            contentColor = accent.foreground,
            shape = RoundedCornerShape(16.dp),
            shadow = 3.dp,
            borderWidth = 1.5.dp,
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "THE BRIEFING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent.foreground.copy(alpha = 0.8f),
                    )
                    Text(
                        text = "Before you set off",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FootnotesTitleFont,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = accent.foreground,
                    )
                }
                Text(
                    text = "PLAY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent.foreground,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StopTimelineRow(
    stop: StopEntity,
    visited: Boolean,
    accent: com.walkingtours.app.ui.theme.CityAccent,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val p = LocalFootnotesPalette.current
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        TimelineRail(
            isLast = isLast,
            accent = accent.background,
            passed = visited,
            content = {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(if (visited) p.teal else accent.background, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (visited) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Visited",
                            tint = p.paperRaised,
                            modifier = Modifier.size(19.dp),
                        )
                    } else {
                        Text(
                            text = stop.order.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FootnotesMonoFont,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = accent.foreground,
                        )
                    }
                }
            },
        )
        InkCard(
            modifier = Modifier.weight(1f),
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            shadow = 3.dp,
            borderWidth = 1.5.dp,
        ) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(62.dp)
                        .background(p.paperDeep, RoundedCornerShape(11.dp)),
                ) {
                    AssetPhoto(
                        assetPath = stop.photoAsset,
                        contentDescription = stop.name,
                        modifier = Modifier.fillMaxSize(),
                        targetWidthPx = 240,
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stop.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.background,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stop.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FootnotesTitleFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            lineHeight = 20.sp,
                        ),
                        color = p.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(5.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Stamp(
                            text = "${stop.suggestedMinutes} min",
                            container = p.paperDeep,
                        )
                        if (stop.entranceFeeTry.isNotBlank()) {
                            Stamp(
                                text = stop.entranceFeeTry,
                                container = if (stop.isFree) {
                                    p.teal.copy(alpha = 0.16f)
                                } else {
                                    p.mustard.copy(alpha = 0.3f)
                                },
                            )
                        }
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = p.inkSoft,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * The dotted line and marker that turn a list of cards into a walk. The rail is drawn behind the
 * marker and stops at the last stop, so the route has a visible beginning and end.
 */
@Composable
private fun TimelineRail(
    isLast: Boolean,
    accent: Color,
    passed: Boolean,
    content: @Composable () -> Unit,
) {
    val p = LocalFootnotesPalette.current
    Box(
        modifier = Modifier
            .width(44.dp)
            .fillMaxHeight()
            .background(Color.Transparent)
            .padding(end = 10.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (isLast) return@Canvas
            val x = size.width / 2f
            val startY = 34.dp.toPx()
            drawLine(
                color = if (passed) p.teal.copy(alpha = 0.7f) else p.ink.copy(alpha = 0.32f),
                start = Offset(x, startY),
                end = Offset(x, size.height + 10.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(2.dp.toPx(), 5.dp.toPx()),
                    0f,
                ),
            )
            drawCircle(
                color = accent.copy(alpha = 0.18f),
                radius = 12.dp.toPx(),
                center = Offset(size.width / 2f, 17.dp.toPx()),
                style = Stroke(1.dp.toPx()),
            )
        }
        Box(Modifier.align(Alignment.TopCenter)) { content() }
    }
}

/**
 * The first sentence of a paragraph, so "Know before you go" can show a compact version of the
 * long authored difficulty and best-time notes. Falls back to the whole text when there is no
 * sentence break to cut at.
 */
private fun firstSentence(text: String): String {
    val trimmed = text.trim()
    val end = trimmed.indexOf(". ")
    return if (end in 1 until trimmed.lastIndex) trimmed.substring(0, end + 1) else trimmed
}

/** A price read out of a free-text fee string: the currency as displayed, and the amount. */
private data class FeeAmount(val currency: String, val value: Double)

/** Numbers in a fee string, commas removed so "1,950" reads as 1950. */
private val FEE_NUMBER = Regex("""\d[\d,]*(?:\.\d+)?""")

/**
 * Currency tokens as they appear in the authored fees, in the order they must be tested (multi-
 * character symbols before the single characters they contain), each mapped to how it is shown.
 */
private val FEE_CURRENCIES = listOf(
    "HK$" to "HK$",
    "MOP" to "MOP",
    "CNY" to "¥",
    "yuan" to "¥",
    "¥" to "¥",
    "₹" to "₹",
    "Rs" to "₹",
    "TL" to "TL",
    "₺" to "TL",
    "EUR" to "€",
    "€" to "€",
    "AZN" to "₼",
    "₼" to "₼",
)

private fun feeCurrency(raw: String): String? =
    FEE_CURRENCIES.firstOrNull { (token, _) -> raw.contains(token) }?.second

/**
 * One short sentence for the ticket card: how many stops need a ticket, plus a total when the
 * fees resolve cleanly.
 *
 * Fees are authored as free text in different currencies and forms ("About ¥30", "Rs 1,000",
 * "About ₹50 (Indians) / ₹1,100 (foreigners)"), so a sum is only shown when every priced stop
 * shares one currency and reads as a single amount. Ranges and dual prices — where a total would
 * be a guess — fall back to the count alone. Blank fees and "Covered" mean no separate charge and
 * are skipped.
 */
private fun ticketSummary(stops: List<StopEntity>): String? {
    if (stops.isEmpty()) return null
    val ticketed = stops.filter { !it.isFree }
    if (ticketed.isEmpty()) return "Every stop on this walk is free."

    val totals = LinkedHashMap<String, Double>()
    for (stop in ticketed) {
        val raw = stop.entranceFeeTry
        val amounts = FEE_NUMBER.findAll(raw)
            .mapNotNull { it.value.replace(",", "").toDoubleOrNull() }
            .toList()
        val currency = feeCurrency(raw)
        if (amounts.isEmpty() || currency == null) continue
        val value = if (amounts.size == 1) amounts.first() else amounts.average()
        totals[currency] = (totals[currency] ?: 0.0) + value
    }

    if (totals.isEmpty()) return "${ticketed.size} of ${stops.size} stops need a ticket."
    val total = totals.entries.joinToString(" + ") { (currency, value) ->
        formatMoney(currency, formatTotal(value))
    }
    return "${ticketed.size} of ${stops.size} stops need a ticket, ~$total in total."
}

private fun formatTotal(total: Double): String =
    if (total % 1.0 == 0.0) {
        String.format(Locale.US, "%,d", total.toLong())
    } else {
        String.format(Locale.US, "%.2f", total)
    }

/** Places the amount the way each currency is written: €25, ₹1,200, HK$88, but 25 ₼. */
private fun formatMoney(currency: String, amount: String): String = when (currency) {
    "₼", "TL" -> "$amount $currency"
    "MOP" -> "MOP $amount"
    else -> "$currency$amount"
}
