pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "open-health-bridge-android"

include(
    ":app",
    ":core:common",
    ":core:models",
    ":core:util",
    ":data:db",
    ":data:repository",
    ":data:sync",
    ":feature:onboarding",
    ":feature:dashboard",
    ":feature:healthconnect",
    ":feature:workouts",
    ":feature:nutrition",
    ":feature:recovery",
    ":feature:trends",
    ":feature:settings",
    ":feature:export",
    ":integration:api"
)
