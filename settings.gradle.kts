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
rootProject.name = "Logix"

include(":app")
include(":feature:tabs")
include(":feature:omnibox")
include(":feature:settings")
include(":core:database")
include(":core:search")
include(":core:adblock")
include(":core:network")
include(":core:chromium-bridge")
