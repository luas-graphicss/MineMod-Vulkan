package io.github.luasgraphicss.minemod.buildlogic.nativeframe.names

import java.nio.file.Path
import java.util.zip.ZipFile
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.tree.MemoryMappingTree

internal object IntermediaryNames {
    const val CLASSIFIER = "v2"
    private const val ENTRY = "mappings/mappings.tiny"

    fun read(archive: Path): MemoryMappingTree {
        val names = MemoryMappingTree()
        ZipFile(archive.toFile()).use { entries ->
            val entry = entries.getEntry(ENTRY) ?: throw IllegalArgumentException("$archive has no $ENTRY")
            entries.getInputStream(entry).bufferedReader().use { tiny ->
                MappingReader.read(tiny, MappingFormat.TINY_2_FILE, names)
            }
        }
        return names
    }
}
