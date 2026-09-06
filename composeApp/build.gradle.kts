import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("buildsrc.convention.compose-multiplatform")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))
                implementation(project(":data"))

                implementation(libs.lifecycle.viewmodel.compose)
                implementation(libs.lifecycle.runtime.compose)

                // Koin
                implementation(libs.koinCore)
                implementation(libs.koinCompose)
                implementation(libs.koinComposeViewModel)

                // Ktor & Serialization
                implementation(libs.ktorClientCore)
                implementation(libs.ktorClientCio)
                implementation(libs.ktorClientContentNegotiation)
                implementation(libs.ktorSerializationKotlinxJson)
                implementation(libs.kotlinxSerialization)

                // Compose Icons
                implementation(libs.material.icons.extended)

                // Dynamic Palette
                implementation(libs.kmpaletteCore)
                implementation(libs.kmpaletteByteArray)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activityCompose)
                implementation(compose.preview)
                implementation(libs.koinAndroid)
                implementation(libs.coilCompose)
                implementation(libs.coilNetworkKtor)
                implementation(libs.androidx.media3.session)
                implementation(libs.androidx.media3.exoplayer)
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(compose.preview)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.coilCompose)
                implementation(libs.coilNetworkKtor)
                implementation(libs.sqliteJdbc)
                implementation(libs.jmtc)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinxCoroutinesTest)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.mockk)
            }
        }
    }
}

android {
    namespace = "com.github.adriianh.melo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.github.adriianh.melo"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "com.github.adriianh.melo.MainKt"

        jvmArgs += listOf(
            "-XX:-UseJVMCICompiler",
            "-Xms32m",
            "-Xmx256m",
            "-Xss384k",
            "-XX:+UseG1GC",
            "-XX:G1PeriodicGCInterval=10000",
            "-XX:MinHeapFreeRatio=15",
            "-XX:MaxHeapFreeRatio=30",
            "-XX:+UseStringDeduplication",

            "-XX:CICompilerCount=2",
            "-XX:ParallelGCThreads=2",
            "-XX:ConcGCThreads=1",
            "-XX:G1ConcRefinementThreads=2",

            "-XX:ReservedCodeCacheSize=48m",
            "-XX:MaxMetaspaceSize=96m",
            "-XX:CompressedClassSpaceSize=32m",

            "-Dskia.resource.cache.maxBytes=33554432",

            "-Dkotlinx.coroutines.defaultParallelism=4",
            "-Dkotlinx.coroutines.io.parallelism=8",
        )

        nativeDistributions {
            // Note: Each format can only be built on its native OS (jpackage limitation).
            // CI builds each on the appropriate runner (macOS, Windows, Ubuntu).
            targetFormats(
                TargetFormat.Dmg,       // macOS disk image
                TargetFormat.Msi,       // Windows MSI installer (for enterprise/silent installs)
                TargetFormat.Exe,       // Windows EXE installer (NSIS, user-friendly wizard)
                TargetFormat.Deb,       // Debian/Ubuntu package
                // TargetFormat.Rpm,    // Fedora/RHEL package (uncomment if rpmbuild is installed)
            )

            packageName = "Melo"
            packageVersion = "1.0.1"
            vendor = "Adriianh"
            description = "Modern, fast, and cross-platform music player"
            copyright = "© 2025 Adriianh. Licensed under GPLv3."
            licenseFile.set(project.rootProject.file("LICENSE"))

            linux {
                packageName = "melo"
                debMaintainer = "adriianh@github.com"
                appCategory = "Audio"
                shortcut = true
                menuGroup = "AudioVideo"
                // iconFile.set(project.file("src/jvmMain/resources/icons/icon.png"))
            }
            macOS {
                bundleID = "com.github.adriianh.melo"
                dockName = "Melo"
                // iconFile.set(project.file("src/jvmMain/resources/icons/icon.icns"))
            }
            windows {
                menuGroup = "Melo"
                menu = true
                shortcut = true
                dirChooser = true
                upgradeUuid = "d3b07384-d9a1-4e3b-8c1a-2f0e5a8d7b42"
                // iconFile.set(project.file("src/jvmMain/resources/icons/icon.ico"))
            }

            modules(
                "java.base",
                "java.desktop",
                "java.logging",
                "java.management",
                "java.naming",
                "java.net.http",
                "java.sql",
                "jdk.unsupported",
                "jdk.crypto.ec",
                "jdk.security.auth",
            )
        }
    }
}

// For distros that don't use .deb/.rpm (Arch, Gentoo, NixOS, Void, etc.)
// Packages the unpacked distributable directory into a portable tarball with an
// optimized launcher script (melo.sh) that sets MALLOC_ARENA_MAX=2.
// Usage: ./gradlew :composeApp:packageLinuxTarGz
val generateLinuxLauncher = tasks.register("generateLinuxLauncher") {
    description = "Generates a Linux launcher script for the application"
    dependsOn(tasks.named("createDistributable"))
    val appDir = layout.buildDirectory.dir("compose/binaries/main/app/Melo")
    outputs.dir(appDir)
    doLast {
        val script = appDir.get().file("melo.sh").asFile
        script.writeText(
            """
            #!/bin/sh
            export MALLOC_ARENA_MAX=2
            SCRIPT_DIR="${'$'}(cd "${'$'}(dirname "${'$'}0")" && pwd)"
            exec "${'$'}SCRIPT_DIR/bin/Melo" "${'$'}@"
            """.trimIndent() + "\n"
        )
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