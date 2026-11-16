package io.github.luasgraphicss.minemod.compatibility.contracts.report;

import java.util.Objects;

public final class CallSite {
    private static final CallSite UNKNOWN = new CallSite("unknown", "unknown", OriginLabel.UNKNOWN);

    private final String className;
    private final String methodName;
    private final String origin;

    public CallSite(String className, String methodName, String origin) {
        this.className = Objects.requireNonNull(className, "className");
        this.methodName = Objects.requireNonNull(methodName, "methodName");
        this.origin = Objects.requireNonNull(origin, "origin");
    }

    public static CallSite unknown() {
        return UNKNOWN;
    }

    public String className() {
        return className;
    }

    public String methodName() {
        return methodName;
    }

    public String origin() {
        return origin;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CallSite)) {
            return false;
        }
        CallSite site = (CallSite) other;
        return className.equals(site.className) && methodName.equals(site.methodName) && origin.equals(site.origin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, origin);
    }

    @Override
    public String toString() {
        return className + "." + methodName + " (" + origin + ")";
    }
}
