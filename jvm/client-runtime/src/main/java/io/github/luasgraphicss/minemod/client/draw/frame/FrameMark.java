package io.github.luasgraphicss.minemod.client.draw.frame;

import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;

interface FrameMark {
    int drawsBefore();

    long streamBytes();

    void write(SceneCommandWriter frame);
}
