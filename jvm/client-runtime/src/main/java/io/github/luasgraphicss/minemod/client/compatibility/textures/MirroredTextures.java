package io.github.luasgraphicss.minemod.client.compatibility.textures;

import io.github.luasgraphicss.minemod.client.opengl.textures.TextureMirror;
import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import io.github.luasgraphicss.minemod.client.textures.TextureShape;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

public final class MirroredTextures implements TextureMirror, AutoCloseable {
    private final Map<Integer, MirroredTexture> textures = new HashMap<>();
    private final TextureAliases aliases;
    private final ProviderTextures provider;
    private long nextResource = 1;

    public MirroredTextures(TextureAliases aliases, ProviderTextures provider) {
        this.aliases = Objects.requireNonNull(aliases, "aliases");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public int generate() {
        MirroredTexture mirrored = new MirroredTexture(nextResource++, provider.create());
        int name = aliases.publish(mirrored.resource(), mirrored.copy());
        textures.put(name, mirrored);
        return name;
    }

    @Override
    public void define(int texture, TextureShape shape) {
        Optional<MirroredTexture> published = publishedAs(texture);
        if (!published.isPresent()) {
            return;
        }
        MirroredTexture mirrored = published.get();
        if (mirrored.hasStorage()) {
            mirrored.moveTo(provider.create());
            aliases.publish(mirrored.resource(), mirrored.copy());
            provider.setParameters(mirrored.copy(), mirrored.parameters());
        }
        if (provider.holds(shape)) {
            provider.allocate(mirrored.copy(), shape);
            mirrored.store(shape);
        }
    }

    @Override
    public void write(int texture, TexelArea area, ByteBuffer rgba) {
        publishedAs(texture)
                .filter(mirrored -> mirrored.holds(area))
                .ifPresent(mirrored -> provider.write(mirrored.copy(), area, rgba));
    }

    @Override
    public void setParameter(int texture, int parameter, int value) {
        publishedAs(texture).ifPresent(mirrored -> {
            mirrored.keep(parameter, value);
            provider.setParameter(mirrored.copy(), parameter, value);
        });
    }

    @Override
    public void delete(int texture) {
        MirroredTexture mirrored = textures.remove(texture);
        if (mirrored != null) {
            aliases.withdraw(mirrored.resource());
        }
    }

    public int mirroredCount() {
        return textures.size();
    }

    @Override
    public void close() {
        textures.clear();
        provider.close();
    }

    private Optional<MirroredTexture> publishedAs(int texture) {
        MirroredTexture mirrored = textures.get(texture);
        if (mirrored == null) {
            return Optional.empty();
        }
        OptionalInt name = aliases.nameOf(mirrored.resource());
        if (name.isPresent() && name.getAsInt() == texture) {
            return Optional.of(mirrored);
        }
        textures.remove(texture);
        return Optional.empty();
    }
}
