package io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.directives.PackSettings;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramStage;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.ColorTargetSpec;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.DepthSnapshot;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.PackTarget;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.RenderTargets;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.TargetBinding;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.TextureFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

final class TargetAllocation {
    private static final int MAX_SHADOW_MAP_RESOLUTION = 16384;

    private final Set<Integer> colorTargets = new TreeSet<>();
    private final Set<Integer> shadowColorTargets = new TreeSet<>();
    private final Set<DepthSnapshot> depthSnapshots = EnumSet.noneOf(DepthSnapshot.class);
    private boolean rendersShadows;

    TargetAllocation() {
        colorTargets.add(0);
    }

    void include(Collection<PassPlan> passes) {
        for (PassPlan pass : passes) {
            rendersShadows |= pass.stage() == ProgramStage.SHADOW;
            List<TargetBinding> bindings = new ArrayList<>(pass.samplers().values());
            bindings.addAll(pass.outputs());
            for (TargetBinding binding : bindings) {
                include(binding.target());
            }
        }
    }

    RenderTargets allocate(PackSettings settings, Set<String> problems) {
        Map<Integer, ColorTargetSpec> specs = new TreeMap<>();
        for (int target : colorTargets) {
            String formatName = settings.formatNames().get(target);
            Optional<TextureFormat> format =
                    formatName == null ? Optional.of(TextureFormat.RGBA) : TextureFormat.named(formatName);
            if (format.isPresent()) {
                specs.put(target, new ColorTargetSpec(format.get(), settings.clears(target)));
            } else {
                problems.add("colortex" + target + " asks for the format " + formatName + ", which is not known");
            }
        }
        int resolution = rendersShadows ? shadowMapResolutionOf(settings, problems) : 0;
        return new RenderTargets(specs, depthSnapshots, shadowColorTargets, resolution);
    }

    private void include(PackTarget target) {
        switch (target.kind()) {
            case COLOR:
                colorTargets.add(target.index());
                break;
            case SHADOW_COLOR:
                shadowColorTargets.add(target.index());
                break;
            case DEPTH:
                for (DepthSnapshot snapshot : DepthSnapshot.values()) {
                    if (snapshot.target().equals(target)) {
                        depthSnapshots.add(snapshot);
                    }
                }
                break;
            default:
                break;
        }
    }

    private static int shadowMapResolutionOf(PackSettings settings, Set<String> problems) {
        String declared = settings.shadowMapResolution();
        if (declared.matches("[0-9]{1,5}")) {
            int resolution = Integer.parseInt(declared);
            if (resolution >= 1 && resolution <= MAX_SHADOW_MAP_RESOLUTION) {
                return resolution;
            }
        }
        problems.add("shadowMapResolution is " + declared + ", expected 1 to " + MAX_SHADOW_MAP_RESOLUTION);
        return 1;
    }
}
