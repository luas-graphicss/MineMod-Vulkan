package io.github.luasgraphicss.minemod.adapters.shaderpacks.targets;

public enum DepthSnapshot {
    WITHOUT_HAND(2),
    WITHOUT_TRANSLUCENTS(1);

    private final int depthTexture;

    DepthSnapshot(int depthTexture) {
        this.depthTexture = depthTexture;
    }

    public PackTarget target() {
        return PackTarget.depth(depthTexture);
    }
}
