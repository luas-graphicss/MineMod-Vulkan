package io.github.luasgraphicss.minemod.buildlogic.nativeframe

import groovy.json.JsonOutput
import java.io.File

object MixinConfig {
    private const val SOURCE_SUFFIX = ".java"
    private const val MINIMUM_MIXIN_VERSION = "0.8"
    private const val MATCHES_PER_INJECTOR = 1

    fun fileNameOf(release: String): String = "minemod-minecraft-$release.mixins.json"

    fun mixinsIn(sourceRoots: Iterable<File>, mixinPackage: String): List<String> {
        val packagePath = mixinPackage.replace('.', '/')
        return sourceRoots
            .map { root -> root.resolve(packagePath) }
            .flatMap { mixins -> classesUnder(mixins) }
            .sorted()
    }

    fun render(mixinPackage: String, javaRuntime: Int, clientMixins: List<String>): String {
        val config = linkedMapOf(
            "required" to true,
            "minVersion" to MINIMUM_MIXIN_VERSION,
            "package" to mixinPackage,
            "compatibilityLevel" to "JAVA_$javaRuntime",
            "client" to clientMixins,
            "injectors" to mapOf("defaultRequire" to MATCHES_PER_INJECTOR),
        )
        return JsonOutput.prettyPrint(JsonOutput.toJson(config)) + "\n"
    }

    private fun classesUnder(mixins: File): List<String> =
        mixins.walkTopDown()
            .filter { source -> source.isFile && source.name.endsWith(SOURCE_SUFFIX) }
            .map { source ->
                source.relativeTo(mixins).invariantSeparatorsPath.removeSuffix(SOURCE_SUFFIX).replace('/', '.')
            }
            .toList()
}
