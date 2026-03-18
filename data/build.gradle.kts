plugins {
    id("buildsrc.convention.kotlin-jvm")
    kotlin("plugin.serialization")
    id("app.cash.sqldelight")
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

    // For audio metadata extraction
    implementation(libs.jaudiotagger)

    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
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

