package io.github.luasgraphicss.minemod.compatibility.bridges.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.bridges.support.DiscoveredBridge;
import io.github.luasgraphicss.minemod.compatibility.bridges.support.RecordingBridge;
import io.github.luasgraphicss.minemod.compatibility.spi.SpiVersion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class BridgeRegistryTest {
    private static final String QUERY_RESULT = "query result across native draws";
    private static final String FEEDBACK = "transform feedback across native draws";

    private final List<String> journal = new ArrayList<>();
    private final BridgeRegistry registry = new BridgeRegistry();

    @Test
    void aBridgeIsFoundByTheOperationItServes() {
        RecordingBridge occlusion = new RecordingBridge("occlusion", journal, QUERY_RESULT);
        registry.register(occlusion);

        assertSame(occlusion, registry.bridgeFor(QUERY_RESULT).get());
        assertFalse(registry.bridgeFor(FEEDBACK).isPresent());
        assertEquals(
                Collections.singleton(FEEDBACK), registry.unserved(Arrays.asList(QUERY_RESULT, FEEDBACK)));
    }

    @Test
    void twoBridgesCannotClaimTheSameOperationOrId() {
        registry.register(new RecordingBridge("occlusion", journal, QUERY_RESULT));

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register(new RecordingBridge("another", journal, FEEDBACK, QUERY_RESULT)));
        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register(new RecordingBridge("occlusion", journal, FEEDBACK)));
        assertEquals(1, registry.bridges().size());
        assertFalse(registry.bridgeFor(FEEDBACK).isPresent());
    }

    @Test
    void aBridgeBuiltForAnotherSpiVersionIsRefused() {
        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register(new RecordingBridge("future", SpiVersion.CURRENT + 1, journal, FEEDBACK)));
    }

    @Test
    void bridgesOnTheClassPathAreDiscoveredAndTheUnusableOnesAreNamed() {
        List<String> refused = registry.discover(getClass().getClassLoader());

        assertTrue(registry.bridgeFor(DiscoveredBridge.OPERATION).isPresent());
        assertEquals(1, refused.size());
        assertTrue(refused.get(0).startsWith("bridge outdated was built for SPI version"));
        assertEquals(Collections.emptySet(), registry.unserved(Collections.singleton(DiscoveredBridge.OPERATION)));
    }
}
