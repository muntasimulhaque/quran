import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

// The fonts the app is drawn with live here, in one module every feature
// reads them from: Inter for the interface, Literata for reading, and Amiri
// Quran for ornaments and Arabic outside the Mushaf.
android {
    namespace = "io.github.muntasimulhaque.quran.content"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
