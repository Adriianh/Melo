package com.github.adriianh.cli.tui.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VolumeControllerTest {

    @Test
    fun testPactlParseClientIds() {
        val sampleClientsOutput = """
            Client #42
                Driver: PipeWire
                Owner Module: n/a
                Properties:
                    application.name = "ffplay"
                    application.process.id = "12345"
            Client #43
                Driver: PipeWire
                Owner Module: n/a
                Properties:
                    application.name = "Simple DirectMedia Layer"
                    pipewire.sec.pid = "12345"
            Client #99
                Driver: PipeWire
                Owner Module: n/a
                Properties:
                    application.name = "Other"
                    application.process.id = "99999"
        """.trimIndent()

        val matching = PactlVolumeController.parseClientIds(sampleClientsOutput, 12345L)
        assertEquals(listOf("42", "43"), matching)

        val nonMatching = PactlVolumeController.parseClientIds(sampleClientsOutput, 54321L)
        assertTrue(nonMatching.isEmpty())
    }

    @Test
    fun testPactlParseSinkInputByClientId() {
        val sampleSinksOutput = """
            Sink Input #84
                Driver: PipeWire
                Owner Module: n/a
                Client: 43
                Sink: 48
                Sample Specification: s16le 2ch 44100Hz
            Sink Input #85
                Driver: PipeWire
                Owner Module: n/a
                Client: 99
                Sink: 48
        """.trimIndent()

        val sinkIndex = PactlVolumeController.parseSinkInputByClientId(sampleSinksOutput, "43")
        assertEquals("84", sinkIndex)

        val missing = PactlVolumeController.parseSinkInputByClientId(sampleSinksOutput, "999")
        assertNull(missing)
    }

    @Test
    fun testWindowsVolumeControllerSafeOnCurrentPlatform() {
        // On non-Windows platforms, isAvailable must safely return false without throwing any exception
        if (!FfplayProcessManager.isWindows) {
            assertFalse(WindowsVolumeController.isAvailable)
            assertFalse(WindowsVolumeController.setProcessVolume(12345L, 50))
        }
    }

    @Test
    fun testApplyVolumeDoesNotCrash() {
        val scope = CoroutineScope(Dispatchers.Default)
        // Calling applyVolume with invalid or non-existent PID should be completely safe and non-blocking
        WindowsVolumeController.applyVolume(scope, -1L, 50)
        PactlVolumeController.applyVolume(scope, -1L, 50)
    }
}
