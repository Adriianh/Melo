@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.player_artist_albums
import com.github.adriianh.melo.resources.player_artist_monthly_listeners
import com.github.adriianh.melo.resources.player_artist_popular
import com.github.adriianh.melo.resources.player_artist_subscribers
import com.github.adriianh.melo.resources.player_artist_view_profile
import com.github.adriianh.melo.ui.components.cards.ArtistCircle
import com.github.adriianh.melo.ui.components.track.RowContextMenuButton
import com.github.adriianh.melo.ui.components.track.SongMenuAnchor
import com.github.adriianh.melo.ui.components.track.songMenuRightClick
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.PlayerUiState
import com.github.adriianh.melo.util.getPlatform
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val ArtistSecondaryText = Color.White.copy(alpha = 0.70f)
private val ARTIST_METRIC_REGEX =
    Regex(
        pattern =
            """^\s*([\d.,]+)\s*([KMB])?\s*(?:de\s+)?""" +
                """(subscribers|suscriptores|monthly\s+listeners|oyentes\s+mensuales)\s*$""",
        option = RegexOption.IGNORE_CASE,
    )

@Composable
internal fun DockedArtistContent(
    state: PlayerUiState,
    artistDetails: SearchResult.Artist?,
    activeAccent: Color,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    queueViewModel: QueueViewModel = koinViewModel(),
    onOpenTrackContextMenu: ((Track, SongMenuAnchor?) -> Unit)? = null,
) {
    DockedArtistSectionContent(
        state = state,
        artistDetails = artistDetails,
        activeAccent = activeAccent,
        onArtistClick = onArtistClick,
        onAlbumClick = onAlbumClick,
        onTrackClick = queueViewModel::playTrack,
        onOpenTrackContextMenu = onOpenTrackContextMenu,
    )
}

@Composable
internal fun DockedArtistSectionContent(
    state: PlayerUiState,
    artistDetails: SearchResult.Artist?,
    activeAccent: Color,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onTrackClick: (Track) -> Unit,
    onOpenTrackContextMenu: ((Track, SongMenuAnchor?) -> Unit)? = null,
) {
    if (artistDetails == null) {
        DockedArtistLoading(state, activeAccent)
        return
    }

    val topSongs =
        remember(artistDetails) {
            artistDetails.topSongs?.take(5) ?: emptyList()
        }

    val albums =
        remember(artistDetails) {
            artistDetails.sections
                .filter { section ->
                    section.title.contains("álbum", ignoreCase = true) ||
                        section.title.contains("album", ignoreCase = true) ||
                        section.title.contains("disc", ignoreCase = true) ||
                        section.title.contains("lanzamiento", ignoreCase = true) ||
                        section.title.contains("release", ignoreCase = true)
                }.flatMap { it.items }
                .filterIsInstance<SearchResult.Album>()
                .ifEmpty {
                    artistDetails.sections.flatMap { it.items }.filterIsInstance<SearchResult.Album>()
                }.distinctBy { it.id }
                .take(4)
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item(key = "artist_header") {
            DockedArtistHeader(
                artistDetails = artistDetails,
                activeAccent = activeAccent,
                onArtistClick = onArtistClick,
            )
        }

        if (topSongs.isNotEmpty()) {
            item(key = "artist_popular_header") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.player_artist_popular),
                    style = MeloType.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ArtistSecondaryText,
                    letterSpacing = 1.sp,
                )
            }
            items(topSongs, key = { "artist_top_${it.id}" }) { track ->
                val isPlayingThis =
                    state.currentTrack?.id == track.id ||
                        (state.title == track.title && state.artist == track.artist)
                DockedArtistTopSongRow(
                    track = track,
                    isPlayingThis = isPlayingThis,
                    activeAccent = activeAccent,
                    onClick = { onTrackClick(track) },
                    onOpenContextMenu =
                        onOpenTrackContextMenu?.let { open -> { anchor -> open(track, anchor) } },
                )
            }
        }

        if (albums.isNotEmpty()) {
            item(key = "artist_albums_header") {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(Res.string.player_artist_albums),
                    style = MeloType.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ArtistSecondaryText,
                    letterSpacing = 1.sp,
                )
            }
            items(albums, key = { "artist_album_${it.id}" }) { album ->
                DockedArtistAlbumRow(
                    album = album,
                    onClick = { onAlbumClick(album.id) },
                )
            }
        }
    }
}

@Composable
private fun DockedArtistLoading(
    state: PlayerUiState,
    activeAccent: Color,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ArtistCircle(
                name = state.artist,
                artworkUrl = state.albumArt,
                onClick = { },
                size = 96.dp,
            )
            CircularProgressIndicator(
                color = activeAccent,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun DockedArtistHeader(
    artistDetails: SearchResult.Artist,
    activeAccent: Color,
    onArtistClick: (String) -> Unit,
) {
    val rawMetric = artistDetails.subscriberCountText?.ifBlank { null } ?: artistDetails.monthlyListenerCount
    val localizedMetric = rawMetric?.let { rememberLocalizedArtistMetric(it) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val reduceTransparency = MeloColors.reduceTransparency
        Box(
            modifier =
                Modifier.drawBehind {
                    if (!reduceTransparency) {
                        drawCircle(
                            brush =
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            activeAccent.copy(alpha = 0.28f),
                                            activeAccent.copy(alpha = 0.08f),
                                            Color.Transparent,
                                        ),
                                    center = center,
                                    radius = size.maxDimension * 0.78f,
                                ),
                            radius = size.maxDimension * 0.78f,
                            center = center,
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            MeloAsyncImage(
                url = artistDetails.artworkUrl,
                contentDescription = artistDetails.name,
                modifier =
                    Modifier
                        .size(96.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        .clickable { onArtistClick(artistDetails.id) },
                size = 96.dp,
                shape = CircleShape,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = artistDetails.name,
            style = MeloType.titleMedium.copy(fontSize = 20.sp, lineHeight = 25.sp),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        if (!localizedMetric.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = localizedMetric,
                style = MeloType.labelSmall.copy(fontSize = 12.sp),
                color = ArtistSecondaryText,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = { onArtistClick(artistDetails.id) },
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, activeAccent.copy(alpha = 0.75f)),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    containerColor = activeAccent.copy(alpha = 0.10f),
                    contentColor = activeAccent,
                ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            modifier = Modifier.defaultMinSize(minHeight = 44.dp),
        ) {
            Text(
                text = stringResource(Res.string.player_artist_view_profile),
                style = MeloType.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = activeAccent,
            )
        }
    }
}

@Composable
private fun rememberLocalizedArtistMetric(rawText: String): String {
    val probe = stringResource(Res.string.player_artist_subscribers, "__NUM__")
    val isSpanish = probe.contains("suscriptores", ignoreCase = true)
    val parsed = remember(rawText, isSpanish) { parseArtistMetric(rawText, isSpanish) }
    return when (parsed) {
        is ParsedArtistMetric.Subscribers ->
            stringResource(Res.string.player_artist_subscribers, parsed.formattedCount)

        is ParsedArtistMetric.MonthlyListeners ->
            stringResource(Res.string.player_artist_monthly_listeners, parsed.formattedCount)

        null -> rawText
    }
}

internal sealed interface ParsedArtistMetric {
    data class Subscribers(
        val formattedCount: String,
    ) : ParsedArtistMetric

    data class MonthlyListeners(
        val formattedCount: String,
    ) : ParsedArtistMetric
}

internal fun parseArtistMetric(
    rawText: String,
    isSpanishLocale: Boolean,
): ParsedArtistMetric? {
    val match = ARTIST_METRIC_REGEX.matchEntire(rawText) ?: return null
    val rawNumber = match.groupValues[1]
    val suffix = match.groupValues[2].uppercase()
    val unit = match.groupValues[3].lowercase()

    val normalizedNumber =
        if (isSpanishLocale) {
            rawNumber.replace('.', ',')
        } else {
            rawNumber.replace(',', '.')
        }
    val formattedCount =
        if (suffix.isNotEmpty()) {
            if (isSpanishLocale) "$normalizedNumber $suffix" else "$normalizedNumber$suffix"
        } else {
            normalizedNumber
        }

    return if (unit.contains("listener") || unit.contains("oyente")) {
        ParsedArtistMetric.MonthlyListeners(formattedCount)
    } else {
        ParsedArtistMetric.Subscribers(formattedCount)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockedArtistTopSongRow(
    track: Track,
    isPlayingThis: Boolean,
    activeAccent: Color,
    onClick: () -> Unit,
    onOpenContextMenu: ((SongMenuAnchor?) -> Unit)? = null,
) {
    val isDesktop = remember { getPlatform().type == PlatformType.DESKTOP }
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()
    var rowBounds by remember { mutableStateOf<Rect?>(null) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        isPlayingThis -> activeAccent.copy(alpha = 0.18f)
                        isHovered -> activeAccent.copy(alpha = 0.10f)
                        else -> Color.Transparent
                    },
                ).hoverable(hoverSource)
                .onGloballyPositioned { rowBounds = it.boundsInWindow() }
                .songMenuRightClick(
                    enabled = onOpenContextMenu != null,
                    onOpenMenu = { anchor -> onOpenContextMenu?.invoke(anchor) },
                ).combinedClickable(
                    onClick = onClick,
                    onLongClick =
                        onOpenContextMenu?.let { open ->
                            { open(rowBounds?.let { SongMenuAnchor.Bounds(it) }) }
                        },
                ).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MeloAsyncImage(
            url = track.artworkUrl,
            contentDescription = track.title,
            size = 44.dp,
            shape = RoundedCornerShape(8.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MeloType.labelMedium,
                fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium,
                color = if (isPlayingThis) activeAccent else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = MeloType.labelSmall.copy(fontSize = 11.sp),
                color = ArtistSecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onOpenContextMenu != null) {
            RowContextMenuButton(
                isVisible = !isDesktop || isHovered,
                onOpenMenu = onOpenContextMenu,
            )
        }
    }
}

@Composable
private fun DockedArtistAlbumRow(
    album: SearchResult.Album,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 60.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MeloAsyncImage(
            url = album.artworkUrl,
            contentDescription = album.title,
            size = 44.dp,
            shape = RoundedCornerShape(8.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = MeloType.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val yearOrAuthor =
                listOfNotNull(album.year, album.author).firstOrNull { it.isNotBlank() }
            if (!yearOrAuthor.isNullOrBlank()) {
                Text(
                    text = yearOrAuthor,
                    style = MeloType.labelSmall.copy(fontSize = 11.sp),
                    color = ArtistSecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
