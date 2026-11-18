package io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction;

import java.util.Optional;

public final class MatrixStacks {
    private static final int MODEL_VIEW = 0x1700;
    private static final int PROJECTION = 0x1701;
    private static final int TEXTURE = 0x1702;
    private static final int MODEL_VIEW_MATRIX = 0x0BA6;
    private static final int PROJECTION_MATRIX = 0x0BA7;
    private static final int TEXTURE_MATRIX = 0x0BA8;

    private final MatrixStack modelView = new MatrixStack();
    private final MatrixStack projection = new MatrixStack();
    private final TextureUnits units;
    private int mode = MODEL_VIEW;

    MatrixStacks(TextureUnits units) {
        this.units = units;
    }

    public void select(int stack) {
        mode = stack;
    }

    public MatrixStack selected() {
        switch (mode) {
            case PROJECTION:
                return projection;
            case TEXTURE:
                return units.activeMatrix();
            default:
                return modelView;
        }
    }

    public MatrixStack modelView() {
        return modelView;
    }

    public MatrixStack projection() {
        return projection;
    }

    public Optional<float[]> read(int name) {
        switch (name) {
            case MODEL_VIEW_MATRIX:
                return Optional.of(modelView.current());
            case PROJECTION_MATRIX:
                return Optional.of(projection.current());
            case TEXTURE_MATRIX:
                return Optional.of(units.activeMatrix().current());
            default:
                return Optional.empty();
        }
    }
}
