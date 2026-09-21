package com.github.adriianh.core.domain.model

enum class SortDirection(val label: String, val symbol: String) {
    ASCENDING("Asc", "↑"),
    DESCENDING("Desc", "↓");

    fun toggle(): SortDirection = if (this == ASCENDING) DESCENDING else ASCENDING
}

enum class TrackSortOrder(val label: String) {
    DEFAULT("Default"),
    TITLE("Title"),
    ARTIST("Artist"),
    DURATION("Duration");

    fun next(): TrackSortOrder = entries[(ordinal + 1) % entries.size]
}

enum class PlaylistSortOrder(val label: String) {
    DEFAULT("Default"),
    NAME("Name"),
    TRACKS("Tracks"),
    AUTHOR("Author");

    fun next(): PlaylistSortOrder = entries[(ordinal + 1) % entries.size]
}

enum class OfflineFilterType(val label: String) {
    ALL("All"),
    MANUAL("Manual"),
    CACHE("Cache");

    fun next(): OfflineFilterType = entries[(ordinal + 1) % entries.size]
}