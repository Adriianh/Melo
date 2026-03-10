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
    implementation("org.xerial:sqlite-jdbc:3.45.2.0")

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

