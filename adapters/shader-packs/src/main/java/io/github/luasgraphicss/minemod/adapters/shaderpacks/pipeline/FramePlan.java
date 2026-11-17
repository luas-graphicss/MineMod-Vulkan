package io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.GeometryProgram;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramStage;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.PackTarget;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.RenderTargets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class FramePlan {
    private final String packName;
    private final List<PassPlan> stagedPasses;
    private final Map<GeometryProgram, PassPlan> geometryPasses;
    private final RenderTargets targets;
    private final List<PackTarget> swappedAtFrameEnd;

    FramePlan(
            String packName,
            List<PassPlan> stagedPasses,
            Map<GeometryProgram, PassPlan> geometryPasses,
            RenderTargets targets,
            List<PackTarget> swappedAtFrameEnd) {
        this.packName = packName;
        this.stagedPasses = Collections.unmodifiableList(new ArrayList<>(stagedPasses));
        this.geometryPasses = Collections.unmodifiableMap(new EnumMap<>(geometryPasses));
        this.targets = targets;
        this.swappedAtFrameEnd = Collections.unmodifiableList(new ArrayList<>(swappedAtFrameEnd));
    }

    public String packName() {
        return packName;
    }

    public RenderTargets targets() {
        return targets;
    }

    public List<PackTarget> swappedAtFrameEnd() {
        return swappedAtFrameEnd;
    }

    public List<PassPlan> passesOf(ProgramStage stage) {
        List<PassPlan> staged = new ArrayList<>();
        for (PassPlan pass : stagedPasses) {
            if (pass.stage() == stage) {
                staged.add(pass);
            }
        }
        return staged;
    }

    public PassPlan geometryPass(GeometryProgram geometry) {
        return geometryPasses.get(geometry);
    }
}
