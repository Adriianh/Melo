package buildsrc.convention

import java.net.URI
import java.util.zip.ZipFile

val desktopAppVersion: String =
    project.providers.gradleProperty("melo.version").getOrElse("2.6.0")

/**
 * Whether [path] (relative to the packaged application root) must stay executable.
 *
 * `jspawnhelper` and `jexec` are invoked by the bundled JVM itself. When they lose
 * their execute bit, `ProcessBuilder.start()` fails on Linux with
 * `posix_spawn failed, error: 13 (Permission denied)` and features such as browser
 * sign-in or the updater cannot launch any child process.
 */
fun requiresExecutableBit(path: String): Boolean {
    val segments = path.split('/')
    val fileName = segments.last()
    return "bin" in segments ||
        fileName.endsWith(".so") ||
        fileName.endsWith(".sh") ||
        fileName == "jspawnhelper" ||
        fileName == "jexec"
}

private val excludedVlcPluginDirs =
    setOf(
        "access_output",
        "control",
        "d3d11",
        "d3d9",
        "gui",
        "lua",
        "mux",
        "services_discovery",
        "spu",
        "stream_out",
        "text_renderer",
        "video_chroma",
        "video_filter",
        "video_output",
        "video_splitter",
        "visualization",
    )

private val excludedVlcPluginFiles =
    setOf(
        "libaccess_srt_plugin.dll",
        "libaom_plugin.dll",
        "libaribsub_plugin.dll",
        "libbluray-j2se-1.3.2.jar",
        "libcc_plugin.dll",
        "libcdda_plugin.dll",
        "libcvdsub_plugin.dll",
        "libdav1d_plugin.dll",
        "libdcp_plugin.dll",
        "libdshow_plugin.dll",
        "libdtv_plugin.dll",
        "libdvbsub_plugin.dll",
        "libdvdnav_plugin.dll",
        "libdvdread_plugin.dll",
        "libftp_plugin.dll",
        "libjpeg_plugin.dll",
        "libkate_plugin.dll",
        "liblibass_plugin.dll",
        "liblibbluray_plugin.dll",
        "liblive555_plugin.dll",
        "libnfs_plugin.dll",
        "libpng_plugin.dll",
        "librist_plugin.dll",
        "librtp_plugin.dll",
        "libsatip_plugin.dll",
        "libschroedinger_plugin.dll",
        "libscreen_plugin.dll",
        "libsdl_image_plugin.dll",
        "libsftp_plugin.dll",
        "libshm_plugin.dll",
        "libsmb_plugin.dll",
        "libspudec_plugin.dll",
        "libstl_plugin.dll",
        "libsubsdec_plugin.dll",
        "libsubstx3g_plugin.dll",
        "libsubsusf_plugin.dll",
        "libsvcdsub_plugin.dll",
        "libsvgdec_plugin.dll",
        "libtelx_plugin.dll",
        "libtheora_plugin.dll",
        "libttml_plugin.dll",
        "libvcd_plugin.dll",
        "libvdr_plugin.dll",
        "libvnc_plugin.dll",
        "libvpx_plugin.dll",
        "libwebvtt_plugin.dll",
        "libx26410b_plugin.dll",
        "libx264_plugin.dll",
        "libx265_plugin.dll",
        "libzvbi_plugin.dll",
    )

private fun shouldIncludeVlcPlugin(relativePluginPath: String): Boolean {
    val parts = relativePluginPath.split('/')
    val category = parts.firstOrNull().orEmpty()
    val fileName = parts.lastOrNull().orEmpty()
    return category !in excludedVlcPluginDirs && fileName !in excludedVlcPluginFiles
}

val prepareVlcWindows = tasks.register("prepareVlcWindows") {
    group = "compose desktop"
    description = "Downloads and bundles audio-only VLC native libraries for Windows distribution"
    onlyIf { org.gradle.internal.os.OperatingSystem.current().isWindows }
    inputs.property("vlcAudioOnlyFilterVersion", 1)
    val vlcDir = layout.buildDirectory.dir("app-resources/windows/vlc")
    outputs.dir(vlcDir)

    doLast {
        val targetDir = vlcDir.get().asFile
        val pluginsDir = File(targetDir, "plugins")
        val pluginsCache = File(pluginsDir, "plugins.dat")
        val hasStaleVideoPlugins = File(pluginsDir, "gui").exists()
        if (File(targetDir, "libvlc.dll").exists() && pluginsCache.exists() && !hasStaleVideoPlugins) {
            return@doLast
        }
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        targetDir.mkdirs()

        val downloadDir = layout.buildDirectory.dir("tmp/vlc-download").get().asFile
        downloadDir.mkdirs()
        val zipFile = File(downloadDir, "vlc-3.0.21-win64.zip")

        if (!zipFile.exists() || zipFile.length() < 70_000_000L) {
            logger.lifecycle("Downloading VLC 3.0.21 x64 for Windows distribution (~74 MB)...")
            URI.create("https://download.videolan.org/pub/videolan/vlc/3.0.21/win64/vlc-3.0.21-win64.zip")
                .toURL().openStream().use { input ->
                    zipFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
        }

        logger.lifecycle("Extracting audio-only VLC native libraries for Windows...")
        val cacheGenExe = File(targetDir, "vlc-cache-gen.exe")
        ZipFile(zipFile).use { zip ->
            for (entry in zip.entries().asSequence()) {
                val name = entry.name.substringAfter('/') // strip "vlc-3.0.21/"
                val isCoreBinary =
                    name == "libvlc.dll" || name == "libvlccore.dll" || name == "vlc-cache-gen.exe"
                val isAudioPlugin =
                    name.startsWith("plugins/") &&
                        !entry.isDirectory &&
                        shouldIncludeVlcPlugin(name.removePrefix("plugins/"))
                if (isCoreBinary || isAudioPlugin) {
                    val destFile = File(targetDir, name)
                    destFile.parentFile.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
        if (cacheGenExe.exists() && pluginsDir.exists()) {
            val exitCode =
                ProcessBuilder(cacheGenExe.absolutePath, pluginsDir.absolutePath)
                    .directory(targetDir)
                    .inheritIO()
                    .start()
                    .waitFor()
            if (exitCode != 0) {
                logger.warn("vlc-cache-gen exited with code $exitCode; continuing without plugins.dat")
            }
            cacheGenExe.delete()
        }
        logger.lifecycle("VLC Windows audio-only libraries ready at: ${targetDir.absolutePath}")
    }
}

tasks.matching {
    it.name == "prepareAppResources" ||
            it.name.contains("packageMsi") ||
            it.name.contains("packageExe") ||
            (org.gradle.internal.os.OperatingSystem.current().isWindows && it.name == "createDistributable")
}.configureEach {
    dependsOn(prepareVlcWindows)
}

tasks.matching {
    org.gradle.internal.os.OperatingSystem.current().isWindows && it.name == "createDistributable"
}.configureEach {
    val distVlcDir = layout.buildDirectory.dir("compose/binaries/main/app/Melo/app/resources/vlc")
    val vlcZipFile = layout.buildDirectory.file("tmp/vlc-download/vlc-3.0.21-win64.zip")
    doLast {
        val targetVlcDir = distVlcDir.get().asFile
        val distPluginsDir = File(targetVlcDir, "plugins")
        val zipFile = vlcZipFile.get().asFile
        if (!distPluginsDir.exists() || !zipFile.exists()) {
            return@doLast
        }
        val cacheGenExe = File(targetVlcDir, "vlc-cache-gen.exe")
        try {
            ZipFile(zipFile).use { zip ->
                val entry = zip.entries().asSequence().firstOrNull {
                    it.name.substringAfter('/') == "vlc-cache-gen.exe"
                }
                if (entry != null) {
                    zip.getInputStream(entry).use { input ->
                        cacheGenExe.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            if (cacheGenExe.exists()) {
                val exitCode =
                    ProcessBuilder(cacheGenExe.absolutePath, distPluginsDir.absolutePath)
                        .directory(targetVlcDir)
                        .inheritIO()
                        .start()
                        .waitFor()
                if (exitCode != 0) {
                    logger.warn("Post-copy vlc-cache-gen exited with code $exitCode")
                }
            }
        } finally {
            cacheGenExe.delete()
        }
    }
}

val generateLinuxLauncher = tasks.register("generateLinuxLauncher") {
    description = "Generates a Linux launcher script for the application"
    dependsOn("createDistributable")
    val appDir = layout.buildDirectory.dir("compose/binaries/main/app/Melo")
    outputs.dir(appDir)
    doLast {
        val script = appDir.get().file("melo.sh").asFile
        val s = "$"
        val scriptContent = """
            #!/bin/sh
            export MALLOC_ARENA_MAX=2
            SCRIPT_DIR="${s}(cd "${s}(dirname "${s}0")" && pwd)"
            exec "${s}SCRIPT_DIR/bin/Melo" "${s}@"
        """.trimIndent() + "\n"
        script.writeText(scriptContent)
        script.setExecutable(true, false)
    }
}

tasks.register<Tar>("packageLinuxTarGz") {
    group = "compose desktop"
    description = "Packages a .tar.gz archive of the Linux distributable for Arch/generic distros"

    dependsOn(generateLinuxLauncher)

    archiveBaseName.set("melo")
    archiveVersion.set(desktopAppVersion)
    archiveClassifier.set("linux-x64")
    archiveExtension.set("tar.gz")
    compression = Compression.GZIP

    destinationDirectory.set(layout.buildDirectory.dir("compose/binaries/main/tar"))

    into("melo-$desktopAppVersion") {
        from(layout.buildDirectory.dir("compose/binaries/main/app/Melo"))
        from(rootProject.file("packaging/linux/melo.desktop"))
        from(rootProject.file("packaging/linux/install.sh"))
        from(rootProject.file("packaging/linux/uninstall.sh"))

        eachFile {
            if (requiresExecutableBit(path)) {
                permissions {
                    unix("rwxr-xr-x")
                }
            }
        }
    }
}

val packageAppImage = tasks.register("packageAppImage") {
    group = "compose desktop"
    description = "Packages a standalone AppImage for generic Linux distributions"
    dependsOn("createDistributable", generateLinuxLauncher)
    onlyIf { org.gradle.internal.os.OperatingSystem.current().isLinux }

    val appDir = layout.buildDirectory.dir("compose/binaries/main/app/Melo")
    val appImageDir = layout.buildDirectory.dir("compose/binaries/main/appimage")
    val outputDir = layout.buildDirectory.dir("compose/binaries/main/appimage/output")
    outputs.dir(outputDir)

    doLast {
        val appDirFile = appDir.get().asFile
        val targetAppDir = File(appImageDir.get().asFile, "AppDir")
        val outDirFile = outputDir.get().asFile
        outDirFile.mkdirs()
        targetAppDir.deleteRecursively()
        targetAppDir.mkdirs()

        // Copy files preserving permissions and symlinks on Linux
        val cpExit = try {
            ProcessBuilder("cp", "-a", "${appDirFile.absolutePath}/.", targetAppDir.absolutePath)
                .inheritIO()
                .start()
                .waitFor()
        } catch (e: Exception) {
            -1
        }
        if (cpExit != 0) {
            appDirFile.copyRecursively(targetAppDir, overwrite = true)
        }

        // Ensure all executables, binaries, and scripts have execution permissions
        targetAppDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val relPath = file.relativeTo(targetAppDir).path
                val srcFile = File(appDirFile, relPath)
                if (srcFile.canExecute() || requiresExecutableBit(relPath)) {
                    file.setExecutable(true, false)
                }
            }
        }

        val desktopFile = rootProject.file("packaging/linux/melo.desktop")
        val iconFile = project.file("src/jvmMain/resources/icons/icon.png")
        if (desktopFile.exists()) {
            desktopFile.copyTo(File(targetAppDir, "melo.desktop"), overwrite = true)
        }
        if (iconFile.exists()) {
            iconFile.copyTo(File(targetAppDir, "melo.png"), overwrite = true)
        }

        val appRun = File(targetAppDir, "AppRun")
        appRun.writeText(
            """
            #!/bin/sh
            HERE="${'$'}(dirname "${'$'}(readlink -f "${'$'}{0}")")"
            export MALLOC_ARENA_MAX=2
            export PATH="${'$'}{HERE}/bin:${'$'}{PATH}"
            export LD_LIBRARY_PATH="${'$'}{HERE}/lib:${'$'}{LD_LIBRARY_PATH}"
            exec "${'$'}{HERE}/bin/Melo" "${'$'}@"
            """.trimIndent() + "\n"
        )
        appRun.setExecutable(true, false)

        val toolDir = layout.buildDirectory.dir("tmp/appimagetool").get().asFile
        toolDir.mkdirs()
        val toolBin = File(toolDir, "appimagetool")

        val systemTool = listOf("/usr/bin/appimagetool", "/usr/local/bin/appimagetool")
            .firstOrNull { File(it).exists() }

        val toolExec = if (systemTool != null) {
            systemTool
        } else {
            if (!toolBin.exists() || toolBin.length() < 1_000_000L) {
                logger.lifecycle("Downloading appimagetool for AppImage generation (~14 MB)...")
                URI.create("https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage")
                    .toURL().openStream().use { input ->
                        toolBin.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                toolBin.setExecutable(true, false)
            }
            toolBin.absolutePath
        }

        logger.lifecycle("Building AppImage with ${toolExec}...")
        val appImageOutputFile = File(outDirFile, "Melo-${desktopAppVersion}-x86_64.AppImage")

        val process = ProcessBuilder(
            toolExec,
            "--appimage-extract-and-run",
            targetAppDir.absolutePath,
            appImageOutputFile.absolutePath
        )
            .directory(targetAppDir)
            .apply {
                environment()["ARCH"] = "x86_64"
            }
            .inheritIO()
            .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException("appimagetool failed with exit code $exitCode")
        }
        logger.lifecycle("AppImage generated at: ${appImageOutputFile.absolutePath}")
    }
}

val packageMacOsZip = tasks.register("packageMacOsZip") {
    group = "compose desktop"
    description = "Packages Melo.app as a macOS .zip archive for in-place auto-updates"
    dependsOn("createDistributable")
    onlyIf { org.gradle.internal.os.OperatingSystem.current().isMacOsX }

    val appBundleDir = layout.buildDirectory.dir("compose/binaries/main/app/Melo.app")
    val outputDir = layout.buildDirectory.dir("compose/binaries/main/zip")
    outputs.dir(outputDir)

    doLast {
        val bundle = appBundleDir.get().asFile
        if (!bundle.isDirectory) {
            throw GradleException("Expected macOS app bundle at ${bundle.absolutePath}")
        }
        val outDir = outputDir.get().asFile
        outDir.mkdirs()
        val zipFile = File(outDir, "Melo-${desktopAppVersion}-macos-app.zip")
        if (zipFile.exists()) {
            zipFile.delete()
        }

        val exitCode =
            ProcessBuilder(
                "ditto",
                "-c",
                "-k",
                "--sequesterRsrc",
                "--keepParent",
                bundle.absolutePath,
                zipFile.absolutePath,
            ).inheritIO().start().waitFor()

        if (exitCode != 0) {
            throw GradleException("ditto failed with exit code $exitCode while zipping ${bundle.absolutePath}")
        }
        logger.lifecycle("macOS auto-update archive generated at: ${zipFile.absolutePath}")
    }
}

val packageInnoSetup = tasks.register<Exec>("packageInnoSetup") {
    group = "compose desktop"
    description = "Builds the modern Inno Setup Windows installer"
    dependsOn("createDistributable")
    onlyIf { org.gradle.internal.os.OperatingSystem.current().isWindows }

    val issFile = rootProject.file("packaging/windows/melo.iss")
    val appSourceDir = layout.buildDirectory.dir("compose/binaries/main/app/Melo").get().asFile
    val outputDir = rootProject.file("packaging/windows/Output")

    val isccCandidate = listOf(
        "ISCC.exe",
        "C:\\Program Files (x86)\\Inno Setup 6\\ISCC.exe",
        "C:\\Program Files\\Inno Setup 6\\ISCC.exe"
    ).firstOrNull { File(it).exists() } ?: "ISCC.exe"

    doFirst {
        outputDir.mkdirs()
    }

    commandLine(
        isccCandidate,
        "/DMyAppVersion=$desktopAppVersion",
        "/DAppSourceDir=${appSourceDir.absolutePath}",
        "/DOutputDir=${outputDir.absolutePath}",
        issFile.absolutePath
    )
}
