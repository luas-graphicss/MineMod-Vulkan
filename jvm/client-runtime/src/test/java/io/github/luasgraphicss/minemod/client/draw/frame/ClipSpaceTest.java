package io.github.luasgraphicss.minemod.client.draw.frame;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ClipSpaceTest {
    private static final float NEAR = 0.05f;
    private static final float FAR = 256.0f;
    private static final float TOLERANCE = 1.0e-5f;
    private static final float[] IDENTITY = {
        1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f,
    };

    @Test
    void depthFromMinusOneToOneBecomesZeroToOne() {
        float[] converted = ClipSpace.fromOpenGl(IDENTITY);

        assertEquals(0.0f, depthOf(converted, -1.0f), TOLERANCE);
        assertEquals(0.5f, depthOf(converted, 0.0f), TOLERANCE);
        assertEquals(1.0f, depthOf(converted, 1.0f), TOLERANCE);
    }

    @Test
    void aPerspectiveOfTheGameKeepsItsNearAndFarPlanes() {
        float[] converted = ClipSpace.fromOpenGl(perspective());

        assertEquals(0.0f, depthOf(converted, -NEAR), TOLERANCE);
        assertEquals(1.0f, depthOf(converted, -FAR), TOLERANCE);
    }

    @Test
    void theOtherRowsAreLeftAsTheyWere() {
        float[] projection = perspective();
        float[] converted = ClipSpace.fromOpenGl(projection);

        assertEquals(projection[0], converted[0]);
        assertEquals(projection[5], converted[5]);
        assertEquals(projection[11], converted[11]);
        assertArrayEquals(perspective(), projection);
    }

    @Test
    void aMatrixHasSixteenValues() {
        assertThrows(IllegalArgumentException.class, () -> ClipSpace.fromOpenGl(new float[12]));
    }

    private static float[] perspective() {
        float[] projection = new float[16];
        projection[0] = 1.0f;
        projection[5] = 1.0f;
        projection[10] = -(FAR + NEAR) / (FAR - NEAR);
        projection[11] = -1.0f;
        projection[14] = -2.0f * FAR * NEAR / (FAR - NEAR);
        return projection;
    }

    private static float depthOf(float[] matrix, float z) {
        float clipZ = matrix[10] * z + matrix[14];
        float clipW = matrix[11] * z + matrix[15];
        return clipZ / clipW;
    }
}
