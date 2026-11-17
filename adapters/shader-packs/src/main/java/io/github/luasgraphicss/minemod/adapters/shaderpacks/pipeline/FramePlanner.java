package io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.directives.PackSettings;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.directives.ProgramDirectives;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.FullscreenProgram;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.GeometryProgram;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramSources;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramStage;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ShaderPack;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.PackTarget;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.SamplerNames;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.uniforms.StandardUniforms;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public final class FramePlanner {
    private static final String SHADOW_PROGRAM = "shadow";
    private static final String FINAL_PROGRAM = "final";
    private static final String[] CUSTOM_UNIFORM_PREFIXES = {"uniform.", "variable."};

    private final StandardUniforms uniforms;
    private final PipelineCapabilities capabilities;

    public FramePlanner(StandardUniforms uniforms, PipelineCapabilities capabilities) {
        this.uniforms = Objects.requireNonNull(uniforms, "uniforms");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
    }

    public PlanningOutcome plan(ShaderPack pack) {
        Set<String> problems = new LinkedHashSet<>();
        refuseFeaturesOutsideTheIntegration(pack, problems);
        SortedSet<FullscreenProgram> fullscreenPrograms = fullscreenProgramsOf(pack, problems);
        PassBinder binder = new PassBinder(pack, uniforms, capabilities, problems);
        List<PassPlan> stagedPasses = new ArrayList<>();
        Map<GeometryProgram, PassPlan> geometryPasses = new EnumMap<>(GeometryProgram.class);

        if (rendersShadows(pack)) {
            stagedPasses.add(
                    pack.programs().containsKey(SHADOW_PROGRAM)
                            ? binder.inPlace(SHADOW_PROGRAM, ProgramStage.SHADOW)
                            : binder.builtIn(ProgramStage.SHADOW));
        }
        bindFullscreen(fullscreenPrograms, ProgramStage.SHADOW_COMPOSITE, binder, stagedPasses);
        bindFullscreen(fullscreenPrograms, ProgramStage.PREPARE, binder, stagedPasses);
        bindGeometry(pack, ProgramStage.OPAQUE_GEOMETRY, binder, geometryPasses);
        bindFullscreen(fullscreenPrograms, ProgramStage.DEFERRED, binder, stagedPasses);
        bindGeometry(pack, ProgramStage.TRANSLUCENT_GEOMETRY, binder, geometryPasses);
        bindFullscreen(fullscreenPrograms, ProgramStage.COMPOSITE, binder, stagedPasses);
        stagedPasses.add(
                pack.programs().containsKey(FINAL_PROGRAM)
                        ? binder.fullscreen(FINAL_PROGRAM, ProgramStage.FINAL)
                        : binder.builtIn(ProgramStage.FINAL));

        TargetAllocation allocation = new TargetAllocation();
        allocation.include(stagedPasses);
        allocation.include(geometryPasses.values());
        FramePlan plan = new FramePlan(
                pack.name(),
                stagedPasses,
                geometryPasses,
                allocation.allocate(PackSettings.of(pack), problems),
                binder.targetsLeftFlipped());
        return PlanningOutcome.of(plan, problems);
    }

    private void refuseFeaturesOutsideTheIntegration(ShaderPack pack, Set<String> problems) {
        for (String property : pack.properties().keySet()) {
            for (String prefix : CUSTOM_UNIFORM_PREFIXES) {
                if (property.startsWith(prefix)) {
                    problems.add(property + " is a custom uniform, whose expression this integration does not evaluate");
                }
            }
        }
        for (Map.Entry<String, ProgramSources> program : pack.programs().entrySet()) {
            ProgramSources sources = program.getValue();
            if (sources.geometryShader()) {
                problems.add(program.getKey() + " has a geometry shader, a stage this integration does not run");
            }
            if (sources.computeShaders() > 0 && !capabilities.computeShaders()) {
                problems.add(program.getKey() + " runs " + sources.computeShaders()
                        + " compute shader(s), which this graphics profile does not support");
            }
        }
    }

    private static SortedSet<FullscreenProgram> fullscreenProgramsOf(ShaderPack pack, Set<String> problems) {
        SortedSet<FullscreenProgram> programs = new TreeSet<>();
        for (String name : pack.programs().keySet()) {
            Optional<FullscreenProgram> fullscreen = FullscreenProgram.named(name);
            if (fullscreen.isPresent()) {
                programs.add(fullscreen.get());
            } else if (!name.equals(SHADOW_PROGRAM) && !GeometryProgram.named(name).isPresent()) {
                problems.add(name + " is not a program this integration runs");
            }
        }
        return programs;
    }

    private static boolean rendersShadows(ShaderPack pack) {
        if (pack.programs().containsKey(SHADOW_PROGRAM)) {
            return true;
        }
        for (ProgramSources sources : pack.programs().values()) {
            for (String uniform : ProgramDirectives.of(sources).uniformTypes().keySet()) {
                if (SamplerNames.targetOf(uniform, false).filter(FramePlanner::isShadowTarget).isPresent()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isShadowTarget(PackTarget target) {
        return target.kind() == PackTarget.Kind.SHADOW_DEPTH || target.kind() == PackTarget.Kind.SHADOW_COLOR;
    }

    private static void bindFullscreen(
            SortedSet<FullscreenProgram> programs, ProgramStage stage, PassBinder binder, List<PassPlan> passes) {
        for (FullscreenProgram program : programs) {
            if (program.stage() == stage) {
                passes.add(binder.fullscreen(program.name(), stage));
            }
        }
    }

    private static void bindGeometry(
            ShaderPack pack, ProgramStage stage, PassBinder binder, Map<GeometryProgram, PassPlan> passes) {
        for (GeometryProgram geometry : GeometryProgram.values()) {
            if (geometry.stage() != stage) {
                continue;
            }
            Optional<String> program = geometry.resolveAmong(pack.programs().keySet());
            passes.put(geometry, program.isPresent() ? binder.inPlace(program.get(), stage) : binder.builtIn(stage));
        }
    }
}
