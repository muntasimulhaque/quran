plugins {
    // AGP 9 builds Kotlin itself; the language plugin is not needed and is
    // refused if it is applied.
    id("com.android.test")
}

android {
    namespace = "io.github.muntasimulhaque.quran.benchmark"
    compileSdk = 37

    defaultConfig {
        // the minSdk of the app under test, so the profile is generated for
        // the oldest phone the app runs on
        minSdk = 24
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        // the profile is generated against the release build, because that is
        // the build the profile ships in
        create("benchmark") {
            isDebuggable = false
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    packaging {
        // the benchmark's own androidx.test classes must not be in the app
        resources.excludes += setOf(
            "META-INF/LICENSE.md",
            "META-INF/LICENSE-notice.md",
        )
    }
}

dependencies {
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test:runner:1.6.2")
    implementation("androidx.test:rules:1.6.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.benchmark:benchmark-macro:1.4.1")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.4.1")
}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        variant.enable = variant.buildType == "benchmark"
    }
}
