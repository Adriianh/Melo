package com.github.adriianh.cli.tui.player.ipc

import com.github.adriianh.cli.config.configDir
import java.io.File
import java.net.Socket

object LocalIpcClient {
    fun sendCommand(command: String, payload: String = ""): String {
        val portFile = File(configDir, "ipc.port")
        if (!portFile.exists()) {
            return "ERROR No active playback session found."
        }
        val port = portFile.readText().trim().toIntOrNull()
            ?: return "ERROR Invalid IPC port file."

        return try {
            Socket("127.0.0.1", port).use { socket ->
                val writer = socket.outputStream.bufferedWriter()
                val reader = socket.inputStream.bufferedReader()

                val req = if (payload.isEmpty()) command else "$command $payload"
                writer.write("$req\n")
                writer.flush()

                reader.readLine() ?: "ERROR Empty response"
            }
        } catch (e: Exception) {
            "ERROR Failed to connect to playback session: ${e.message}"
        }
    }
}