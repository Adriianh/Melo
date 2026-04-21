plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":core"))
                implementation(libs.ktorClientCore)
                implementation(libs.ktorClientContentNegotiation)
                implementation(libs.ktorSerializationKotlinxJson)
                implementation(libs.ktorClientEncoding)
                implementation(libs.brotli)
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.ktorClientOkhttp)
                implementation(libs.newpipeExtractor)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.ktorClientOkhttp)
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(libs.ktorClientDarwin)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.junit)
                implementation(kotlin("test"))
                runtimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
            }
        }
    }
}