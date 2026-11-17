package io.github.luasgraphicss.minemod.client.compatibility.state;

public enum WrapperCall {
    VIEWPORT("glViewport", 4),
    SCISSOR_BOX("glScissor", 4),
    BLEND_FACTORS("glBlendFuncSeparate", 4),
    DEPTH_FUNCTION("glDepthFunc", 1),
    DEPTH_MASK("glDepthMask", 1),
    COLOR_MASK("glColorMask", 4),
    DEPTH_OFFSET("glPolygonOffset", 2),
    LOGIC_OPERATION("glLogicOp", 1);

    private final String function;
    private final int argumentCount;

    WrapperCall(String function, int argumentCount) {
        this.function = function;
        this.argumentCount = argumentCount;
    }

    public String function() {
        return function;
    }

    public int argumentCount() {
        return argumentCount;
    }
}
