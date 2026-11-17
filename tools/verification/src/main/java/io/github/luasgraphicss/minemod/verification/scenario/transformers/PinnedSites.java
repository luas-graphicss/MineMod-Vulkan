package io.github.luasgraphicss.minemod.verification.scenario.transformers;

import io.github.luasgraphicss.minemod.verification.hooks.model.HookPoint;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.scan.CallSite;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class PinnedSites {
    private PinnedSites() {}

    public static Map<String, Integer> pinnedBy(ReleaseHooks hooks) {
        Map<String, Integer> sites = new TreeMap<>();
        for (HookPoint point : hooks.points()) {
            sites.put(point.hook() + " in " + point.within(), point.matches());
        }
        return sites;
    }

    public static Map<String, Integer> foundAt(List<CallSite> sites) {
        Map<String, Integer> counted = new TreeMap<>();
        for (CallSite site : sites) {
            counted.merge(site.call().hook() + " in " + site.within(), 1, Integer::sum);
        }
        return counted;
    }
}
