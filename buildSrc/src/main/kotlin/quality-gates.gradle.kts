package buildsrc.convention

plugins {
    // Plugin jars are resolved from the buildSrc classpath (see buildSrc/build.gradle.kts),
    // so no versions are allowed here.
    id("dev.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    baseline = file("$projectDir/detekt-baseline.xml")
}

ktlint {
    baseline.set(file("$projectDir/ktlint-baseline.xml"))
}

// detekt 2.x analyzes source sets individually (the plain `detekt` task is a no-op on Kotlin
// Multiplatform). Keep the gate focused on the production source sets that the current CI
// already compiles: commonMain + jvmMain. ktlint still covers the style of every other source
// set, and iOS/Android units can be added to detekt later.
//
// The source-set tasks are registered lazily by the detekt plugin, so the task sets are
// resolved at execution time via providers instead of being captured during configuration.
fun isDetektCheckTask(name: String): Boolean =
    name == "detekt" ||
        name == "detektCommonMainSourceSet" ||
        name == "detektJvmMainSourceSet"

fun isDetektBaselineTask(name: String): Boolean =
    name == "detektBaseline" ||
        name == "detektBaselineCommonMainSourceSet" ||
        name == "detektBaselineJvmMainSourceSet"

tasks.register("detektCheck") {
    group = "verification"
    description = "Runs the detekt quality gate (source-set aware)"
    dependsOn(provider { tasks.filter { isDetektCheckTask(it.name) } })
}

tasks.register("detektBaselineAll") {
    group = "verification"
    description = "Regenerates the detekt baseline(s) for this project"
    dependsOn(provider { tasks.filter { isDetektBaselineTask(it.name) } })
}

// Quality gates also run as part of the standard `check` lifecycle.
tasks.named("check") {
    dependsOn("ktlintCheck", "detektCheck")
}