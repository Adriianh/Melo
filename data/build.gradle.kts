plugins {
    id("buildsrc.convention.kotlin-jvm")
    kotlin("plugin.serialization")
    id("app.cash.sqldelight")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.bundles.ktor)
    implementation(libs.kotlinxSerialization)
    implementation(libs.dotenv)
    implementation(libs.sqldelightRuntime)
    implementation(libs.sqldelightCoroutinesExtensions)
    implementation(libs.sqldelightSqliteDriver)
    implementation(libs.sqliteJdbc)
    implementation(libs.jaudiotagger)

    implementation(project(":core"))
    implementation(project(":innertube"))
    testImplementation(libs.kotlinxCoroutinesTest)
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
