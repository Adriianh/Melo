pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            mavenContent {
                snapshotsOnly()
            }
        }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Melo"

include(":core")
include(":data")
include(":tui")
include(":innertube")
include(":composeApp")
