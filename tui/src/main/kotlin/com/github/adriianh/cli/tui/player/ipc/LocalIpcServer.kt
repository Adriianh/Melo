package com.github.adriianh.cli.tui.player.ipc

import com.github.adriianh.cli.config.configDir
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.PlaybackStatusDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
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
    private val onPlayNow: (Track) -> Unit = {},
    private val onPlayList: (List<Track>) -> Unit = {},
    private val onQueueAddList: (List<Track>) -> Unit = {},
    private val getStatus: () -> PlaybackStatusDto = { PlaybackStatusDto() },
    private val onVolumeGet: () -> Int = { 75 },
    private val onVolumeSet: (Int) -> Unit = {},
    private val onVolumeAdjust: (Int) -> Unit = {},
    private val onCustomCommand: (String, String) -> String = { _, _ -> "ERROR Not implemented" },
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

            serverJob =
                scope.launch(Dispatchers.IO) {
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

                val handled =
                    handleControlCommand(cmd, writer) ||
                        handleQueueMutation(cmd, payload, writer) ||
                        handlePlayCommand(cmd, payload, writer) ||
                        handleStatusAndVolumeCommand(cmd, payload, writer)

                if (!handled) {
                    val response = onCustomCommand(cmd, payload)
                    writer.write("$response\n")
                }
                writer.flush()
            }
        } catch (e: Exception) {
            logger.error("Error handling IPC client", e)
        }
    }

    private fun handleControlCommand(
        cmd: String,
        writer: BufferedWriter,
    ): Boolean =
        when (cmd) {
            "PING" -> {
                writer.write("OK PONG\n")
                true
            }
            "PAUSE", "RESUME" -> {
                onPlayPause()
                writer.write("OK\n")
                true
            }
            "NEXT" -> {
                onNext()
                writer.write("OK\n")
                true
            }
            "PREV" -> {
                onPrevious()
                writer.write("OK\n")
                true
            }
            "STOP" -> {
                onStop()
                writer.write("OK\n")
                true
            }
            else -> false
        }

    private fun handleQueueMutation(
        cmd: String,
        payload: String,
        writer: BufferedWriter,
    ): Boolean =
        when (cmd) {
            "QUEUE_ADD" -> {
                try {
                    val track = json.decodeFromString(Track.serializer(), payload)
                    onQueueAdd(track)
                    writer.write("OK\n")
                } catch (_: Exception) {
                    writer.write("ERROR Invalid payload\n")
                }
                true
            }
            "QUEUE_ADD_LIST" -> {
                try {
                    val tracks = json.decodeFromString(ListSerializer(Track.serializer()), payload)
                    onQueueAddList(tracks)
                    writer.write("OK\n")
                } catch (_: Exception) {
                    writer.write("ERROR Invalid payload\n")
                }
                true
            }
            "QUEUE_REMOVE" -> {
                val index = payload.toIntOrNull()
                if (index != null && onQueueRemove(index)) {
                    writer.write("OK\n")
                } else {
                    writer.write("ERROR Invalid index\n")
                }
                true
            }
            "QUEUE_CLEAR" -> {
                onQueueClear()
                writer.write("OK\n")
                true
            }
            "QUEUE_LIST" -> {
                val queue = getQueue()
                val res = json.encodeToString(ListSerializer(Track.serializer()), queue)
                writer.write("OK $res\n")
                true
            }
            else -> false
        }

    private fun handlePlayCommand(
        cmd: String,
        payload: String,
        writer: BufferedWriter,
    ): Boolean =
        when (cmd) {
            "PLAY_NOW" -> {
                try {
                    val track = json.decodeFromString(Track.serializer(), payload)
                    onPlayNow(track)
                    writer.write("OK\n")
                } catch (_: Exception) {
                    writer.write("ERROR Invalid payload\n")
                }
                true
            }
            "PLAY_LIST" -> {
                try {
                    val tracks = json.decodeFromString(ListSerializer(Track.serializer()), payload)
                    onPlayList(tracks)
                    writer.write("OK\n")
                } catch (_: Exception) {
                    writer.write("ERROR Invalid payload\n")
                }
                true
            }
            else -> false
        }

    private fun handleStatusAndVolumeCommand(
        cmd: String,
        payload: String,
        writer: BufferedWriter,
    ): Boolean =
        when (cmd) {
            "STATUS" -> {
                val status = getStatus()
                val jsonStr = json.encodeToString(PlaybackStatusDto.serializer(), status)
                writer.write("OK $jsonStr\n")
                true
            }

            "VOLUME_GET" -> {
                val vol = onVolumeGet()
                writer.write("OK $vol\n")
                true
            }

            "VOLUME_SET" -> {
                val level = payload.toIntOrNull()
                if (level != null) {
                    onVolumeSet(level)
                    val newVol = onVolumeGet()
                    writer.write("OK $newVol\n")
                } else {
                    writer.write("ERROR Invalid volume level\n")
                }
                true
            }

            "VOLUME_ADJUST" -> {
                val delta = payload.toIntOrNull()
                if (delta != null) {
                    onVolumeAdjust(delta)
                    val newVol = onVolumeGet()
                    writer.write("OK $newVol\n")
                } else {
                    writer.write("ERROR Invalid volume delta\n")
                }
                true
            }

            else -> false
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
