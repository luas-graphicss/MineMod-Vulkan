package io.github.luasgraphicss.minemod.verification.targets.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoaderTarget(
        Kind loader,
        String artifact,
        Channel channel,
        Bootstrap bootstrap,
        String mappings,
        Toolchain toolchain) {

    public enum Kind {
        @JsonProperty("forge") FORGE,
        @JsonProperty("fabric") FABRIC,
        @JsonProperty("neoforge") NEOFORGE,
        @JsonProperty("client-bootstrap") CLIENT_BOOTSTRAP
    }

    public enum Channel {
        @JsonProperty("recommended") RECOMMENDED,
        @JsonProperty("latest") LATEST,
        @JsonProperty("stable") STABLE,
        @JsonProperty("beta") BETA
    }

    public enum Bootstrap {
        @JsonProperty("launchwrapper") LAUNCHWRAPPER,
        @JsonProperty("modlauncher") MODLAUNCHER,
        @JsonProperty("fabric-knot") FABRIC_KNOT,
        @JsonProperty("client-bootstrap") CLIENT_BOOTSTRAP
    }

    public enum Toolchain {
        @JsonProperty("forgegradle") FORGEGRADLE,
        @JsonProperty("fabric-loom") FABRIC_LOOM,
        @JsonProperty("neogradle") NEOGRADLE,
        @JsonProperty("client-bootstrap") CLIENT_BOOTSTRAP
    }

    public static LoaderTarget clientBootstrap() {
        return new LoaderTarget(
                Kind.CLIENT_BOOTSTRAP, null, null, Bootstrap.CLIENT_BOOTSTRAP, null, Toolchain.CLIENT_BOOTSTRAP);
    }
}
