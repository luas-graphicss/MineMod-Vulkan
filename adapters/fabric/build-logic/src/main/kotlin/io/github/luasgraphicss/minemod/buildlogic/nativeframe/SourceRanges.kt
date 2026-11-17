package io.github.luasgraphicss.minemod.buildlogic.nativeframe

import java.io.File

class SourceRanges(private val releaseOrder: List<String>) {
    fun coveringIn(directory: File, release: String): List<File> {
        val position = positionOf(release)
        return directory.listFiles { candidate -> candidate.isDirectory }
            .orEmpty()
            .filter { range -> position in positionsOf(range.name) }
            .sortedBy { range -> range.name }
    }

    private fun positionsOf(range: String): IntRange {
        val bounds = range.split(BOUND_SEPARATOR)
        require(bounds.size <= 2) { "a range is named <first>-<last> or <release>, found: $range" }
        val positions = positionOf(bounds.first())..positionOf(bounds.last())
        require(!positions.isEmpty()) { "the range $range ends before it starts" }
        return positions
    }

    private fun positionOf(release: String): Int {
        val position = releaseOrder.indexOf(release)
        require(position >= 0) { "$release is not a release of the target manifest" }
        return position
    }

    private companion object {
        const val BOUND_SEPARATOR = "-"
    }
}
