import groovy.json.JsonOutput
import io.github.luasgraphicss.minemod.buildlogic.adapters.AdapterResources
import io.github.luasgraphicss.minemod.buildlogic.adapters.FabricMixinLayout
import io.github.luasgraphicss.minemod.buildlogic.adapters.LoaderAdapter
import java.util.Properties

plugins {
    `java-library`
}

val adapter = the<LoaderAdapter>()
val libs = the<VersionCatalogsExtension>().named("libs")

val mixinApi = configurations.create("mixinApi") {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    mixinApi(libs.findLibrary("sponge-mixin").get())
}

val generateMixinTargets = tasks.register("generateMixinTargets") {
    description = "Writes the mixin that names the classes whose calls the plan redirects."
    val plan = layout.projectDirectory.file("src/main/resources/${AdapterResources.PLAN}")
    val sources = layout.buildDirectory.dir("generated/sources/mixinTargets")
    inputs.file(plan)
    inputs.property("targetsClass", "${FabricMixinLayout.TARGETS_PACKAGE}.${FabricMixinLayout.TARGETS_CLASS}")
    outputs.dir(sources)
    doLast {
        val pinned = Properties().apply { plan.asFile.inputStream().use { load(it) } }
        val targets = pinned.getProperty("points").split(',')
            .map { pinned.getProperty("point.$it.within").substringBefore('.').replace('/', '.') }
            .distinct()
            .sorted()
        val source = sources.get()
            .file("${FabricMixinLayout.TARGETS_PACKAGE.replace('.', '/')}/${FabricMixinLayout.TARGETS_CLASS}.java")
            .asFile
        source.parentFile.mkdirs()
        source.writeText(
            """
            |package ${FabricMixinLayout.TARGETS_PACKAGE};
            |
            |import org.spongepowered.asm.mixin.Mixin;
            |
            |@Mixin(targets = {${targets.joinToString(", ") { "\"$it\"" }}})
            |public abstract class ${FabricMixinLayout.TARGETS_CLASS} {}
            |
            """.trimMargin(),
        )
    }
}

val generateFabricMetadata = tasks.register("generateFabricMetadata") {
    description = "Writes fabric.mod.json and the mixin configuration of the pinned calls."
    val resources = layout.buildDirectory.dir("generated/resources/fabricMetadata")
    val mod = mapOf(
        "schemaVersion" to 1,
        "id" to "minemod-vulkan",
        "version" to adapter.version,
        "name" to "MineMod Vulkan",
        "description" to "Routes the window and presentation calls of Minecraft to a single owner.",
        "license" to "GPL-3.0-only",
        "environment" to "client",
        "mixins" to listOf(FabricMixinLayout.CONFIG),
        "depends" to mapOf(
            "fabricloader" to ">=${adapter.target.artifact!!.substringAfterLast(':')}",
            "minecraft" to adapter.target.release,
            "java" to ">=${adapter.target.javaRuntime}",
        ),
    )
    val mixins = mapOf(
        "required" to true,
        "minVersion" to "0.8",
        "package" to FabricMixinLayout.TARGETS_PACKAGE,
        "compatibilityLevel" to "JAVA_8",
        "plugin" to adapter.launch.entryClass,
        "client" to listOf(FabricMixinLayout.TARGETS_CLASS),
    )
    inputs.property("mod", mod)
    inputs.property("mixins", mixins)
    outputs.dir(resources)
    doLast {
        val directory = resources.get().asFile
        directory.mkdirs()
        directory.resolve(FabricMixinLayout.MOD_METADATA)
            .writeText(JsonOutput.prettyPrint(JsonOutput.toJson(mod)) + "\n")
        directory.resolve(FabricMixinLayout.CONFIG)
            .writeText(JsonOutput.prettyPrint(JsonOutput.toJson(mixins)) + "\n")
    }
}

sourceSets.main {
    compileClasspath += mixinApi
    java.srcDir(generateMixinTargets)
    resources.srcDir(generateFabricMetadata)
}
