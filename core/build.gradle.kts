plugins {
    id("buildsrc.convention.kotlin-jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(libs.bundles.kotlinxEcosystem)

    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinxCoroutinesTest)
}