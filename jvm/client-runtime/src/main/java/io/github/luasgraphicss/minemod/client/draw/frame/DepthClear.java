package io.github.luasgraphicss.minemod.client.draw.frame;

import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;
import io.github.luasgraphicss.minemod.codec.scene.SceneOpcode;

final class DepthClear implements FrameMark {
    private final int drawsBefore;

    DepthClear(int drawsBefore) {
        this.drawsBefore = drawsBefore;
    }

    @Override
    public int drawsBefore() {
        return drawsBefore;
    }

    @Override
    public long streamBytes() {
        return SceneOpcode.CLEAR_DEPTH.fixedBytes();
    }

    @Override
    public void write(SceneCommandWriter frame) {
        frame.clearDepth();
    }
}
