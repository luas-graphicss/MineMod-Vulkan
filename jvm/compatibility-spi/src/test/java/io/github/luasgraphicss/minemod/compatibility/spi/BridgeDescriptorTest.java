package io.github.luasgraphicss.minemod.compatibility.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class BridgeDescriptorTest {
    @Test
    void aDescriptorKeepsItsOwnCopyOfTheOperations() {
        List<String> operations = new ArrayList<>(Arrays.asList("query result across native draws", "front buffer"));
        BridgeDescriptor descriptor = new BridgeDescriptor("occlusion", SpiVersion.CURRENT, operations);

        operations.clear();

        assertEquals(2, descriptor.operations().size());
        assertThrows(UnsupportedOperationException.class, () -> descriptor.operations().add("another"));
    }

    @Test
    void aBridgeIsNamedAndServesSomething() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BridgeDescriptor(" ", SpiVersion.CURRENT, Collections.singleton("front buffer")));
        assertThrows(
                IllegalArgumentException.class,
                () -> new BridgeDescriptor("occlusion", SpiVersion.CURRENT, Collections.<String>emptyList()));
    }

    @Test
    void onlyTheCurrentSpiVersionIsAccepted() {
        assertTrue(SpiVersion.accepts(SpiVersion.CURRENT));
        assertFalse(SpiVersion.accepts(SpiVersion.CURRENT + 1));
    }

    @Test
    void sceneAttachmentsAreNeverEmpty() {
        assertEquals(854, new SceneAttachments(854, 480, 1).width());
        assertThrows(IllegalArgumentException.class, () -> new SceneAttachments(0, 480, 1));
        assertThrows(IllegalArgumentException.class, () -> new SceneAttachments(854, 480, 0));
    }
}
