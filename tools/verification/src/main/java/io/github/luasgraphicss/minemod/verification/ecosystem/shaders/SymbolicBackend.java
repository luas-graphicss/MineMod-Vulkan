package io.github.luasgraphicss.minemod.verification.ecosystem.shaders;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.execution.PipelineBackend;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramStage;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline.PassPlan;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.BufferSide;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.DepthSnapshot;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.PackTarget;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.RenderTargets;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.TargetBinding;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SymbolicBackend implements PipelineBackend {
    private static final String EMPTY = "empty";
    private static final PackTarget LIVE_DEPTH = PackTarget.depth(0);

    private final Map<String, String> contents = new HashMap<>();
    private String sceneDepth = EMPTY;
    private String presented = EMPTY;
    private int frame;
    private int allocations;
    private int releases;

    public void startFrame(int startedFrame) {
        frame = startedFrame;
        sceneDepth = EMPTY;
    }

    public String presented() {
        return presented;
    }

    public int allocations() {
        return allocations;
    }

    public int releases() {
        return releases;
    }

    public boolean holdsTargets() {
        return allocations > releases;
    }

    @Override
    public void allocate(RenderTargets targets) {
        contents.clear();
        allocations++;
    }

    @Override
    public void clear(List<Integer> colorTargets) {
        for (int target : colorTargets) {
            for (BufferSide side : BufferSide.values()) {
                contents.put(new TargetBinding(PackTarget.color(target), side).toString(), "cleared" + stamp());
            }
        }
    }

    @Override
    public void renderShadows(PassPlan pass) {
        contents.put(PackTarget.shadowDepth(0).toString(), "shadows" + stamp());
        contents.put(PackTarget.shadowDepth(1).toString(), "shadows" + stamp());
        pass.outputs().forEach(output -> contents.put(output.toString(), pass.program() + stamp()));
    }

    @Override
    public void snapshotDepth(DepthSnapshot snapshot) {
        contents.put(snapshot.target().toString(), sceneDepth);
    }

    @Override
    public void bindGeometry(PassPlan pass) {
        boolean translucent = pass.stage() == ProgramStage.TRANSLUCENT_GEOMETRY;
        sceneDepth = (translucent ? "depth(opaque+translucents)" : "depth(opaque)") + stamp();
        for (TargetBinding output : pass.outputs()) {
            String drawn = translucent
                    ? "translucents" + stamp() + " over " + contents.getOrDefault(output.toString(), EMPTY)
                    : "opaque" + stamp();
            contents.put(output.toString(), drawn);
        }
    }

    @Override
    public void drawFullscreen(PassPlan pass) {
        List<String> sampled = pass.samplers().values().stream().map(this::contentOf).toList();
        String drawn = pass.program() + "(" + String.join(", ", sampled) + ")";
        if (pass.stage() == ProgramStage.FINAL) {
            presented = drawn;
        }
        pass.outputs().forEach(output -> contents.put(output.toString(), drawn));
    }

    @Override
    public void swapSides(List<PackTarget> targets) {
        for (PackTarget target : targets) {
            String main = new TargetBinding(target, BufferSide.MAIN).toString();
            String alt = new TargetBinding(target, BufferSide.ALT).toString();
            String held = contents.getOrDefault(main, EMPTY);
            contents.put(main, contents.getOrDefault(alt, EMPTY));
            contents.put(alt, held);
        }
    }

    @Override
    public void release() {
        contents.clear();
        releases++;
    }

    private String contentOf(TargetBinding binding) {
        return binding.target().equals(LIVE_DEPTH) ? sceneDepth : contents.getOrDefault(binding.toString(), EMPTY);
    }

    private String stamp() {
        return "@f" + frame;
    }
}
