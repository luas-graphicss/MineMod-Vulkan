package io.github.luasgraphicss.minemod.verification.scenario.transformers;

import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformViolation;
import io.github.luasgraphicss.minemod.adapters.transformers.preflight.PlanPreflight;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.rewrite.CallSiteRewriter;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.PresentationCalls;
import io.github.luasgraphicss.minemod.verification.hooks.model.HookPoint;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.plan.ReleasePlans;
import io.github.luasgraphicss.minemod.verification.hooks.scan.CallSite;
import io.github.luasgraphicss.minemod.verification.hooks.scan.CallSiteScanner;
import io.github.luasgraphicss.minemod.verification.hooks.scan.ClientArchive;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ClientVerification {
    static final String PROBE_HOOK_OWNER = "io/github/luasgraphicss/minemod/verification/probe/PresentationProbe";

    private ClientVerification() {}

    public static List<String> findFailures(ReleaseHooks hooks, ClientArchive client) throws IOException {
        List<String> failures = new ArrayList<>();
        List<LibraryCall> calls = PresentationCalls.of(hooks.windowing());
        ReleasePlan plan = ReleasePlans.planOf(hooks);
        CallSiteRewriter rewriter = new CallSiteRewriter(plan.redirects(), PROBE_HOOK_OWNER);

        for (TransformViolation violation :
                PlanPreflight.findViolations(plan.transformPlan(), rewriter, client, List.of())) {
            failures.add(violation.description());
        }

        Map<String, Integer> pinned = pinnedSitesOf(hooks);
        Map<String, Integer> found = sitesOf(new CallSiteScanner(calls).scan(client));
        if (!found.equals(pinned)) {
            failures.add("the client calls " + found + ", the manifest pins " + pinned);
            return failures;
        }

        List<LibraryCall> probeCalls = calls.stream()
                .map(call -> new LibraryCall(call.hook(), PROBE_HOOK_OWNER, call.name(), call.descriptor()))
                .toList();
        Map<String, Integer> redirected = new TreeMap<>();
        for (String className : rewriter.rewrittenClasses()) {
            byte[] rewritten = rewriter.rewrite(className, client.classFile(className).orElseThrow()).classFile();
            List<CallSite> left = new CallSiteScanner(calls).scan(rewritten);
            if (!left.isEmpty()) {
                failures.add(className + " still calls the library after the redirect: " + sitesOf(left));
            }
            sitesOf(new CallSiteScanner(probeCalls).scan(rewritten)).forEach((site, count) -> redirected.merge(site, count, Integer::sum));
        }
        if (!redirected.equals(pinned)) {
            failures.add("the redirect reached " + redirected + ", the manifest pins " + pinned);
        }
        return failures;
    }

    private static Map<String, Integer> pinnedSitesOf(ReleaseHooks hooks) {
        Map<String, Integer> sites = new TreeMap<>();
        for (HookPoint point : hooks.points()) {
            sites.put(point.hook() + " in " + point.within(), point.matches());
        }
        return sites;
    }

    private static Map<String, Integer> sitesOf(List<CallSite> sites) {
        Map<String, Integer> counted = new TreeMap<>();
        for (CallSite site : sites) {
            counted.merge(site.call().hook() + " in " + site.within(), 1, Integer::sum);
        }
        return counted;
    }
}
