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
            artists.add(Artist(name = name, id = currentId))
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

    return artists.ifEmpty {
        val combined = joinToString("") { it.text }.trim()
        if (combined.isNotEmpty()) listOf(Artist(name = combined, id = null)) else emptyList()
    }
}

fun List<Run>.oddElements(): List<Run> {
    return extractArtists().map {
        Run(text = it.name, navigationEndpoint = it.id?.let { id ->
            NavigationEndpoint(browseEndpoint = BrowseEndpoint(browseId = id))
        })
    }
}
