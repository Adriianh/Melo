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
    implementation(libs.com.android.kotlin.multiplatform.library.gradle.plugin)
    implementation(libs.gradle)
    implementation(libs.compose.gradle.plugin)
    implementation(libs.compose.compiler.gradle.plugin)
    implementation(kotlin("gradle-plugin"))
}