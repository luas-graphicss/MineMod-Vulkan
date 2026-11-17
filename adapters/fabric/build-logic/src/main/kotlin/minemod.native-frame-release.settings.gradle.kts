plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

val release = settingsDir.name.removePrefix("mc-")
val repositoryRoot = settingsDir.resolve("../../..")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files(repositoryRoot.resolve("gradle/libs.versions.toml")))
        }
    }
}

rootProject.name = "minemod-vulkan-fabric-$release"

includeBuild(repositoryRoot)

include(":minecraft-adapter")
project(":minecraft-adapter").projectDir = repositoryRoot.resolve("adapters/minecraft/mc-$release")
