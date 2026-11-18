package io.github.luasgraphicss.minemod.buildlogic.adapters

import java.io.InputStream
import java.util.Properties

object FramePlanStandIns {
    private const val STAND_IN = ".hook"
    private const val CLASS_SUFFIX = ".class"

    fun classEntriesOf(plan: InputStream): Set<String> {
        val entries = Properties().apply { load(plan) }
        return entries.stringPropertyNames()
            .filter { it.endsWith(STAND_IN) }
            .map { entries.getProperty(it).trim() + CLASS_SUFFIX }
            .toSortedSet()
    }
}
