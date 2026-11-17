package io.github.luasgraphicss.minemod.adapters.presentation.renderer.session;

import io.github.luasgraphicss.minemod.client.frame.FrameContent;
import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;

final class ClearedFrame implements FrameContent {
    private static final float BLACK = 0.0f;
    private static final float OPAQUE = 1.0f;

    @Override
    public void writePart(SceneCommandWriter frame) {
        throw new IllegalStateException("a cleared frame is submitted whole");
    }

    @Override
    public void write(SceneCommandWriter frame) {
        frame.beginInterfaceFrame(BLACK, BLACK, BLACK, OPAQUE).endFrame();
    }
}
