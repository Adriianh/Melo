package com.github.adriianh.melo.ui.library.components

object PlaylistNameGenerator {

    private val SITUATIONAL_ACTIONS = listOf(
        "Bailando en la cocina",
        "Caminando bajo la lluvia",
        "Viendo el amanecer",
        "Escapando de la realidad",
        "Pensando en lo que pudo ser",
        "Mirando por la ventana del autobús",
        "Perdiendo la noción del tiempo",
        "Viviendo en una película indie",
        "Escribiendo historias mentales",
        "Cocinando a fuego lento",
        "Limpiando la casa con energía",
        "Desconectando del mundo",
        "Buscando respuestas",
        "Gritando el coro a todo pulmón",
        "Recordando viejos tiempos",
        "Observando las luces de la ciudad"
    )

    private val ATMOSPHERE_MODIFIERS = listOf(
        "en días nublados",
        "a las 3 de la mañana",
        "en un domingo solitario",
        "sin mirar el reloj",
        "con un café en la mano",
        "mientras todo arde",
        "en una noche de verano",
        "sin rumbo fijo",
        "a volumen máximo",
        "en cámara lenta",
        "a 180 BPM",
        "con luces apagadas",
        "en carretera abierta",
        "con los ojos cerrados",
        "después de un día largo"
    )

    private val AESTHETIC_VIBES = listOf(
        "Nostalgia analógica",
        "Dopamina instantánea",
        "Melancolía en alta fidelidad",
        "Santuario acústico",
        "Frecuencia cósmica",
        "Ritmo hipnótico",
        "Serotonina pura",
        "Burbuja sonora",
        "Ecos del pasado",
        "Vanguardia nocturna",
        "Oasis de calma",
        "Gravedad cero",
        "Vibraciones cálidas",
        "Bajos subterráneos",
        "Pausa existencial"
    )

    fun generateIdea(
        selectedIconId: String,
        userArtists: List<String>,
        existingPlaylistNames: List<String>,
        currentValue: String
    ): String {
        val existingNormalized = existingPlaylistNames.map { it.trim().lowercase() }.toSet()
        val validArtists = userArtists.filter {
            it.isNotBlank() && it.lowercase() !in listOf("unknown", "various artists", "varios")
        }

        val candidateIdeas = mutableListOf<String>()

        for (action in SITUATIONAL_ACTIONS.shuffled().take(6)) {
            val mod = ATMOSPHERE_MODIFIERS.random()
            candidateIdeas.add("$action $mod")
        }

        for (vibe in AESTHETIC_VIBES.shuffled().take(5)) {
            val mod = ATMOSPHERE_MODIFIERS.random()
            candidateIdeas.add("$vibe $mod")
        }

        if (validArtists.isNotEmpty()) {
            val artist = validArtists.random()
            val artistTemplates = listOf(
                "Terapia acústica con $artist",
                "Efecto $artist en bucle",
                "El universo sonoro de $artist",
                "Nostalgia cortesía de $artist",
                "Si la vida fuera musicalizada por $artist",
                "Viaje nocturno con $artist",
                "$artist & otros dilemas",
                "Obsesión temporal: $artist"
            )
            candidateIdeas.add(artistTemplates.random())

            if (validArtists.size >= 2) {
                val pair = validArtists.shuffled().take(2)
                candidateIdeas.add("De ${pair[0]} a ${pair[1]} sin escalas")
            }
        }

        when (selectedIconId) {
            "favorite" -> candidateIdeas.addAll(
                listOf(
                    "Canciones que me salvaron la vida",
                    "Tesoros imposibles de saltar",
                    "Mi santuario sonoro",
                    "Obsesiones auditivas"
                )
            )

            "fire" -> candidateIdeas.addAll(
                listOf(
                    "Dopamina pura sin receta",
                    "Modo protagonista activado",
                    "Ritmo cardiaco acelerado",
                    "Prohibido quedarse quieto"
                )
            )

            "night" -> candidateIdeas.addAll(
                listOf(
                    "Pensamientos a medianoche",
                    "Luces de neón y ciudad vacía",
                    "Melancolía de madrugada",
                    "Insomnio en clave de sol"
                )
            )

            "bolt" -> candidateIdeas.addAll(
                listOf(
                    "Cardio para escapar del estrés",
                    "Rompiendo récords personales",
                    "Energía nivel supernova",
                    "Modo bestia activado"
                )
            )

            "coffee" -> candidateIdeas.addAll(
                listOf(
                    "Lofi para ver llover",
                    "Domingo sin alarmas",
                    "Café caliente y mente en calma",
                    "Vibras de suéter y lluvia"
                )
            )

            "trip" -> candidateIdeas.addAll(
                listOf(
                    "Ventana abajo y volumen arriba",
                    "Carretera hacia ninguna parte",
                    "Soundtrack de viaje",
                    "Kilómetros con buena música"
                )
            )

            "headphones" -> candidateIdeas.addAll(
                listOf(
                    "Desconexión del mundo exterior",
                    "Mi burbuja auditiva privada",
                    "Para perderse con audífonos"
                )
            )

            "radio" -> candidateIdeas.addAll(
                listOf(
                    "Frecuencia nostálgica",
                    "Transmisión de otra época",
                    "Sintonizando buenas vibras"
                )
            )

            "star" -> candidateIdeas.addAll(
                listOf(
                    "Obras maestras atemporales",
                    "Joyas ocultas bajo el radar",
                    "Colección de oro puro"
                )
            )

            else -> candidateIdeas.addAll(
                listOf(
                    "Banda sonora de mis días",
                    "Fragmentos de memoria",
                    "Expedición sonora"
                )
            )
        }

        val available = candidateIdeas.filter { it.trim().lowercase() !in existingNormalized }
        val pool = (available.ifEmpty { candidateIdeas }).shuffled()
        return pool.firstOrNull { it != currentValue } ?: pool.firstOrNull() ?: "Mi Playlist"
    }
}