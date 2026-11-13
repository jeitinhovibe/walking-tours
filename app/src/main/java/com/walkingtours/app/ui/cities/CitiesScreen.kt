package com.walkingtours.app.ui.cities

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walkingtours.app.ServiceLocator
import com.walkingtours.app.data.db.TourEntity
import com.walkingtours.app.ui.components.AssetPhoto
import com.walkingtours.app.ui.components.DottedRule
import com.walkingtours.app.ui.components.InkCard
import com.walkingtours.app.ui.components.Kicker
import com.walkingtours.app.ui.components.PaperBackground
import com.walkingtours.app.ui.components.Sparkle
import com.walkingtours.app.ui.components.Stamp
import com.walkingtours.app.ui.components.Sticker
import com.walkingtours.app.ui.components.Sunburst
import com.walkingtours.app.ui.theme.FootnotesBrand
import com.walkingtours.app.ui.theme.FootnotesMonoFont
import com.walkingtours.app.ui.theme.FootnotesTitleFont
import com.walkingtours.app.ui.theme.LocalFootnotesPalette
import com.walkingtours.app.ui.theme.cityAccent
import com.walkingtours.app.util.CountryFlags
import com.walkingtours.app.util.Formatters
import java.time.LocalDate

/**
 * In-memory UI state for the home list. The same [LazyGridState] object is reused on every
 * composition, so leaving for a city and coming back lands exactly where the walker left off —
 * there is no save/restore round-trip to lose the position.
 */
private object CitiesMemory {
    val gridState = LazyGridState()
    var country: String? by mutableStateOf(null)

    /**
     * Last full tour list. Re-collecting the Room flow starts from [initialValue], so without
     * this every return would flash an empty list — and an empty-then-full flash is exactly
     * what drops the list position. Seeding from the cache means the list never goes empty.
     */
    var tours: List<TourEntity> = emptyList()
}

/**
 * The cover of the zine: a masthead, a walk of the day, and the whole atlas in postcards.
 *
 * Everything here is one scrolling grid rather than a fixed top bar, because the masthead is half
 * the fun and should be allowed to leave when the reader starts browsing.
 */
@Composable
fun CitiesScreen(
    onOpenCity: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val repository = ServiceLocator.repository
    val p = LocalFootnotesPalette.current

    LaunchedEffect(Unit) { repository.ensureContentLoaded() }

    val freshTours by remember { repository.observeTours() }
        .collectAsStateWithLifecycle(initialValue = CitiesMemory.tours)
    LaunchedEffect(freshTours) {
        if (freshTours.isNotEmpty()) CitiesMemory.tours = freshTours
    }
    val tours = freshTours.ifEmpty { CitiesMemory.tours }

    val cities = tours.groupBy { it.city }.toSortedMap()
    val countries = tours.map { it.country }.distinct().sorted()
    val visible = if (CitiesMemory.country == null) {
        cities
    } else {
        cities.filterValues { cityTours -> cityTours.firstOrNull()?.country == CitiesMemory.country }
    }

    // One walk is the cover story each day. Deterministic, so it does not shuffle under the
    // reader's thumb while the Room flow emits: a magazine does not reprint itself every second.
    val featuredCity = visible.keys.elementAtOrNull(
        (LocalDate.now().toEpochDay() % (visible.size.coerceAtLeast(1))).toInt(),
    )

    PaperBackground {
        LazyVerticalGrid(
            state = CitiesMemory.gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Masthead(
                    tourCount = tours.size,
                    cityCount = cities.size,
                    onOpenSettings = onOpenSettings,
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(2.dp))
                NumbersStrip(tourCount = tours.size, countryCount = countries.size)
            }

            if (tours.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { UnfoldingTheMap() }
            } else {
                if (featuredCity != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Kicker("Today's wander")
                                Spacer(Modifier.width(8.dp))
                                Sparkle(Modifier.size(13.dp), color = p.mustard)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = "ONE PLACE. GO.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = p.inkSoft,
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            FeaturedCityCard(
                                city = featuredCity,
                                cityTours = visible.getValue(featuredCity),
                                onClick = { onOpenCity(featuredCity) },
                            )
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    CountryFilter(
                        countries = countries,
                        selected = CitiesMemory.country,
                        onSelect = { CitiesMemory.country = it },
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(top = 8.dp)) {
                        DottedRule(color = p.ink.copy(alpha = 0.25f))
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Column(Modifier.weight(1f)) {
                                Kicker("The atlas")
                                Text(
                                    text = if (CitiesMemory.country == null) {
                                        "Everywhere, alphabetically"
                                    } else {
                                        CitiesMemory.country.orEmpty()
                                    },
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = FootnotesTitleFont,
                                    ),
                                    color = p.ink,
                                )
                            }
                            Text(
                                text = "${visible.size} PLACES",
                                style = MaterialTheme.typography.labelSmall,
                                color = p.inkSoft,
                            )
                        }
                    }
                }

                items(visible.entries.toList(), key = { it.key }) { (city, cityTours) ->
                    CityPostcard(
                        city = city,
                        cityTours = cityTours,
                        onClick = { onOpenCity(city) },
                    )
                }

                if (visible.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyCountry(onClear = { CitiesMemory.country = null })
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) { Footer() }
            }
        }
    }
}

@Composable
private fun Masthead(
    tourCount: Int,
    cityCount: Int,
    onOpenSettings: () -> Unit,
) {
    val p = LocalFootnotesPalette.current
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Sunburst(
                modifier = Modifier.size(30.dp),
                color = p.tomato,
                rays = 10,
                filled = true,
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = "FOOTNOTES",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FootnotesTitleFont,
                    fontWeight = FontWeight.Black,
                    fontSize = 27.sp,
                    letterSpacing = (-0.4).sp,
                ),
                color = p.ink,
            )
            Spacer(Modifier.weight(1f))
            Surface(
                onClick = onOpenSettings,
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = p.paperRaised,
                contentColor = p.ink,
                border = BorderStroke(1.5.dp, p.ink),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = "Settings",
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Audio walks\nwith opinions.",
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = FootnotesTitleFont,
                fontSize = 45.sp,
                lineHeight = 42.sp,
            ),
            color = p.ink,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Put something interesting in your ears and go outside.",
            style = MaterialTheme.typography.bodyLarge,
            color = p.inkSoft,
            modifier = Modifier.fillMaxWidth(0.9f),
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (tourCount == 0) {
                Text(
                    text = "COUNTING THE PAVEMENTS…",
                    style = MaterialTheme.typography.labelMedium,
                    color = p.inkSoft,
                )
            } else {
                Text(
                    text = "$cityCount places",
                    style = MaterialTheme.typography.labelMedium,
                    color = p.ink,
                )
                Text("  ·  ", color = p.inkSoft, style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "$tourCount walks",
                    style = MaterialTheme.typography.labelMedium,
                    color = p.ink,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "NO BUS. NO QUEUE.",
                style = MaterialTheme.typography.labelSmall,
                color = p.tomato,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** The loud ticket under the masthead: three promises, one of which is a lie. */
@Composable
private fun NumbersStrip(tourCount: Int, countryCount: Int) {
    val p = LocalFootnotesPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { rotationZ = -0.8f }
            .background(p.tomato, RoundedCornerShape(3.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val lines = if (tourCount == 0) {
            listOf("unfolding the map", "back in a sec", "worth it")
        } else {
            listOf("$tourCount walks", "$countryCount countries", "0 gift shops")
        }
        lines.forEachIndexed { index, text ->
            if (index > 0) {
                Text(
                    text = "  ✳  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.paperRaised.copy(alpha = 0.65f),
                )
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = p.paperRaised,
            )
        }
    }
}

@Composable
private fun FeaturedCityCard(
    city: String,
    cityTours: List<TourEntity>,
    onClick: () -> Unit,
) {
    val p = LocalFootnotesPalette.current
    val accent = cityAccent(city)
    val flag = CountryFlags.flag(cityTours.firstOrNull()?.country.orEmpty())
    val first = cityTours.firstOrNull()

    InkCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        shadow = 6.dp,
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(188.dp)) {
                AssetPhoto(
                    assetPath = first?.heroImage,
                    contentDescription = "$city",
                    modifier = Modifier.fillMaxSize(),
                )
                Sticker(
                    text = "Start here",
                    modifier = Modifier.padding(12.dp),
                    container = p.mustard,
                    rotation = -3f,
                )
                if (first != null) {
                    Stamp(
                        text = "${first.distanceKm} km  ·  ${Formatters.duration(first.totalWalkMinutes)}",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        container = p.paperRaised,
                        rotation = -1f,
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(accent.background)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = flag.ifEmpty { "◎" },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = cityTours.firstOrNull()?.country.orEmpty().uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.foreground.copy(alpha = 0.82f),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${cityTours.size} ${if (cityTours.size == 1) "WALK" else "WALKS"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent.foreground,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = city,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = FootnotesTitleFont,
                        fontSize = 40.sp,
                        lineHeight = 42.sp,
                    ),
                    color = accent.foreground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (cityTours.size) {
                            1 -> "One walk. No excuses."
                            else -> "${cityTours.size} different ways to get lost."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = accent.foreground.copy(alpha = 0.88f),
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(p.paperRaised, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = p.ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryFilter(
    countries: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    val p = LocalFootnotesPalette.current
    Column(Modifier.padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Kicker("Passport control", color = p.teal)
            Spacer(Modifier.weight(1f))
            Text(
                text = "PICK A STAMP",
                style = MaterialTheme.typography.labelSmall,
                color = p.inkSoft,
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    label = "Everywhere",
                    flag = "🌍",
                    selected = selected == null,
                    onClick = { onSelect(null) },
                )
            }
            items(countries.size) { index ->
                val country = countries[index]
                FilterChip(
                    label = country,
                    flag = CountryFlags.flag(country),
                    selected = selected == country,
                    onClick = { onSelect(country) },
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    flag: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val p = LocalFootnotesPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) p.ink else p.paperRaised,
        contentColor = if (selected) p.paperRaised else p.ink,
        border = BorderStroke(1.5.dp, p.ink),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = flag.ifEmpty { "•" }, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(6.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CityPostcard(
    city: String,
    cityTours: List<TourEntity>,
    onClick: () -> Unit,
) {
    val p = LocalFootnotesPalette.current
    val accent = cityAccent(city)
    val first = cityTours.firstOrNull()
    val flag = CountryFlags.flag(first?.country.orEmpty())

    InkCard(
        modifier = Modifier.fillMaxWidth().height(212.dp),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        shadow = 4.dp,
        borderWidth = 1.5.dp,
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(128.dp)) {
                AssetPhoto(
                    assetPath = first?.heroImage,
                    contentDescription = "$city",
                    modifier = Modifier.fillMaxSize(),
                    targetWidthPx = 640,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .background(p.paperRaised, RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "${cityTours.size} ${if (cityTours.size == 1) "WALK" else "WALKS"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = p.ink,
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(accent.background)
                    .padding(horizontal = 11.dp, vertical = 9.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = flag.ifEmpty { "◎" }, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = first?.country.orEmpty().uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = accent.foreground.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = city,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FootnotesTitleFont,
                            fontSize = 23.sp,
                            lineHeight = 24.sp,
                        ),
                        color = accent.foreground,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = accent.foreground.copy(alpha = 0.8f),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun UnfoldingTheMap() {
    val p = LocalFootnotesPalette.current
    val transition = rememberInfiniteTransition(label = "unfolding")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "unfolding-alpha",
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Sunburst(
            modifier = Modifier.size(46.dp).alpha(alpha),
            color = p.tomato,
            rays = 12,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Unfolding the map…",
            style = MaterialTheme.typography.headlineSmall,
            color = p.ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Good things are coming out of the glove box.",
            style = MaterialTheme.typography.bodySmall,
            color = p.inkSoft,
        )
    }
}

@Composable
private fun EmptyCountry(onClear: () -> Unit) {
    val p = LocalFootnotesPalette.current
    InkCard(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        container = p.mustard,
        contentColor = Color(0xFF221A0E),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = "Nothing here. Yet.",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF221A0E),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Either we haven't walked it, or passport control is having a moment.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF221A0E).copy(alpha = 0.78f),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "SHOW EVERYWHERE →",
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF221A0E))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clickable(onClick = onClear),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FootnotesMonoFont),
                fontWeight = FontWeight.Bold,
                color = p.paperRaised,
            )
        }
    }
}

@Composable
private fun Footer() {
    val p = LocalFootnotesPalette.current
    Column(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 14.dp)) {
        DottedRule(color = p.ink.copy(alpha = 0.25f))
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "Go on.\nThe city isn't\ngoing to walk itself.",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FootnotesTitleFont,
                    lineHeight = 30.sp,
                ),
                color = p.ink,
                modifier = Modifier.weight(1f),
            )
            Sparkle(Modifier.padding(top = 4.dp).size(30.dp), color = p.tomato)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = FootnotesBrand.SIGN_OFF.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = p.inkSoft,
        )
    }
}
