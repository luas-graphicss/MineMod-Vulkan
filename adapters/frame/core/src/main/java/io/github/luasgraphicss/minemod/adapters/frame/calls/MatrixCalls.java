package io.github.luasgraphicss.minemod.adapters.frame.calls;

import io.github.luasgraphicss.minemod.adapters.frame.pipeline.Matrices;
import io.github.luasgraphicss.minemod.adapters.frame.pipeline.MatrixStack;
import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrames;
import java.nio.FloatBuffer;
import java.util.function.Consumer;

public final class MatrixCalls {
    private MatrixCalls() {}

    public static void glMatrixMode(int mode) {
        GameFrames.run(frame -> frame.pipeline().matrices().select(mode));
    }

    public static void glLoadIdentity() {
        change(stack -> stack.load(Matrices.identity()));
    }

    public static void glPushMatrix() {
        change(MatrixStack::push);
    }

    public static void glPopMatrix() {
        change(MatrixStack::pop);
    }

    public static void glMultMatrix(FloatBuffer values) {
        if (values.remaining() < Matrices.VALUES) {
            throw new IllegalArgumentException(
                    "a matrix has " + Matrices.VALUES + " values, found " + values.remaining());
        }
        float[] matrix = new float[Matrices.VALUES];
        for (int value = 0; value < Matrices.VALUES; value++) {
            matrix[value] = values.get(values.position() + value);
        }
        multiply(matrix);
    }

    public static void glMultMatrixf(FloatBuffer values) {
        glMultMatrix(values);
    }

    public static void glTranslatef(float x, float y, float z) {
        multiply(Matrices.translation(x, y, z));
    }

    public static void glTranslated(double x, double y, double z) {
        multiply(Matrices.translation((float) x, (float) y, (float) z));
    }

    public static void glRotatef(float degrees, float x, float y, float z) {
        multiply(Matrices.rotation(degrees, x, y, z));
    }

    public static void glRotated(double degrees, double x, double y, double z) {
        multiply(Matrices.rotation((float) degrees, (float) x, (float) y, (float) z));
    }

    public static void glScalef(float x, float y, float z) {
        multiply(Matrices.scale(x, y, z));
    }

    public static void glScaled(double x, double y, double z) {
        multiply(Matrices.scale((float) x, (float) y, (float) z));
    }

    public static void glOrtho(double left, double right, double bottom, double top, double near, double far) {
        multiply(Matrices.orthographic(left, right, bottom, top, near, far));
    }

    public static void gluPerspective(float verticalDegrees, float aspect, float near, float far) {
        multiply(Matrices.perspective(verticalDegrees, aspect, near, far));
    }

    public static void glGetFloat(int name, FloatBuffer into) {
        GameFrames.current().pipeline().matrices().read(name).ifPresent(matrix -> {
            for (int value = 0; value < Matrices.VALUES && value < into.remaining(); value++) {
                into.put(into.position() + value, matrix[value]);
            }
        });
    }

    public static void glGetFloatv(int name, FloatBuffer into) {
        glGetFloat(name, into);
    }

    private static void multiply(float[] matrix) {
        change(stack -> stack.multiply(matrix));
    }

    private static void change(Consumer<MatrixStack> change) {
        GameFrames.run(frame -> change.accept(frame.pipeline().matrices().selected()));
    }
}
