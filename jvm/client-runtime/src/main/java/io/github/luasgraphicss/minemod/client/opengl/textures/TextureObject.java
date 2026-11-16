package io.github.luasgraphicss.minemod.client.opengl.textures;

import io.github.luasgraphicss.minemod.client.textures.TextureShape;
import io.github.luasgraphicss.minemod.codec.scene.TextureSampling;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class TextureObject {
    private static final int DEPTH_COMPONENT = 0x1902;
    private static final int DEPTH_COMPONENT_24 = 0x81A6;
    private static final int DEPTH_COMPONENT_32_FLOAT = 0x8CAC;
    private static final int WIDTH = 0;
    private static final int HEIGHT = 1;

    private final SamplerParameters parameters = new SamplerParameters();
    private final List<int[]> levels = new ArrayList<>();
    private boolean holdsColor;
    private boolean isPublished;

    boolean set(int parameter, int value) {
        return parameters.set(parameter, value);
    }

    Set<TextureSampling> sampling() {
        return parameters.sampling();
    }

    void specify(int level, int internalFormat, int width, int height) {
        if (level == 0) {
            levels.clear();
            holdsColor = !isDepth(internalFormat);
            isPublished = false;
        }
        if (level == levels.size()) {
            levels.add(new int[] {width, height});
        }
    }

    boolean holdsColor() {
        return holdsColor && !levels.isEmpty();
    }

    boolean isPublished() {
        return isPublished;
    }

    void publish() {
        isPublished = true;
    }

    int widthOf(int level) {
        return sizeOf(level, WIDTH);
    }

    int heightOf(int level) {
        return sizeOf(level, HEIGHT);
    }

    Optional<TextureShape> shape() {
        if (levels.isEmpty()) {
            return Optional.empty();
        }
        int width = levels.get(0)[WIDTH];
        int height = levels.get(0)[HEIGHT];
        if (width < 1 || height < 1 || width > TextureShape.MAX_EXTENT || height > TextureShape.MAX_EXTENT) {
            return Optional.empty();
        }
        int specified = Math.min(levels.size(), TextureShape.fullChainOf(width, height));
        int sampled = (int) Math.min(specified, parameters.maxLevel() + 1L);
        return Optional.of(new TextureShape(width, height, sampled));
    }

    private int sizeOf(int level, int side) {
        return level >= 0 && level < levels.size() ? levels.get(level)[side] : 0;
    }

    private static boolean isDepth(int internalFormat) {
        return internalFormat == DEPTH_COMPONENT
                || internalFormat == DEPTH_COMPONENT_24
                || internalFormat == DEPTH_COMPONENT_32_FLOAT;
    }
}
