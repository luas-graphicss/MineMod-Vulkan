package io.github.luasgraphicss.minemod.buildlogic.nativeframe

import groovy.json.JsonSlurper
import java.io.File

class ReleaseTargets private constructor(private val releases: List<Map<*, *>>) {
    val order: List<String> = releases.map { it["id"] as String }

    fun of(release: String): ReleaseTarget {
        val target = releases.singleOrNull { it["id"] == release }
            ?: throw IllegalArgumentException("$release is not a release of the target manifest")
        val fabric = entries(target["loaders"]).singleOrNull { it["loader"] == FABRIC }
            ?: throw IllegalArgumentException("the target manifest lists no Fabric Loader for $release")
        return ReleaseTarget(
            release = release,
            javaRuntime = (target["runtime"] as Map<*, *>)["majorVersion"] as Int,
            fabricLoader = fabric["artifact"] as String,
        )
    }

    companion object {
        private const val FABRIC = "fabric"

        fun read(manifest: File): ReleaseTargets =
            ReleaseTargets(entries((JsonSlurper().parse(manifest) as Map<*, *>)["releases"]))

        private fun entries(listed: Any?): List<Map<*, *>> = (listed as List<*>).map { it as Map<*, *> }
    }
}
