package io.github.luasgraphicss.minemod.client.frame;

import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;

public interface FrameContent {
    void writePart(SceneCommandWriter frame);

    void write(SceneCommandWriter frame);
}
