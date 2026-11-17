package io.github.luasgraphicss.minemod.adapters.transformers.release;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PinnedReleases {
    private final Map<String, ReleasePlan> plansByClient = new LinkedHashMap<>();

    public PinnedReleases(Collection<ReleasePlan> plans) {
        for (ReleasePlan plan : plans) {
            ReleasePlan earlier = plansByClient.put(plan.clientSha1(), plan);
            if (earlier != null) {
                throw new IllegalArgumentException("releases " + earlier.release() + " and " + plan.release()
                        + " are pinned to the same client " + plan.clientSha1());
            }
        }
    }

    public Optional<ReleasePlan> planFor(String clientSha1) {
        return Optional.ofNullable(plansByClient.get(clientSha1));
    }

    public ReleasePlan requirePlanFor(String clientSha1) {
        ReleasePlan plan = plansByClient.get(clientSha1);
        if (plan == null) {
            throw new UnsupportedClientException(clientSha1, releases());
        }
        return plan;
    }

    public List<String> releases() {
        List<String> releases = new ArrayList<>(plansByClient.size());
        for (ReleasePlan plan : plansByClient.values()) {
            releases.add(plan.release());
        }
        return releases;
    }
}
