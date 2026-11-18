package io.github.luasgraphicss.minemod.buildlogic.nativeframe

class ReleaseTarget(
    val release: String,
    val javaRuntime: Int,
    val fabricLoader: String,
    val hasMojangNames: Boolean,
) {
    val fabricLoaderVersion: String = fabricLoader.substringAfterLast(':')
}
