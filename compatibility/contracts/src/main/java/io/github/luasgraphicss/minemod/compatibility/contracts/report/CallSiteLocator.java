package io.github.luasgraphicss.minemod.compatibility.contracts.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class CallSiteLocator {
    private final List<String> infrastructurePrefixes;
    private final OriginResolver origins;

    public CallSiteLocator(Collection<String> infrastructurePrefixes, OriginResolver origins) {
        this.infrastructurePrefixes = new ArrayList<>(infrastructurePrefixes);
        this.origins = Objects.requireNonNull(origins, "origins");
    }

    public CallSite locate(StackTraceElement[] stack) {
        for (StackTraceElement frame : stack) {
            String className = frame.getClassName();
            if (!isInfrastructure(className)) {
                String origin = origins.locationOf(className).map(OriginLabel::of).orElse(OriginLabel.UNKNOWN);
                return new CallSite(className, frame.getMethodName(), origin);
            }
        }
        return CallSite.unknown();
    }

    private boolean isInfrastructure(String className) {
        for (String prefix : infrastructurePrefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
