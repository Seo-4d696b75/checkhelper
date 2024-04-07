// for dependencyResolutionManagement
@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "Ekisagasu"
include(":app")
include(":widget")
include(":diagram")
project(":widget").projectDir = File(settingsDir, "../MyAndroidLibrary/library/widget")
project(":diagram").projectDir = File(settingsDir, "../MyAndroidLibrary/library/diagram")
