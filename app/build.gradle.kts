import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The upload keystore lives in the shared Google Play Signing Key folder in
// the owner's vault, outside every repository, so no credential can ever
// enter the tree. The folder has moved before, and each drive is tried
// against each layout it has worn rather than one path that a tidy-up turns
// into a silent unsigned build. When the file is absent (a fresh clone, CI),
// the release build degrades to unsigned instead of failing.
val keystoreLayouts = listOf(
    "BSCPLC/DM (Development)/Personal Docs/Pers/My Apps/Google Play Signing Key/keystore.properties",
    "BSCPLC/DM (Development)/Personal Docs/Pers/Google Play Signing Key/keystore.properties",
)
val keystoreFile = listOf("D:", "E:")
    .flatMap { drive -> keystoreLayouts.map { file("$drive/GDrive/$it") } }
    .firstOrNull { it.exists() }

val releaseKeystore = Properties()
if (keystoreFile != null) {
    releaseKeystore.load(keystoreFile.inputStream())
}

// The storeFile inside keystore.properties carries a drive letter that may not
// match this machine (the properties file and the keystore always sit together
// in the same folder). If the literal path does not exist, resolve the store
// file against the keystore.properties' own directory instead.
val keystoreStoreFile: java.io.File? = run {
    val kf = keystoreFile ?: return@run null
    val raw = releaseKeystore.getProperty("storeFile") ?: return@run null
    val literal = file(raw)
    if (literal.exists()) literal
    else {
        val name = raw.substringAfterLast('/').substringAfterLast('\\')
        file("${kf.parentFile.absolutePath}/$name")
    }
}
val canSignRelease = keystoreStoreFile != null &&
    releaseKeystore.containsKey("storePassword") &&
    releaseKeystore.containsKey("keyAlias") &&
    releaseKeystore.containsKey("keyPassword")

val preparedAssets = layout.buildDirectory.dir("generated/quranAssets")

val prepareAppAssets = tasks.register("prepareAppAssets") {
    group = "content"
    description = "Copies the content database and extracts the fonts into app assets."
    dependsOn(":tools:fetchAssets")
    inputs.file(rootProject.file("content/quran.db"))
    inputs.file(rootProject.file("content/build-report.json"))
    inputs.file(rootProject.file("content/raw/qul/qpc-v2-font.zip"))
    inputs.file(rootProject.file("content/raw/qul/qpc-hafs-font.zip"))
    outputs.dir(preparedAssets)
    doLast {
        val out = preparedAssets.get().asFile
        out.deleteRecursively()
        val content = File(out, "content").apply { mkdirs() }
        rootProject.file("content/quran.db").copyTo(File(content, "quran.db"), overwrite = true)
        val report = rootProject.file("content/build-report.json").readText()
        val hash = Regex("\"databaseSha256\"\\s*:\\s*\"([0-9a-f]+)\"")
            .find(report)?.groupValues?.get(1)
            ?: throw GradleException("content/build-report.json has no databaseSha256")
        File(content, "version.txt").writeText(hash)
        val pages = File(out, "fonts/pages").apply { mkdirs() }
        project.copy {
            from(project.zipTree(rootProject.file("content/raw/qul/qpc-v2-font.zip"))) { include("*.ttf") }
            into(pages)
        }
        val study = File(out, "fonts").apply { mkdirs() }
        project.copy {
            from(project.zipTree(rootProject.file("content/raw/qul/qpc-hafs-font.zip"))) { include("*.ttf") }
            into(study)
        }
    }
}

android {
    namespace = "io.github.muntasimulhaque.quran"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.muntasimulhaque.quran"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = keystoreStoreFile
                storePassword = releaseKeystore.getProperty("storePassword")
                keyAlias = releaseKeystore.getProperty("keyAlias")
                keyPassword = releaseKeystore.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = if (canSignRelease) signingConfigs.getByName("release") else null
        }
    }
    buildFeatures {
        compose = true
    }
    sourceSets.getByName("main").assets.directories.add(preparedAssets.get().asFile.absolutePath)
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

tasks.named("preBuild") {
    dependsOn(prepareAppAssets)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(project(":data"))
}
