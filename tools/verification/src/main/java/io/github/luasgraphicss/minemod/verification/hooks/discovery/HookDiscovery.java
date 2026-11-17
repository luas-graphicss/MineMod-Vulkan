package io.github.luasgraphicss.minemod.verification.hooks.discovery;

import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;
import io.github.luasgraphicss.minemod.verification.hooks.model.HookPoint;
import io.github.luasgraphicss.minemod.verification.hooks.scan.CallSite;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public final class HookDiscovery {
    private static final String INDEX_SEPARATOR = "#";

    private HookDiscovery() {}

    public static List<HookPoint> pointsOf(
            List<LibraryCall> calls, List<CallSite> sites, Function<CallSite, String> namedMethodOf) {
        List<HookPoint> points = new ArrayList<>();
        for (LibraryCall call : calls) {
            Map<String, List<CallSite>> sitesByMethod = new TreeMap<>();
            for (CallSite site : sites) {
                if (site.call().equals(call)) {
                    sitesByMethod.computeIfAbsent(site.within(), within -> new ArrayList<>()).add(site);
                }
            }
            int index = 1;
            for (List<CallSite> withinMethod : sitesByMethod.values()) {
                CallSite first = withinMethod.getFirst();
                String id = sitesByMethod.size() == 1 ? call.hook() : call.hook() + INDEX_SEPARATOR + index;
                points.add(new HookPoint(
                        id,
                        call.hook(),
                        call.signature(),
                        first.within(),
                        namedMethodOf.apply(first),
                        withinMethod.size()));
                index++;
            }
        }
        return points;
    }
}
