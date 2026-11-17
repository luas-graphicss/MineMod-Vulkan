package io.github.luasgraphicss.minemod.verification.adapters.names;

import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;

public final class NamespacePolicy {
    private static final int FIRST_FORGE_ON_MOJANG_NAMES = 50;
    private static final String NEOFORGE_FORK_OF_FORGE = "net.neoforged:forge:";

    private NamespacePolicy() {}

    public static RuntimeNamespace of(LoaderTarget loader) {
        return switch (loader.loader()) {
            case CLIENT_BOOTSTRAP -> RuntimeNamespace.OFFICIAL;
            case FABRIC -> RuntimeNamespace.INTERMEDIARY;
            case FORGE -> forgeNamespaceOf(loader);
            case NEOFORGE -> loader.artifact().startsWith(NEOFORGE_FORK_OF_FORGE)
                    ? RuntimeNamespace.SRG
                    : RuntimeNamespace.MOJANG;
        };
    }

    private static RuntimeNamespace forgeNamespaceOf(LoaderTarget loader) {
        if (loader.bootstrap() == LoaderTarget.Bootstrap.LAUNCHWRAPPER) {
            return RuntimeNamespace.OFFICIAL;
        }
        return forgeMajorOf(loader.artifact()) >= FIRST_FORGE_ON_MOJANG_NAMES
                ? RuntimeNamespace.MOJANG
                : RuntimeNamespace.SRG;
    }

    private static int forgeMajorOf(String artifact) {
        String version = artifact.substring(artifact.lastIndexOf(':') + 1);
        String forgeVersion = version.substring(version.indexOf('-') + 1);
        return Integer.parseInt(forgeVersion.substring(0, forgeVersion.indexOf('.')));
    }
}
