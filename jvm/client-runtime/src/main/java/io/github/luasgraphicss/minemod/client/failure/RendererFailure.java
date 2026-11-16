package io.github.luasgraphicss.minemod.client.failure;

public final class RendererFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RendererFailure(String reason) {
        super(reason);
    }
}
