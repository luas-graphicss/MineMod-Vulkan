package io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction;

import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.IntSupplier;

public final class FixedPipeline {
    private static final FixedPipeline CURRENT =
            new FixedPipeline(() -> ClientRendering.openGl().textures().activeUnit());

    private final TextureUnits units;
    private final MatrixStacks matrices;
    private final AlphaTest alphaTest = new AlphaTest();
    private final FogSettings fog = new FogSettings();
    private final DiffuseLights lights = new DiffuseLights();
    private final Deque<EnabledSteps> savedSteps = new ArrayDeque<>();
    private final float[] color = {1.0f, 1.0f, 1.0f, 1.0f};
    private float lineWidth = 1.0f;

    private FixedPipeline(IntSupplier activeUnitName) {
        this.units = new TextureUnits(activeUnitName);
        this.matrices = new MatrixStacks(units);
    }

    public static FixedPipeline current() {
        return CURRENT;
    }

    public TextureUnits units() {
        return units;
    }

    public MatrixStacks matrices() {
        return matrices;
    }

    public AlphaTest alphaTest() {
        return alphaTest;
    }

    public FogSettings fog() {
        return fog;
    }

    public DiffuseLights lights() {
        return lights;
    }

    public void color(float red, float green, float blue, float alpha) {
        color[0] = red;
        color[1] = green;
        color[2] = blue;
        color[3] = alpha;
    }

    public float[] color() {
        return color.clone();
    }

    public void lineWidth(float pixels) {
        lineWidth = pixels;
    }

    public float lineWidth() {
        return lineWidth;
    }

    public void saveSteps() {
        savedSteps.push(EnabledSteps.of(this));
    }

    public void restoreSteps() {
        if (!savedSteps.isEmpty()) {
            savedSteps.pop().restoreTo(this);
        }
    }
}
