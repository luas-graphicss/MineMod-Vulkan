package io.github.luasgraphicss.minemod.adapters.transformers.callsite;

import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformKind;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPoint;
import java.util.Objects;

public final class CallSiteRedirect {
    private final TransformPoint point;
    private final MethodTarget callee;

    public CallSiteRedirect(TransformPoint point, MethodTarget callee) {
        if (point.kind() != TransformKind.HOOK) {
            throw new IllegalArgumentException("point " + point.id() + " replaces " + point.target()
                    + "; a redirected call only hooks the method around it");
        }
        this.point = point;
        this.callee = Objects.requireNonNull(callee, "callee");
    }

    public TransformPoint point() {
        return point;
    }

    public MethodTarget callee() {
        return callee;
    }

    public String enclosingClass() {
        return point.target().owner();
    }

    public boolean isWithin(String methodName, String methodDescriptor) {
        return point.target().name().equals(methodName) && point.target().descriptor().equals(methodDescriptor);
    }

    public boolean redirects(String owner, String name, String descriptor) {
        return callee.owner().equals(owner) && callee.name().equals(name) && callee.descriptor().equals(descriptor);
    }
}
