pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "plant-market"

include(
    "platform-common",
    "services:api-gateway",
    "services:auth-service",
    "services:catalog-service",
    "services:order-service",
    "tests:e2e",
)
