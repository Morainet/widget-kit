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

// Maven 坐标
gradle.rootProject {
    group = "com.morainet.widget"
    version = "0.1.0"
}

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
