// Convention plugin for Kotlin Multiplatform modules.
// Configures common and JVM targets with shared test settings.
package buildsrc.convention

import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("multiplatform")
}

kotlin {
    // JVM toolchain at extension level (affects all JVM targets)
    jvmToolchain(21)

    // JVM target (Desktop + TUI)
    jvm()

    // Source set dependencies
    sourceSets {
        commonMain {
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
    // Don't fail if a module has no tests (like data module which currently only has manual runners)
    filter {
        isFailOnNoMatchingTests = false
    }
    failOnNoDiscoveredTests = false
}
