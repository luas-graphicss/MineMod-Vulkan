package io.github.luasgraphicss.minemod.compatibility.provider.diagnosis;

import io.github.luasgraphicss.minemod.compatibility.provider.identity.ProviderIdentity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ProviderDiagnosis {
    private final ProviderIdentity identity;
    private final Map<RejectionReason, String> rejections;

    ProviderDiagnosis(ProviderIdentity identity, Map<RejectionReason, String> rejections) {
        this.identity = identity;
        this.rejections = Collections.unmodifiableMap(new EnumMap<>(rejections));
    }

    public ProviderIdentity identity() {
        return identity;
    }

    public boolean isAccepted() {
        return rejections.isEmpty();
    }

    public Map<RejectionReason, String> rejections() {
        return rejections;
    }

    public List<String> lines() {
        List<String> lines = new ArrayList<>();
        lines.add("driver: " + identity.driverKind().description() + " on " + identity.deviceName());
        lines.add("OpenGL " + identity.glVersion() + " " + identity.profile() + ", Mesa "
                + identity.mesaVersion().orElse("not reported"));
        for (Map.Entry<RejectionReason, String> rejection : rejections.entrySet()) {
            lines.add("rejected, " + rejection.getKey() + ": " + rejection.getValue());
        }
        if (rejections.isEmpty()) {
            lines.add("accepted");
        }
        return lines;
    }
}
