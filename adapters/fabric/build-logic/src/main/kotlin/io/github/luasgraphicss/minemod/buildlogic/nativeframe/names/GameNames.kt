package io.github.luasgraphicss.minemod.buildlogic.nativeframe.names

import io.github.luasgraphicss.minemod.buildlogic.nativeframe.ReleaseTarget
import io.github.luasgraphicss.minemod.buildlogic.nativeframe.ReleaseTargets
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.artifacts.Dependency

class GameNames(private val loom: LoomGradleExtensionAPI, private val targets: ReleaseTargets) {
    fun of(target: ReleaseTarget): Dependency {
        if (target.hasMojangNames) return loom.officialMojangMappings()
        val donor = targets.namesDonorOf(target.release)
        return loom.layered { addLayer(DerivedNamesSpec(donor)) }
    }
}
