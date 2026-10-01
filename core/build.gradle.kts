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
