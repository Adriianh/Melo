package buildsrc.convention

import java.net.URI
import java.util.zip.ZipFile

val prepareVlcWindows = tasks.register("prepareVlcWindows") {
    group = "compose desktop"
    description = "Downloads and bundles VLC native libraries for Windows distribution"
    onlyIf { org.gradle.internal.os.OperatingSystem.current().isWindows }
    val vlcDir = layout.buildDirectory.dir("app-resources/windows/vlc")
    outputs.dir(vlcDir)

    doLast {
        val targetDir = vlcDir.get().asFile
        if (File(targetDir, "libvlc.dll").exists()) {
            return@doLast
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

        logger.lifecycle("Extracting VLC native libraries for Windows...")
        ZipFile(zipFile).use { zip ->
            for (entry in zip.entries().asSequence()) {
                val name = entry.name.substringAfter('/') // strip "vlc-3.0.21/"
                if (name == "libvlc.dll" || name == "libvlccore.dll" || name.startsWith("plugins/")) {
                    val destFile = File(targetDir, name)
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        }
        logger.lifecycle("VLC Windows native libraries ready at: ${targetDir.absolutePath}")
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
    archiveVersion.set("1.0.1")
    archiveClassifier.set("linux-x64")
    archiveExtension.set("tar.gz")
    compression = Compression.GZIP

    destinationDirectory.set(layout.buildDirectory.dir("compose/binaries/main/tar"))

    into("melo-1.0.1") {
        from(layout.buildDirectory.dir("compose/binaries/main/app/Melo"))
        from(rootProject.file("packaging/linux/melo.desktop"))
        from(rootProject.file("packaging/linux/install.sh"))
        from(rootProject.file("packaging/linux/uninstall.sh"))

        eachFile {
            if (path.contains("/bin/") || path.endsWith(".so") || path.contains("/runtime/bin/") || path.endsWith(
                    ".sh"
                )
            ) {
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

        appDirFile.copyRecursively(targetAppDir, overwrite = true)

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
        val appImageOutputFile = File(outDirFile, "Melo-1.0.1-x86_64.AppImage")

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
        "/DMyAppVersion=1.0.1",
        "/DAppSourceDir=${appSourceDir.absolutePath}",
        "/DOutputDir=${outputDir.absolutePath}",
        issFile.absolutePath
    )
}