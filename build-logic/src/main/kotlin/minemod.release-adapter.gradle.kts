plugins {
    `java-library`
}

val release = name.removePrefix("mc-")

version = providers.gradleProperty("modVersion").get()

base {
    archivesName = "minemod-vulkan-minecraft-$release"
}

tasks.jar {
    from(rootDir.resolve("LICENSE"))
}
