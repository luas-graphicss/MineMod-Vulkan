package io.github.luasgraphicss.minemod.buildlogic.nativeframe.names

import java.io.Reader
import java.nio.file.Path
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader
import net.fabricmc.mappingio.tree.MappingTree

class MojangNamesByIntermediary private constructor(
    private val classes: Map<String, String>,
    private val fields: Map<String, String>,
    private val methods: Map<String, String>,
) {
    fun ofClass(intermediary: String): String? = classes[intermediary] ?: insideNamedOuterClass(intermediary)

    fun ofField(intermediary: String): String? = fields[intermediary]

    fun ofMethod(intermediary: String): String? = methods[intermediary]

    private fun insideNamedOuterClass(intermediary: String): String? {
        val innerName = intermediary.lastIndexOf(INNER_CLASS_SEPARATOR)
        if (innerName < 0) return null
        return ofClass(intermediary.substring(0, innerName))?.let { outer -> outer + intermediary.substring(innerName) }
    }

    companion object {
        private const val INNER_CLASS_SEPARATOR = '$'
        private val COMPILER_GENERATED_NAME = Regex("^(access|this|val\\\$this|lambda\\\$.*)\\\$[0-9]+\$")

        fun read(intermediaryArchive: Path, publishedNames: Reader): MojangNamesByIntermediary {
            val names = IntermediaryNames.read(intermediaryArchive)
            ProGuardFileReader.read(
                publishedNames,
                MappingsNamespace.NAMED.toString(),
                MappingsNamespace.OFFICIAL.toString(),
                MappingSourceNsSwitch(names, MappingsNamespace.OFFICIAL.toString()),
            )
            return collect(names)
        }

        private fun collect(names: MappingTree): MojangNamesByIntermediary {
            val intermediary = names.getNamespaceId(MappingsNamespace.INTERMEDIARY.toString())
            val named = names.getNamespaceId(MappingsNamespace.NAMED.toString())
            val classes = HashMap<String, String>()
            val fields = HashMap<String, String>()
            val methods = HashMap<String, String>()
            for (owner in names.classes) {
                record(classes, owner.getDstName(intermediary), owner.getDstName(named))
                owner.fields.forEach { record(fields, it.getDstName(intermediary), it.getDstName(named)) }
                owner.methods.forEach { record(methods, it.getDstName(intermediary), it.getDstName(named)) }
            }
            return MojangNamesByIntermediary(classes, fields, methods)
        }

        private fun record(known: MutableMap<String, String>, intermediary: String?, mojang: String?) {
            if (intermediary == null || mojang == null || mojang == intermediary) return
            if (COMPILER_GENERATED_NAME.matches(mojang)) return
            val earlier = known.putIfAbsent(intermediary, mojang)
            check(earlier == null || earlier == mojang) {
                "$intermediary is both $earlier and $mojang in the names Mojang published"
            }
        }
    }
}
