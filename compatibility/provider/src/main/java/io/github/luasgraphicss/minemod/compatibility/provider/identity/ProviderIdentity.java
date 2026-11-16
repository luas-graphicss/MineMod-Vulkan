package io.github.luasgraphicss.minemod.compatibility.provider.identity;

import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.GlVersion;
import java.util.Optional;

public final class ProviderIdentity {
    private final DriverKind driverKind;
    private final GlVersion glVersion;
    private final ContextProfile profile;
    private final Optional<String> mesaVersion;
    private final String deviceName;
    private final boolean softwareDevice;

    ProviderIdentity(
            DriverKind driverKind,
            GlVersion glVersion,
            ContextProfile profile,
            Optional<String> mesaVersion,
            String deviceName,
            boolean softwareDevice) {
        this.driverKind = driverKind;
        this.glVersion = glVersion;
        this.profile = profile;
        this.mesaVersion = mesaVersion;
        this.deviceName = deviceName;
        this.softwareDevice = softwareDevice;
    }

    public DriverKind driverKind() {
        return driverKind;
    }

    public GlVersion glVersion() {
        return glVersion;
    }

    public ContextProfile profile() {
        return profile;
    }

    public Optional<String> mesaVersion() {
        return mesaVersion;
    }

    public String deviceName() {
        return deviceName;
    }

    public boolean runsOnSoftwareDevice() {
        return softwareDevice;
    }
}
