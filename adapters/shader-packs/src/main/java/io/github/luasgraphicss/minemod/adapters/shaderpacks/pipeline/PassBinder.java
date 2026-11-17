package io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.directives.ProgramDirectives;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramStage;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ShaderPack;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.BufferSide;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.PackTarget;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.SamplerNames;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.targets.TargetBinding;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.uniforms.StandardUniforms;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

final class PassBinder {
    private static final int FIRST_COLOR_TARGET_OF_GEOMETRY = 4;
    private static final String SAMPLER_TYPE = "sampler";
    private static final String FLIP_PROPERTY = "flip.";
    private static final String SAMPLED_BY_BUILT_IN_FINAL = "colortex0";

    private final ShaderPack pack;
    private final StandardUniforms uniforms;
    private final PipelineCapabilities capabilities;
    private final Set<String> problems;
    private final BufferFlips flips = new BufferFlips();

    PassBinder(ShaderPack pack, StandardUniforms uniforms, PipelineCapabilities capabilities, Set<String> problems) {
        this.pack = pack;
        this.uniforms = uniforms;
        this.capabilities = capabilities;
        this.problems = problems;
    }

    PassPlan fullscreen(String program, ProgramStage stage) {
        ProgramDirectives directives = ProgramDirectives.of(pack.programs().get(program));
        Map<String, TargetBinding> samplers = new TreeMap<>();
        Set<String> gameSamplers = new TreeSet<>();
        bindUniforms(program, directives, 0, samplers, gameSamplers);
        List<TargetBinding> outputs = new ArrayList<>();
        if (stage != ProgramStage.FINAL) {
            for (PackTarget target : targetsWrittenBy(program, stage, directives)) {
                outputs.add(new TargetBinding(target, flips.readSide(target).other()));
            }
            flipAfter(program, outputs);
        }
        return new PassPlan(stage, program, samplers, gameSamplers, outputs, computeShadersOf(program));
    }

    PassPlan inPlace(String program, ProgramStage stage) {
        ProgramDirectives directives = ProgramDirectives.of(pack.programs().get(program));
        Map<String, TargetBinding> samplers = new TreeMap<>();
        Set<String> gameSamplers = new TreeSet<>();
        int firstColorTarget = stage == ProgramStage.SHADOW ? 0 : FIRST_COLOR_TARGET_OF_GEOMETRY;
        bindUniforms(program, directives, firstColorTarget, samplers, gameSamplers);
        List<TargetBinding> outputs = new ArrayList<>();
        for (PackTarget target : targetsWrittenBy(program, stage, directives)) {
            outputs.add(new TargetBinding(target, flips.readSide(target)));
        }
        return new PassPlan(stage, program, samplers, gameSamplers, outputs, computeShadersOf(program));
    }

    PassPlan builtIn(ProgramStage stage) {
        Map<String, TargetBinding> samplers = new TreeMap<>();
        List<TargetBinding> outputs = new ArrayList<>();
        PackTarget sceneColor = PackTarget.color(0);
        if (stage == ProgramStage.FINAL) {
            samplers.put(SAMPLED_BY_BUILT_IN_FINAL, new TargetBinding(sceneColor, flips.readSide(sceneColor)));
        } else if (stage != ProgramStage.SHADOW) {
            outputs.add(new TargetBinding(sceneColor, flips.readSide(sceneColor)));
        }
        return new PassPlan(stage, PassPlan.BUILT_IN_PROGRAM, samplers, Collections.<String>emptySet(), outputs, 0);
    }

    List<PackTarget> targetsLeftFlipped() {
        return flips.leftFlipped();
    }

    private void bindUniforms(
            String program,
            ProgramDirectives directives,
            int firstColorTarget,
            Map<String, TargetBinding> samplers,
            Set<String> gameSamplers) {
        boolean declaresWaterShadow = directives.uniformTypes().containsKey(SamplerNames.WATER_SHADOW);
        for (Map.Entry<String, String> uniform : directives.uniformTypes().entrySet()) {
            String name = uniform.getKey();
            if (!uniform.getValue().contains(SAMPLER_TYPE)) {
                checkUniform(program, name, uniform.getValue());
            } else if (SamplerNames.boundByTheGame(name)) {
                gameSamplers.add(name);
            } else {
                Optional<PackTarget> target = SamplerNames.targetOf(name, declaresWaterShadow);
                if (!target.isPresent()) {
                    problems.add(program + " samples " + name + ", which neither the pipeline nor the game provides");
                } else if (target.get().kind() == PackTarget.Kind.COLOR && target.get().index() < firstColorTarget) {
                    problems.add(program + " draws geometry and cannot sample " + target.get()
                            + ", only colortex" + firstColorTarget + " and above");
                } else {
                    samplers.put(name, new TargetBinding(target.get(), flips.readSide(target.get())));
                }
            }
        }
    }

    private void checkUniform(String program, String name, String declaredType) {
        Optional<String> standardType = uniforms.typeOf(name);
        if (!standardType.isPresent()) {
            problems.add(program + " declares uniform " + name + ", which the pipeline does not provide");
        } else if (!standardType.get().equals(declaredType)) {
            problems.add(program + " declares uniform " + name + " as " + declaredType + ", the pipeline provides "
                    + standardType.get());
        }
    }

    private List<PackTarget> targetsWrittenBy(String program, ProgramStage stage, ProgramDirectives directives) {
        boolean writesShadowColor = stage == ProgramStage.SHADOW || stage == ProgramStage.SHADOW_COMPOSITE;
        int available =
                writesShadowColor ? PackTarget.Kind.SHADOW_COLOR.count() : capabilities.colorTargets();
        List<PackTarget> targets = new ArrayList<>();
        if (directives.drawBuffers().size() > capabilities.drawBuffers()) {
            problems.add(program + " writes " + directives.drawBuffers().size() + " draw buffers, the profile allows "
                    + capabilities.drawBuffers());
            return targets;
        }
        for (int index : directives.drawBuffers()) {
            if (index >= available || index >= PackTarget.Kind.COLOR.count()) {
                problems.add(program + " writes draw buffer " + index + ", the profile has " + available);
            } else {
                targets.add(writesShadowColor ? PackTarget.shadowColor(index) : PackTarget.color(index));
            }
        }
        return targets;
    }

    private void flipAfter(String program, List<TargetBinding> outputs) {
        Set<PackTarget> flipping = new HashSet<>();
        for (TargetBinding output : outputs) {
            flipping.add(output.target());
        }
        String prefix = FLIP_PROPERTY + program + ".";
        for (Map.Entry<String, String> property : pack.properties().entrySet()) {
            if (!property.getKey().startsWith(prefix)) {
                continue;
            }
            String buffer = property.getKey().substring(prefix.length());
            Optional<PackTarget> target = SamplerNames.targetOf(buffer, false);
            if (!target.isPresent() || !target.get().flips()) {
                problems.add(property.getKey() + " names " + buffer + ", which is not a buffer that flips");
            } else if (Boolean.parseBoolean(property.getValue().trim())) {
                flipping.add(target.get());
            } else {
                flipping.remove(target.get());
            }
        }
        for (PackTarget target : flipping) {
            flips.flip(target);
        }
    }

    private int computeShadersOf(String program) {
        return pack.programs().get(program).computeShaders();
    }
}
