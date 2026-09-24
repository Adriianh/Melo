plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    kotlin("plugin.serialization")
}

// Single source of truth for the app version. Stable local builds default to the
// value in gradle.properties; CI overrides it (e.g. -Pmelo.version=2.1.5-nightly.20260923).
val meloVersion: String = providers.gradleProperty("melo.version").getOrElse("2.1.5")

// Generate MeloVersion.kt so the version baked into every binary matches the
// `melo.version` property used by the build (stable vs nightly).
val generateMeloVersion =
    tasks.register("generateMeloVersion") {
        group = "build"
        description = "Generates MeloVersion.kt from the melo.version build property"
        val outputDir = layout.buildDirectory.dir("generated/melo-version/kotlin")
        outputs.dir(outputDir)
        inputs.property("meloVersion", meloVersion)
        doLast {
            val packageDir = outputDir.get().file("com/github/adriianh/core/util").asFile
            packageDir.mkdirs()
            File(packageDir, "MeloVersion.kt").writeText(
                """
                package com.github.adriianh.core.util

                object MeloVersion {
                    const val CURRENT = "$meloVersion"
                }
                """.trimIndent() + "\n",
            )
        }
    }

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir(generateMeloVersion.map { it.outputs.files.singleFile })
            dependencies {
                implementation(libs.kotlinxDatetime)
                implementation(libs.kotlinxSerialization)
                implementation(libs.kotlinxCoroutines)
                implementation(libs.koinCore)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.media3.exoplayer)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.vlcj)
                implementation(libs.jmtc)
                implementation(libs.ktorClientCore)
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
