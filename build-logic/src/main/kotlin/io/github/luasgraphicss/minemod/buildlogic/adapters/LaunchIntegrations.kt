package io.github.luasgraphicss.minemod.buildlogic.adapters

import org.gradle.api.GradleException

object LaunchIntegrations {
    private const val LOADERS = "io.github.luasgraphicss.minemod.adapters.loaders"
    private const val PRESENTATION = "io.github.luasgraphicss.minemod.adapters.presentation"
    private const val GLFW_HOOKS = "$PRESENTATION.glfw.GlfwPresentationHooks"
    private const val DISPLAY_HOOKS = "$PRESENTATION.display.DisplayPresentationHooks"
    private const val TRANSFORMATION_SERVICES = "META-INF/services/cpw.mods.modlauncher.api.ITransformationService"
    private const val AUTOMATIC_MODULE = "io.github.luasgraphicss.minemod.vulkan"
    private const val NEOFORGE = "net.neoforged:neoforge:"
    private val firstNeoForgeWithTypedTargets = listOf(20, 5)

    fun of(target: LoaderTarget, drawsFrame: Boolean): LaunchIntegration = when (target.bootstrap) {
        "client-bootstrap" -> LaunchIntegration(
            module = "agent",
            entryClass = "$LOADERS.agent.ClientBootstrapAgent",
            hooks = GLFW_HOOKS,
            manifestAttributes = mapOf("Premain-Class" to "$LOADERS.agent.ClientBootstrapAgent"),
            metadataEntries = listOf(AdapterResources.FRAME_PLAN),
            packagingPlugin = "minemod.loader-adapter-launcher",
        )
        "launchwrapper" -> LaunchIntegration(
            module = "launchwrapper",
            entryClass = "$LOADERS.launchwrapper.PinnedCallsTweaker",
            hooks = DISPLAY_HOOKS,
            manifestAttributes = mapOf("TweakClass" to "$LOADERS.launchwrapper.PinnedCallsTweaker"),
            metadataEntries = listOf(AdapterResources.FRAME_PLAN),
        )
        "fabric-knot" -> LaunchIntegration(
            module = "knot",
            entryClass = "$LOADERS.knot.PinnedCallsPlugin",
            hooks = GLFW_HOOKS,
            metadataEntries = listOf(
                FabricMixinLayout.MOD_METADATA,
                FabricMixinLayout.CONFIG,
                classEntryOf("${FabricMixinLayout.TARGETS_PACKAGE}.${FabricMixinLayout.TARGETS_CLASS}"),
            ),
            packagingPlugin = "minemod.loader-adapter-fabric",
        )
        "modlauncher" -> if (drawsFrame) modLauncherFrame() else modLauncher(target.artifact)
        else -> throw GradleException("no launch integration for the bootstrap ${target.bootstrap}")
    }

    private fun modLauncherFrame() = LaunchIntegration(
        module = "modlauncher-frame",
        entryClass = "$LOADERS.modlauncher.frame.PinnedFrameService",
        hooks = GLFW_HOOKS,
        manifestAttributes = mapOf("Automatic-Module-Name" to AUTOMATIC_MODULE),
        metadataEntries = listOf(TRANSFORMATION_SERVICES, AdapterResources.FRAME_PLAN),
    )

    private fun modLauncher(artifact: String?): LaunchIntegration {
        val typedTargets = artifact != null && artifact.startsWith(NEOFORGE) && hasTypedTargets(artifact)
        val module = if (typedTargets) "modlauncher11" else "modlauncher"
        return LaunchIntegration(
            module = module,
            entryClass = "$LOADERS.$module.PinnedCallsService",
            hooks = GLFW_HOOKS,
            manifestAttributes = mapOf("Automatic-Module-Name" to AUTOMATIC_MODULE),
            metadataEntries = listOf(TRANSFORMATION_SERVICES),
        )
    }

    private fun hasTypedTargets(neoForge: String): Boolean {
        val (major, minor) = neoForge.substringAfterLast(':').split('.').take(2).map { it.toInt() }
        val (firstMajor, firstMinor) = firstNeoForgeWithTypedTargets
        return major > firstMajor || (major == firstMajor && minor >= firstMinor)
    }
}
