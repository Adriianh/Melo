plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinGradlePlugin)
    implementation(libs.kotlinSerializationPlugin)
    implementation(libs.sqldelightGradlePlugin)
    implementation(libs.kotlinMultiplatformPlugin)
    implementation(libs.gradle)
    implementation(libs.composePlugin)
    implementation(libs.composeCompilerPlugin)
    implementation(kotlin("gradle-plugin"))

    implementation("dev.detekt:detekt-gradle-plugin:2.0.0-alpha.6")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:14.2.0")
}
