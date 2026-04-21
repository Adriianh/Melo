plugins {
    id("buildsrc.convention.kotlin-multiplatform")
    kotlin("plugin.serialization")
    id("app.cash.sqldelight")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":core"))
                implementation(project(":innertube"))
                implementation(libs.bundles.ktor)
                implementation(libs.kotlinxSerialization)
                implementation(libs.sqldelightRuntime)
                implementation(libs.sqldelightCoroutinesExtensions)
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
        jvmTest {
            dependencies {
                implementation(libs.kotlinxCoroutinesTest)
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
