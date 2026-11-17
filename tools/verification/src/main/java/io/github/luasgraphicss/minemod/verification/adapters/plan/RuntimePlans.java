package io.github.luasgraphicss.minemod.verification.adapters.plan;

import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPoint;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.verification.adapters.names.NameTable;
import io.github.luasgraphicss.minemod.verification.adapters.names.RuntimeNamespace;
import io.github.luasgraphicss.minemod.verification.adapters.names.TsrgNames.SrgNames;
import io.github.luasgraphicss.minemod.verification.hooks.model.HookPoint;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class RuntimePlans {
    private RuntimePlans() {}

    public static ReleasePlan written(ReleaseHooks hooks, RuntimeNamespace namespace) {
        return switch (namespace) {
            case OFFICIAL -> planOf(hooks, point -> MethodTarget.parse(point.within()));
            case MOJANG -> planOf(hooks, point -> mojangMethodOf(hooks, point));
            case INTERMEDIARY, SRG ->
                throw new IllegalArgumentException(namespace + " names are read from a mapping file");
        };
    }

    public static ReleasePlan intermediary(ReleaseHooks hooks, NameTable names) {
        return planOf(hooks, point -> names.translate(MethodTarget.parse(point.within())));
    }

    public static ReleasePlan srg(ReleaseHooks hooks, SrgNames names) {
        if (!names.leavesClassNamesToMojang()) {
            return intermediary(hooks, names.table());
        }
        return planOf(hooks, point -> {
            MethodTarget mojang = mojangMethodOf(hooks, point);
            String srgName = names.table().methodNameOf(MethodTarget.parse(point.within()));
            return new MethodTarget(mojang.owner(), srgName, mojang.descriptor());
        });
    }

    private static MethodTarget mojangMethodOf(ReleaseHooks hooks, HookPoint point) {
        if (point.named() == null) {
            throw new IllegalArgumentException(
                    "release " + hooks.release() + " has no Mojang name for point " + point.id());
        }
        return MethodTarget.parse(point.named());
    }

    private static ReleasePlan planOf(ReleaseHooks hooks, Function<HookPoint, MethodTarget> enclosingMethodOf) {
        List<CallSiteRedirect> redirects = new ArrayList<>();
        for (HookPoint point : hooks.points()) {
            redirects.add(new CallSiteRedirect(
                    TransformPoint.hook(point.id(), enclosingMethodOf.apply(point), point.matches()),
                    MethodTarget.parse(point.callee())));
        }
        return new ReleasePlan(hooks.release(), hooks.clientSha1(), redirects);
    }
}
