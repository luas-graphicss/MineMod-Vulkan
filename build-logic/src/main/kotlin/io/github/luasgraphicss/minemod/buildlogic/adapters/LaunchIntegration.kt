package io.github.luasgraphicss.minemod.buildlogic.adapters

data class LaunchIntegration(
    val module: String,
    val entryClass: String,
    val hooks: String,
    val manifestAttributes: Map<String, String> = emptyMap(),
    val metadataEntries: List<String> = emptyList(),
    val packagingPlugin: String? = null,
) {
    val requiredEntries: List<String>
        get() = listOf(AdapterResources.PLAN, classEntryOf(entryClass), classEntryOf(hooks)) + metadataEntries
}

internal fun classEntryOf(className: String) = className.replace('.', '/') + ".class"
