package com.github.adriianh.core.domain.player

import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmAudioOutputNameTest {
    @Test
    fun `fixUtf8Mojibake repairs Windows-1252 decoded UTF-8 Spanish device names`() {
        val original = "Auriculares con micrófono (Dispositivo de audio — Español)"
        val garbled = String(original.toByteArray(Charsets.UTF_8), Charset.forName("windows-1252"))

        assertEquals(original, JvmMeloPlayer.fixUtf8Mojibake(garbled))
    }

    @Test
    fun `fixUtf8Mojibake preserves already valid UTF-8 and ASCII names`() {
        val validSpanish = "Auriculares con micrófono (Realtek® Audio)"
        val ascii = "Speakers (USB Audio DAC)"

        assertEquals(validSpanish, JvmMeloPlayer.fixUtf8Mojibake(validSpanish))
        assertEquals(ascii, JvmMeloPlayer.fixUtf8Mojibake(ascii))
    }
}
