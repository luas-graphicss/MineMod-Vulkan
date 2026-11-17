package io.github.luasgraphicss.minemod.adapters.transformers.release;

import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPlan;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class ReleasePlan {
    private static final Pattern SHA1 = Pattern.compile("[0-9a-f]{40}");

    private final String release;
    private final String clientSha1;
    private final List<CallSiteRedirect> redirects;
    private final TransformPlan transformPlan;

    public ReleasePlan(String release, String clientSha1, List<CallSiteRedirect> redirects) {
        if (release.trim().isEmpty()) {
            throw new IllegalArgumentException("a plan names the release it was pinned for");
        }
        if (!SHA1.matcher(clientSha1).matches()) {
            throw new IllegalArgumentException("the plan of " + release + " has no SHA-1 of its client: " + clientSha1);
        }
        if (redirects.isEmpty()) {
            throw new IllegalArgumentException("the plan of " + release + " redirects no call");
        }
        this.release = release;
        this.clientSha1 = clientSha1;
        this.redirects = Collections.unmodifiableList(new ArrayList<>(redirects));
        this.transformPlan = new TransformPlan(pointsOf(redirects));
    }

    public String release() {
        return release;
    }

    public String clientSha1() {
        return clientSha1;
    }

    public List<CallSiteRedirect> redirects() {
        return redirects;
    }

    public TransformPlan transformPlan() {
        return transformPlan;
    }

    public boolean isPinnedFor(String sha1OfClient) {
        return clientSha1.equals(sha1OfClient);
    }

    private static List<TransformPoint> pointsOf(List<CallSiteRedirect> redirects) {
        List<TransformPoint> points = new ArrayList<>(redirects.size());
        for (CallSiteRedirect redirect : redirects) {
            points.add(redirect.point());
        }
        return points;
    }
}
