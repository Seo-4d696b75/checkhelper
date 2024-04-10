// for dependencyResolutionManagement
@file:Suppress("UnstableApiUsage")

import java.util.Properties

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

val props = Properties().apply {
    file("github_credential.properties").inputStream().use {
        load(it)
    }
}

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
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Seo-4d696b75/diagram")
            credentials {
                username = props.getProperty("username")
                password = props.getProperty("token")
            }
        }
    }
}

rootProject.name = "Ekisagasu"
include(":app")
include(":ui")
include(":domain")
include(":data")
