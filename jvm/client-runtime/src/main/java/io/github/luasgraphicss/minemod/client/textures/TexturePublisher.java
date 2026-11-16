package io.github.luasgraphicss.minemod.client.textures;

import io.github.luasgraphicss.minemod.client.draw.texture.SceneTextures;
import io.github.luasgraphicss.minemod.client.frame.StreamBudget;
import io.github.luasgraphicss.minemod.codec.scene.SceneCommandWriter;
import io.github.luasgraphicss.minemod.codec.scene.SceneOpcode;
import io.github.luasgraphicss.minemod.codec.scene.TextureSampling;
import io.github.luasgraphicss.minemod.contract.handle.HandleAllocator;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TexturePublisher implements SceneTextures {
    private static final long DEFINITION_BYTES = SceneOpcode.DEFINE_TEXTURE.fixedBytes();
    private static final long DESTRUCTION_BYTES = SceneOpcode.DESTROY_TEXTURE.fixedBytes();

    private final HandleAllocator handles;
    private final int maxTextures;
    private final Map<Integer, PublishedTexture> textures = new HashMap<>();
    private final Deque<ResourceHandle> destroyed = new ArrayDeque<>();
    private final Set<PublishedTexture> undefined = new LinkedHashSet<>();
    private final Set<PublishedTexture> unwritten = new LinkedHashSet<>();
    private long refusedTextures;
    private long refusedWrites;

    public TexturePublisher(HandleAllocator handles, int maxTextures) {
        if (maxTextures < 1) {
            throw new IllegalArgumentException("the scene holds at least one texture: " + maxTextures);
        }
        this.handles = handles;
        this.maxTextures = maxTextures;
    }

    public static StreamBudget definitionBudgetFor(int maxTextures) {
        return StreamBudget.holding(2L * maxTextures, DEFINITION_BYTES);
    }

    public static int handlesFor(int maxTextures) {
        return 2 * maxTextures;
    }

    public void define(int gameTexture, TextureShape shape, Set<TextureSampling> sampling) {
        destroy(gameTexture);
        if (textures.size() >= maxTextures) {
            refusedTextures++;
            return;
        }
        PublishedTexture texture = new PublishedTexture(handles.allocate(), shape, sampling);
        textures.put(gameTexture, texture);
        undefined.add(texture);
    }

    public void sample(int gameTexture, Set<TextureSampling> sampling) {
        PublishedTexture texture = textures.get(gameTexture);
        if (texture != null && undefined.contains(texture)) {
            texture.sampleBy(sampling);
        }
    }

    public void write(int gameTexture, TexelArea area, ByteBuffer handedOverRgba) {
        PublishedTexture texture = textures.get(gameTexture);
        if (texture == null || !texture.holds(area) || handedOverRgba.remaining() != area.bytes()) {
            refusedWrites++;
            return;
        }
        texture.keep(area, handedOverRgba);
        unwritten.add(texture);
    }

    public void destroy(int gameTexture) {
        PublishedTexture texture = textures.remove(gameTexture);
        if (texture == null) {
            return;
        }
        unwritten.remove(texture);
        if (undefined.remove(texture)) {
            handles.release(texture.handle());
        } else {
            destroyed.add(texture.handle());
        }
    }

    @Override
    public Optional<ResourceHandle> find(int gameTexture) {
        PublishedTexture texture = textures.get(gameTexture);
        return texture == null ? Optional.empty() : Optional.of(texture.handle());
    }

    public void publishDefinitions(SceneCommandWriter scene, StreamBudget budget) {
        for (Iterator<PublishedTexture> next = undefined.iterator(); next.hasNext(); ) {
            PublishedTexture texture = next.next();
            if (!budget.take(DEFINITION_BYTES)) {
                return;
            }
            texture.define(scene);
            next.remove();
        }
    }

    public void publishDestructions(SceneCommandWriter scene, StreamBudget budget) {
        while (!destroyed.isEmpty() && budget.take(DESTRUCTION_BYTES)) {
            ResourceHandle handle = destroyed.remove();
            scene.destroyTexture(handle);
            handles.release(handle);
        }
    }

    public void publishTexels(SceneCommandWriter scene, StreamBudget budget) {
        for (Iterator<PublishedTexture> next = unwritten.iterator(); next.hasNext(); ) {
            PublishedTexture texture = next.next();
            if (undefined.contains(texture) || !texture.writeTexels(scene, budget)) {
                return;
            }
            next.remove();
        }
    }

    public int liveTextures() {
        return textures.size();
    }

    public long refusedTextures() {
        return refusedTextures;
    }

    public long refusedWrites() {
        return refusedWrites;
    }
}
