import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("buildsrc.convention.compose-multiplatform")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))
                implementation(project(":data"))
                
                // Add lifecycle for ViewModels
                implementation(libs.lifecycle.viewmodel.compose)
                implementation(libs.lifecycle.runtime.compose)

                // Koin
                implementation(libs.koinCore)
                implementation(libs.koinCompose)
                implementation(libs.koinComposeViewModel)

                // Ktor & Serialization
                implementation(libs.ktorClientCore)
                implementation(libs.ktorClientCio)
                implementation(libs.ktorClientContentNegotiation)
                implementation(libs.ktorSerializationKotlinxJson)
                implementation(libs.kotlinxSerialization)

                // Compose Icons
                implementation(libs.material.icons.extended)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activityCompose)
                implementation(compose.preview)
                implementation(libs.koinAndroid)
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(compose.preview)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.coilCompose)
                implementation(libs.coilNetworkKtor)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinxCoroutinesTest)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.mockk)
            }
        }
    }
}

android {
    namespace = "com.github.adriianh.melo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.github.adriianh.melo"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "com.github.adriianh.melo.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.github.adriianh.melo"
            packageVersion = "1.0.1"
        }
    }
}
