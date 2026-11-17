package io.github.luasgraphicss.minemod.verification.ecosystem.pack;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.Category;

public enum ModRole {
    @JsonProperty("gameplay")
    GAMEPLAY(Category.GAMEPLAY),
    @JsonProperty("interface-overlay")
    INTERFACE_OVERLAY(Category.INTERFACES),
    @JsonProperty("map")
    MAP(Category.MAPS),
    @JsonProperty("custom-models")
    CUSTOM_MODELS(Category.MODELS),
    @JsonProperty("technical-content")
    TECHNICAL_CONTENT(Category.TECHNICAL_CONTENT),
    @JsonProperty("cpu-optimization")
    CPU_OPTIMIZATION(Category.CPU_OPTIMIZATIONS),
    @JsonProperty("renderer-replacement")
    RENDERER_REPLACEMENT(Category.RENDERER_REPLACEMENTS),
    @JsonProperty("shader-loader")
    SHADER_LOADER(Category.SHADER_LOADERS),
    @JsonProperty("capture")
    CAPTURE(Category.CAPTURE);

    private final Category category;

    ModRole(Category category) {
        this.category = category;
    }

    public Category category() {
        return category;
    }
}
