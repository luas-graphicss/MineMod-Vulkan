package io.github.luasgraphicss.minemod.compatibility.dispatch.catalog;

public enum CallFamily {
    IMMEDIATE_MODE(true),
    MATRIX_STACK(true),
    DISPLAY_LIST(true),
    FIXED_FUNCTION(true),
    CLIENT_ARRAYS(true),
    STATE(false),
    DRAW(false),
    TEXTURE(false),
    BUFFER(false),
    VERTEX_ARRAY(false),
    FRAMEBUFFER(false),
    SHADER(false),
    UNIFORM(false),
    SAMPLER(false),
    QUERY(false),
    TRANSFORM_FEEDBACK(false),
    SYNC(false),
    READBACK(false),
    SUBMISSION(false),
    CONTEXT_QUERY(false);

    private final boolean legacy;

    CallFamily(boolean legacy) {
        this.legacy = legacy;
    }

    public boolean isLegacy() {
        return legacy;
    }
}
