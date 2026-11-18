package io.github.luasgraphicss.minemod.buildlogic.nativeframe

class ReleaseTarget(
    val release: String,
    val javaRuntime: Int,
    val fabricLoader: String,
) {
    val fabricLoaderVersion: String = fabricLoader.substringAfterLast(':')
}
