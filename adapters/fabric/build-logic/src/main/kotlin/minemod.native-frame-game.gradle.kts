import io.github.luasgraphicss.minemod.buildlogic.nativeframe.MixinConfig
import io.github.luasgraphicss.minemod.buildlogic.nativeframe.ReleaseTargets
import io.github.luasgraphicss.minemod.buildlogic.nativeframe.SourceRanges

plugins {
    id("fabric-loom")
}

val buildJdk = 21
val mixinPackage = "io.github.luasgraphicss.minemod.adapters.minecraft.mixin"

val release = projectDir.name.removePrefix("mc-")
val repositoryRoot = projectDir.resolve("../../..")
val targets = ReleaseTargets.read(repositoryRoot.resolve("manifests/minecraft-targets.json"))
val target = targets.of(release)
val sourceRoots = SourceRanges(targets.order)
    .coveringIn(projectDir.resolve("../ranges"), release)
    .map { range -> range.resolve("src/main/java") }
val libs = the<VersionCatalogsExtension>().named("libs")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(buildJdk)
    }
}

val generateMixinConfig = tasks.register("generateMixinConfig") {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Lists the mixins of this release in the configuration Mixin reads."
    val mixinSources = sourceRoots
    val listedPackage = mixinPackage
    val javaRuntime = target.javaRuntime
    val configName = MixinConfig.fileNameOf(release)
    val generated = layout.buildDirectory.dir("generated/mixin-config")
    inputs.files(mixinSources).withPropertyName("mixinSources")
    inputs.property("javaRuntime", javaRuntime)
    outputs.dir(generated)
    doLast {
        val mixins = MixinConfig.mixinsIn(mixinSources, listedPackage)
        generated.get().file(configName).asFile.writeText(MixinConfig.render(listedPackage, javaRuntime, mixins))
    }
}

sourceSets.main {
    java.setSrcDirs(sourceRoots)
    resources.srcDir(generateMixinConfig)
}

dependencies {
    minecraft("com.mojang:minecraft:$release")
    mappings(loom.officialMojangMappings())
    modCompileOnly(target.fabricLoader)
    compileOnly(libs.findLibrary("errorprone-annotations").get())
    api("minemod-vulkan.jvm:client-runtime")
    api("minemod-vulkan.compatibility:lwjgl3")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = target.javaRuntime
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing", "-Werror"))
}
