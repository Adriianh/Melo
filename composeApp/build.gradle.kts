import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("buildsrc.convention.compose-multiplatform")
    id("buildsrc.convention.desktop-packaging")
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

                implementation(libs.reorderable)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activityCompose)
                implementation(libs.koinAndroid)
                implementation(libs.coilCompose)
                implementation(libs.coilNetworkKtor)
                implementation(libs.androidx.media3.session)
                implementation(libs.androidx.media3.exoplayer)
            }
        }
        val iosMain by getting {
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.coilCompose)
                implementation(libs.coilNetworkKtor)
                implementation(libs.sqliteJdbc)
                implementation(libs.jmtc)
                implementation(libs.jnaPlatform)
                implementation(libs.slf4jSimple)
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

configure<com.android.build.api.dsl.ApplicationExtension> {
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
            "-Xms64m",
            "-Xmx384m",
            "-Xss512k",
            "-XX:+UseG1GC",
            "-XX:G1PeriodicGCInterval=10000",
            "-XX:MinHeapFreeRatio=15",
            "-XX:MaxHeapFreeRatio=30",
            "-XX:+UseStringDeduplication",

            "-XX:ParallelGCThreads=2",
            "-XX:ConcGCThreads=1",
            "-XX:G1ConcRefinementThreads=2",

            "-XX:ReservedCodeCacheSize=64m",
            "-XX:MaxMetaspaceSize=160m",
            "-XX:CompressedClassSpaceSize=48m",

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
            description = "Melo"
            copyright = "© 2025 Adriianh. Licensed under GPLv3."
            // licenseFile.set(project.rootProject.file("LICENSE"))

            linux {
                packageName = "melo"
                debMaintainer = "adriianh@github.com"
                appCategory = "Audio"
                shortcut = true
                menuGroup = "AudioVideo"
                iconFile.set(project.file("src/jvmMain/resources/icons/icon.png"))
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
                iconFile.set(project.file("src/jvmMain/resources/icons/icon.ico"))
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

            appResourcesRootDir.set(layout.buildDirectory.dir("app-resources"))
        }
    }
}