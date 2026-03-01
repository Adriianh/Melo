plugins {
    id("buildsrc.convention.kotlin-jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":core"))
    implementation(libs.bundles.ktor)
    implementation(libs.kotlinxSerialization)
    implementation(libs.dotenv)

    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinxCoroutinesTest)
}
