package io.github.luasgraphicss.minemod.adapters.frame.pipeline;

import java.util.function.IntSupplier;

public final class TextureUnits {
    public static final int SAMPLED = 0;
    public static final int LIGHTMAP = 1;
    public static final int FIRST_NAME = 0x84C0;
    private static final int COUNT = 8;
    private static final int COORDINATES = 2;

    private final IntSupplier activeName;
    private final boolean[] isEnabled = new boolean[COUNT];
    private final boolean[] generatesCoordinates = new boolean[COUNT];
    private final MatrixStack[] matrices = new MatrixStack[COUNT];
    private final float[][] coordinates = new float[COUNT][COORDINATES];

    TextureUnits(IntSupplier activeName) {
        this.activeName = activeName;
        for (int unit = 0; unit < COUNT; unit++) {
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

    public void coordinates(int unitName, float horizontal, float vertical) {
        float[] current = coordinates[indexOf(unitName)];
        current[0] = horizontal;
        current[1] = vertical;
    }

    public float[] coordinates(int unit) {
        return coordinates[unit].clone();
    }

    MatrixStack activeMatrix() {
        return matrices[active()];
    }

    boolean[] enabledUnits() {
        return isEnabled.clone();
    }

    void restore(boolean[] enabledUnits) {
        System.arraycopy(enabledUnits, 0, isEnabled, 0, COUNT);
    }

    private int active() {
        return indexOf(activeName.getAsInt());
    }

    private static int indexOf(int unitName) {
        return Math.max(0, Math.min(COUNT - 1, unitName - FIRST_NAME));
    }
}
