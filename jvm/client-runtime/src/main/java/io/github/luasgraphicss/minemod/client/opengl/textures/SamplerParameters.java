package io.github.luasgraphicss.minemod.client.opengl.textures;

import io.github.luasgraphicss.minemod.codec.scene.TextureSampling;
import java.util.EnumSet;
import java.util.Set;

final class SamplerParameters {
    private static final int MAGNIFICATION_FILTER = 0x2800;
    private static final int MINIFICATION_FILTER = 0x2801;
    private static final int WRAP_S = 0x2802;
    private static final int MAX_LEVEL = 0x813D;
    private static final int LINEAR = 0x2601;
    private static final int NEAREST_BETWEEN_LEVELS = 0x2702;
    private static final int LINEAR_BETWEEN_LEVELS = 0x2703;
    private static final int REPEAT = 0x2901;
    private static final int MIRRORED_REPEAT = 0x8370;
    private static final int UNLIMITED_LEVEL = 1000;

    private int magnification = LINEAR;
    private int minification = NEAREST_BETWEEN_LEVELS;
    private int wrap = REPEAT;
    private int maxLevel = UNLIMITED_LEVEL;

    boolean set(int parameter, int value) {
        switch (parameter) {
            case MAGNIFICATION_FILTER:
                magnification = value;
                return true;
            case MINIFICATION_FILTER:
                minification = value;
                return true;
            case WRAP_S:
                wrap = value;
                return true;
            case MAX_LEVEL:
                maxLevel = Math.max(0, value);
                return false;
            default:
                return false;
        }
    }

    int maxLevel() {
        return maxLevel;
    }

    Set<TextureSampling> sampling() {
        Set<TextureSampling> choices = EnumSet.noneOf(TextureSampling.class);
        if (magnification == LINEAR) {
            choices.add(TextureSampling.LINEAR);
        }
        if (minification == NEAREST_BETWEEN_LEVELS || minification == LINEAR_BETWEEN_LEVELS) {
            choices.add(TextureSampling.BLEND_LEVELS);
        }
        if (wrap == REPEAT || wrap == MIRRORED_REPEAT) {
            choices.add(TextureSampling.REPEAT);
        }
        return choices;
    }
}
