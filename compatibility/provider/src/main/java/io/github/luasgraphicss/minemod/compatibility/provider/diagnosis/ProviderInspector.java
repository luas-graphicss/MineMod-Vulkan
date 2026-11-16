package io.github.luasgraphicss.minemod.compatibility.provider.diagnosis;

import io.github.luasgraphicss.minemod.compatibility.provider.artifact.ProviderPin;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.CapabilityRequirements;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ProviderCapabilities;
import io.github.luasgraphicss.minemod.compatibility.provider.identity.DriverKind;
import io.github.luasgraphicss.minemod.compatibility.provider.identity.ProviderIdentity;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class ProviderInspector {
    private ProviderInspector() {
    }

    public static ProviderDiagnosis inspect(
            ProviderCapabilities capabilities, ProviderPin pin, CapabilityRequirements requirements) {
        ProviderIdentity identity = capabilities.identity();
        Map<RejectionReason, String> rejections = new EnumMap<>(RejectionReason.class);
        if (identity.driverKind() != DriverKind.ZINK) {
            rejections.put(
                    RejectionReason.NOT_ZINK, "the context is served by " + identity.driverKind().description());
        }
        if (identity.runsOnSoftwareDevice() && !requirements.softwareDeviceAllowed()) {
            rejections.put(RejectionReason.SOFTWARE_DEVICE, identity.deviceName() + " renders on the processor");
        }
        if (!identity.mesaVersion().filter(pin.mesaVersion()::equals).isPresent()) {
            rejections.put(
                    RejectionReason.MESA_VERSION_MISMATCH,
                    "pinned " + pin.mesaVersion() + ", found " + identity.mesaVersion().orElse("no Mesa version"));
        }
        if (!identity.glVersion().isAtLeast(requirements.minimumVersion())) {
            rejections.put(
                    RejectionReason.GL_VERSION_TOO_LOW,
                    "needs OpenGL " + requirements.minimumVersion() + ", found " + identity.glVersion());
        }
        if (identity.profile() != requirements.profile()) {
            rejections.put(
                    RejectionReason.WRONG_PROFILE,
                    "needs a " + requirements.profile() + " context, found " + identity.profile());
        }
        Set<String> missing = new TreeSet<>(requirements.requiredExtensions());
        missing.removeAll(capabilities.extensions());
        if (!missing.isEmpty()) {
            rejections.put(RejectionReason.MISSING_EXTENSIONS, String.join(", ", missing));
        }
        return new ProviderDiagnosis(identity, rejections);
    }
}
