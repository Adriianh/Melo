plugins {
    id("buildsrc.convention.kotlin-jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientOkhttp)
    implementation(libs.ktorClientContentNegotiation)
    implementation(libs.ktorSerializationKotlinxJson)
    implementation(libs.ktorClientEncoding)
    implementation(libs.brotli)
    implementation(libs.newpipeExtractor)
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
}