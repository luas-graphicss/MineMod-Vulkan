package io.github.luasgraphicss.minemod.buildlogic.nativeframe.names

import net.fabricmc.loom.api.mappings.layered.MappingContext
import net.fabricmc.loom.api.mappings.layered.spec.MappingsSpec

data class DerivedNamesSpec(private val donor: NamesDonor) : MappingsSpec<DerivedNamesLayer> {
    override fun createLayer(context: MappingContext): DerivedNamesLayer = DerivedNamesLayer(donor, context)
}
