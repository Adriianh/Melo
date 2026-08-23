package com.github.adriianh.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Runs(
    val runs: List<Run>?,
)

@Serializable
data class Run(
    val text: String,
    val navigationEndpoint: NavigationEndpoint?,
)

fun List<Run>.splitBySeparator(): List<List<Run>> {
    val res = mutableListOf<List<Run>>()
    var tmp = mutableListOf<Run>()
    forEach { run ->
        if (run.text.trim() == "•") {
            if (tmp.isNotEmpty()) {
                res.add(tmp)
                tmp = mutableListOf()
            }
        } else {
            tmp.add(run)
        }
    }
    if (tmp.isNotEmpty()) {
        res.add(tmp)
    }
    return res
}

fun String.isTimeDuration(): Boolean {
    val trimmed = this.trim()
    return trimmed.matches(Regex("""^\d{1,2}:\d{2}(:\d{2})?$"""))
}

fun String.isKnownTypeLabel(): Boolean {
    val lower = this.trim().lowercase()
    return lower in setOf(
        "cancion", "canción", "song", "songs",
        "video", "videos", "vídeo", "vídeos",
        "álbum", "album", "albumes", "álbumes", "albums",
        "artista", "artist", "artistas", "artists",
        "sencillo", "single", "singles", "ep",
        "lista de reproducción", "playlist", "playlists"
    )
}

fun String.isInvalidArtistName(): Boolean {
    val trimmed = this.trim()
    if (trimmed.isEmpty()) return true
    if (trimmed.isTimeDuration()) return true
    val lower = trimmed.lowercase()
    if (lower.isKnownTypeLabel()) return true
    if (lower.matches(Regex("""^\d{4}$"""))) return true
    if (lower.matches(
            Regex(
                """^[\d.,]+\s*[kmb]?\s*(plays?|views?|reproducciones|visualizaciones|escuchas|oyentes|vistas)""",
                RegexOption.IGNORE_CASE
            )
        )
    ) return true
    if (lower.endsWith("plays") || lower.endsWith("play") ||
        lower.endsWith("views") || lower.endsWith("view") ||
        lower.endsWith("reproducciones") || lower.endsWith("visualizaciones") ||
        lower.endsWith("escuchas") || lower.endsWith("oyentes") ||
        lower.endsWith("vistas") || lower.contains("de reproducciones") ||
        lower.contains("de visualizaciones")
    ) {
        return true
    }
    return false
}

fun List<List<Run>>.clean(): List<List<Run>> =
    if (getOrNull(0)?.getOrNull(0)?.navigationEndpoint != null) this
    else this.drop(1)

fun List<Run>.extractArtists(): List<Artist> {
    if (isEmpty()) return emptyList()

    val artists = mutableListOf<Artist>()
    var currentName = StringBuilder()
    var currentId: String? = null

    fun flush() {
        val name = currentName.toString().trim()
        if (name.isNotEmpty() && name != "•" && name != "&" && name != "," && name != "/" && name != "|") {
            if (!name.isInvalidArtistName()) {
                artists.add(Artist(name = name, id = currentId))
            }
        }
        currentName = StringBuilder()
        currentId = null
    }

    for (run in this) {
        val trimmed = run.text.trim()
        val isSeparator = trimmed == "," || trimmed == "&" || trimmed == "•" ||
                trimmed == "/" || trimmed == "|" ||
                trimmed.equals("feat.", ignoreCase = true) ||
                trimmed.equals("ft.", ignoreCase = true) ||
                trimmed.equals("feat", ignoreCase = true) ||
                trimmed.equals("featuring", ignoreCase = true)

        val runBrowseId = run.navigationEndpoint?.browseEndpoint?.browseId

        if (isSeparator) {
            flush()
        } else if (runBrowseId != null) {
            if (currentId != null && currentId != runBrowseId) {
                flush()
            }
            if (currentId == null) {
                currentId = runBrowseId
            }
            currentName.append(run.text)
        } else {
            currentName.append(run.text)
        }
    }
    flush()

    return artists.filter { !it.name.isInvalidArtistName() }.ifEmpty {
        val combined = joinToString("") { it.text }.trim()
        if (combined.isNotEmpty() && !combined.isInvalidArtistName()) {
            listOf(Artist(name = combined, id = null))
        } else {
            emptyList()
        }
    }
}

fun List<Run>.oddElements(): List<Run> {
    return extractArtists().map {
        Run(text = it.name, navigationEndpoint = it.id?.let { id ->
            NavigationEndpoint(browseEndpoint = BrowseEndpoint(browseId = id))
        })
    }
}
