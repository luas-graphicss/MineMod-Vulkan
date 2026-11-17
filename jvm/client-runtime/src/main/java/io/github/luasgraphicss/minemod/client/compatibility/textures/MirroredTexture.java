package io.github.luasgraphicss.minemod.client.compatibility.textures;

import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import io.github.luasgraphicss.minemod.client.textures.TextureShape;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class MirroredTexture {
    private final long resource;
    private final Map<Integer, Integer> parameters = new LinkedHashMap<>();
    private int copy;
    private TextureShape stored;

    MirroredTexture(long resource, int copy) {
        this.resource = resource;
        this.copy = copy;
    }

    long resource() {
        return resource;
    }

    int copy() {
        return copy;
    }

    boolean hasStorage() {
        return stored != null;
    }

    boolean holds(TexelArea area) {
        return stored != null && stored.holds(area);
    }

    Map<Integer, Integer> parameters() {
        return Collections.unmodifiableMap(parameters);
    }

    void keep(int parameter, int value) {
        parameters.put(parameter, value);
    }

    void store(TextureShape shape) {
        stored = shape;
    }

    void moveTo(int emptyCopy) {
        copy = emptyCopy;
        stored = null;
    }
}
