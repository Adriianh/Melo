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