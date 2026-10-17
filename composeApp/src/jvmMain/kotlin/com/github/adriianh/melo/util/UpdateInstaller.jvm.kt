package com.github.adriianh.melo.util

import com.github.adriianh.core.domain.model.update.UpdateInstallMethod
import com.github.adriianh.core.platform.DesktopInstall
import com.github.adriianh.core.platform.DesktopInstallEnvironment
import com.github.adriianh.core.platform.PlatformFileSystem
import com.github.adriianh.core.platform.currentDesktopInstallEnvironment
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess

/**
 * Applies desktop updates using the method Melo was installed with.
 *
 * The downloaded app package is never launched directly. Instead a small shell
 * helper is written next to it, launched detached, and this process exits: the
 * helper waits for the process to disappear, swaps the install in place and
 * relaunches Melo. Formats we cannot replace ourselves (MSI, Inno Setup, DMG) are
 * handed over to the operating system, and every failure is reported instead of
 * being swallowed.
 */
actual class PlatformUpdateInstaller actual constructor() {
    actual fun installAndRestart(installerPath: String): UpdateInstallResult {
        val packageFile = File(installerPath)
        return if (!packageFile.isFile) {
            UpdateInstallResult.Failed("No se encontró el paquete descargado en $installerPath")
        } else {
            install(packageFile)
        }
    }

    private fun install(packageFile: File): UpdateInstallResult {
        val environment = currentDesktopInstallEnvironment()
        val exists: (String) -> Boolean = { File(it).exists() }
        val readText: (String) -> String? = { PlatformFileSystem.readText(it) }

        return when (DesktopInstall.detect(environment, exists, readText)) {
            UpdateInstallMethod.APPIMAGE -> applyAppImage(packageFile, environment.env, exists)
            UpdateInstallMethod.TARBALL -> applyPortable(packageFile, environment, readText)
            else -> handOverToSystem(packageFile, environment.osName)
        }
    }

    private fun applyAppImage(
        packageFile: File,
        env: Map<String, String>,
        exists: (String) -> Boolean,
    ): UpdateInstallResult =
        when (val target = DesktopInstall.appImagePath(env, exists)?.let(::File)) {
            null -> UpdateInstallResult.Failed("No se pudo localizar el AppImage en ejecución")
            else ->
                if (target.parentFile?.canWrite() != true) {
                    UpdateInstallResult.Failed("Sin permisos para reemplazar ${target.absolutePath}")
                } else {
                    startReplacement(
                        resourceName = APPIMAGE_HELPER,
                        arguments = listOf(target.absolutePath, packageFile.absolutePath),
                    )
                }
        }

    private fun applyPortable(
        packageFile: File,
        environment: DesktopInstallEnvironment,
        readText: (String) -> String?,
    ): UpdateInstallResult =
        when (val install = DesktopInstall.portableInstall(environment, readText)) {
            null -> UpdateInstallResult.Failed("No se pudo localizar la instalación de Melo")
            else ->
                if (File(install.installDir).parentFile?.canWrite() != true) {
                    UpdateInstallResult.Failed("Sin permisos para actualizar ${install.installDir}")
                } else {
                    startReplacement(
                        resourceName = PORTABLE_HELPER,
                        arguments =
                            listOf(
                                packageFile.absolutePath,
                                install.installDir,
                                install.binDir,
                                LAUNCHER_NAME,
                            ),
                    )
                }
        }

    private fun handOverToSystem(
        packageFile: File,
        osName: String,
    ): UpdateInstallResult {
        val name = packageFile.name.lowercase()
        val command =
            when {
                osName.contains("win") && name.endsWith(".msi") ->
                    listOf("msiexec.exe", "/i", packageFile.absolutePath, "/passive")

                osName.contains("win") && name.endsWith(".exe") ->
                    listOf(packageFile.absolutePath, "/SILENT", "/CLOSEAPPLICATIONS", "/RESTARTAPPLICATIONS")

                osName.contains("mac") -> listOf("open", packageFile.absolutePath)

                else -> listOf("xdg-open", packageFile.absolutePath)
            }

        val result = start(command, "el instalador del sistema")

        // Windows installers replace the running app themselves, so Melo steps aside.
        return if (osName.contains("win") && result == UpdateInstallResult.Started) {
            exitProcess(0)
        } else {
            result
        }
    }

    private fun startReplacement(
        resourceName: String,
        arguments: List<String>,
    ): UpdateInstallResult {
        val script = materialize(resourceName)
        if (script == null) {
            return UpdateInstallResult.Failed("No se pudo preparar el asistente de actualización ($resourceName)")
        }

        val command = listOf("sh", script.absolutePath, currentPid().toString()) + arguments
        val result = start(command, "el asistente de actualización")
        if (result is UpdateInstallResult.Failed) {
            return result
        }

        // The helper waits for this process to disappear before swapping the install.
        exitProcess(0)
    }

    private fun start(
        command: List<String>,
        label: String,
    ): UpdateInstallResult =
        try {
            ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile()))
                .start()
            UpdateInstallResult.Started
        } catch (e: IOException) {
            UpdateInstallResult.Failed("No se pudo iniciar $label: ${e.message}")
        }

    private fun materialize(resourceName: String): File? {
        val stream = javaClass.getResourceAsStream("/update/$resourceName") ?: return null
        val text = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

        return runCatching {
            val script = File(tempDir(), "melo-$resourceName")
            script.writeText(text.replace("\r\n", "\n"))
            script.setExecutable(true, true)
            script
        }.getOrNull()
    }

    private fun tempDir(): File = File(PlatformFileSystem.getTempDirectory())

    private fun logFile(): File = File(tempDir(), "melo-update.log").also { it.parentFile?.mkdirs() }

    private fun currentPid(): Long = ProcessHandle.current().pid()

    private companion object {
        const val APPIMAGE_HELPER = "appimage-update.sh"
        const val PORTABLE_HELPER = "portable-update.sh"
        const val LAUNCHER_NAME = "melo-gui"
    }
}
