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
                implementation(libs.koinCore)
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
                implementation(libs.newpipeExtractor)
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
                implementation(libs.mockk)
                implementation(libs.ktorClientMock)
                implementation(libs.ktorClientCio)
                implementation(libs.kotlinxCoroutinesTest)
                runtimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
            }
        }
    }
}