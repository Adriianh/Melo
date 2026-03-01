plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(libs.bundles.kotlinxEcosystem)

    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinxCoroutinesTest)
}