package io.github.luasgraphicss.minemod.adapters.frame.draw;

import io.github.luasgraphicss.minemod.adapters.frame.arrays.Geometry;
import io.github.luasgraphicss.minemod.adapters.frame.pipeline.Matrices;
import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class EyeSpace {
    private static final int AXES = 3;
    private static final int FLOAT_BYTES = 4;
    private static final float LONGEST_NORMAL_BYTE = 127.0f;

    private EyeSpace() {}

    static Geometry moved(Geometry geometry, float[] modelView) {
        ByteBuffer source = geometry.vertices();
        int bytes = geometry.vertexCount() * geometry.stride();
        source.limit(source.position() + bytes);
        ByteBuffer moved = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
        moved.put(source);
        moved.flip();
        float[] turn = normalTurnOf(modelView);
        int position = geometry.offsetOf(VertexElement.POSITION);
        int normal = geometry.offsetOf(VertexElement.NORMAL);
        for (int vertex = 0; vertex < geometry.vertexCount(); vertex++) {
            int at = vertex * geometry.stride();
            movePosition(moved, at + position, modelView);
            if (normal >= 0) {
                turnNormal(moved, at + normal, turn);
            }
        }
        return new Geometry(geometry.mode(), moved, geometry.vertexCount(), geometry.stride(), geometry.offsets());
    }

    private static void movePosition(ByteBuffer vertices, int at, float[] modelView) {
        float x = vertices.getFloat(at);
        float y = vertices.getFloat(at + FLOAT_BYTES);
        float z = vertices.getFloat(at + 2 * FLOAT_BYTES);
        for (int row = 0; row < AXES; row++) {
            vertices.putFloat(
                    at + row * FLOAT_BYTES,
                    modelView[Matrices.at(0, row)] * x
                            + modelView[Matrices.at(1, row)] * y
                            + modelView[Matrices.at(2, row)] * z
                            + modelView[Matrices.at(3, row)]);
        }
    }

    private static void turnNormal(ByteBuffer vertices, int at, float[] turn) {
        float x = vertices.get(at) / LONGEST_NORMAL_BYTE;
        float y = vertices.get(at + 1) / LONGEST_NORMAL_BYTE;
        float z = vertices.get(at + 2) / LONGEST_NORMAL_BYTE;
        float[] turned = new float[AXES];
        float squaredLength = 0.0f;
        for (int row = 0; row < AXES; row++) {
            turned[row] = turn[row * AXES] * x + turn[row * AXES + 1] * y + turn[row * AXES + 2] * z;
            squaredLength += turned[row] * turned[row];
        }
        float length = squaredLength > 0.0f ? (float) Math.sqrt(squaredLength) : 1.0f;
        for (int row = 0; row < AXES; row++) {
            vertices.put(at + row, (byte) Math.round(turned[row] / length * LONGEST_NORMAL_BYTE));
        }
    }

    private static float[] normalTurnOf(float[] modelView) {
        float[] linear = new float[AXES * AXES];
        for (int row = 0; row < AXES; row++) {
            for (int column = 0; column < AXES; column++) {
                linear[row * AXES + column] = modelView[Matrices.at(column, row)];
            }
        }
        float[] cofactors = new float[AXES * AXES];
        for (int row = 0; row < AXES; row++) {
            for (int column = 0; column < AXES; column++) {
                int nextRow = (row + 1) % AXES;
                int lastRow = (row + 2) % AXES;
                int nextColumn = (column + 1) % AXES;
                int lastColumn = (column + 2) % AXES;
                cofactors[row * AXES + column] = linear[nextRow * AXES + nextColumn] * linear[lastRow * AXES + lastColumn]
                        - linear[nextRow * AXES + lastColumn] * linear[lastRow * AXES + nextColumn];
            }
        }
        float determinant = linear[0] * cofactors[0] + linear[1] * cofactors[1] + linear[2] * cofactors[2];
        if (determinant < 0.0f) {
            for (int value = 0; value < cofactors.length; value++) {
                cofactors[value] = -cofactors[value];
            }
        }
        return cofactors;
    }
}
