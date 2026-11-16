package io.github.luasgraphicss.minemod.compatibility.provider.capability;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class CapabilityRequirements {
    private final GlVersion minimumVersion;
    private final ContextProfile profile;
    private final Set<String> requiredExtensions;
    private final boolean softwareDeviceAllowed;

    public CapabilityRequirements(
            GlVersion minimumVersion,
            ContextProfile profile,
            Collection<String> requiredExtensions,
            boolean softwareDeviceAllowed) {
        this.minimumVersion = Objects.requireNonNull(minimumVersion, "minimumVersion");
        this.profile = Objects.requireNonNull(profile, "profile");
        this.requiredExtensions = Collections.unmodifiableSet(new TreeSet<>(requiredExtensions));
        this.softwareDeviceAllowed = softwareDeviceAllowed;
    }

    public GlVersion minimumVersion() {
        return minimumVersion;
    }

    public ContextProfile profile() {
        return profile;
    }

    public Set<String> requiredExtensions() {
        return requiredExtensions;
    }

    public boolean softwareDeviceAllowed() {
        return softwareDeviceAllowed;
    }
}
