pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "minemod-vulkan"

include(":jvm:render-contract")
include(":jvm:command-codec")
include(":jvm:native-bridge")
include(":tools:verification")
include(":compatibility:contracts")
include(":compatibility:resources")
include(":compatibility:contexts")
include(":compatibility:state")
include(":adapters:transformers")
include(":adapters:renderer-ownership")
include(":jvm:compatibility-spi")
include(":compatibility:provider")
include(":compatibility:dispatch")
include(":compatibility:interop")
include(":compatibility:translation")
include(":compatibility:bridges")
include(":compatibility:layer")
include(":jvm:client-runtime")
