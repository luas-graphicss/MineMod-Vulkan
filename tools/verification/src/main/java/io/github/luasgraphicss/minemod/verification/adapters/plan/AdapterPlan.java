package io.github.luasgraphicss.minemod.verification.adapters.plan;

import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.verification.adapters.names.RuntimeNamespace;
import java.util.Objects;

public record AdapterPlan(ReleasePlan plan, RuntimeNamespace namespace, String namesUrl, String namesSha1) {
    private static final String NOT_MAPPED = "";

    public AdapterPlan {
        Objects.requireNonNull(plan, "plan");
        if (namespace.isReadFromMappings() == namesUrl.isEmpty()) {
            throw new IllegalArgumentException(namespace.isReadFromMappings()
                    ? "a plan in " + namespace.id() + " names tells the mapping file they were read from"
                    : "a plan in " + namespace.id() + " names is read from no mapping file");
        }
    }

    public static AdapterPlan written(ReleasePlan plan, RuntimeNamespace namespace) {
        return new AdapterPlan(plan, namespace, NOT_MAPPED, NOT_MAPPED);
    }

    public static AdapterPlan mapped(ReleasePlan plan, RuntimeNamespace namespace, String namesUrl, String namesSha1) {
        return new AdapterPlan(plan, namespace, namesUrl, namesSha1);
    }
}
