package io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction;

import java.util.ArrayDeque;
import java.util.Deque;

public final class MatrixStack {
    private final Deque<float[]> saved = new ArrayDeque<>();
    private float[] current = Matrix.identity();

    public float[] current() {
        return current.clone();
    }

    public boolean isIdentity() {
        return Matrix.isIdentity(current);
    }

    public void load(float[] matrix) {
        if (matrix.length != Matrix.VALUES) {
            throw new IllegalArgumentException("a matrix has " + Matrix.VALUES + " values, found " + matrix.length);
        }
        current = matrix.clone();
    }

    public void multiply(float[] right) {
        current = Matrix.product(current, right);
    }

    public void push() {
        saved.push(current.clone());
    }

    public void pop() {
        if (!saved.isEmpty()) {
            current = saved.pop();
        }
    }
}
