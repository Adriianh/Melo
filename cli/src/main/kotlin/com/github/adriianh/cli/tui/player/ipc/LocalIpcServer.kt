package com.github.adriianh.cli.tui.player.ipc

import com.github.adriianh.cli.config.configDir
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class LocalIpcServer(
    private val onPlayPause: () -> Unit,
    private val onNext: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onStop: () -> Unit,
    private val onQueueAdd: (Track) -> Unit,
    private val onQueueRemove: (Int) -> Boolean,
    private val onQueueClear: () -> Unit,
    private val getQueue: () -> List<Track>,
    private val onCustomCommand: (String, String) -> String = { _, _ -> "ERROR Not implemented" }
) {
    private val logger = LoggerFactory.getLogger(LocalIpcServer::class.java)
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun start(scope: CoroutineScope) {
        val configFolder = File(configDir)
        if (!configFolder.exists()) configFolder.mkdirs()
        val portFile = File(configFolder, "ipc.port")

        try {
            serverSocket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
            val port = serverSocket!!.localPort
            portFile.writeText(port.toString())
            logger.info("Local IPC server started on port {}", port)

            serverJob = scope.launch(Dispatchers.IO) {
                while (!serverSocket!!.isClosed) {
                    try {
                        val client = serverSocket!!.accept()
                        launch { handleClient(client) }
                    } catch (_: SocketException) {
                        break // Closed
                    } catch (e: Exception) {
                        logger.error("Error accepting IPC connection", e)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to start IPC server", e)
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.use { s ->
                val reader = s.inputStream.bufferedReader()
                val writer = s.outputStream.bufferedWriter()
                val line = reader.readLine() ?: return

                val parts = line.split(" ", limit = 2)
                val cmd = parts[0]
                val payload = if (parts.size > 1) parts[1] else ""

                when (cmd) {
                    "PAUSE", "RESUME" -> {
                        onPlayPause(); writer.write("OK\n")
                    }

                    "NEXT" -> {
                        onNext(); writer.write("OK\n")
                    }

                    "PREV" -> {
                        onPrevious(); writer.write("OK\n")
                    }

                    "STOP" -> {
                        onStop(); writer.write("OK\n")
                    }

                    "QUEUE_ADD" -> {
                        try {
                            val track = json.decodeFromString(Track.serializer(), payload)
                            onQueueAdd(track)
                            writer.write("OK\n")
                        } catch (_: Exception) {
                            writer.write("ERROR Invalid payload\n")
                        }
                    }

                    "QUEUE_REMOVE" -> {
                        val index = payload.toIntOrNull()
                        if (index != null && onQueueRemove(index)) {
                            writer.write("OK\n")
                        } else {
                            writer.write("ERROR Invalid index\n")
                        }
                    }

                    "QUEUE_CLEAR" -> {
                        onQueueClear(); writer.write("OK\n")
                    }

                    "QUEUE_LIST" -> {
                        val queue = getQueue()
                        val res = json.encodeToString(ListSerializer(Track.serializer()), queue)
                        writer.write("OK $res\n")
                    }

                    else -> {
                        val response = onCustomCommand(cmd, payload)
                        writer.write("$response\n")
                    }
                }
                writer.flush()
            }
        } catch (e: Exception) {
            logger.error("Error handling IPC client", e)
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        try {
            File(configDir, "ipc.port").delete()
        } catch (_: Exception) {
        }
        serverJob?.cancel()
    }
}