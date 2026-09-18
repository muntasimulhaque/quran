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
include(":feature-mushaf")
include(":feature-study")
include(":feature-search")
include(":feature-browse")
include(":feature-playback")
include(":feature-settings")
include(":tools")
include(":data")
include(":app")
