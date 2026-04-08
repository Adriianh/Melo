dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
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
include(":cli")
include(":innertube")
