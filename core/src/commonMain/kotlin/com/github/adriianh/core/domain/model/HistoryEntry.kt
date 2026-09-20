package com.github.adriianh.core.domain.model

data class HistoryEntry(
    val track: Track,
    val playedAt: Long,
)

/**
 * Merges local playback history and remote YouTube Music history,
 * deduplicating entries by ID and YouTube sourceId, preserving local play order first.
 */
fun mergeHistoryEntries(
    localHistory: List<HistoryEntry>,
    remoteHistory: List<HistoryEntry>
): List<HistoryEntry> {
    if (localHistory.isEmpty()) return remoteHistory
    if (remoteHistory.isEmpty()) return localHistory
    val localNormalizedIds = localHistory.map { it.track.id.removePrefix("piped:") }.toSet()
    val localSourceIds = localHistory.mapNotNull { it.track.sourceId?.removePrefix("piped:") }
        .filter { it.isNotBlank() }.toSet()
    val filteredRemote = remoteHistory.filterNot { remote ->
        val remoteRawId = remote.track.id.removePrefix("piped:")
        val remoteRawSourceId = remote.track.sourceId?.removePrefix("piped:")
        remoteRawId in localNormalizedIds ||
                remoteRawId in localSourceIds ||
                (!remoteRawSourceId.isNullOrBlank() && (remoteRawSourceId in localNormalizedIds || remoteRawSourceId in localSourceIds))
    }
    return localHistory + filteredRemote
}