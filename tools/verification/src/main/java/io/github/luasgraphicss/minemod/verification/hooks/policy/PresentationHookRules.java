package io.github.luasgraphicss.minemod.verification.hooks.policy;

import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.PresentationCalls;
import io.github.luasgraphicss.minemod.verification.hooks.model.HookPoint;
import io.github.luasgraphicss.minemod.verification.hooks.model.PresentationHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks.Windowing;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class PresentationHookRules {
    private PresentationHookRules() {}

    public static List<String> findViolations(PresentationHooks hooks, TargetManifest targets) {
        List<String> violations = new ArrayList<>();
        if (hooks.schemaVersion() != PresentationHooks.SCHEMA_VERSION) {
            violations.add("schemaVersion is " + hooks.schemaVersion() + ", expected " + PresentationHooks.SCHEMA_VERSION);
        }
        List<String> pinnedIds = targets.releases().stream().map(ReleaseTarget::id).toList();
        List<String> listedIds = hooks.releases().stream().map(ReleaseHooks::release).toList();
        if (!listedIds.equals(pinnedIds)) {
            violations.add("hooks are pinned for " + listedIds + ", expected exactly " + pinnedIds);
            return violations;
        }
        for (int index = 0; index < pinnedIds.size(); index++) {
            checkRelease(targets.releases().get(index), hooks.releases().get(index), violations);
        }
        return violations;
    }

    private static void checkRelease(ReleaseTarget target, ReleaseHooks hooks, List<String> violations) {
        String subject = "release " + hooks.release() + ": ";
        if (!target.client().sha1().equals(hooks.clientSha1())) {
            violations.add(subject + "hooks were pinned on client " + hooks.clientSha1()
                    + ", the target manifest pins " + target.client().sha1());
        }
        Windowing expected = Windowing.ofLwjglGeneration(target.lwjgl().generation());
        if (hooks.windowing() != expected) {
            violations.add(subject + "windowing is " + hooks.windowing() + ", LWJGL " + target.lwjgl().generation()
                    + " expects " + expected);
            return;
        }
        List<LibraryCall> calls = PresentationCalls.of(expected);
        Set<String> seenIds = new HashSet<>();
        for (HookPoint point : hooks.points()) {
            checkPoint(point, calls, seenIds, subject, violations);
        }
        for (LibraryCall call : calls) {
            if (hooks.points().stream().noneMatch(point -> point.hook().equals(call.hook()))) {
                violations.add(subject + "no call of " + call.signature() + " is pinned for " + call.hook());
            }
        }
    }

    private static void checkPoint(
            HookPoint point, List<LibraryCall> calls, Set<String> seenIds, String subject, List<String> violations) {
        String pointSubject = subject + "point " + point.id() + " ";
        if (!seenIds.add(point.id())) {
            violations.add(pointSubject + "is listed twice");
        }
        Optional<LibraryCall> call = calls.stream().filter(candidate -> candidate.hook().equals(point.hook())).findFirst();
        if (call.isEmpty()) {
            violations.add(pointSubject + "names the unknown hook " + point.hook());
        } else if (!call.get().signature().equals(point.callee())) {
            violations.add(pointSubject + "redirects " + point.callee() + ", " + point.hook() + " is "
                    + call.get().signature());
        }
        if (point.matches() < 1) {
            violations.add(pointSubject + "expects " + point.matches() + " matches");
        }
        try {
            MethodTarget.parse(point.within());
        } catch (IllegalArgumentException malformed) {
            violations.add(pointSubject + "is not within an exact method: " + malformed.getMessage());
        }
    }
}
