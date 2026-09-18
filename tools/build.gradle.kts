plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("io.github.muntasimulhaque.quran.tools.MainKt")
    applicationDefaultJvmArgs = listOf("-Djava.awt.headless=true", "-Djava.net.preferIPv4Stack=true")
}

tasks.named<JavaExec>("run") {
    // The pipeline always runs from the repository root.
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("fetchAssets") {
    group = "content"
    description = "Downloads and verifies large raw content assets such as the page fonts."
    mainClass.set("io.github.muntasimulhaque.quran.tools.MainKt")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("fetch")
    workingDir = rootProject.projectDir
    jvmArgs = listOf("-Djava.awt.headless=true", "-Djava.net.preferIPv4Stack=true")
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.sqlite.jdbc)
}
