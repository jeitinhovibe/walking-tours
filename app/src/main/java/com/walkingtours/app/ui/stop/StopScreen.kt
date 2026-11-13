package com.walkingtours.app.ui.stop

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbsUpDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walkingtours.app.ServiceLocator
import com.walkingtours.app.ai.guideSignature
import com.walkingtours.app.ai.stripSpeechTags
import com.walkingtours.app.audio.NarrationState
import com.walkingtours.app.data.db.StopEntity
import com.walkingtours.app.tour.TourEntry
import com.walkingtours.app.tour.TourSessionManager
import com.walkingtours.app.ui.chat.ChatBottomSheet
import com.walkingtours.app.ui.components.BackSticker
import com.walkingtours.app.ui.components.InkButton
import com.walkingtours.app.ui.components.InkCard
import com.walkingtours.app.ui.components.InkIconTile
import com.walkingtours.app.ui.components.InkTextLink
import com.walkingtours.app.ui.components.Kicker
import com.walkingtours.app.ui.components.NarrationTransport
import com.walkingtours.app.ui.components.NoteCard
import com.walkingtours.app.ui.components.PaperBackground
import com.walkingtours.app.ui.components.SectionHeading
import com.walkingtours.app.ui.components.Stamp
import com.walkingtours.app.ui.components.StatusBarScrim
import com.walkingtours.app.ui.components.TopEdgeFade
import com.walkingtours.app.ui.components.StopHero
import com.walkingtours.app.ui.components.Transcript
import com.walkingtours.app.ui.components.VisitedCheck
import com.walkingtours.app.ui.theme.FootnotesMonoFont
import com.walkingtours.app.ui.theme.FootnotesTitleFont
import com.walkingtours.app.ui.theme.LocalFootnotesPalette
import com.walkingtours.app.ui.theme.cityAccent
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Height of the hero every page carries, and of the empty panel that holds its place before a page
 * is ready. One value, because the two have to match: the panel is there so that nothing moves when
 * the real thing arrives.
 */
private val HERO_HEIGHT = 304.dp

/**
 * The one screen for walking and for looking at a stop.
 *
 * A tour is one introduction plus N stops, and this screen shows them as a single pager: page zero is
 * the introduction when the tour has one, and the stops follow. The [entry] the screen arrived with
 * decides where it lands; after that the session's current stop is the only authority on which page
 * is showing. Swiping to a page makes that stop current, and a geofence arrival moves the page.
 */
@Composable
fun StopScreen(
    tourId: String,
    /** Which page to land on: the introduction, the first stop still to see, or one stop. */
    entry: TourEntry,
    onBack: () -> Unit,
    onOpenPersona: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val session = ServiceLocator.session
    val state by session.state.collectAsStateWithLifecycle()
    val narration by session.narration.progress.collectAsStateWithLifecycle()
    val p = LocalFootnotesPalette.current

    // The guide's personalised narration: the rewrite once it has loaded for this tour's guide, the
    // authored text until then. Collected so the page updates when a rewrite lands or the guide is
    // changed, and read through guideText so the audio and the transcript always agree.
    val guide = ServiceLocator.guide
    val guideNarrations by guide.narrations.collectAsStateWithLifecycle()
    val guideLoading by guide.loading.collectAsStateWithLifecycle()
    val persona by ServiceLocator.personaSettings.state.collectAsStateWithLifecycle()

    val aiSettings by ServiceLocator.aiSettings.state.collectAsStateWithLifecycle()
    val narrationSignature = guideSignature(
        persona.guide(tourId),
        persona.tone(tourId),
        aiSettings.narrationLanguage,
    )

    fun guideText(key: String, authored: String): String {
        val cached = guideNarrations["$tourId|$key"] ?: return authored
        return if (narrationSignature != null && cached.signature == narrationSignature) {
            stripSpeechTags(cached.text)
        } else {
            authored
        }
    }

    val headingProvider = ServiceLocator.headingProvider
    val compassHeading by headingProvider.headingDegrees.collectAsStateWithLifecycle()

    val locationTracker = ServiceLocator.locationTracker
    val trackerFix by locationTracker.lastLocation.collectAsStateWithLifecycle(initialValue = null)
    DisposableEffect(Unit) {
        headingProvider.start()
        locationTracker.acquire()
        onDispose {
            headingProvider.stop()
            locationTracker.release()
        }
    }

    LaunchedEffect(tourId, entry) {
        session.startTour(tourId, entry)
    }

    val mapLat = trackerFix?.latitude ?: state.userLat
    val mapLng = trackerFix?.longitude ?: state.userLng
    val mapAccuracy = trackerFix?.accuracy ?: state.userAccuracyMeters

    val isLive = state.isRunning && state.tourId == tourId

    val pageStops = if (isLive) state.stops else emptyList()
    val visitedIds = if (isLive) state.visitedStopIds else emptySet()

    val introPages = if (isLive && state.overviewText.isNotBlank()) 1 else 0
    fun stopIndexFor(page: Int) = page - introPages
    fun pageForStop(stopId: String): Int? =
        pageStops.indexOfFirst { it.id == stopId }.takeIf { it >= 0 }?.plus(introPages)

    val pagerState = rememberPagerState(pageCount = { pageStops.size + introPages })
    val scope = rememberCoroutineScope()

    val pageScrollStates = remember(pageStops.size, introPages) {
        List(pageStops.size + introPages) { ScrollState(0) }
    }

    var landingApplied by remember { mutableStateOf(false) }

    val settledStopIndex = if (landingApplied) {
        stopIndexFor(pagerState.currentPage)
    } else {
        state.currentStopId?.let { pageForStop(it)?.minus(introPages) } ?: -1
    }
    val settledStop = pageStops.getOrNull(settledStopIndex)

    LaunchedEffect(narrationSignature, settledStop?.id, isLive, introPages) {
        if (!isLive) return@LaunchedEffect
        val id = settledStop?.id
        if (id != null) {
            if (!guide.hasNarration(tourId, id)) session.prepareStopNarration(id)
        } else if (introPages > 0 && !guide.hasNarration(tourId, TourSessionManager.OVERVIEW_ID)) {
            session.prepareOverviewNarration()
        }
    }

    LaunchedEffect(state.currentStopId, isLive, pageStops, introPages) {
        if (!isLive || pageStops.isEmpty()) return@LaunchedEffect
        val current = state.currentStopId
        val target = when {
            current == null -> if (introPages > 0) 0 else return@LaunchedEffect
            else -> pageForStop(current) ?: return@LaunchedEffect
        }
        when {
            target == pagerState.currentPage -> landingApplied = true
            landingApplied -> pagerState.animateScrollToPage(target)
            else -> {
                pagerState.scrollToPage(target)
                landingApplied = true
            }
        }
    }

    LaunchedEffect(isLive, pageStops, introPages) {
        if (!isLive) return@LaunchedEffect
        snapshotFlow { pagerState.settledPage }
            .drop(1)
            .collect { page ->
                val index = stopIndexFor(page)
                if (index < 0) {
                    session.prepareOverviewNarration()
                    return@collect
                }
                val id = pageStops.getOrNull(index)?.id ?: return@collect
                if (session.state.value.currentStopId != id) {
                    if (ServiceLocator.aiSettings.current.autoPlayChapters) {
                        session.playStop(id)
                    } else {
                        session.focusStop(id)
                    }
                }
            }
    }

    fun moveToPage(target: Int) {
        if (target in 0 until pagerState.pageCount) scope.launch { pagerState.animateScrollToPage(target) }
    }

    val overviewPlaying = narration.stopId == TourSessionManager.OVERVIEW_ID
    val isPlaying = narration.state == NarrationState.PLAYING ||
        narration.state == NarrationState.PREPARING

    val leaveStop: () -> Unit = {
        if (isLive) session.endTour()
        onBack()
    }

    BackHandler(enabled = isLive) { leaveStop() }

    var showChat by remember { mutableStateOf(false) }
    var showStopList by remember { mutableStateOf(false) }
    var showTune by remember { mutableStateOf(false) }

    if (showChat) {
        ChatBottomSheet(
            tourId = tourId,
            stopId = settledStop?.id,
            onDismiss = { showChat = false },
            onOpenSettings = onOpenSettings,
        )
    }
    if (showStopList) {
        StopListSheet(
            stops = pageStops,
            visitedIds = visitedIds,
            currentStopId = state.currentStopId,
            nextStopId = state.nextStopId,
            introShown = introPages > 0,
            introSelected = settledStopIndex < 0,
            onPickIntro = {
                showStopList = false
                moveToPage(0)
            },
            onPick = { picked ->
                showStopList = false
                pageForStop(picked)?.let { moveToPage(it) }
            },
            onDismiss = { showStopList = false },
        )
    }

    val tunedStop = settledStop
    val tuningIntro = tunedStop == null && introPages > 0
    if (showTune && (tunedStop != null || tuningIntro)) {
        TuneStopSheet(
            initialTone = persona.tone(tourId).orEmpty(),
            onApply = { note ->
                showTune = false
                ServiceLocator.personaSettings.setTone(tourId, note)
                if (tunedStop != null) session.rerollStop(tunedStop.id) else session.rerollOverview()
            },
            onChangeGuide = {
                showTune = false
                onOpenPersona()
            },
            onDismiss = { showTune = false },
        )
    }

    fun playThisStop(id: String) = session.playStop(id)

    PaperBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                val currentPage = pagerState.currentPage
                val pageCount = pagerState.pageCount
                val canGoPrevious = currentPage > 0
                val canGoNext = currentPage < pageCount - 1
                fun goNext() {
                    if (!canGoNext) return
                    if (stopIndexFor(currentPage) < 0) {
                        session.skipIntroduction()
                        pageScrollStates.getOrNull(currentPage + 1)?.let { next ->
                            scope.launch { next.scrollTo(0) }
                        }
                    }
                    moveToPage(currentPage + 1)
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(p.paperRaised),
                ) {
                    Box(Modifier.fillMaxWidth().height(2.dp).background(p.ink))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircleControl(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous stop",
                            enabled = canGoPrevious,
                            onClick = { moveToPage(currentPage - 1) },
                        )
                        Surface(
                            onClick = { showChat = true },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = p.paperRaised,
                            contentColor = p.ink,
                            border = BorderStroke(1.5.dp, p.ink),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = p.tomato,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Ask anything",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                )
                            }
                        }
                        CircleControl(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next stop",
                            enabled = canGoNext,
                            onClick = { goNext() },
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (landingApplied) 1f else 0f),
                ) { page ->
                    val stopIndex = stopIndexFor(page)
                    val pageStop = pageStops.getOrNull(stopIndex)
                    val pageStopId = pageStop?.id
                    var transcriptOpen by remember(page, aiSettings.autoPlayChapters) {
                        mutableStateOf(!aiSettings.autoPlayChapters)
                    }
                    val isCurrentStop = pageStopId != null && pageStopId == state.currentStopId
                    val highlightApplies = isCurrentStop && narration.stopId == pageStopId
                    val previousPage = page - 1
                    val previousInRoute = pageStops.getOrNull(stopIndex - 1)
                    val nextInRoute = pageStops.getOrNull(stopIndex + 1)
                    val scrollState = pageScrollStates[page]
                    val accent = cityAccent(pageStop?.name ?: "briefing-$tourId")

                    Box(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                    ) {
                        Box(Modifier.fillMaxWidth().height(HERO_HEIGHT)) {
                            StopHero(
                                stop = pageStop,
                                stops = pageStops,
                                fallbackPhotoAsset = state.overviewImage,
                                visitedIds = visitedIds,
                                userLat = mapLat,
                                userLng = mapLng,
                                userHeading = compassHeading ?: state.userBearingDegrees,
                                userAccuracyMeters = mapAccuracy,
                                onOpenStop = { tapped -> pageForStop(tapped.id)?.let { moveToPage(it) } },
                                focusStops = if (stopIndex < 0) {
                                    null
                                } else {
                                    listOfNotNull(previousInRoute, pageStop, nextInRoute)
                                },
                                geofenceStop = if (stopIndex < 0) null else nextInRoute,
                                heroHeight = HERO_HEIGHT,
                                initialPage = if (isLive) 1 else 0,
                            )
                            // A paper-coloured fade behind the floating controls, so the status bar
                            // icons stay legible on a bright map in either theme.
                            TopEdgeFade(height = 124.dp)
                            BackSticker(
                                onBack = leaveStop,
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(start = 14.dp, top = 8.dp),
                            )
                            Surface(
                                onClick = { showStopList = true },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(14.dp)
                                    .size(42.dp),
                                shape = CircleShape,
                                color = p.paperRaised,
                                contentColor = p.ink,
                                border = BorderStroke(1.5.dp, p.ink),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        contentDescription = "All stops",
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                            }
                            Stamp(
                                text = pageStop?.let { "STOP ${it.order} / ${pageStops.size}" } ?: "THE BRIEFING",
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp),
                                container = p.paperRaised,
                                rotation = -2f,
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(2.dp).background(p.ink))

                        state.locationIssue?.let { issue ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = p.tomato,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = issue,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = p.tomato,
                                )
                            }
                        }

                        // ---- The briefing, on its own page ----------------------------------
                        if (stopIndex < 0) {
                            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Kicker("The briefing", color = p.tomato)
                                        Text(
                                            text = "Before you\nset off",
                                            style = MaterialTheme.typography.displaySmall.copy(
                                                fontFamily = FootnotesTitleFont,
                                                fontSize = 37.sp,
                                                lineHeight = 36.sp,
                                            ),
                                            color = p.ink,
                                        )
                                    }
                                    CircleControl(
                                        icon = Icons.Filled.ThumbsUpDown,
                                        contentDescription = "Tune your guide",
                                        enabled = true,
                                        onClick = { showTune = true },
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "A few minutes of context, a little local gossip, and then " +
                                        "the city takes over. Walking optional, listening mandatory.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = p.inkSoft,
                                )
                                Spacer(Modifier.height(16.dp))
                                if (TourSessionManager.OVERVIEW_ID in guideLoading) {
                                    LoadingContent()
                                } else {
                                    NarrationTransport(
                                        state = if (overviewPlaying) narration.state else NarrationState.IDLE,
                                        positionMs = if (overviewPlaying) narration.positionMs else 0L,
                                        durationMs = if (overviewPlaying) narration.durationMs else 0L,
                                        rate = narration.rate,
                                        onPlayPause = {
                                            if (overviewPlaying && isPlaying) {
                                                session.pauseNarration()
                                            } else if (overviewPlaying && narration.state == NarrationState.PAUSED) {
                                                session.resumeNarration()
                                            } else {
                                                session.playOverview()
                                            }
                                        },
                                        onRewind = { session.skipNarrationBy(-15_000) },
                                        onForward = { session.skipNarrationBy(15_000) },
                                        onPrevious = { moveToPage(page - 1) },
                                        onNext = { moveToPage(page + 1) },
                                        onSeekFraction = { fraction ->
                                            session.seekNarrationTo((fraction * narration.durationMs).toLong())
                                        },
                                        onRateChange = { session.setNarrationRate(it) },
                                        message = if (overviewPlaying) narration.message else null,
                                    )
                                }
                            }

                            if (TourSessionManager.OVERVIEW_ID !in guideLoading) {
                                TranscriptCard(
                                    text = guideText(TourSessionManager.OVERVIEW_ID, state.overviewText),
                                    open = transcriptOpen,
                                    onToggle = { transcriptOpen = !transcriptOpen },
                                    accent = p.teal,
                                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp),
                                )
                            }
                            Spacer(Modifier.height(120.dp))
                        }

                        // ---- This stop -------------------------------------------------------
                        pageStop?.let { current ->
                            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    VisitedCheck(
                                        checked = current.id in visitedIds,
                                        onCheckedChange = { session.setStopVisited(tourId, current.id, it) },
                                        number = current.order,
                                        size = 40.dp,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Kicker(current.category, color = accent.background)
                                        Text(
                                            text = current.name,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontFamily = FootnotesTitleFont,
                                                fontSize = 29.sp,
                                                lineHeight = 31.sp,
                                            ),
                                            color = p.ink,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    CircleControl(
                                        icon = Icons.Filled.ThumbsUpDown,
                                        contentDescription = "Tune your guide",
                                        enabled = true,
                                        onClick = { showTune = true },
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = when {
                                        current.id in visitedIds -> "Been here. Ticked off. Still worth a look."
                                        isCurrentStop -> "You're here. Ears on."
                                        else -> "Ahead of you. No rush."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = p.inkSoft,
                                )
                                Spacer(Modifier.height(14.dp))
                                if (current.id in guideLoading) {
                                    LoadingContent()
                                } else {
                                    NarrationTransport(
                                        state = if (highlightApplies) narration.state else NarrationState.IDLE,
                                        positionMs = if (highlightApplies) narration.positionMs else 0L,
                                        durationMs = if (highlightApplies) narration.durationMs else 0L,
                                        rate = narration.rate,
                                        onPlayPause = {
                                            if (isPlaying && highlightApplies) {
                                                session.pauseNarration()
                                            } else if (highlightApplies && narration.state == NarrationState.PAUSED) {
                                                session.resumeNarration()
                                            } else {
                                                playThisStop(current.id)
                                            }
                                        },
                                        onRewind = { session.skipNarrationBy(-15_000) },
                                        onForward = { session.skipNarrationBy(15_000) },
                                        onPrevious = { moveToPage(previousPage) },
                                        onNext = { moveToPage(page + 1) },
                                        onSeekFraction = { fraction ->
                                            session.seekNarrationTo((fraction * narration.durationMs).toLong())
                                        },
                                        onRateChange = { session.setNarrationRate(it) },
                                        message = if (highlightApplies) narration.message else null,
                                    )
                                }
                            }

                            if (current.id !in guideLoading) {
                                TranscriptCard(
                                    text = guideText(current.id, current.narration),
                                    open = transcriptOpen,
                                    onToggle = { transcriptOpen = !transcriptOpen },
                                    accent = accent.background,
                                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp),
                                )
                            }

                            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 26.dp)) {
                                SectionHeading(
                                    kicker = "Field notes",
                                    title = "The practical bits",
                                    kickerColor = p.cobalt,
                                )
                                Spacer(Modifier.height(10.dp))
                                NoteCard(accent = p.cobalt) {
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        if (current.entranceFeeTry.isNotBlank()) {
                                            PracticalRow(
                                                icon = Icons.Filled.ConfirmationNumber,
                                                label = "Entrance",
                                                value = buildString {
                                                    append(current.entranceFeeTry)
                                                    if (current.entranceFeeNote.isNotBlank()) {
                                                        append("\n${current.entranceFeeNote}")
                                                    }
                                                },
                                                accent = p.tomato,
                                            )
                                        }
                                        PracticalRow(
                                            icon = Icons.Filled.Schedule,
                                            label = "Open",
                                            value = current.openingHours,
                                            accent = p.mustard,
                                        )
                                        PracticalRow(
                                            icon = Icons.Filled.Place,
                                            label = "Give it",
                                            value = "${current.suggestedMinutes} minutes",
                                            accent = p.teal,
                                        )
                                        PracticalRow(
                                            icon = Icons.Filled.Info,
                                            label = "Access",
                                            value = current.accessibility,
                                            accent = p.cobalt,
                                        )
                                    }
                                }
                            }

                            current.insiderTip.takeIf { it.isNotBlank() }?.let { tip ->
                                Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp)) {
                                    InkCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        container = p.mustard,
                                        contentColor = Color(0xFF221A0E),
                                        shape = RoundedCornerShape(16.dp),
                                        shadow = 4.dp,
                                    ) {
                                        Column(Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.Star,
                                                    contentDescription = null,
                                                    tint = Color(0xFF221A0E),
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = "PSST. INSIDER TIP.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF221A0E),
                                                )
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = tip,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF221A0E),
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(120.dp))
                        }
                    }
                    StatusBarScrim(
                        visible = scrollState.value > 24,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                    }
                }

                if (!isLive || pageStops.isEmpty() || !landingApplied) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        Box(Modifier.fillMaxWidth().height(HERO_HEIGHT))
                    }
                }
            }
        }
    }
}

/**
 * The circular utility control used for tuning, previous and next. The same object everywhere, so
 * the page has one kind of small button and the walker's thumb learns it once.
 */
@Composable
private fun CircleControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 42.dp,
) {
    val p = LocalFootnotesPalette.current
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(size).alpha(if (enabled) 1f else 0.35f),
        shape = CircleShape,
        color = p.paperRaised,
        contentColor = p.ink,
        border = BorderStroke(1.5.dp, p.ink),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(size * 0.45f))
        }
    }
}

@Composable
private fun TranscriptCard(
    text: String,
    open: Boolean,
    onToggle: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val p = LocalFootnotesPalette.current
    NoteCard(
        modifier = modifier,
        accent = accent,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "THE SCRIPT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FootnotesMonoFont,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = p.inkSoft,
                    )
                    Text(
                        text = if (open) "Read along" else "Words, if you want them",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = p.ink,
                    )
                }
                Icon(
                    imageVector = if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (open) "Hide transcript" else "Show transcript",
                    tint = p.inkSoft,
                )
            }
            if (open) {
                Spacer(Modifier.height(10.dp))
                Transcript(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(color = p.ink),
                )
            }
        }
    }
}

/** One line of practical info: a printed icon, a mono label, and the useful answer. */
@Composable
private fun PracticalRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color,
) {
    if (value.isBlank()) return
    val p = LocalFootnotesPalette.current
    Row(verticalAlignment = Alignment.Top) {
        InkIconTile(
            icon = icon,
            background = accent,
            tint = if (accent == p.mustard) Color(0xFF221A0E) else p.paperRaised,
            size = 34.dp,
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = p.inkSoft,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = p.ink,
            )
        }
    }
}

/**
 * Guide tuning: change the guide, or leave quick feedback that the guide folds into every
 * rewrite for this tour. Tour-wide, not per-stop — the title says so.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TuneStopSheet(
    initialTone: String,
    onApply: (String) -> Unit,
    onChangeGuide: () -> Unit,
    onDismiss: () -> Unit,
) {
    val p = LocalFootnotesPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var note by remember { mutableStateOf(initialTone) }
    val quick = listOf(
        "More direct, fewer jokes",
        "More facts, fewer asides",
        "More detail",
        "Shorter",
        "More local flavour",
        "More dramatic",
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = p.paper,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Kicker("Tune the guide", color = p.tomato)
            Text(
                text = "Less of that,\nmore of this.",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = FootnotesTitleFont,
                    fontSize = 34.sp,
                    lineHeight = 33.sp,
                ),
                color = p.ink,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tell your guide what to change. The walk gets rewritten in the new way, and " +
                    "the note sticks for the rest of the tour.",
                style = MaterialTheme.typography.bodyMedium,
                color = p.inkSoft,
            )
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                quick.forEach { chip ->
                    Surface(
                        onClick = { note = if (note.isBlank()) chip else "$note; $chip" },
                        shape = RoundedCornerShape(50),
                        color = p.paperRaised,
                        contentColor = p.ink,
                        border = BorderStroke(1.2.dp, p.ink),
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Anything else?") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InkTextLink(
                    text = "CHANGE THE GUIDE",
                    onClick = onChangeGuide,
                    modifier = Modifier.weight(1f),
                )
                InkButton(
                    text = if (note.isBlank()) "Clear" else "Apply",
                    onClick = { onApply(note) },
                    enabled = note.isNotBlank() || initialTone.isNotBlank(),
                    modifier = Modifier.width(128.dp),
                    height = 48.dp,
                    shadow = 3.dp,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * Shown where a stop's transport and transcript go while the guide's version of it is being
 * written. Deliberately per stop: only the narration the walker is waiting on shows a loading
 * state, never the whole tour at once.
 */
@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Channeling your guide…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The jump list, so any stop can be reached from anywhere. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopListSheet(
    stops: List<StopEntity>,
    visitedIds: Set<String>,
    currentStopId: String?,
    nextStopId: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    introShown: Boolean = false,
    introSelected: Boolean = false,
    onPickIntro: () -> Unit = {},
) {
    val p = LocalFootnotesPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = p.paper,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Kicker("Jump ahead", color = p.tomato)
                Text(
                    text = "The whole route",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FootnotesTitleFont),
                    color = p.ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "No one is keeping score. Start where you like.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.inkSoft,
                )
                Spacer(Modifier.height(12.dp))
            }
            LazyColumn {
                if (introShown) {
                    item {
                        RouteRow(
                            number = null,
                            title = "The briefing",
                            subtitle = "Before you set off",
                            markerIcon = Icons.Filled.PlayArrow,
                            container = if (introSelected) p.mustard else p.paperRaised,
                            foreground = Color(0xFF221A0E),
                            stamp = if (introSelected) "HERE" else null,
                            onClick = onPickIntro,
                        )
                    }
                }
                items(stops.size) { i ->
                    val stop = stops[i]
                    val current = stop.id == currentStopId
                    val next = stop.id == nextStopId
                    RouteRow(
                        number = stop.order,
                        title = stop.name,
                        subtitle = stop.category,
                        markerIcon = null,
                        container = when {
                            current -> p.tomato
                            next -> p.mustard
                            else -> p.paperRaised
                        },
                        foreground = when {
                            current -> p.paperRaised
                            next -> Color(0xFF221A0E)
                            else -> p.ink
                        },
                        stamp = when {
                            stop.id in visitedIds -> "DONE"
                            current -> "HERE"
                            next -> "NEXT"
                            else -> null
                        },
                        onClick = { onPick(stop.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteRow(
    number: Int?,
    title: String,
    subtitle: String,
    markerIcon: androidx.compose.ui.graphics.vector.ImageVector?,
    container: Color,
    foreground: Color,
    stamp: String?,
    onClick: () -> Unit,
) {
    val p = LocalFootnotesPalette.current
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp),
        shape = RoundedCornerShape(14.dp),
        color = container,
        contentColor = foreground,
        border = BorderStroke(1.2.dp, p.ink),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(foreground.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (markerIcon != null) {
                    Icon(markerIcon, contentDescription = null, tint = foreground, modifier = Modifier.size(18.dp))
                } else {
                    Text(
                        text = number?.toString().orEmpty(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FootnotesMonoFont,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = foreground,
                    )
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = foreground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = foreground.copy(alpha = 0.72f),
                    maxLines = 1,
                )
            }
            if (stamp != null) {
                Stamp(
                    text = stamp,
                    container = foreground.copy(alpha = 0.16f),
                    contentColor = foreground,
                )
            }
        }
    }
}
