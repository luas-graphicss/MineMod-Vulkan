plugins {
    id("fabric-loom")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    minecraft(targetLibs.minecraft)
    mappings(loom.officialMojangMappings())
    modCompileOnly(targetLibs.fabric.loader)
    api("minemod-vulkan.jvm:client-runtime")
    api("minemod-vulkan.compatibility:lwjgl3")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing", "-Werror"))
}
