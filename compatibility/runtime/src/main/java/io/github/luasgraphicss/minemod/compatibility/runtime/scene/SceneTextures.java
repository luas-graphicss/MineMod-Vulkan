package io.github.luasgraphicss.minemod.compatibility.runtime.scene;

public final class SceneTextures {
    private final int color;
    private final int depthStencil;

    public SceneTextures(int color, int depthStencil) {
        if (color == 0 || depthStencil == 0) {
            throw new IllegalArgumentException("the scene has a color texture and a depth-stencil texture");
        }
        this.color = color;
        this.depthStencil = depthStencil;
    }

    public int color() {
        return color;
    }

    public int depthStencil() {
        return depthStencil;
    }
}
