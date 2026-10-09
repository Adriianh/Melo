package com.github.adriianh.cli.service

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

interface SelfUpdater {
    fun getCurrentExecutable(): File?

    fun getDefaultExecutable(): File

    fun extractArchive(
        archiveFile: File,
        destinationDir: File,
    )

    fun findBinaryInExtracted(extractedDir: File): File?

    fun applyUpdate(
        extractedDir: File,
        targetExecutable: File,
    )

    fun cleanupOldBackups(installDir: File)
}

private object ArchiveExtractor {
    fun extractZip(
        archiveFile: File,
        destinationDir: File,
    ) {
        val canonicalDest = destinationDir.canonicalFile.toPath()
        val bufferedInput = archiveFile.inputStream().buffered()
        ZipInputStream(bufferedInput).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val targetFile = File(destinationDir, entry.name)
                val canonicalTarget = targetFile.canonicalFile.toPath()
                if (!canonicalTarget.startsWith(canonicalDest)) {
                    throw SecurityException("Zip slip detected in archive entry: ${entry.name}")
                }

                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.parentFile?.mkdirs()
                    targetFile.outputStream().use { out ->
                        zis.copyTo(out)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    fun extractTarGz(
        archiveFile: File,
        destinationDir: File,
    ) {
        val pb =
            ProcessBuilder(
                "tar",
                "-xzf",
                archiveFile.absolutePath,
                "-C",
                destinationDir.absolutePath,
            )
        pb.redirectErrorStream(true)
        val process = pb.start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val errorOutput = process.inputStream.bufferedReader().use { it.readText() }
            throw IOException("Failed to extract tar.gz (exit code $exitCode): $errorOutput")
        }
    }
}

private object WindowsUpdateHelper {
    fun backupRunningExecutable(
        targetExecutable: File,
        installDir: File,
    ) {
        if (!targetExecutable.exists()) return
        val oldExecutable = File(installDir, "${targetExecutable.name}.old")
        if (oldExecutable.exists()) {
            oldExecutable.delete()
        }
        if (!targetExecutable.renameTo(oldExecutable)) {
            val timestamped = File(installDir, "${targetExecutable.name}.old.${System.currentTimeMillis()}")
            if (!targetExecutable.renameTo(timestamped)) {
                throw IOException("Could not rename running binary ${targetExecutable.absolutePath}")
            }
        }
    }

    fun copyExtractedDlls(
        extractedDir: File,
        installDir: File,
    ) {
        val extractedDlls = extractedDir.walkTopDown().filter { it.isFile && it.name.endsWith(".dll") }
        for (dll in extractedDlls) {
            val targetDll = File(installDir, dll.name)
            safeWindowsFileCopy(dll, targetDll)

            val binDir = File(installDir, "bin")
            if (binDir.exists() && binDir.isDirectory) {
                val binDll = File(binDir, dll.name)
                safeWindowsFileCopy(dll, binDll)
            }
        }
    }

    fun copyExtractedScripts(
        extractedDir: File,
        installDir: File,
    ) {
        extractedDir
            .walkTopDown()
            .filter { file ->
                file.isFile && (file.name.endsWith(".ps1") || file.name.endsWith(".cmd") || file.name.endsWith(".bat"))
            }.forEach { scriptFile ->
                val relative = scriptFile.relativeTo(extractedDir)
                val subPath =
                    if (relative.path.startsWith("melo-") && relative.path.contains(File.separator)) {
                        relative.path.substringAfter(File.separator)
                    } else {
                        relative.path
                    }
                val targetScript = File(installDir, subPath)
                targetScript.parentFile?.mkdirs()
                safeWindowsFileCopy(scriptFile, targetScript)
            }
    }

    fun safeWindowsFileCopy(
        source: File,
        target: File,
    ) {
        if (target.exists()) {
            val oldFile = File(target.parentFile, "${target.name}.old")
            if (oldFile.exists()) {
                oldFile.delete()
            }
            if (!target.renameTo(oldFile)) {
                val timestamped = File(target.parentFile, "${target.name}.old.${System.currentTimeMillis()}")
                target.renameTo(timestamped)
            }
        }
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

private val NATIVE_MAGIC_PREFIXES =
    listOf(
        byteArrayOf(0x7F, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte()),
        byteArrayOf('M'.code.toByte(), 'Z'.code.toByte()),
        byteArrayOf(0xCF.toByte(), 0xFA.toByte(), 0xED.toByte(), 0xFE.toByte()),
        byteArrayOf(0xFE.toByte(), 0xED.toByte(), 0xFA.toByte(), 0xCF.toByte()),
        byteArrayOf(0xCE.toByte(), 0xFA.toByte(), 0xED.toByte(), 0xFE.toByte()),
        byteArrayOf(0xFE.toByte(), 0xED.toByte(), 0xFA.toByte(), 0xCE.toByte()),
        byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte()),
    )

open class NativeSelfUpdater(
    private val isWindows: Boolean = System.getProperty("os.name", "").lowercase().contains("win"),
) : SelfUpdater {
    override fun getCurrentExecutable(): File? {
        val processCmd =
            runCatching {
                ProcessHandle
                    .current()
                    .info()
                    .command()
                    .orElse(null)
            }.getOrNull()

        if (!processCmd.isNullOrBlank()) {
            val file = File(processCmd)
            if (file.isFile && file.exists()) {
                val name = file.name.lowercase()
                if (name == "melo" || name == "melo.exe") {
                    return file.canonicalFile
                }
            }
        }

        val fallbackCandidates =
            if (isWindows) {
                val localAppData = System.getenv("LOCALAPPDATA").orEmpty()
                val userProfile = System.getenv("USERPROFILE").orEmpty()
                listOfNotNull(
                    if (localAppData.isNotBlank()) File(localAppData, "melo-tui/melo.exe") else null,
                    if (localAppData.isNotBlank()) File(localAppData, "Programs/melo-tui/melo.exe") else null,
                    if (userProfile.isNotBlank()) File(userProfile, ".local/share/melo-tui/melo.exe") else null,
                )
            } else {
                val home = System.getProperty("user.home").orEmpty()
                listOfNotNull(
                    if (home.isNotBlank()) File(home, ".local/share/melo-tui/melo") else null,
                    if (home.isNotBlank()) File(home, ".local/bin/melo") else null,
                    File("/usr/local/bin/melo"),
                    File("/usr/bin/melo"),
                )
            }

        return fallbackCandidates.firstOrNull { it.isFile && it.exists() }?.canonicalFile
    }

    override fun getDefaultExecutable(): File =
        if (isWindows) {
            val base =
                System.getenv("LOCALAPPDATA")
                    ?: (System.getProperty("user.home") + "\\AppData\\Local")
            File(base, "melo-tui/melo.exe")
        } else {
            File(System.getProperty("user.home"), ".local/share/melo-tui/melo")
        }

    override fun extractArchive(
        archiveFile: File,
        destinationDir: File,
    ) {
        if (!archiveFile.exists()) {
            throw FileNotFoundException("Archive file does not exist: ${archiveFile.absolutePath}")
        }
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        val name = archiveFile.name.lowercase()
        val isTar = name.endsWith(".tar.gz") || name.endsWith(".tgz")
        when {
            name.endsWith(".zip") -> ArchiveExtractor.extractZip(archiveFile, destinationDir)
            isTar -> ArchiveExtractor.extractTarGz(archiveFile, destinationDir)
            else -> throw IllegalArgumentException("Unsupported archive format: ${archiveFile.name}")
        }
    }

    override fun findBinaryInExtracted(extractedDir: File): File? {
        val binaryName = if (isWindows) "melo.exe" else "melo"
        val candidates =
            extractedDir
                .walkTopDown()
                .filter { it.isFile && it.name.equals(binaryName, ignoreCase = isWindows) }
                .toList()

        if (candidates.isEmpty()) return null

        val nonScripts = candidates.filterNot { isShebangScript(it) }
        val pool = if (nonScripts.isNotEmpty()) nonScripts else candidates

        val outsideBin = pool.filterNot { it.parentFile?.name.equals("bin", ignoreCase = true) }
        val preferredPool = if (outsideBin.isNotEmpty()) outsideBin else pool

        val nativeBinaries = preferredPool.filter { isNativeBinary(it) }
        return nativeBinaries.maxByOrNull { it.length() }
            ?: preferredPool.maxByOrNull { it.length() }
            ?: candidates.firstOrNull()
    }

    internal fun isShebangScript(file: File): Boolean {
        if (!file.isFile || file.length() < 2L) return false
        return runCatching {
            file.inputStream().buffered().use { input ->
                val b1 = input.read()
                val b2 = input.read()
                b1 == '#'.code && b2 == '!'.code
            }
        }.getOrDefault(false)
    }

    internal fun isNativeBinary(file: File): Boolean {
        if (!file.isFile || file.length() < 4L) return false
        return runCatching {
            val header = ByteArray(4)
            file.inputStream().buffered().use { it.read(header) }
            NATIVE_MAGIC_PREFIXES.any { prefix ->
                header.take(prefix.size).toByteArray().contentEquals(prefix)
            }
        }.getOrDefault(false)
    }

    override fun applyUpdate(
        extractedDir: File,
        targetExecutable: File,
    ) {
        val newBinary =
            findBinaryInExtracted(extractedDir)
                ?: throw FileNotFoundException(
                    "Could not find Melo binary in extracted update at ${extractedDir.absolutePath}",
                )

        val installDir = targetExecutable.parentFile ?: targetExecutable.canonicalFile.parentFile
        if (!installDir.exists()) {
            installDir.mkdirs()
        }

        if (isWindows) {
            applyWindowsUpdate(newBinary, targetExecutable, installDir, extractedDir)
        } else {
            applyUnixUpdate(newBinary, targetExecutable, installDir, extractedDir)
        }

        cleanupOldBackups(installDir)
    }

    private fun applyWindowsUpdate(
        newBinary: File,
        targetExecutable: File,
        installDir: File,
        extractedDir: File,
    ) {
        WindowsUpdateHelper.backupRunningExecutable(targetExecutable, installDir)
        Files.copy(newBinary.toPath(), targetExecutable.toPath(), StandardCopyOption.REPLACE_EXISTING)
        WindowsUpdateHelper.copyExtractedDlls(extractedDir, installDir)
        WindowsUpdateHelper.copyExtractedScripts(extractedDir, installDir)
    }

    private fun applyUnixUpdate(
        newBinary: File,
        targetExecutable: File,
        installDir: File,
        extractedDir: File,
    ) {
        newBinary.setExecutable(true, false)

        val oldExecutable = File(installDir, "${targetExecutable.name}.old")
        val hadOld = targetExecutable.exists()
        if (hadOld) {
            if (oldExecutable.exists()) {
                oldExecutable.delete()
            }
            if (!targetExecutable.renameTo(oldExecutable)) {
                val timestamped = File(installDir, "${targetExecutable.name}.old.${System.currentTimeMillis()}")
                targetExecutable.renameTo(timestamped)
            }
        }

        try {
            Files.move(
                newBinary.toPath(),
                targetExecutable.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: IOException) {
            try {
                Files.move(
                    newBinary.toPath(),
                    targetExecutable.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (ex: IOException) {
                if (hadOld && oldExecutable.exists() && !targetExecutable.exists()) {
                    oldExecutable.renameTo(targetExecutable)
                }
                throw ex
            }
        }

        targetExecutable.setExecutable(true, false)

        extractedDir
            .walkTopDown()
            .filter { file ->
                file.isFile && (file.name.endsWith(".so") || file.name.endsWith(".dylib"))
            }.forEach { libFile ->
                val targetLib = File(installDir, libFile.name)
                Files.copy(libFile.toPath(), targetLib.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

        val uninstallScript = extractedDir.walkTopDown().firstOrNull { it.isFile && it.name == "uninstall.sh" }
        if (uninstallScript != null) {
            val targetUninstall = File(installDir, "uninstall.sh")
            Files.copy(uninstallScript.toPath(), targetUninstall.toPath(), StandardCopyOption.REPLACE_EXISTING)
            targetUninstall.setExecutable(true, false)
        }
    }

    override fun cleanupOldBackups(installDir: File) {
        if (!installDir.exists() || !installDir.isDirectory) {
            return
        }

        installDir.walkTopDown().maxDepth(3).forEach { file ->
            val name = file.name
            val isBackup = name.endsWith(".old") || name.contains(".old.") || name.endsWith(".bak")
            if (file.isFile && isBackup) {
                runCatching { file.delete() }
            }
        }
    }
}
