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

// The base app carries the content database, its fingerprint, the study
// font, and the 604 Mushaf page fonts. One self-contained install, no
// permissions, no Play delivery library; a release APK from GitHub is a
// complete Quran. The rejected alternative (fonts in a fast-follow pack)
// is recorded in decisions D-018.
val contentAssets = layout.buildDirectory.dir("generated/contentAssets")

val prepareContentAssets = tasks.register("prepareContentAssets") {
    group = "content"
    description = "Copies the content database, its version, and the study font into base assets."
    dependsOn(":tools:fetchAssets")
    inputs.file(rootProject.file("content/quran.db"))
    inputs.file(rootProject.file("content/build-report.json"))
    inputs.file(rootProject.file("content/recitation-manifest.json"))
    inputs.dir(rootProject.file("content/work/fonts-hafs"))
    inputs.dir(rootProject.file("content/work/fonts-v2"))
    outputs.dir(contentAssets)
    doLast {
        val out = contentAssets.get().asFile
        out.deleteRecursively()
        val content = File(out, "content").apply { mkdirs() }
        rootProject.file("content/quran.db").copyTo(File(content, "quran.db"), overwrite = true)
        val report = rootProject.file("content/build-report.json").readText()
        val hash = Regex("\"databaseSha256\"\\s*:\\s*\"([0-9a-f]+)\"")
            .find(report)?.groupValues?.get(1)
            ?: throw GradleException("content/build-report.json has no databaseSha256")
        File(content, "version.txt").writeText(hash)
        val studyFont = rootProject.file("content/work/fonts-hafs").walkTopDown()
            .firstOrNull { it.isFile && it.name.endsWith(".ttf") }
            ?: throw GradleException("the study font is missing; run ./gradlew :tools:run --args=fetch")
        val fonts = File(out, "fonts").apply { mkdirs() }
        studyFont.copyTo(File(fonts, studyFont.name), overwrite = true)
        val pages = File(fonts, "pages").apply { mkdirs() }
        project.copy {
            from(rootProject.file("content/work/fonts-v2/fonts/pages")) { include("*.ttf") }
            into(pages)
        }
        val manifest = rootProject.file("content/recitation-manifest.json")
        if (manifest.exists()) {
            val recitations = File(out, "recitations").apply { mkdirs() }
            manifest.copyTo(File(recitations, "manifest.json"), overwrite = true)
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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    sourceSets.getByName("main").assets.directories.add(contentAssets.get().asFile.absolutePath)
    // The development audio sample ships in debug builds only, so the player
    // can be exercised without bundling gigabytes into anything shipped.
    sourceSets.getByName("debug").assets.directories.add(
        layout.buildDirectory.dir("generated/audioDevAssets").get().asFile.absolutePath,
    )
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

tasks.named("preBuild") {
    dependsOn(prepareContentAssets)
}

val copyAudioDevAssets = tasks.register<Sync>("copyAudioDevAssets") {
    val audioDev = rootProject.file("content/work/audio-dev")
    if (audioDev.isDirectory) from(audioDev)
    into(layout.buildDirectory.dir("generated/audioDevAssets/audio-dev"))
}

tasks.named("preBuild") {
    dependsOn(copyAudioDevAssets)
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
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(project(":data"))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
