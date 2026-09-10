package buildsrc.convention

import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    android {
        namespace = "com.github.adriianh.${project.name}"
        compileSdk = 37
        minSdk = 24
    }

    // JVM toolchain at extension level (affects all JVM targets)
    jvmToolchain(21)

    // JVM target (Desktop + TUI)
    jvm()

    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    // Source set dependencies
    sourceSets {
        commonMain {
        }
        val androidMain by getting {
        }
        val iosMain by getting {
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
    }
    filter {
        isFailOnNoMatchingTests = false
    }
    failOnNoDiscoveredTests = false
}