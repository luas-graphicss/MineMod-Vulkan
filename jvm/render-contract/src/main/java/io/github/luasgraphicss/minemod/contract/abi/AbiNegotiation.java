package io.github.luasgraphicss.minemod.contract.abi;

import io.github.luasgraphicss.minemod.contract.capability.Capability;
import java.util.Set;

public final class AbiNegotiation {
    private final AbiVersion libraryVersion;
    private final long supportedCapabilities;
    private final long maxStreamBytes;
    private final long maxStreamCommands;

    public AbiNegotiation(
            AbiVersion libraryVersion, long supportedCapabilities, long maxStreamBytes, long maxStreamCommands) {
        this.libraryVersion = libraryVersion;
        this.supportedCapabilities = supportedCapabilities;
        this.maxStreamBytes = maxStreamBytes;
        this.maxStreamCommands = maxStreamCommands;
    }

    public AbiVersion libraryVersion() {
        return libraryVersion;
    }

    public Set<Capability> supportedCapabilities() {
        return Capability.knownIn(supportedCapabilities);
    }

    public long maxStreamBytes() {
        return maxStreamBytes;
    }

    public long maxStreamCommands() {
        return maxStreamCommands;
    }
}
