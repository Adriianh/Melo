package com.github.adriianh.melo.ui.settings

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSearchTest {
    private val sampleEntries =
        listOf(
            SettingEntry(
                id = "theme",
                category = SettingsCategory.APPEARANCE,
                section = "Apariencia y tema",
                title = "Modo de tema",
                description = "Sigue la configuración del sistema o fuerza claro/oscuro.",
                keywords = listOf("theme", "dark", "oscuro", "light", "claro"),
                content = {},
            ),
            SettingEntry(
                id = "cache",
                category = SettingsCategory.PLAYBACK,
                section = "Almacenamiento y caché",
                title = "Límite de caché",
                description = "Tamaño máximo en disco para reproducción.",
                keywords = listOf("cache", "storage", "almacenamiento", "disco"),
                content = {},
            ),
            SettingEntry(
                id = "spanish_chars",
                category = SettingsCategory.ACCOUNT,
                section = "Cuenta",
                title = "Sincronizar reseñas y álbumes",
                description = "Configuración de música en español.",
                keywords = listOf("nino", "música", "español"),
                content = {},
            ),
            SettingEntry(
                id = "hidden_discord",
                category = SettingsCategory.INTEGRATIONS,
                section = "Discord Rich Presence",
                title = "Mostrar en Discord",
                description = "Actividad de reproducción.",
                keywords = listOf("discord", "rpc"),
                visible = false,
                content = {},
            ),
        )

    @Test
    fun `foldForSearch folds accents enye and case identically`() {
        assertEquals("limpieza de cache y musica", foldForSearch("Limpieza de Caché y Música"))
        assertEquals("ano nuevo nandu", foldForSearch("Año Nuevo Ñandú"))
        assertEquals("guia", foldForSearch("Güía"))
    }

    @Test
    fun `filterSettingEntries matches accent-folded and English keywords`() {
        val byAccentless = filterSettingEntries(sampleEntries, "limite cache")
        assertEquals(listOf("cache"), byAccentless.map { it.id })

        val byEnglishKeyword = filterSettingEntries(sampleEntries, "dark theme")
        assertEquals(listOf("theme"), byEnglishKeyword.map { it.id })

        val byEnye = filterSettingEntries(sampleEntries, "espanol resenas")
        assertEquals(listOf("spanish_chars"), byEnye.map { it.id })
    }

    @Test
    fun `filterSettingEntries ignores hidden entries and empty queries`() {
        assertEquals(emptyList<String>(), filterSettingEntries(sampleEntries, "   ").map { it.id })
        assertEquals(emptyList<String>(), filterSettingEntries(sampleEntries, "discord").map { it.id })
    }

    @Test
    fun `filterSettingEntries requires all tokens to match`() {
        assertEquals(emptyList<String>(), filterSettingEntries(sampleEntries, "tema discord").map { it.id })
        assertEquals(listOf("cache"), filterSettingEntries(sampleEntries, "reproduccion disco").map { it.id })
    }

    @Test
    fun `buildHighlightedTitle applies span style across folded accent indices`() {
        val annotated =
            buildHighlightedTitle(
                title = "Límite de caché",
                query = "limite cache",
                highlightColor = Color.Cyan,
            )
        assertEquals("Límite de caché", annotated.text)
        assertEquals(2, annotated.spanStyles.size)
        assertEquals(0 until 6, annotated.spanStyles[0].let { it.start until it.end })
        assertEquals(10 until 15, annotated.spanStyles[1].let { it.start until it.end })
    }
}
