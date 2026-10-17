package com.github.adriianh.core.platform

import com.github.adriianh.core.domain.model.update.UpdateInstallMethod
import java.io.File

/**
 * A portable GUI install that can be replaced in place. Resolved either from its
 * `.melo-install` marker or from the app-image location itself.
 */
data class MeloPortableInstall(
    val installDir: String,
    val binDir: String,
    val scope: String,
)

/**
 * Everything the desktop install resolution reads: OS, home, environment and the
 * process facts that reveal the app-image directory. Bundled so the resolution
 * stays pure and unit-testable.
 */
data class DesktopInstallEnvironment(
    val osName: String,
    val home: String,
    val env: Map<String, String>,
    val processCommand: String?,
    val classpath: String,
)

/**
 * Resolves how the desktop application was installed from disk and process facts.
 */
object DesktopInstall {
    const val MARKER_FILE = ".melo-install"
    private const val GUI_COMPONENT = "gui"
    private const val GUI_DIR_NAME = "melo-gui"
    private const val DEFAULT_SCOPE = "user"
    private const val LAUNCHER_NAME = "Melo"
    private const val BIN_DIR_NAME = "bin"
    private const val LIB_RUNTIME_MARKER = "/lib/runtime/"
    private const val LIB_APP_MARKER = "/lib/app/"
    private val SYSTEM_DIRS = listOf("/opt/", "/usr/")

    fun detect(
        environment: DesktopInstallEnvironment,
        exists: (String) -> Boolean,
        readFile: (String) -> String?,
    ): UpdateInstallMethod =
        when {
            appImagePath(environment.env, exists) != null -> UpdateInstallMethod.APPIMAGE
            portableInstall(environment, readFile) != null -> UpdateInstallMethod.TARBALL
            isPackageManaged(environment) -> UpdateInstallMethod.PACKAGE_MANAGER
            else -> UpdateInstallMethod.UNKNOWN
        }

    /** The AppImage file this process was started from, when it exists. */
    fun appImagePath(
        env: Map<String, String>,
        exists: (String) -> Boolean,
    ): String? = env["APPIMAGE"]?.takeIf { it.isNotBlank() && exists(it) }

    fun portableInstall(
        environment: DesktopInstallEnvironment,
        readFile: (String) -> String?,
    ): MeloPortableInstall? {
        val home = environment.home
        val candidates =
            buildList {
                environment.env["MELO_INSTALL_DIR"]?.takeIf { it.isNotBlank() }?.let { add(it) }
                if (home.isNotBlank()) add(File(File(home, ".local/share"), GUI_DIR_NAME).path)
                add("/opt/$GUI_DIR_NAME")
            }
        return candidates.firstNotNullOfOrNull { portableInstallAt(it, home, readFile) }
            ?: markerlessUserInstall(environment)
    }

    fun parseMarker(text: String): Map<String, String> =
        text
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull(::parseMarkerLine)
            .toMap()

    /**
     * The app-image directory this process runs from, taken from the process command
     * (`<appDir>/lib/runtime/bin/java` or `<appDir>/bin/Melo`) or from the classpath
     * entries under `<appDir>/lib/app`.
     */
    fun resolveAppDir(
        processCommand: String?,
        classpath: String,
    ): String? =
        processCommand?.let { appDirBefore(it, LIB_RUNTIME_MARKER) ?: launcherDir(it) }
            ?: classpath.split(File.pathSeparator).firstNotNullOfOrNull { appDirBefore(it, LIB_APP_MARKER) }

    private fun userGuiDir(home: String): String? =
        home.takeIf { it.isNotBlank() }?.let { File(File(it, ".local/share"), GUI_DIR_NAME).path }

    private fun portableInstallAt(
        dir: String,
        home: String,
        readFile: (String) -> String?,
    ): MeloPortableInstall? =
        readFile(File(dir, MARKER_FILE).path)
            ?.let(::parseMarker)
            ?.takeIf { it["MELO_COMPONENT"] == GUI_COMPONENT }
            ?.let { values ->
                MeloPortableInstall(
                    installDir = values["MELO_INSTALL_DIR"] ?: dir,
                    binDir = values["MELO_BIN_DIR"] ?: File(File(home, ".local"), BIN_DIR_NAME).path,
                    scope = values["MELO_SCOPE"] ?: DEFAULT_SCOPE,
                )
            }

    /**
     * Installs created before the `.melo-install` marker existed still live at the
     * standard per-user location, which is enough to recognise and update them.
     */
    private fun markerlessUserInstall(environment: DesktopInstallEnvironment): MeloPortableInstall? =
        userGuiDir(environment.home)
            ?.takeIf { it == resolveAppDir(environment.processCommand, environment.classpath) }
            ?.let {
                MeloPortableInstall(
                    installDir = it,
                    binDir = File(File(environment.home, ".local"), BIN_DIR_NAME).path,
                    scope = DEFAULT_SCOPE,
                )
            }

    private fun isPackageManaged(environment: DesktopInstallEnvironment): Boolean {
        val osName = environment.osName
        val isLinux = osName.contains("linux") || osName.contains("nux")
        val appDir = resolveAppDir(environment.processCommand, environment.classpath)
        return isLinux && appDir != null && SYSTEM_DIRS.any { appDir.startsWith(it) }
    }

    private fun appDirBefore(
        path: String,
        marker: String,
    ): String? {
        val index = path.indexOf(marker)
        return if (index > 0) path.substring(0, index) else null
    }

    private fun launcherDir(command: String): String? {
        val launcher = File(command)
        return if (launcher.name == LAUNCHER_NAME && launcher.parentFile?.name == BIN_DIR_NAME) {
            launcher.parentFile?.parentFile?.path
        } else {
            null
        }
    }
}

/** Reads the environment and process facts of the running JVM. */
fun currentDesktopInstallEnvironment(): DesktopInstallEnvironment =
    DesktopInstallEnvironment(
        osName = System.getProperty("os.name", "").lowercase(),
        home = System.getProperty("user.home").orEmpty(),
        env = System.getenv(),
        processCommand = currentProcessCommand(),
        classpath = System.getProperty("java.class.path").orEmpty(),
    )

private fun currentProcessCommand(): String? {
    val handle = ProcessHandle.current()
    val command = handle.info().command()
    return runCatching { command.orElse(null) }.getOrNull()
}

private fun parseMarkerLine(line: String): Pair<String, String>? {
    val separator = line.indexOf('=')
    return if (separator <= 0) {
        null
    } else {
        line.substring(0, separator).trim() to line.substring(separator + 1).trim()
    }
}
