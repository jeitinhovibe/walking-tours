package com.walkingtours.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walkingtours.app.ServiceLocator
import com.walkingtours.app.ai.MODEL_PROVIDERS
import com.walkingtours.app.ai.MODEL_PROVIDER_OPENCODE
import com.walkingtours.app.ai.THEME_MODES
import com.walkingtours.app.ai.TTS_ENGINES
import com.walkingtours.app.ai.TTS_ENGINE_ELEVENLABS
import com.walkingtours.app.ui.components.BackSticker
import com.walkingtours.app.ui.components.Kicker
import com.walkingtours.app.ui.components.NoteCard
import com.walkingtours.app.ui.components.PaperBackground
import com.walkingtours.app.ui.components.Sticker
import com.walkingtours.app.ui.theme.FootnotesMonoFont
import com.walkingtours.app.ui.theme.FootnotesTitleFont
import com.walkingtours.app.ui.theme.LocalFootnotesPalette
import kotlinx.coroutines.launch

/**
 * Settings for the optional cloud services, and the few choices that change the walk itself.
 *
 * Nothing here is required: with no key at all the app still runs a complete tour on the phone's own
 * voice. The page is written like the notes pinned inside a cupboard door, not a corporate admin
 * panel.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val settings = ServiceLocator.aiSettings
    val state by settings.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val p = LocalFootnotesPalette.current

    var apiStatus by remember { mutableStateOf<String?>(null) }
    var apiOk by remember { mutableStateOf<Boolean?>(null) }
    val openCode = state.modelProvider == MODEL_PROVIDER_OPENCODE
    val providerName = if (openCode) "OpenCode Go" else "Gemini"

    fun checkApi() {
        if (!state.modelProviderReady) {
            apiOk = false
            apiStatus = if (openCode) {
                "No OpenCode Go key in this build"
            } else {
                "No Gemini API key in this build"
            }
            return
        }
        apiOk = null
        apiStatus = "Checking…"
        scope.launch {
            try {
                val client = ServiceLocator.modelClient
                val models = client.availableModels()
                val wanted = client.activeModel
                if (models.none { it == wanted }) {
                    apiOk = false
                    apiStatus = "$wanted is not available on this key"
                } else {
                    apiOk = true
                    apiStatus = if (openCode) {
                        "OpenCode Go key accepted · $wanted"
                    } else {
                        "Gemini API working · $wanted"
                    }
                }
            } catch (e: Exception) {
                apiOk = false
                apiStatus = "$providerName not working: ${e.message}"
            }
        }
    }

    LaunchedEffect(state.modelProvider) { checkApi() }

    PaperBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BackSticker(onBack = onBack)
                        Spacer(Modifier.width(12.dp))
                        Kicker("The control room", color = p.cobalt)
                        Spacer(Modifier.weight(1f))
                        Sticker("Handle with care", container = p.mustard, rotation = 3f)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Knobs, dials\nand voices.",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FootnotesTitleFont,
                            fontSize = 43.sp,
                            lineHeight = 41.sp,
                        ),
                        color = p.ink,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Nothing here is required. With no keys at all, the whole walk still runs " +
                            "on the voice already living in your phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.inkSoft,
                    )
                }
            }

            // ---------------------------------------------------------- models
            item {
                SettingsCard(title = "The brains", accent = p.cobalt) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Who writes the words",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = p.ink,
                            )
                            Text(
                                text = if (openCode) {
                                    "Guides, rewrites, detours and chat run on your OpenCode Go " +
                                        "plan, using DeepSeek V4.1 Flash."
                                } else {
                                    "Guides, rewrites, detours and chat run on Google Gemini, " +
                                        "billed per token."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = p.inkSoft,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            val label = MODEL_PROVIDERS.firstOrNull { it.first == state.modelProvider }?.second
                                ?: state.modelProvider
                            PillButton(text = label) { expanded = true }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                MODEL_PROVIDERS.forEach { (id, text) ->
                                    DropdownMenuItem(
                                        text = { Text(text) },
                                        onClick = {
                                            expanded = false
                                            settings.update { it.copy(modelProvider = id) }
                                        },
                                    )
                                }
                            }
                        }
                    }
                    StatusLine(
                        text = apiStatus ?: "Checking…",
                        isError = apiOk == false,
                        isSuccess = apiOk == true,
                    )
                    if (openCode) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "DeepSeek V4.1 Flash is included in the Go plan, accepts photos, " +
                                "and its provider keeps no data and does not train on prompts.",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.inkSoft,
                        )
                    }
                }
            }

            // ---------------------------------------------------------- appearance
            item {
                SettingsCard(title = "The look", accent = p.plum) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Light or dark",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = p.ink,
                            )
                            Text(
                                text = "The paper gets a night mode. The opinions stay.",
                                style = MaterialTheme.typography.bodySmall,
                                color = p.inkSoft,
                            )
                        }
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            val label = THEME_MODES.firstOrNull { it.first == state.themeMode }?.second
                                ?: state.themeMode
                            PillButton(text = label) { expanded = true }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                THEME_MODES.forEach { (tag, text) ->
                                    DropdownMenuItem(
                                        text = { Text(text) },
                                        onClick = {
                                            expanded = false
                                            settings.update { it.copy(themeMode = tag) }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---------------------------------------------------------- voice
            item {
                SettingsCard(title = "The voice", accent = p.teal) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Use a cloud voice",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = p.ink,
                            )
                            Text(
                                text = when {
                                    state.ttsEngine == TTS_ENGINE_ELEVENLABS && !state.hasElevenLabsKey ->
                                        "No ElevenLabs key in this build, so the phone's own voice is used."
                                    state.ttsEngine != TTS_ENGINE_ELEVENLABS && !state.hasGeminiKey ->
                                        "No Gemini key in this build, so the phone's own voice is used."
                                    state.ttsEngine == TTS_ENGINE_ELEVENLABS ->
                                        "ElevenLabs reads the guide in a voice cast for that character. " +
                                            "Billed per character."
                                    else ->
                                        "Gemini reads the guide. Billed per request after a free allowance."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = p.inkSoft,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Switch(
                            checked = state.useCloudVoice,
                            enabled = state.cloudEngineReady,
                            onCheckedChange = { on -> settings.update { it.copy(useCloudVoice = on) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = p.paperRaised,
                                checkedTrackColor = p.teal,
                                uncheckedThumbColor = p.paperRaised,
                                uncheckedTrackColor = p.ink.copy(alpha = 0.35f),
                                uncheckedBorderColor = p.ink,
                            ),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Which engine",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = p.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            val label = TTS_ENGINES.firstOrNull { it.first == state.ttsEngine }?.second
                                ?: state.ttsEngine
                            PillButton(text = label) { expanded = true }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                TTS_ENGINES.forEach { (id, text) ->
                                    DropdownMenuItem(
                                        text = { Text(text) },
                                        onClick = {
                                            expanded = false
                                            val ready = if (id == TTS_ENGINE_ELEVENLABS) {
                                                state.hasElevenLabsKey
                                            } else {
                                                state.hasGeminiKey
                                            }
                                            settings.update {
                                                it.copy(
                                                    ttsEngine = id,
                                                    useCloudVoice = it.useCloudVoice && ready,
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.ttsEngine == TTS_ENGINE_ELEVENLABS) {
                        StatusLine(
                            text = if (state.hasElevenLabsKey) {
                                "ElevenLabs key configured. Each guide speaks in its own cast voice; " +
                                    "audio tags like [laughs] and [sighs] are performed."
                            } else {
                                "No ElevenLabs key in this build. Add elevenlabs.apiKey to local.properties."
                            },
                            isError = !state.hasElevenLabsKey,
                            isSuccess = state.hasElevenLabsKey,
                        )
                    } else {
                        StatusLine(
                            text = if (state.hasGeminiKey) {
                                "Gemini TTS key configured. This is separate from the model provider above."
                            } else {
                                "No Gemini key in this build, so the phone's own voice is used."
                            },
                            isError = !state.hasGeminiKey,
                            isSuccess = state.hasGeminiKey,
                        )
                    }
                }
            }

            // ---------------------------------------------------------- playback
            item {
                SettingsCard(title = "The walk", accent = p.mustard) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Play chapters by themselves",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = p.ink,
                            )
                            Text(
                                text = if (state.autoPlayChapters) {
                                    "The introduction plays when the tour starts, and each stop's " +
                                        "narration starts as soon as you arrive."
                                } else {
                                    "Nothing plays by itself. The introduction and each stop wait for " +
                                        "you to press play."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = p.inkSoft,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Switch(
                            checked = state.autoPlayChapters,
                            onCheckedChange = { on -> settings.update { it.copy(autoPlayChapters = on) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = p.paperRaised,
                                checkedTrackColor = p.tomato,
                                uncheckedThumbColor = p.paperRaised,
                                uncheckedTrackColor = p.ink.copy(alpha = 0.35f),
                                uncheckedBorderColor = p.ink,
                            ),
                        )
                    }
                }
            }

            item {
                Column(Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "That's the whole control room. Mind the dust.",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FootnotesTitleFont,
                            fontSize = 22.sp,
                        ),
                        color = p.ink,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "KEYS LIVE IN local.properties · NEVER IN A SCREENSHOT",
                        style = MaterialTheme.typography.labelSmall,
                        color = p.inkSoft,
                    )
                }
            }
        }
    }
}

/** The little capsule button every dropdown uses. */
@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
) {
    val p = LocalFootnotesPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = p.paperRaised,
        contentColor = p.ink,
        border = BorderStroke(1.2.dp, p.ink),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    accent: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit,
) {
    val p = LocalFootnotesPalette.current
    NoteCard(accent = accent) {
        Column {
            Kicker(title, color = accent)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun StatusLine(text: String, isError: Boolean = false, isSuccess: Boolean = false) {
    val p = LocalFootnotesPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = when {
                isSuccess -> Icons.Filled.Check
                isError -> Icons.Filled.Warning
                else -> Icons.Filled.Info
            },
            contentDescription = null,
            tint = when {
                isSuccess -> p.teal
                isError -> p.tomato
                else -> p.inkSoft
            },
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = when {
                isSuccess -> p.teal
                isError -> p.tomato
                else -> p.inkSoft
            },
        )
    }
}
