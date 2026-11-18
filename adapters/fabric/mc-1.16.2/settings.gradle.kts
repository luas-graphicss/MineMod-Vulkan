pluginManagement {
    includeBuild("../build-logic")
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        gradlePluginPortal()
    }
}

plugins {
    id("minemod.native-frame-release")
}
