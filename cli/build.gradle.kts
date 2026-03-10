import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("io.github.goooler.shadow") version "8.1.8"
    alias(libs.plugins.graalvmNative)
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

    implementation(libs.bundles.ktor)
    implementation(libs.bundles.tamboui)
    implementation(libs.jmtc)
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

// ─── GraalVM native image ───────────────────────────────────────────────────

/**
 * Produces a self-contained native binary at cli/build/native/nativeCompile/melo.
 * The binary requires no JVM and starts in milliseconds with ~15-30 MB RSS.
 *
 * Prerequisites:
 *   - GraalVM JDK 21+ installed and set as JAVA_HOME
 *   - Run: ./gradlew :cli:nativeCompile
 *
 * Reflection config for Ktor, kotlinx-serialization, JNA, JPEG/image-io, SQLite,
 * and all app DTOs lives in:
 *   cli/src/main/resources/META-INF/native-image/reachability-metadata.json
 *
 * Reference: https://ktor.io/docs/graalvm.html
 */
graalvmNative {
    toolchainDetection.set(false)
    binaries {
        named("main") {
            imageName.set(appName)
            mainClass.set("com.github.adriianh.cli.MeloKt")
            fallback.set(false)
            verbose.set(true)

            // ── Ktor / Kotlin / kotlinx ecosystem ────────────────────────
            buildArgs.addAll(
                "--initialize-at-build-time=io.ktor",
                "--initialize-at-build-time=kotlin",
                "--initialize-at-run-time=kotlin.uuid.SecureRandomHolder",
                "--initialize-at-build-time=io.github.selemba1000.linux.LinuxJMTC",
                "--initialize-at-build-time=kotlinx.coroutines",
                "--initialize-at-build-time=kotlinx.serialization",
                "--initialize-at-build-time=kotlinx.serialization.json.Json",
                "--initialize-at-build-time=kotlinx.serialization.json.JsonImpl",
                "--initialize-at-build-time=kotlinx.serialization.json.ClassDiscriminatorMode",
                "--initialize-at-build-time=kotlinx.serialization.modules.SerializersModuleKt",
                "--initialize-at-build-time=kotlinx.io",
                // ── SLF4J / logging / SQLite ─────────────────────────
                "--initialize-at-build-time=org.slf4j",
                "--initialize-at-build-time=org.sqlite",
                "--initialize-at-run-time=java.sql.DriverManager",
                // ── Native image housekeeping ───────────────────────────
                "-H:+InstallExitHandlers",
                "-H:+ReportUnsupportedElementsAtRuntime",
                "-H:+ReportExceptionStackTraces",
                // ── Required for JNA / DBus (MediaSessionManager / SMTC) ────────────
                "--enable-native-access=ALL-UNNAMED",
                "-H:DynamicProxyConfigurationFiles=${projectDir}/src/main/resources/proxy-config.json",
                "-H:ReflectionConfigurationFiles=${projectDir}/src/main/resources/reflection-config.json",
            )
        }
    }

    // Wire nativeCompile to depend on shadowJar so the fat-jar is ready first
    tasks.named("nativeCompile") {
        dependsOn(tasks.named("shadowJar"))
    }
}

// ─── distribution tasks ───────────────────────────────────────────────────────────────────

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

        jarFile.copyTo(File(rootDir, "$appName.jar"), overwrite = true)

        distSrc.resolve("unix").walkTopDown().forEach { src ->
            if (src.isFile) {
                val dest = File(rootDir, src.relativeTo(distSrc.resolve("unix")).path)
                dest.parentFile.mkdirs()
                src.copyTo(dest, overwrite = true)
                dest.setExecutable(true)
            }
        }

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

        jarFile.copyTo(File(rootDir, "$appName.jar"), overwrite = true)

        distSrc.resolve("windows").walkTopDown().forEach { src ->
            if (src.isFile) {
                val dest = File(rootDir, src.relativeTo(distSrc.resolve("windows")).path)
                dest.parentFile.mkdirs()
                src.copyTo(dest, overwrite = true)
            }
        }

        val zipFile = File(distDir, "$appName-$appVersion-windows.zip")
        ZipOutputStream(zipFile.outputStream().buffered()).use { zos ->
            stageDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.relativeTo(stageDir).path.replace("\\\\", "/")
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }

        println("Distribution ready: ${zipFile.absolutePath}")
    }
}

tasks.register("dist") {
    dependsOn(tasks.shadowJar)
    val os = System.getProperty("os.name").lowercase()
    when {
        os.contains("win") -> dependsOn("distWindows")
        else               -> dependsOn("distUnix")
    }
}
