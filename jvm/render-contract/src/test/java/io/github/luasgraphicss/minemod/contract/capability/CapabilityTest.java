package io.github.luasgraphicss.minemod.contract.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class CapabilityTest {
    @Test
    void encodesCapabilitiesAsTheBitsOfTheNativeHeader() {
        assertEquals(0b01L, Capability.COMMAND_STREAM.mask());
        assertEquals(0b10L, Capability.BUFFER_RESOURCES.mask());
        assertEquals(0b11L, Capability.maskOf(EnumSet.allOf(Capability.class)));
        assertEquals(0L, Capability.maskOf(EnumSet.noneOf(Capability.class)));
    }

    @Test
    void ignoresBitsOfCapabilitiesItDoesNotKnow() {
        long maskWithUnknownBit = Capability.BUFFER_RESOURCES.mask() | (1L << 63);

        assertEquals(EnumSet.of(Capability.BUFFER_RESOURCES), Capability.knownIn(maskWithUnknownBit));
    }
}
