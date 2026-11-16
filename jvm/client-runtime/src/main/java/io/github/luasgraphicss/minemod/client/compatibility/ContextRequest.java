package io.github.luasgraphicss.minemod.client.compatibility;

import io.github.luasgraphicss.minemod.compatibility.provider.capability.CapabilityRequirements;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.GlVersion;
import java.util.Collections;
import java.util.Objects;

public final class ContextRequest {
    private final int majorVersion;
    private final int minorVersion;
    private final ContextProfile profile;
    private final boolean softwareDeviceAllowed;

    public ContextRequest(int majorVersion, int minorVersion, ContextProfile profile, boolean softwareDeviceAllowed) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.profile = Objects.requireNonNull(profile, "profile");
        this.softwareDeviceAllowed = softwareDeviceAllowed;
    }

    public int majorVersion() {
        return majorVersion;
    }

    public int minorVersion() {
        return minorVersion;
    }

    public CapabilityRequirements requirements() {
        return new CapabilityRequirements(
                GlVersion.of(majorVersion, minorVersion), profile, Collections.<String>emptySet(), softwareDeviceAllowed);
    }
}
