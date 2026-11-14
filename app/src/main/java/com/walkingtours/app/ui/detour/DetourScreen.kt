package com.walkingtours.app.ui.detour

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walkingtours.app.ServiceLocator
import com.walkingtours.app.ai.DetourPhoto
import com.walkingtours.app.ai.DetourTopic
import com.walkingtours.app.ai.fetchDetourPhoto
import com.walkingtours.app.audio.NarrationState
import com.walkingtours.app.tour.detourUtteranceId
import com.walkingtours.app.ui.chat.AskBar
import com.walkingtours.app.ui.chat.ChatBottomSheet
import com.walkingtours.app.ui.components.BackSticker
import com.walkingtours.app.ui.components.InkButton
import com.walkingtours.app.ui.components.InkCard
import com.walkingtours.app.ui.components.Kicker
import com.walkingtours.app.ui.components.NarrationTransport
import com.walkingtours.app.ui.components.NoteCard
import com.walkingtours.app.ui.components.PaperBackground
import com.walkingtours.app.ui.components.Sparkle
import com.walkingtours.app.ui.components.StatusBarScrim
import com.walkingtours.app.ui.components.TopEdgeFade
import com.walkingtours.app.ui.components.Sticker
import com.walkingtours.app.ui.components.Sunburst
import com.walkingtours.app.ui.components.Transcript
import com.walkingtours.app.ui.theme.FootnotesMonoFont
import com.walkingtours.app.ui.theme.FootnotesTitleFont
import com.walkingtours.app.ui.theme.LocalFootnotesPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A detour: one generated deep-dive that is not a stop.
 *
 * It borrows the stop page's shape — photograph, player, collapsible transcript — but there is
 * no map (a detour is about a country, not a coordinate), no visitor information, and no
 * previous/next: a detour is a single chapter with nowhere to step to. The words come from the
 * tour's guide persona, exactly like a stop's; leaving the page stops the audio only if the detour
 * is still the thing playing, and never touches the walk itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetourScreen(
    tourId: String,
    topicId: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val session = ServiceLocator.session
    val guide = ServiceLocator.guide
    val narration by session.narration.progress.collectAsStateWithLifecycle()
    val loading by guide.loading.collectAsStateWithLifecycle()
    val detourError by session.detourError.collectAsStateWithLifecycle()
    val p = LocalFootnotesPalette.current

    var topics by remember { mutableStateOf<List<DetourTopic>?>(null) }
    LaunchedEffect(tourId) {
        topics = guide.suggestDetourTopics(tourId)
    }
    val topic = topics?.find { it.id == topicId }

    val signature = guide.detourSignature(tourId)
    val detourKey = "detour|$topicId"
    val text = guide.detourText(tourId, topicId)
    val isLoading = detourKey in loading
    val utteranceId = detourUtteranceId(topicId)
    val highlightApplies = narration.stopId == utteranceId
    val isPlaying = narration.state == NarrationState.PLAYING ||
        narration.state == NarrationState.PREPARING

    LaunchedEffect(topic?.id, signature, text, isLoading, detourError) {
        val resolved = topic ?: return@LaunchedEffect
        if (text == null && !isLoading && detourError == null) {
            session.playDetour(tourId, resolved)
        }
    }

    val context = LocalContext.current
    var photo by remember(topicId) { mutableStateOf<DetourPhoto?>(null) }
    var photoDone by remember(topicId) { mutableStateOf(false) }
    LaunchedEffect(topic) {
        val resolved = topic ?: return@LaunchedEffect
        photo = fetchDetourPhoto(context, resolved.id, resolved.imageQuery)
        photoDone = true
    }

    var transcriptOpen by remember { mutableStateOf(true) }
    var showChat by remember { mutableStateOf(false) }
    if (showChat) {
        ChatBottomSheet(
            tourId = tourId,
            stopId = null,
            onDismiss = { showChat = false },
            onOpenSettings = onOpenSettings,
        )
    }

    fun leave() {
        if (session.narration.progress.value.stopId == utteranceId) session.stopNarration()
        onBack()
    }
    BackHandler { leave() }

    PaperBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                AskBar(
                    placeholder = "Ask about this detour",
                    onClick = { showChat = true },
                )
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
                Box(Modifier.fillMaxWidth().height(260.dp)) {
                    DetourImage(
                        photo = photo,
                        contentDescription = topic?.title,
                        modifier = Modifier.fillMaxSize(),
                    )
                    TopEdgeFade(height = 130.dp)
                    BackSticker(
                        onBack = ::leave,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 14.dp, top = 8.dp),
                    )
                    Sticker(
                        text = "Detour",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(end = 14.dp, top = 12.dp),
                        container = p.mustard,
                        rotation = 3f,
                    )
                    Sticker(
                        text = "No walking required",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        container = p.paperRaised,
                        contentColor = p.ink,
                        rotation = -1.5f,
                    )
                }
                Box(Modifier.fillMaxWidth().height(2.dp).background(p.ink))

                Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Kicker("A necessary detour", color = p.tomato)
                        Spacer(Modifier.weight(1f))
                        Sparkle(Modifier.size(18.dp), color = p.teal)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = topic?.title ?: "Loading detour…",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FootnotesTitleFont,
                            fontSize = 36.sp,
                            lineHeight = 37.sp,
                        ),
                        color = p.ink,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    topic?.blurb?.takeIf { it.isNotBlank() }?.let { blurb ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = blurb,
                            style = MaterialTheme.typography.bodyMedium,
                            color = p.inkSoft,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    when {
                        topic == null -> LoadingDetour()
                        isLoading -> LoadingDetour()
                        detourError != null -> DetourError(
                            message = detourError.orEmpty(),
                            onRetry = { session.playDetour(tourId, topic) },
                        )
                        else -> NarrationTransport(
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
                                    session.playDetour(tourId, topic)
                                }
                            },
                            onRewind = { session.skipNarrationBy(-15_000) },
                            onForward = { session.skipNarrationBy(15_000) },
                            onPrevious = {},
                            onNext = {},
                            showPrevNext = false,
                            onSeekFraction = { fraction ->
                                session.seekNarrationTo((fraction * narration.durationMs).toLong())
                            },
                            onRateChange = { session.setNarrationRate(it) },
                            message = if (highlightApplies) narration.message else null,
                        )
                    }
                }

                if (!isLoading && detourError == null && text != null) {
                    NoteCard(
                        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp),
                        accent = p.teal,
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { transcriptOpen = !transcriptOpen },
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
                                        text = if (transcriptOpen) "Read along" else "The full story",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = p.ink,
                                    )
                                }
                                Icon(
                                    imageVector = if (transcriptOpen) {
                                        Icons.Filled.ExpandLess
                                    } else {
                                        Icons.Filled.ExpandMore
                                    },
                                    contentDescription = if (transcriptOpen) {
                                        "Hide transcript"
                                    } else {
                                        "Show transcript"
                                    },
                                    tint = p.inkSoft,
                                )
                            }
                            if (transcriptOpen) {
                                Spacer(Modifier.height(10.dp))
                                Transcript(
                                    text = text,
                                    style = MaterialTheme.typography.bodyLarge.copy(color = p.ink),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
            StatusBarScrim(
                visible = scrollState.value > 24,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            }
        }
    }
}

/** The detour's illustration: the fetched photograph, or a printed placeholder of the same size. */
@Composable
private fun DetourImage(
    photo: DetourPhoto?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    height: Dp = 260.dp,
) {
    val p = LocalFootnotesPalette.current
    var bitmap by remember(photo) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photo) {
        val file = photo?.file
        bitmap = if (file == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, bounds)
                    var sample = 1
                    while (bounds.outWidth / (sample * 2) >= 1080 && bounds.outHeight / (sample * 2) >= 1080) {
                        sample *= 2
                    }
                    BitmapFactory.decodeFile(
                        file.absolutePath,
                        BitmapFactory.Options().apply { inSampleSize = sample },
                    )?.asImageBitmap()
                }.getOrNull()
            }
        }
    }

    Box(modifier.background(p.paperDeep)) {
        val current = bitmap
        if (current != null) {
            Image(
                bitmap = current,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // A designed placeholder rather than an empty panel: the photograph comes from the
            // network and can take a moment, and a blank rectangle reads as a bug. Stripes, a
            // star and a little honesty make the wait part of the zine.
            Box(Modifier.fillMaxSize().background(p.mustard.copy(alpha = 0.5f))) {
                Canvas(Modifier.fillMaxSize()) {
                    val step = 22.dp.toPx()
                    val paint = p.ink.copy(alpha = 0.06f)
                    var x = -size.height
                    while (x < size.width) {
                        drawLine(
                            color = paint,
                            start = Offset(x, 0f),
                            end = Offset(x + size.height, size.height),
                            strokeWidth = 8.dp.toPx(),
                        )
                        x += step
                    }
                }
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Sunburst(Modifier.size(54.dp), color = p.ink.copy(alpha = 0.55f), rays = 12)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "PICTURE ON ITS WAY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FootnotesMonoFont,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = p.ink.copy(alpha = 0.7f),
                    )
                    Text(
                        text = "Use your imagination. It has better lighting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.ink.copy(alpha = 0.62f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingDetour() {
    val p = LocalFootnotesPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = p.tomato,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Digging up the good stuff…",
            style = MaterialTheme.typography.bodyMedium,
            color = p.inkSoft,
        )
    }
}

@Composable
private fun DetourError(message: String, onRetry: () -> Unit) {
    val p = LocalFootnotesPalette.current
    InkCard(
        modifier = Modifier.fillMaxWidth(),
        container = p.tomato,
        contentColor = p.paperRaised,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "That detour took a wrong turn.",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FootnotesTitleFont),
                color = p.paperRaised,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = p.paperRaised.copy(alpha = 0.88f),
            )
            Spacer(Modifier.height(10.dp))
            InkButton(
                text = "Try again",
                onClick = onRetry,
                icon = Icons.Filled.Refresh,
                container = p.paperRaised,
                contentColor = p.ink,
                height = 44.dp,
                shadow = 3.dp,
            )
        }
    }
}

/**
 * The topic picker: up to five detours, or the reason there are none. A missing Gemini key is a
 * different state from a failed generation — one needs Settings, the other a retry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetourTopicsSheet(
    tourId: String,
    onPick: (DetourTopic) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val guide = ServiceLocator.guide
    val aiSettings by ServiceLocator.aiSettings.state.collectAsStateWithLifecycle()
    val p = LocalFootnotesPalette.current
    var topics by remember { mutableStateOf<List<DetourTopic>?>(null) }
    var attempted by remember { mutableStateOf(false) }
    var retryKey by remember { mutableStateOf(0) }

    LaunchedEffect(tourId, retryKey) {
        topics = null
        attempted = false
        topics = guide.suggestDetourTopics(tourId)
        attempted = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = p.paper,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Kicker("Take a detour", color = p.tomato)
            Text(
                text = "Where are\nwe going?",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = FootnotesTitleFont,
                    fontSize = 35.sp,
                    lineHeight = 34.sp,
                ),
                color = p.ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Country-level context behind this walk — history, politics, culture and more, " +
                    "told by your guide. Not a single step required.",
                style = MaterialTheme.typography.bodyMedium,
                color = p.inkSoft,
            )
            Spacer(Modifier.height(14.dp))

            when {
                topics == null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = p.tomato,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Dreaming up detours…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = p.inkSoft,
                        )
                    }
                }

                topics!!.isEmpty() && !aiSettings.hasGeminiKey -> {
                    Text(
                        text = "Detours need a Gemini API key — they are generated, not written in advance. " +
                            "Add one in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.inkSoft,
                    )
                    Spacer(Modifier.height(12.dp))
                    InkButton(
                        text = "Open Settings",
                        onClick = onOpenSettings,
                        container = p.ink,
                        contentColor = p.paperRaised,
                        height = 48.dp,
                    )
                }

                topics!!.isEmpty() -> {
                    Text(
                        text = "Couldn't dream up detours right now. Check your connection and try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.inkSoft,
                    )
                    Spacer(Modifier.height(12.dp))
                    InkButton(
                        text = "Try again",
                        onClick = { retryKey++ },
                        container = p.ink,
                        contentColor = p.paperRaised,
                        height = 48.dp,
                    )
                }

                else -> {
                    topics!!.forEachIndexed { index, topic ->
                        InkCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            onClick = { onPick(topic) },
                            shape = RoundedCornerShape(16.dp),
                            shadow = 3.dp,
                            borderWidth = 1.5.dp,
                        ) {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .background(
                                            if (index % 2 == 0) p.mustard else p.teal,
                                            RoundedCornerShape(9.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = (index + 1).toString(),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FootnotesMonoFont,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = if (index % 2 == 0) Color(0xFF221A0E) else p.paperRaised,
                                    )
                                }
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = topic.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = p.ink,
                                    )
                                    if (topic.blurb.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = topic.blurb,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = p.inkSoft,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
