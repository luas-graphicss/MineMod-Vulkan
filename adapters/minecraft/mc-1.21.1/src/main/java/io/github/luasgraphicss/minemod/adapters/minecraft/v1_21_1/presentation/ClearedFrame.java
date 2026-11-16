package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.presentation;

import io.github.luasgraphicss.minemod.client.frame.FrameContent;
import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;

public final class ClearedFrame implements FrameContent {
    private static final float OPAQUE = 1.0f;

    @Override
    public void write(SceneCommandWriter frame) {
        frame.beginInterfaceFrame(0.0f, 0.0f, 0.0f, OPAQUE).endFrame();
    }
}
