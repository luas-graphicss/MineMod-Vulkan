package io.github.luasgraphicss.minemod.verification.scenario.matrix;

import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClassRenamings {
    public record Renaming(String runtimeClass, Map<String, MethodTarget> methods) {}

    private ClassRenamings() {}

    public static Map<String, Renaming> between(ReleasePlan official, ReleasePlan runtime, List<String> failures) {
        Map<String, MethodTarget> runtimeMethods = new HashMap<>();
        for (CallSiteRedirect redirect : runtime.redirects()) {
            runtimeMethods.put(redirect.point().id(), redirect.point().target());
        }
        Map<String, Renaming> renamings = new LinkedHashMap<>();
        for (CallSiteRedirect redirect : official.redirects()) {
            MethodTarget from = redirect.point().target();
            MethodTarget to = runtimeMethods.get(redirect.point().id());
            if (to == null) {
                failures.add("the plan of the loader has no point " + redirect.point().id());
                continue;
            }
            Renaming renaming = renamings.computeIfAbsent(from.owner(), owner -> new Renaming(to.owner(), new HashMap<>()));
            if (!renaming.runtimeClass().equals(to.owner())) {
                failures.add(from.owner() + " is named both " + renaming.runtimeClass() + " and " + to.owner());
            }
            renaming.methods().put(from.name() + from.descriptor(), to);
        }
        return renamings;
    }
}
