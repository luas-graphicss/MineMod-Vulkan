package io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramStage;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.TargetBinding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class PassPlan {
    public static final String BUILT_IN_PROGRAM = "built-in";

    private final ProgramStage stage;
    private final String program;
    private final Map<String, TargetBinding> samplers;
    private final Set<String> gameSamplers;
    private final List<TargetBinding> outputs;
    private final int computeShaders;

    PassPlan(
            ProgramStage stage,
            String program,
            Map<String, TargetBinding> samplers,
            Set<String> gameSamplers,
            List<TargetBinding> outputs,
            int computeShaders) {
        this.stage = stage;
        this.program = program;
        this.samplers = Collections.unmodifiableMap(new TreeMap<>(samplers));
        this.gameSamplers = Collections.unmodifiableSet(new TreeSet<>(gameSamplers));
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
        this.computeShaders = computeShaders;
    }

    public ProgramStage stage() {
        return stage;
    }

    public String program() {
        return program;
    }

    public Map<String, TargetBinding> samplers() {
        return samplers;
    }

    public Set<String> gameSamplers() {
        return gameSamplers;
    }

    public List<TargetBinding> outputs() {
        return outputs;
    }

    public int computeShaders() {
        return computeShaders;
    }

    @Override
    public String toString() {
        return program + " reads " + samplers + " writes " + outputs;
    }
}
