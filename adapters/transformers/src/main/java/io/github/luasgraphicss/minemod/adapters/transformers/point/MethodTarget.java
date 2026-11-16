package io.github.luasgraphicss.minemod.adapters.transformers.point;

import java.util.Objects;
import java.util.regex.Pattern;

public final class MethodTarget {
    private static final String FIELD_TYPE = "\\[*(?:[BCDFIJSZ]|L[^;\\[.]+;)";
    private static final Pattern OWNER = Pattern.compile("[^;\\[./]+(?:/[^;\\[./]+)*");
    private static final Pattern NAME = Pattern.compile("<init>|<clinit>|[^;\\[./<>*]+");
    private static final Pattern DESCRIPTOR =
            Pattern.compile("\\((?:" + FIELD_TYPE + ")*\\)(?:V|" + FIELD_TYPE + ")");

    private final String owner;
    private final String name;
    private final String descriptor;

    public MethodTarget(String owner, String name, String descriptor) {
        this.owner = requireExact(OWNER, owner, "owner is not a JVM internal class name");
        this.name = requireExact(NAME, name, "name is not an exact method name");
        this.descriptor = requireExact(DESCRIPTOR, descriptor, "descriptor is not an exact method descriptor");
    }

    public String owner() {
        return owner;
    }

    public String name() {
        return name;
    }

    public String descriptor() {
        return descriptor;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MethodTarget)) {
            return false;
        }
        MethodTarget target = (MethodTarget) other;
        return owner.equals(target.owner) && name.equals(target.name) && descriptor.equals(target.descriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, name, descriptor);
    }

    @Override
    public String toString() {
        return owner + "." + name + descriptor;
    }

    private static String requireExact(Pattern shape, String text, String complaint) {
        if (!shape.matcher(text).matches()) {
            throw new IllegalArgumentException(complaint + ": " + text);
        }
        return text;
    }
}
