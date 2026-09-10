buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.versions.agp.get().let { "com.android.tools.build:gradle:$it" })
    }
}