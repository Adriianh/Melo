plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    kotlin("plugin.serialization")
    id("app.cash.sqldelight")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":core"))
                implementation(project(":innertube"))
                implementation(libs.bundles.ktor)
                implementation(libs.kotlinxSerialization)
                implementation(libs.kotlinxCoroutines)
                implementation(libs.sqldelightRuntime)
                implementation(libs.sqldelightCoroutinesExtensions)
                implementation(libs.kotlinxDatetime)
                implementation(libs.koinCore)
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.dotenv)
                implementation(libs.sqldelightSqliteDriver)
                implementation(libs.sqliteJdbc)
                implementation(libs.jaudiotagger)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelightAndroidDriver)
                implementation(libs.jaudiotagger)
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(libs.sqldelightNativeDriver)
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
                implementation(libs.ktorClientMock)
            }
        }
    }
}

sqldelight {
    databases {
        create("MeloDatabase") {
            packageName.set("com.github.adriianh.data.local")
            version = 3
            verifyMigrations = true
        }
    }
}
