import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("io.github.goooler.shadow") version "8.1.8"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(libs.clikt)
    implementation(libs.kotlinxCoroutines)
    implementation(libs.kotlinxDatetime)
    implementation(libs.koinCore)
    implementation(libs.dotenv)
    implementation(libs.scrimageCore)
    runtimeOnly(libs.slf4jSimple)
    implementation(libs.jlayer)

    implementation(libs.bundles.ktor)
    implementation(libs.bundles.tamboui)
}

val appVersion = "1.0.0"
val appName    = "melo"

// Root of the distribution script templates
val distSrc = file("src/dist")

tasks {
    shadowJar {
        archiveBaseName.set(appName)
        archiveClassifier.set("")
        archiveVersion.set("")
        manifest {
            attributes["Main-Class"] = "com.github.adriianh.cli.MeloKt"
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

// ─── distribution tasks ───────────────────────────────────────────────────────

/**
 * Produces  cli/build/dist/melo-<version>-linux.tar.gz
 *                        or melo-<version>-macos.tar.gz
 * Layout inside the archive:
 *   melo-<version>/
 *     melo.jar
 *     bin/melo       (from src/dist/unix/bin/melo)
 *     install.sh     (from src/dist/unix/install.sh)
 *     uninstall.sh   (from src/dist/unix/uninstall.sh)
 */
tasks.register("distUnix") {
    dependsOn(tasks.shadowJar)

    val osTag    = if (System.getProperty("os.name").lowercase().contains("mac")) "macos" else "linux"
    val distOut  = layout.buildDirectory.dir("dist")
    val stageOut = layout.buildDirectory.dir("dist/stage")

    inputs.dir(distSrc.resolve("unix"))
    outputs.file(layout.buildDirectory.file("dist/$appName-$appVersion-$osTag.tar.gz"))

    doLast {
        val jarFile  = tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar")
                            .get().archiveFile.get().asFile
        val stageDir = stageOut.get().asFile
        val rootDir  = File(stageDir, "$appName-$appVersion")
        val distDir  = distOut.get().asFile

        stageDir.deleteRecursively()
        distDir.mkdirs()

        // Copy the JAR
        jarFile.copyTo(File(rootDir, "$appName.jar"), overwrite = true)

        // Copy scripts from src/dist/unix/ preserving the directory tree
        distSrc.resolve("unix").walkTopDown().forEach { src ->
            if (src.isFile) {
                val dest = File(rootDir, src.relativeTo(distSrc.resolve("unix")).path)
                dest.parentFile.mkdirs()
                src.copyTo(dest, overwrite = true)
                dest.setExecutable(true)
            }
        }

        // Package as tar.gz
        val tarName = "$appName-$appVersion-$osTag.tar.gz"
        val result  = ProcessBuilder("tar", "-czf", File(distDir, tarName).absolutePath, "$appName-$appVersion")
            .directory(stageDir)
            .inheritIO()
            .start()
            .waitFor()
        check(result == 0) { "tar failed with exit code $result" }

        println("Distribution ready: ${File(distDir, tarName).absolutePath}")
    }
}

/**
 * Produces  cli/build/dist/melo-<version>-windows.zip
 * Layout inside the archive:
 *   melo-<version>/
 *     melo.jar
 *     bin/melo.bat   (from src/dist/windows/bin/melo.bat)
 *     bin/melo.ps1   (from src/dist/windows/bin/melo.ps1)
 *     install.ps1    (from src/dist/windows/install.ps1)
 *     uninstall.ps1  (from src/dist/windows/uninstall.ps1)
 */
tasks.register("distWindows") {
    dependsOn(tasks.shadowJar)

    val distOut  = layout.buildDirectory.dir("dist")
    val stageOut = layout.buildDirectory.dir("dist/stage")

    inputs.dir(distSrc.resolve("windows"))
    outputs.file(layout.buildDirectory.file("dist/$appName-$appVersion-windows.zip"))

    doLast {
        val jarFile  = tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar")
                            .get().archiveFile.get().asFile
        val stageDir = stageOut.get().asFile
        val rootDir  = File(stageDir, "$appName-$appVersion")
        val distDir  = distOut.get().asFile

        stageDir.deleteRecursively()
        distDir.mkdirs()

        // Copy the JAR
        jarFile.copyTo(File(rootDir, "$appName.jar"), overwrite = true)

        // Copy scripts from src/dist/windows/ preserving the directory tree
        distSrc.resolve("windows").walkTopDown().forEach { src ->
            if (src.isFile) {
                val dest = File(rootDir, src.relativeTo(distSrc.resolve("windows")).path)
                dest.parentFile.mkdirs()
                src.copyTo(dest, overwrite = true)
            }
        }

        // Package as zip
        val zipFile = File(distDir, "$appName-$appVersion-windows.zip")
        ZipOutputStream(zipFile.outputStream().buffered()).use { zos ->
            stageDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.relativeTo(stageDir).path.replace("\\", "/")
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }

        println("Distribution ready: ${zipFile.absolutePath}")
    }
}

// Convenience task: build the right distribution for the current OS
tasks.register("dist") {
    dependsOn(tasks.shadowJar)
    val os = System.getProperty("os.name").lowercase()
    when {
        os.contains("win") -> dependsOn("distWindows")
        else               -> dependsOn("distUnix")
    }
}