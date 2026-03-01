plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("io.github.goooler.shadow") version "8.1.8"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(libs.clikt)
    implementation(libs.kotlinxCoroutines)
    implementation(libs.koinCore)
    implementation(libs.dotenv)
    implementation(libs.scrimageCore)

    implementation(libs.bundles.ktor)
    implementation(libs.bundles.tamboui)
}

tasks {
    shadowJar {
        archiveBaseName.set("melo")
        archiveClassifier.set("")
        archiveVersion.set("")
        manifest {
            attributes["Main-Class"] = "com.github.adriianh.cli.MeloKt"
        }
    }
}

tasks.register<Exec>("packageApp") {
    dependsOn(tasks.shadowJar)

    val jarFile = tasks.shadowJar.get().archiveFile.get().asFile
    val outputDir = layout.buildDirectory.dir("installer").get().asFile
    val jpackage = "${System.getProperty("java.home")}/bin/jpackage"

    doFirst { outputDir.mkdirs() }

    commandLine(
        jpackage,
        "--input", jarFile.parent,
        "--main-jar", jarFile.name,
        "--main-class", "com.github.adriianh.cli.MeloKt",
        "--name", "melo",
        "--app-version", "1.0.0",
        "--dest", outputDir.absolutePath,
        "--type", "app-image"
    )
}

tasks.build {
    dependsOn(tasks.shadowJar)
}