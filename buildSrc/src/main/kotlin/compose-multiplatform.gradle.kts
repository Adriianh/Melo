package buildsrc.convention

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val compose = extensions.getByType<ComposeExtension>().dependencies
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<KotlinMultiplatformExtension> {
    jvm()

    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    @Suppress("DEPRECATION")
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.findLibrary("runtime").get())
                implementation(libs.findLibrary("foundation").get())
                implementation(libs.findLibrary("material3").get())
                implementation(libs.findLibrary("ui").get())
                implementation(libs.findLibrary("components-resources").get())
                implementation(libs.findLibrary("ui-tooling-preview").get())
            }
        }
        val androidMain by getting {
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}