package io.github.luasgraphicss.minemod.adapters.shaderpacks.targets;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class RenderTargets {
    private final Map<Integer, ColorTargetSpec> colorTargets;
    private final Set<DepthSnapshot> depthSnapshots = EnumSet.noneOf(DepthSnapshot.class);
    private final Set<Integer> shadowColorTargets;
    private final int shadowMapResolution;

    public RenderTargets(
            Map<Integer, ColorTargetSpec> colorTargets,
            Collection<DepthSnapshot> depthSnapshots,
            Collection<Integer> shadowColorTargets,
            int shadowMapResolution) {
        if (shadowMapResolution < 0) {
            throw new IllegalArgumentException("a shadow map has no negative resolution: " + shadowMapResolution);
        }
        this.colorTargets = Collections.unmodifiableMap(new TreeMap<>(colorTargets));
        this.depthSnapshots.addAll(depthSnapshots);
        this.shadowColorTargets = Collections.unmodifiableSet(new TreeSet<>(shadowColorTargets));
        this.shadowMapResolution = shadowMapResolution;
    }

    public Map<Integer, ColorTargetSpec> colorTargets() {
        return colorTargets;
    }

    public Set<DepthSnapshot> depthSnapshots() {
        return Collections.unmodifiableSet(depthSnapshots);
    }

    public Set<Integer> shadowColorTargets() {
        return shadowColorTargets;
    }

    public boolean rendersShadows() {
        return shadowMapResolution > 0;
    }

    public int shadowMapResolution() {
        return shadowMapResolution;
    }
}
