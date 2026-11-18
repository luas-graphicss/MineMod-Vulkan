package io.github.luasgraphicss.minemod.adapters.minecraft.frame;

public final class MenuBlur {
    private static final float FULL_RADIUS = 10.0f;

    private MenuBlur() {}

    public static int radiusOf(double blurriness) {
        return (int) ((float) blurriness * FULL_RADIUS);
    }
}
