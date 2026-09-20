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
//
// A build can also point at a keystore properties file of its own with
// `-Pquran.keystore=<file>`. CI uses that to sign from repository secrets
// that exist only inside the runner, and the owner's machine uses the vault
// probe below when the folder is mounted.
val keystoreLayouts = listOf(
    "BSCPLC/DM (Development)/Personal Docs/Pers/My Apps/Google Play Signing Key/keystore.properties",
    "BSCPLC/DM (Development)/Personal Docs/Pers/Google Play Signing Key/keystore.properties",
)
val keystoreFile: java.io.File? = (project.findProperty("quran.keystore") as String?)
    ?.let { file(it) }
    ?.takeIf { it.isFile }
    ?: listOf("D:", "E:")
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

// The fonts, the core pack, and the catalog are owned by :content-assets;
// this module generates the runtime assets (the page fonts, the study font,
// the core pack, the catalog, and the recitation manifest) and wires them in.
//
// The packs a build carries are decided by its variant, not by a condition:
// the main assets hold the core pack and the catalog, and only the debug
// variant adds the rest, so a release bundle cannot contain them by accident.
val contentAssets = layout.buildDirectory.dir("generated/contentAssets")
val devPackAssets = layout.buildDirectory.dir("generated/devPackAssets")

val prepareContentAssets = tasks.register("prepareContentAssets") {
    group = "content"
    description = "Copies the core pack, the pack catalog, the fonts, and the recitation manifest."
    dependsOn(":tools:fetchAssets")
    inputs.file(rootProject.file("content/build-report.json"))
    inputs.file(rootProject.file("content/catalog.json"))
    inputs.file(rootProject.file("content/recitation-manifest.json"))
    inputs.dir(rootProject.file("content/packs"))
    inputs.dir(rootProject.file("content/work/fonts-hafs"))
    inputs.dir(rootProject.file("content/work/fonts-v2"))
    outputs.dir(contentAssets)
    outputs.dir(devPackAssets)
    doLast {
        val out = contentAssets.get().asFile
        out.deleteRecursively()
        val content = File(out, "content").apply { mkdirs() }
        // The app ships the Quran text and its page layout, and nothing else:
        // one small pack, plus the catalog of what can be downloaded.
        val packs = rootProject.file("content/packs")
        packs.resolve("core.db").copyTo(File(content, "core.db"), overwrite = true)
        rootProject.file("content/catalog.json").copyTo(File(content, "catalog.json"), overwrite = true)
        // Development builds carry every pack, so the whole app works with no
        // network at all while it is being built and tested. They are written
        // to the debug variant's own asset directory, never to the main one.
        val devPacks = devPackAssets.get().asFile
        devPacks.deleteRecursively()
        File(devPacks, "packs").mkdirs()
        packs.listFiles { file -> file.name.endsWith(".db") }?.forEach { pack ->
            pack.copyTo(File(File(devPacks, "packs"), pack.name), overwrite = true)
        }
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

val copyAudioDevAssets = tasks.register<Sync>("copyAudioDevAssets") {
    val audioDev = rootProject.file("content/work/audio-dev")
    if (audioDev.isDirectory) from(audioDev)
    into(layout.buildDirectory.dir("generated/audioDevAssets/audio-dev"))
}

android {
    namespace = "io.github.muntasimulhaque.quran"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.muntasimulhaque.quran"
        minSdk = 24
        targetSdk = 37
        versionCode = 7
        versionName = "0.7"
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
        buildConfig = true
    }
    sourceSets.getByName("main").assets.directories.add(contentAssets.get().asFile.absolutePath)
    // The packs that exist only for development, on the debug variant alone.
    sourceSets.getByName("debug").assets.directories.add(devPackAssets.get().asFile.absolutePath)
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
    dependsOn(prepareContentAssets, copyAudioDevAssets)
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
    implementation(project(":content-assets"))
    implementation(project(":ui-kit"))
    implementation(project(":feature-mushaf"))
    implementation(project(":feature-study"))
    implementation(project(":feature-search"))
    implementation(project(":feature-browse"))
    implementation(project(":feature-playback"))
    implementation(project(":feature-settings"))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.ui.test.manifest)
}
