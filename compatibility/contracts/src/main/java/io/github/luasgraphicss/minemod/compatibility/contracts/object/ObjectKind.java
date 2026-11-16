package io.github.luasgraphicss.minemod.compatibility.contracts.object;

public enum ObjectKind {
    TEXTURE(ObjectScope.SHARE_GROUP),
    BUFFER(ObjectScope.SHARE_GROUP),
    RENDERBUFFER(ObjectScope.SHARE_GROUP),
    SAMPLER(ObjectScope.SHARE_GROUP),
    SHADER_OR_PROGRAM(ObjectScope.SHARE_GROUP),
    DISPLAY_LIST(ObjectScope.SHARE_GROUP),
    FRAMEBUFFER(ObjectScope.CONTEXT),
    VERTEX_ARRAY(ObjectScope.CONTEXT),
    QUERY(ObjectScope.CONTEXT),
    TRANSFORM_FEEDBACK(ObjectScope.CONTEXT),
    PROGRAM_PIPELINE(ObjectScope.CONTEXT);

    private final ObjectScope scope;

    ObjectKind(ObjectScope scope) {
        this.scope = scope;
    }

    public ObjectScope scope() {
        return scope;
    }
}
