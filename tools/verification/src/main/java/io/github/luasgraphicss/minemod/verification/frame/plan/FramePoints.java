package io.github.luasgraphicss.minemod.verification.frame.plan;

import io.github.luasgraphicss.minemod.verification.frame.catalog.StandIns;
import io.github.luasgraphicss.minemod.verification.frame.scan.LibraryCallCount;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.PresentationCalls;
import io.github.luasgraphicss.minemod.verification.hooks.discovery.HookDiscovery;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks.Windowing;
import io.github.luasgraphicss.minemod.verification.hooks.scan.CallSite;
import io.github.luasgraphicss.minemod.verification.hooks.scan.CallSiteScanner;
import io.github.luasgraphicss.minemod.verification.hooks.scan.ClientArchive;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class FramePoints {
    private static final String OPENGL_OWNERS = "org/lwjgl/opengl/";

    private FramePoints() {}

    public static ReleaseHooks pinnedOn(ReleaseTarget release, ClientArchive client, StandIns standIns)
            throws IOException {
        Windowing windowing = Windowing.ofLwjglGeneration(release.lwjgl().generation());
        Set<String> answeredOwners = standIns.owners();
        Set<String> presented = presentedBy(windowing);
        List<String> unanswered = LibraryCallCount.byCallee(client, answeredOwners::contains).keySet().stream()
                .filter(callee -> standIns.answering(callee).isEmpty() && !presented.contains(callee))
                .toList();
        if (!unanswered.isEmpty()) {
            throw new IOException("release " + release.id() + " calls what no stand-in answers: " + unanswered);
        }
        List<CallSite> sites = new CallSiteScanner(standIns.calls()).scan(client);
        return new ReleaseHooks(
                release.id(),
                release.client().sha1(),
                windowing,
                HookDiscovery.pointsOf(standIns.calls(), sites, site -> null));
    }

    public static Map<String, Integer> leftWithTheLibrary(ClientArchive client, StandIns standIns, Windowing windowing)
            throws IOException {
        Set<String> answeredOwners = standIns.owners();
        Set<String> presented = presentedBy(windowing);
        Map<String, Integer> callsByOwner = new TreeMap<>();
        LibraryCallCount.byCallee(client, owner -> owner.startsWith(OPENGL_OWNERS) && !answeredOwners.contains(owner))
                .forEach((callee, count) -> {
                    if (!presented.contains(callee)) {
                        callsByOwner.merge(ownerOf(callee), count, Integer::sum);
                    }
                });
        return callsByOwner;
    }

    private static Set<String> presentedBy(Windowing windowing) {
        return PresentationCalls.of(windowing).stream().map(LibraryCall::signature).collect(Collectors.toSet());
    }

    private static String ownerOf(String callee) {
        String owner = callee.substring(0, callee.indexOf('.'));
        return owner.substring(owner.lastIndexOf('/') + 1);
    }
}
