package io.github.luasgraphicss.minemod.client.textures;

import io.github.luasgraphicss.minemod.client.frame.StreamBudget;
import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;
import io.github.luasgraphicss.minemod.codec.scene.TextureSampling;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class PublishedTexture {
    private final ResourceHandle handle;
    private final TextureShape shape;
    private final Deque<WaitingTexels> waiting = new ArrayDeque<>();
    private final Map<TexelArea, WaitingTexels> untouched = new HashMap<>();
    private Set<TextureSampling> sampling;

    PublishedTexture(ResourceHandle handle, TextureShape shape, Set<TextureSampling> sampling) {
        this.handle = handle;
        this.shape = shape;
        this.sampling = copyOf(sampling);
    }

    ResourceHandle handle() {
        return handle;
    }

    boolean holds(TexelArea area) {
        return shape.holds(area);
    }

    void sampleBy(Set<TextureSampling> choices) {
        sampling = copyOf(choices);
    }

    void define(SceneCommandWriter scene) {
        scene.defineTexture(handle, shape.width(), shape.height(), shape.levels(), sampling);
    }

    void keep(TexelArea area, ByteBuffer texels) {
        WaitingTexels replaced = untouched.remove(area);
        if (replaced != null) {
            replaced.replace();
        }
        WaitingTexels kept = new WaitingTexels(area, texels);
        waiting.add(kept);
        untouched.put(area, kept);
    }

    boolean writeTexels(SceneCommandWriter scene, StreamBudget budget) {
        for (WaitingTexels next = waiting.peek(); next != null; next = waiting.peek()) {
            next.writeRowsThatFit(handle, scene, budget);
            if (!next.isUntouched()) {
                untouched.remove(next.area(), next);
            }
            if (!next.isWritten()) {
                return false;
            }
            waiting.remove();
        }
        return true;
    }

    private static Set<TextureSampling> copyOf(Set<TextureSampling> choices) {
        return choices.isEmpty() ? EnumSet.noneOf(TextureSampling.class) : EnumSet.copyOf(choices);
    }
}
