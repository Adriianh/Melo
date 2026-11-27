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
    actual fun supportsAutoRestart(installerPath: String): Boolean {
        val environment = currentDesktopInstallEnvironment()
        val exists: (String) -> Boolean = { File(it).exists() }
        val readText: (String) -> String? = { PlatformFileSystem.readText(it) }
        val name = File(installerPath).name.lowercase()

        return when (DesktopInstall.detect(environment, exists, readText)) {
            UpdateInstallMethod.APPIMAGE,
            UpdateInstallMethod.TARBALL,
            -> true

            UpdateInstallMethod.MACOS_APP ->
                name.endsWith(".zip") &&
                    DesktopInstall.macAppBundlePath(environment)?.let { File(it).parentFile?.canWrite() } == true

            else -> environment.osName.contains("win") && (name.endsWith(".exe") || name.endsWith(".msi"))
        }
    }

    actual fun installAndRestart(installerPath: String): UpdateInstallResult {
        val packageFile = File(installerPath)
        return if (!packageFile.isFile) {
            UpdateInstallResult.Failed(
                reason = InstallErrorReason.PACKAGE_NOT_FOUND,
                detail = installerPath,
            )
        } else {
            install(packageFile)
        }
    }

    private fun install(packageFile: File): UpdateInstallResult {
        val environment = currentDesktopInstallEnvironment()
        val exists: (String) -> Boolean = { File(it).exists() }
        val readText: (String) -> String? = { PlatformFileSystem.readText(it) }
        val name = packageFile.name.lowercase()

        return when (DesktopInstall.detect(environment, exists, readText)) {
            UpdateInstallMethod.APPIMAGE -> applyAppImage(packageFile, environment.env, exists)
            UpdateInstallMethod.TARBALL -> applyPortable(packageFile, environment, readText)
            UpdateInstallMethod.MACOS_APP ->
                if (name.endsWith(".zip")) {
                    applyMacOsApp(packageFile, environment)
                } else {
                    handOverToSystem(packageFile, environment.osName)
                }

            else ->
                if (environment.osName.contains("win") && (name.endsWith(".exe") || name.endsWith(".msi"))) {
                    applyWindows(packageFile, environment, exists)
                } else {
                    handOverToSystem(packageFile, environment.osName)
                }
        }
    }

    private fun applyAppImage(
        packageFile: File,
        env: Map<String, String>,
        exists: (String) -> Boolean,
    ): UpdateInstallResult =
        when (val target = DesktopInstall.appImagePath(env, exists)?.let(::File)) {
            null -> UpdateInstallResult.Failed(reason = InstallErrorReason.APPIMAGE_NOT_FOUND)
            else ->
                if (target.parentFile?.canWrite() != true) {
                    UpdateInstallResult.Failed(
                        reason = InstallErrorReason.NO_WRITE_PERMISSION,
                        detail = target.absolutePath,
                    )
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
            null -> UpdateInstallResult.Failed(reason = InstallErrorReason.PORTABLE_INSTALL_NOT_FOUND)
            else ->
                if (File(install.installDir).parentFile?.canWrite() != true) {
                    UpdateInstallResult.Failed(
                        reason = InstallErrorReason.PORTABLE_NO_PERMISSION,
                        detail = install.installDir,
                    )
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

    private fun applyMacOsApp(
        packageFile: File,
        environment: DesktopInstallEnvironment,
    ): UpdateInstallResult =
        when (val bundle = DesktopInstall.macAppBundlePath(environment)?.let(::File)) {
            null -> UpdateInstallResult.Failed(reason = InstallErrorReason.PORTABLE_INSTALL_NOT_FOUND)
            else ->
                if (bundle.parentFile?.canWrite() != true) {
                    UpdateInstallResult.Failed(
                        reason = InstallErrorReason.NO_WRITE_PERMISSION,
                        detail = bundle.absolutePath,
                    )
                } else {
                    startReplacement(
                        resourceName = MACOS_HELPER,
                        arguments = listOf(packageFile.absolutePath, bundle.absolutePath),
                    )
                }
        }

    private fun applyWindows(
        packageFile: File,
        environment: DesktopInstallEnvironment,
        exists: (String) -> Boolean,
    ): UpdateInstallResult {
        val script = materialize(WINDOWS_HELPER)
        if (script == null) {
            return UpdateInstallResult.Failed(
                reason = InstallErrorReason.HELPER_PREPARE_FAILED,
                detail = WINDOWS_HELPER,
            )
        }

        val installerType = if (packageFile.name.lowercase().endsWith(".msi")) "msi" else "exe"
        val appExecutable = DesktopInstall.windowsExecutablePath(environment, exists).orEmpty()
        val command =
            listOf(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-WindowStyle",
                "Hidden",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                script.absolutePath,
                "-ParentPid",
                currentPid().toString(),
                "-InstallerPath",
                packageFile.absolutePath,
                "-InstallerType",
                installerType,
                "-AppExecutable",
                appExecutable,
            )

        val result = start(command, InstallErrorReason.START_HELPER_FAILED)
        if (result is UpdateInstallResult.Failed) {
            return result
        }

        // The helper waits for this process to disappear before launching the installer.
        exitProcess(0)
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
                    listOf(packageFile.absolutePath, "/SILENT", "/CLOSEAPPLICATIONS")

                osName.contains("mac") -> listOf("open", packageFile.absolutePath)

                else -> listOf("xdg-open", packageFile.absolutePath)
            }

        val result = start(command, InstallErrorReason.START_SYSTEM_FAILED)

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
            return UpdateInstallResult.Failed(
                reason = InstallErrorReason.HELPER_PREPARE_FAILED,
                detail = resourceName,
            )
        }

        val command = listOf("sh", script.absolutePath, currentPid().toString()) + arguments
        val result = start(command, InstallErrorReason.START_HELPER_FAILED)
        if (result is UpdateInstallResult.Failed) {
            return result
        }

        // The helper waits for this process to disappear before swapping the install.
        exitProcess(0)
    }

    private companion object {
        const val APPIMAGE_HELPER = "appimage-update.sh"
        const val PORTABLE_HELPER = "portable-update.sh"
        const val MACOS_HELPER = "macos-update.sh"
        const val WINDOWS_HELPER = "windows-update.ps1"
        const val LAUNCHER_NAME = "melo-gui"
    }
}

private fun start(
    command: List<String>,
    errorReason: InstallErrorReason,
): UpdateInstallResult =
    try {
        val builder =
            ProcessBuilder(command)
                .directory(tempDir())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile()))
        val devNull = File("/dev/null")
        if (devNull.exists()) {
            builder.redirectInput(ProcessBuilder.Redirect.from(devNull))
        }
        builder.start()
        UpdateInstallResult.Started
    } catch (e: IOException) {
        UpdateInstallResult.Failed(
            reason = errorReason,
            detail = e.message.orEmpty(),
        )
    }

private fun materialize(resourceName: String): File? {
    val stream = PlatformUpdateInstaller::class.java.getResourceAsStream("/update/$resourceName") ?: return null
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
