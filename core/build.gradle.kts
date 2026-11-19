import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Bytecode for Java 17, compiled by whatever JDK runs Gradle: the Android
// Studio JBR on the owner's machines, Temurin 17 in CI. No jvmToolchain
// pin, so the build never hunts for a second JDK (owner decision).
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
}

/**
 * The pack-id gate in this module's tests reads every shipping module's
 * `main` sources, which Gradle does not know are its inputs: without this,
 * the task is called up to date after a change in another module, and a rule
 * broken there would wait for CI to be seen (owner decision, 20.2).
 */
tasks.withType<Test>().configureEach {
    inputs.files(
        rootProject.fileTree("app/src/main") { include("**/*.kt") },
        rootProject.fileTree("data/src/main") { include("**/*.kt") },
        rootProject.fileTree("ui-kit/src/main") { include("**/*.kt") },
        rootProject.fileTree("content-assets/src/main") { include("**/*.kt") },
        rootProject.fileTree("feature-reading/src/main") { include("**/*.kt") },
        rootProject.fileTree("feature-search/src/main") { include("**/*.kt") },
        rootProject.fileTree("feature-browse/src/main") { include("**/*.kt") },
        rootProject.fileTree("feature-playback/src/main") { include("**/*.kt") },
        rootProject.fileTree("feature-settings/src/main") { include("**/*.kt") },
    ).withPropertyName("gateSources")
}
