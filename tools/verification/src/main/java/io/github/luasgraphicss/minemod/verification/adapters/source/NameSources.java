package io.github.luasgraphicss.minemod.verification.adapters.source;

import io.github.luasgraphicss.minemod.verification.adapters.names.RuntimeNamespace;

public final class NameSources {
    private static final String INTERMEDIARY =
            "https://maven.fabricmc.net/net/fabricmc/intermediary/%1$s/intermediary-%1$s-v2.jar";
    private static final String INTERMEDIARY_ENTRY = "mappings/mappings.tiny";
    private static final String MCP_CONFIG =
            "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/%1$s/mcp_config-%1$s.zip";
    private static final String MCP_CONFIG_ENTRY = "config/joined.tsrg";

    private NameSources() {}

    public static NameSource of(String release, RuntimeNamespace namespace) {
        return switch (namespace) {
            case INTERMEDIARY -> new NameSource(String.format(INTERMEDIARY, release), INTERMEDIARY_ENTRY);
            case SRG -> new NameSource(String.format(MCP_CONFIG, release), MCP_CONFIG_ENTRY);
            case OFFICIAL, MOJANG ->
                throw new IllegalArgumentException(namespace + " names are not read from a mapping file");
        };
    }
}
