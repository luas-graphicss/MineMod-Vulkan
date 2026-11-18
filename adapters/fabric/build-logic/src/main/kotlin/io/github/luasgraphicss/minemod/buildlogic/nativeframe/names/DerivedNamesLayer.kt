package io.github.luasgraphicss.minemod.buildlogic.nativeframe.names

import java.nio.file.Files
import java.nio.file.Path
import net.fabricmc.loom.api.mappings.layered.MappingContext
import net.fabricmc.loom.api.mappings.layered.MappingLayer
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch

class DerivedNamesLayer(
    private val donor: NamesDonor,
    private val context: MappingContext,
) : MappingLayer {
    override fun visit(mappingVisitor: MappingVisitor) {
        val published = publishedNames()
        warnAboutLicenseOf(published)
        val mojangNames = Files.newBufferedReader(published).use { names ->
            MojangNamesByIntermediary.read(donorIntermediary(), names)
        }
        val ownNames = MappingSourceNsSwitch(DerivedNames(mappingVisitor, mojangNames), sourceNamespace.toString())
        context.intermediaryTree().get().accept(ownNames)
    }

    override fun getSourceNamespace(): MappingsNamespace = MappingsNamespace.INTERMEDIARY

    private fun publishedNames(): Path {
        val published = context.workingDirectory(WORKING_DIRECTORY).resolve("${donor.release}-client.txt")
        context.download(donor.mojangNamesUrl).sha1(donor.mojangNamesSha1).downloadPath(published)
        return published
    }

    private fun donorIntermediary(): Path =
        context.resolveMavenDependency("${donor.intermediary}:${IntermediaryNames.CLASSIFIER}")

    private fun warnAboutLicenseOf(published: Path) {
        val license = Files.newBufferedReader(published).useLines { lines ->
            lines.takeWhile { it.startsWith(LICENSE_LINE) }.toList()
        }
        context.logger.warn("The names of ${context.minecraftVersion()} are derived from the mappings Mojang")
        context.logger.warn("published for ${donor.release}, which come under this license:")
        license.forEach(context.logger::warn)
    }

    private companion object {
        const val WORKING_DIRECTORY = "derived-names"
        const val LICENSE_LINE = "#"
    }
}
