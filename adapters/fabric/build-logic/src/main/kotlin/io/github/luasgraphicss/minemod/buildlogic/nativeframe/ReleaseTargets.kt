package io.github.luasgraphicss.minemod.buildlogic.nativeframe

import groovy.json.JsonSlurper
import io.github.luasgraphicss.minemod.buildlogic.nativeframe.names.NamesDonor
import java.io.File

class ReleaseTargets private constructor(private val releases: List<Map<*, *>>) {
    val order: List<String> = releases.map { it["id"] as String }

    fun of(release: String): ReleaseTarget {
        val target = releases.singleOrNull { it["id"] == release }
            ?: throw IllegalArgumentException("$release is not a release of the target manifest")
        return ReleaseTarget(
            release = release,
            javaRuntime = (target["runtime"] as Map<*, *>)["majorVersion"] as Int,
            fabricLoader = fabricOf(target)["artifact"] as String,
            hasMojangNames = target[MOJANG_NAMES] != null,
        )
    }

    fun namesDonorOf(release: String): NamesDonor {
        val donor = releases.drop(order.indexOf(release) + 1).firstOrNull { it[MOJANG_NAMES] != null }
            ?: throw IllegalArgumentException("no release after $release has names published by Mojang")
        val published = donor[MOJANG_NAMES] as Map<*, *>
        return NamesDonor(
            release = donor["id"] as String,
            intermediary = fabricOf(donor)["mappings"] as String,
            mojangNamesUrl = published["url"] as String,
            mojangNamesSha1 = published["sha1"] as String,
        )
    }

    private fun fabricOf(target: Map<*, *>): Map<*, *> =
        entries(target["loaders"]).singleOrNull { it["loader"] == FABRIC }
            ?: throw IllegalArgumentException("the target manifest lists no Fabric Loader for ${target["id"]}")

    companion object {
        private const val FABRIC = "fabric"
        private const val MOJANG_NAMES = "clientMappings"

        fun read(manifest: File): ReleaseTargets =
            ReleaseTargets(entries((JsonSlurper().parse(manifest) as Map<*, *>)["releases"]))

        private fun entries(listed: Any?): List<Map<*, *>> = (listed as List<*>).map { it as Map<*, *> }
    }
}
