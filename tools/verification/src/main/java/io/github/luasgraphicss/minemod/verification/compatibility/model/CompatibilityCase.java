package io.github.luasgraphicss.minemod.verification.compatibility.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import java.util.List;

public record CompatibilityCase(
        String id,
        String release,
        LoaderTarget.Kind loader,
        List<Category> categories,
        List<ModRelease> mods,
        String capability,
        String scenario,
        Outcome outcome,
        String reason,
        List<Evidence> evidence) {

    public enum Category {
        @JsonProperty("gameplay") GAMEPLAY,
        @JsonProperty("interfaces") INTERFACES,
        @JsonProperty("maps") MAPS,
        @JsonProperty("models") MODELS,
        @JsonProperty("technical-content") TECHNICAL_CONTENT,
        @JsonProperty("cpu-optimizations") CPU_OPTIMIZATIONS,
        @JsonProperty("renderer-replacements") RENDERER_REPLACEMENTS,
        @JsonProperty("shader-loaders") SHADER_LOADERS,
        @JsonProperty("capture") CAPTURE
    }

    public enum Outcome {
        @JsonProperty("verified") VERIFIED,
        @JsonProperty("requires-bridge") REQUIRES_BRIDGE,
        @JsonProperty("unsupported") UNSUPPORTED,
        @JsonProperty("unverified") UNVERIFIED
    }

    public record ModRelease(String id, String version) {}

    public record Evidence(String path, String sha256) {}
}
