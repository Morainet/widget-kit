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

rootProject.name = "morainet-widget-kit"

include(
    ":widget-core",
    ":widget-state",
    ":widget-workmanager",
    ":widget-animation",
    ":widget-preview",
    ":widget-debugger",
    ":widget-dsl",
    ":sample",
)
