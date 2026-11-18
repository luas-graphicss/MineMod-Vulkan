import groovy.json.JsonSlurper

pluginManagement {
    val loomVersion: String by settings
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        gradlePluginPortal()
    }
    plugins {
        id("fabric-loom") version loomVersion
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val release = "1.21.1"
val repositoryRoot = settingsDir.resolve("../../../..")

fun entries(listed: Any?): List<Map<*, *>> = (listed as List<*>).map { it as Map<*, *> }

val targetManifest = JsonSlurper().parse(repositoryRoot.resolve("manifests/minecraft-targets.json")) as Map<*, *>
val releaseTarget = entries(targetManifest["releases"]).single { it["id"] == release }
val fabricLoader = entries(releaseTarget["loaders"]).single { it["loader"] == "fabric" }["artifact"] as String

dependencyResolutionManagement {
    versionCatalogs {
        create("targetLibs") {
            library("minecraft", "com.mojang:minecraft:$release")
            library("fabric-loader", fabricLoader)
        }
    }
}

rootProject.name = "minemod-vulkan-journey-$release"
