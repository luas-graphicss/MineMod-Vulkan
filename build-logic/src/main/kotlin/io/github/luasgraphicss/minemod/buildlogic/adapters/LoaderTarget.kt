package io.github.luasgraphicss.minemod.buildlogic.adapters

import groovy.json.JsonSlurper
import java.io.File

class LoaderTarget(
    val release: String,
    val bootstrap: String,
    val artifact: String?,
    val javaRuntime: Int,
    val releaseTime: String,
    val mainClass: String,
) {
    companion object {
        private const val BOOTSTRAP_FAMILY = "bootstrap"
        private const val BOOTSTRAP_LOADER = "client-bootstrap"

        fun read(manifest: File, family: String, release: String): LoaderTarget {
            val document = JsonSlurper().parse(manifest) as Map<*, *>
            val releaseTarget = entries(document["releases"]).single { it["id"] == release }
            val loader = if (family == BOOTSTRAP_FAMILY) BOOTSTRAP_LOADER else family
            val loaderTarget = entries(releaseTarget["loaders"]).single { it["loader"] == loader }
            return LoaderTarget(
                release = release,
                bootstrap = loaderTarget["bootstrap"] as String,
                artifact = loaderTarget["artifact"] as String?,
                javaRuntime = (releaseTarget["runtime"] as Map<*, *>)["majorVersion"] as Int,
                releaseTime = releaseTarget["releaseTime"] as String,
                mainClass = (releaseTarget["launch"] as Map<*, *>)["mainClass"] as String,
            )
        }

        private fun entries(listed: Any?): List<Map<*, *>> = (listed as List<*>).map { it as Map<*, *> }
    }
}
