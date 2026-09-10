plugins {
    id("com.android.library")
    alias(libs.plugins.chaquopy)
}

android {
    namespace = "com.github.adriianh.ytdlpipe"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

chaquopy {
    defaultConfig {
        version = "3.14"
        pip {
            options("--no-deps")
            install("yt-dlp")
            install("certifi")
            install("websockets")
        }
    }
}