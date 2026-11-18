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
        exclusiveContent {
            forRepository {
                maven {
                    name = "MinecraftLibraries"
                    url = uri("https://libraries.minecraft.net")
                }
            }
            filter {
                includeGroup("org.lwjgl.lwjgl")
                includeModule("net.minecraft", "launchwrapper")
            }
        }
        exclusiveContent {
            forRepository {
                maven {
                    name = "NeoForged"
                    url = uri("https://maven.neoforged.net/releases")
                }
            }
            filter {
                includeModule("cpw.mods", "modlauncher")
            }
        }
        exclusiveContent {
            forRepository {
                maven {
                    name = "Fabric"
                    url = uri("https://maven.fabricmc.net")
                }
            }
            filter {
                includeModule("net.fabricmc", "sponge-mixin")
            }
        }
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
include(":adapters:events")
include(":adapters:shader-packs")
include(":adapters:configuration")
include(":adapters:presentation:core")
include(":adapters:presentation:glfw")
include(":adapters:presentation:display")
include(":adapters:presentation:renderer")
include(":adapters:frame:core")
include(":adapters:frame:display")
include(":adapters:frame:glfw")
include(":adapters:loaders:launch")
include(":adapters:loaders:agent")
include(":adapters:loaders:launchwrapper")
include(":adapters:loaders:knot")
include(":adapters:loaders:modlauncher")
include(":adapters:loaders:modlauncher-frame")
include(":adapters:loaders:modlauncher11")
include(":jvm:compatibility-spi")
include(":compatibility:provider")
include(":compatibility:dispatch")
include(":compatibility:interop")
include(":compatibility:translation")
include(":compatibility:bridges")
include(":compatibility:layer")
include(":compatibility:natives")
include(":compatibility:runtime")
include(":compatibility:lwjgl3")
include(":compatibility:lwjgl2")
include(":jvm:client-runtime")

val pinnedCallAdapter = Regex("""plugins \{\s+id\("minemod\.(release|loader)-adapter"\)\s+}\s*""")

listOf("minecraft", "bootstrap", "forge", "fabric", "neoforge").forEach { family ->
    rootDir.resolve("adapters/$family")
        .listFiles { module -> module.resolve("build.gradle.kts").isFile }
        .orEmpty()
        .filter { module -> pinnedCallAdapter.matches(module.resolve("build.gradle.kts").readText()) }
        .sortedBy { module -> module.name }
        .forEach { module -> include(":adapters:$family:${module.name}") }
}
