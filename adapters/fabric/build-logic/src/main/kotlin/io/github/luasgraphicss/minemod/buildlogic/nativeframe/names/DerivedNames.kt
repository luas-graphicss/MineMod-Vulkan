package io.github.luasgraphicss.minemod.buildlogic.nativeframe.names

import net.fabricmc.loom.api.mappings.layered.MappingsNamespace
import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.MappingVisitor
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor

class DerivedNames(
    next: MappingVisitor,
    private val mojangNames: MojangNamesByIntermediary,
) : ForwardingMappingVisitor(next) {
    override fun visitNamespaces(srcNamespace: String, dstNamespaces: List<String>) {
        next.visitNamespaces(srcNamespace, listOf(MappingsNamespace.NAMED.toString()))
    }

    override fun visitClass(srcName: String): Boolean =
        named(next.visitClass(srcName), MappedElementKind.CLASS, mojangNames.ofClass(srcName))

    override fun visitField(srcName: String, srcDesc: String?): Boolean =
        named(next.visitField(srcName, srcDesc), MappedElementKind.FIELD, mojangNames.ofField(srcName))

    override fun visitMethod(srcName: String, srcDesc: String?): Boolean =
        named(next.visitMethod(srcName, srcDesc), MappedElementKind.METHOD, mojangNames.ofMethod(srcName))

    override fun visitDstName(targetKind: MappedElementKind, namespace: Int, name: String) = Unit

    override fun visitDstDesc(targetKind: MappedElementKind, namespace: Int, desc: String) = Unit

    private fun named(isVisited: Boolean, kind: MappedElementKind, mojangName: String?): Boolean {
        if (isVisited && mojangName != null) next.visitDstName(kind, NAMED_COLUMN, mojangName)
        return isVisited
    }

    private companion object {
        const val NAMED_COLUMN = 0
    }
}
