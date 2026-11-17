package io.github.luasgraphicss.minemod.adapters.transformers.rewrite;

import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class MatchCounter {
    private final Map<CallSiteRedirect, Integer> matches = new IdentityHashMap<>();

    MatchCounter(List<CallSiteRedirect> redirects) {
        for (CallSiteRedirect redirect : redirects) {
            matches.put(redirect, 0);
        }
    }

    void count(CallSiteRedirect redirect) {
        matches.put(redirect, matches.get(redirect) + 1);
    }

    int of(CallSiteRedirect redirect) {
        return matches.get(redirect);
    }
}
