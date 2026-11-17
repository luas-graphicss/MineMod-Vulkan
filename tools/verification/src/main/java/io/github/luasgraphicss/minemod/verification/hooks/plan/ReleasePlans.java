package io.github.luasgraphicss.minemod.verification.hooks.plan;

import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPoint;
import io.github.luasgraphicss.minemod.adapters.transformers.release.PinnedReleases;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.verification.hooks.model.HookPoint;
import io.github.luasgraphicss.minemod.verification.hooks.model.PresentationHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import java.util.ArrayList;
import java.util.List;

public final class ReleasePlans {
    private ReleasePlans() {}

    public static PinnedReleases pinnedBy(PresentationHooks hooks) {
        return new PinnedReleases(hooks.releases().stream().map(ReleasePlans::planOf).toList());
    }

    public static ReleasePlan planOf(ReleaseHooks hooks) {
        List<CallSiteRedirect> redirects = new ArrayList<>();
        for (HookPoint point : hooks.points()) {
            redirects.add(new CallSiteRedirect(
                    TransformPoint.hook(point.id(), MethodTarget.parse(point.within()), point.matches()),
                    MethodTarget.parse(point.callee())));
        }
        return new ReleasePlan(hooks.release(), hooks.clientSha1(), redirects);
    }
}
