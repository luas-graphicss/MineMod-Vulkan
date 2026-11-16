package io.github.luasgraphicss.minemod.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SceneMetricsTest {
    @Test
    void readsTheFieldsInTheOrderOfTheNativeMetrics() {
        SceneMetrics metrics = new SceneMetrics(new long[] {1, 2, 3, 4, 5, 6, 7, 8});

        assertEquals(1, metrics.framesMeasured());
        assertEquals(2, metrics.meanFrameNanoseconds());
        assertEquals(3, metrics.worstFrameNanoseconds());
        assertEquals(4, metrics.peakTextureBytes());
        assertEquals(5, metrics.peakStagingBytes());
        assertEquals(6, metrics.peakPipelines());
        assertEquals(7, metrics.retiredResources());
        assertEquals(8, metrics.resourceGeneration());
    }

    @Test
    void refusesAnotherNumberOfFields() {
        assertThrows(IllegalArgumentException.class, () -> new SceneMetrics(new long[7]));
    }

    @Test
    void keepsItsFieldsWhenTheSourceArrayChanges() {
        long[] fields = {1, 2, 3, 4, 5, 6, 7, 8};
        SceneMetrics metrics = new SceneMetrics(fields);

        fields[0] = 99;

        assertEquals(1, metrics.framesMeasured());
    }
}
