pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.xposed.info")
        }
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "Fuck Launcher"
include(":app")
include(":ui")
