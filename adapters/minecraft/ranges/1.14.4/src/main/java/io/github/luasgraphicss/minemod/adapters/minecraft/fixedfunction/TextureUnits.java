package io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction;

import io.github.luasgraphicss.minemod.client.opengl.textures.TextureObjects;
import java.util.function.IntSupplier;

public final class TextureUnits {
    public static final int SAMPLED = 0;
    public static final int LIGHTMAP = 1;
    public static final int FIRST_NAME = 0x84C0;

    private final IntSupplier activeName;
    private final boolean[] isEnabled = new boolean[TextureObjects.UNIT_COUNT];
    private final boolean[] generatesCoordinates = new boolean[TextureObjects.UNIT_COUNT];
    private final MatrixStack[] matrices = new MatrixStack[TextureObjects.UNIT_COUNT];

    TextureUnits(IntSupplier activeName) {
        this.activeName = activeName;
        for (int unit = 0; unit < matrices.length; unit++) {
            matrices[unit] = new MatrixStack();
        }
    }

    public void enable(boolean isSampled) {
        isEnabled[active()] = isSampled;
    }

    public boolean isEnabled(int unit) {
        return isEnabled[unit];
    }

    public void generateCoordinates(boolean isGenerated) {
        generatesCoordinates[active()] = isGenerated;
    }

    public boolean generatesCoordinates(int unit) {
        return generatesCoordinates[unit];
    }

    public MatrixStack matrix(int unit) {
        return matrices[unit];
    }

    MatrixStack activeMatrix() {
        return matrices[active()];
    }

    boolean[] enabledUnits() {
        return isEnabled.clone();
    }

    void restore(boolean[] enabledUnits) {
        System.arraycopy(enabledUnits, 0, isEnabled, 0, isEnabled.length);
    }

    private int active() {
        return activeName.getAsInt() - FIRST_NAME;
    }
}
