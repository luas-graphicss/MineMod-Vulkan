package io.github.luasgraphicss.minemod.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.contract.abi.AbiException;
import io.github.luasgraphicss.minemod.contract.abi.AbiNegotiation;
import io.github.luasgraphicss.minemod.contract.abi.AbiResult;
import io.github.luasgraphicss.minemod.contract.abi.AbiVersion;
import io.github.luasgraphicss.minemod.contract.capability.Capability;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NativeBridgeTest {
    private static final Set<Capability> ALL_CAPABILITIES = EnumSet.allOf(Capability.class);
    private static final long MAX_STREAM_BYTES = 16L * 1024 * 1024;
    private static final long MAX_STREAM_COMMANDS = 65536;

    @Test
    void negotiatesTheCurrentAbiWithTheLoadedLibrary() {
        AbiNegotiation negotiation = LoadedBridge.BRIDGE.negotiation();

        assertEquals(AbiVersion.CURRENT, negotiation.libraryVersion());
        assertEquals(ALL_CAPABILITIES, negotiation.supportedCapabilities());
        assertEquals(MAX_STREAM_BYTES, negotiation.maxStreamBytes());
        assertEquals(MAX_STREAM_COMMANDS, negotiation.maxStreamCommands());
    }

    @Test
    void rejectsClientsBuiltForAnotherAbi() {
        AbiVersion nextMajor = new AbiVersion(AbiVersion.CURRENT.major() + 1, 0);
        AbiVersion nextMinor = new AbiVersion(AbiVersion.CURRENT.major(), AbiVersion.CURRENT.minor() + 1);

        assertEquals(AbiResult.INCOMPATIBLE_ABI, negotiationFailure(nextMajor).result());
        assertEquals(AbiResult.INCOMPATIBLE_ABI, negotiationFailure(nextMinor).result());
    }

    @Test
    void refusesStreamsHeldInHeapBuffers() {
        assertThrows(IllegalArgumentException.class, () -> LoadedBridge.BRIDGE.validate(ByteBuffer.allocate(16)));
    }

    private static AbiException negotiationFailure(AbiVersion clientVersion) {
        LoadedBridge.BRIDGE.negotiation();
        return assertThrows(AbiException.class, () -> NativeBridge.negotiate(clientVersion, ALL_CAPABILITIES));
    }
}
