package io.github.luasgraphicss.minemod.adapters.frame.pipeline;

import java.util.ArrayDeque;
import java.util.Deque;

public final class MatrixStack {
    private final Deque<float[]> saved = new ArrayDeque<>();
    private float[] current = Matrices.identity();

    public float[] current() {
        return current.clone();
    }

    public boolean isIdentity() {
        return Matrices.isIdentity(current);
    }

    public void load(float[] matrix) {
        if (matrix.length != Matrices.VALUES) {
            throw new IllegalArgumentException("a matrix has " + Matrices.VALUES + " values, found " + matrix.length);
        }
        current = matrix.clone();
    }

    public void multiply(float[] right) {
        current = Matrices.product(current, right);
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
