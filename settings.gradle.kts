pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Quran"
include(":core")
include(":content-assets")
include(":ui-kit")
include(":feature-study")
include(":feature-search")
include(":feature-browse")
include(":feature-playback")
include(":feature-settings")
include(":tools")
include(":data")
include(":app")
// The startup profile's generator. It is a development tool: it never ships
// and it is not in the bundle. `generateReleaseBaselineProfile` needs a
// connected device, so the profile itself is generated on a machine with an
// emulator or a phone and committed; the module only ever builds here.
include(":benchmark")
