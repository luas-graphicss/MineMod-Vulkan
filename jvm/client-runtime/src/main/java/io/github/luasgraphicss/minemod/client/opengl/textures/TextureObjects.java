package io.github.luasgraphicss.minemod.client.opengl.textures;

import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import io.github.luasgraphicss.minemod.client.textures.TexturePublisher;
import io.github.luasgraphicss.minemod.client.textures.TextureShape;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TextureObjects {
    public static final int UNIT_COUNT = 32;
    private static final int FIRST_UNIT = 0x84C0;
    private static final int TEXTURE_2D = 0x0DE1;
    private static final int PROXY_TEXTURE_2D = 0x8064;
    private static final int LEVEL_WIDTH = 0x1000;
    private static final int LEVEL_HEIGHT = 0x1001;
    private static final int UNSIGNED_BYTE = 0x1401;
    private static final int NO_TEXTURE = 0;
    private static final long MAX_WRITE_BYTES = 256L * 1024 * 1024;

    private final TexturePublisher published;
    private final Map<Integer, TextureObject> objects = new HashMap<>();
    private final int[] bound = new int[UNIT_COUNT];
    private final PixelStore unpacking = new PixelStore();
    private int activeUnit;
    private int lastName;
    private int proxyWidth;
    private int proxyHeight;
    private TextureMirror mirror;

    public TextureObjects(TexturePublisher published) {
        this.published = published;
    }

    public void mirrorTo(TextureMirror follower) {
        if (lastName != NO_TEXTURE) {
            throw new IllegalStateException("the textures of the game are mirrored from the first one on");
        }
        mirror = Objects.requireNonNull(follower, "follower");
    }

    public void stopMirroring() {
        mirror = null;
    }

    public int generate() {
        if (mirror == null) {
            return ++lastName;
        }
        int name = mirror.generate();
        discard(name);
        lastName = Math.max(lastName, name);
        return name;
    }

    public void delete(int texture) {
        if (mirror != null) {
            mirror.delete(texture);
        }
        discard(texture);
    }

    public void activate(int unit) {
        int index = unit - FIRST_UNIT;
        if (index >= 0 && index < UNIT_COUNT) {
            activeUnit = index;
        }
    }

    public int activeUnit() {
        return FIRST_UNIT + activeUnit;
    }

    public void bind(int texture) {
        bound[activeUnit] = Math.max(NO_TEXTURE, texture);
    }

    public int bound() {
        return bound[activeUnit];
    }

    public void setParameter(int target, int parameter, int value) {
        if (target != TEXTURE_2D || bound() == NO_TEXTURE) {
            return;
        }
        TextureObject texture = boundObject();
        if (texture.set(parameter, value) && texture.isPublished()) {
            published.sample(bound(), texture.sampling());
        }
        if (mirror != null) {
            mirror.setParameter(bound(), parameter, value);
        }
    }

    public void specify(int target, int level, int internalFormat, int width, int height) {
        if (target == PROXY_TEXTURE_2D) {
            proxyWidth = width;
            proxyHeight = height;
            return;
        }
        if (target != TEXTURE_2D || bound() == NO_TEXTURE || level < 0) {
            return;
        }
        TextureObject texture = boundObject();
        if (level == 0 && texture.isPublished()) {
            published.destroy(bound());
        }
        texture.specify(level, internalFormat, width, height);
    }

    public int levelParameter(int target, int level, int parameter) {
        boolean isWidth = parameter == LEVEL_WIDTH;
        if (!isWidth && parameter != LEVEL_HEIGHT) {
            return 0;
        }
        if (target == PROXY_TEXTURE_2D) {
            boolean fits = proxyWidth <= TextureShape.MAX_EXTENT && proxyHeight <= TextureShape.MAX_EXTENT;
            return !fits ? 0 : isWidth ? proxyWidth : proxyHeight;
        }
        TextureObject texture = objects.get(bound());
        if (target != TEXTURE_2D || texture == null) {
            return 0;
        }
        return isWidth ? texture.widthOf(level) : texture.heightOf(level);
    }

    public void store(int parameter, int value) {
        unpacking.set(parameter, value);
    }

    public long sourceBytes(int format, int width, int height) {
        Optional<TexelFormat> texels = TexelFormat.ofOpenGl(format);
        if (!texels.isPresent() || width < 1 || height < 1) {
            return 0;
        }
        return unpacking.sourceBytes(texels.get(), width, height);
    }

    public void write(int target, TexelArea area, int format, int type, ByteBuffer source) {
        Optional<TexelFormat> texels = TexelFormat.ofOpenGl(format);
        TextureObject texture = objects.get(bound());
        boolean isReadable = texels.isPresent() && type == UNSIGNED_BYTE && area.bytes() <= MAX_WRITE_BYTES;
        if (target != TEXTURE_2D || texture == null || !texture.holdsColor() || !isReadable) {
            return;
        }
        if (!texture.isPublished() && !publish(texture)) {
            return;
        }
        ByteBuffer rgba = TexelUnpacker.toRgba(source, texels.get(), unpacking, area.width(), area.height());
        if (mirror != null) {
            mirror.write(bound(), area, rgba);
        }
        published.write(bound(), area, rgba);
    }

    private boolean publish(TextureObject texture) {
        Optional<TextureShape> shape = texture.shape();
        if (!shape.isPresent()) {
            return false;
        }
        published.define(bound(), shape.get(), texture.sampling());
        texture.publish();
        if (mirror != null) {
            mirror.define(bound(), shape.get());
        }
        return true;
    }

    private void discard(int texture) {
        if (objects.remove(texture) == null) {
            return;
        }
        published.destroy(texture);
        for (int unit = 0; unit < UNIT_COUNT; unit++) {
            if (bound[unit] == texture) {
                bound[unit] = NO_TEXTURE;
            }
        }
    }

    private TextureObject boundObject() {
        return objects.computeIfAbsent(bound(), created -> new TextureObject());
    }
}
