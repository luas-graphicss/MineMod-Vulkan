package io.github.luasgraphicss.minemod.verification.differential;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.verification.differential.ScriptStep.Action;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.SimulatedProvider;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.TrackedState;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class ScriptGenerator {
    static final List<ObjectKind> KINDS = List.of(
            ObjectKind.TEXTURE,
            ObjectKind.BUFFER,
            ObjectKind.VERTEX_ARRAY,
            ObjectKind.SAMPLER,
            ObjectKind.FRAMEBUFFER,
            ObjectKind.RENDERBUFFER,
            ObjectKind.SHADER_OR_PROGRAM);

    private static final int CHOICES = 14;
    static final int UNGENERATED_NAMES = 8;
    private static final List<ObjectKind> CREATED_BY_FIRST_USE =
            List.of(ObjectKind.TEXTURE, ObjectKind.BUFFER, ObjectKind.RENDERBUFFER, ObjectKind.FRAMEBUFFER);
    private static final TrackedState[] STATES = TrackedState.values();

    private ScriptGenerator() {}

    static List<ScriptStep> generate(long seed, ContextProfile profile, int length) {
        Random random = new Random(seed);
        Map<ObjectKind, Integer> generated = new EnumMap<>(ObjectKind.class);
        List<ScriptStep> steps = new ArrayList<>(length + 1);
        while (steps.size() < length) {
            ObjectKind kind = KINDS.get(random.nextInt(KINDS.size()));
            int existing = generated.getOrDefault(kind, 0);
            int choice = existing == 0 ? 0 : random.nextInt(CHOICES);
            steps.add(switch (choice) {
                case 0, 1, 2 -> {
                    generated.merge(kind, 1, Integer::sum);
                    yield new ScriptStep(Action.GENERATE, kind, existing, 0);
                }
                case 3, 4, 5 -> new ScriptStep(Action.BIND, kind, random.nextInt(existing), 0);
                case 6 -> kind == ObjectKind.FRAMEBUFFER
                        ? new ScriptStep(Action.BIND_NOTHING, kind, 0, 0)
                        : new ScriptStep(Action.DELETE, kind, random.nextInt(existing), 0);
                case 7 -> new ScriptStep(Action.IS_OBJECT, kind, random.nextInt(existing), 0);
                case 8 -> new ScriptStep(Action.ENABLE, kind, 0, capability(random));
                case 9 -> new ScriptStep(Action.DISABLE, kind, 0, capability(random));
                case 10 -> new ScriptStep(Action.DRAW, kind, 0, 0);
                case 11 -> new ScriptStep(Action.READ, kind, 0, 0);
                case 12 -> legacyStep(random, profile);
                default -> new ScriptStep(Action.GET_ERROR, kind, 0, 0);
            });
        }
        steps.add(new ScriptStep(Action.GET_ERROR, ObjectKind.TEXTURE, 0, 0));
        return steps;
    }

    private static long capability(Random random) {
        int index = random.nextInt(STATES.length + 1);
        return index == STATES.length ? SimulatedProvider.INVALID_CAPABILITY : STATES[index].capability();
    }

    private static ScriptStep legacyStep(Random random, ContextProfile profile) {
        if (profile == ContextProfile.CORE) {
            return new ScriptStep(Action.DRAW, ObjectKind.TEXTURE, 0, 0);
        }
        if (random.nextBoolean()) {
            return new ScriptStep(Action.IMMEDIATE_TRIANGLE, ObjectKind.TEXTURE, 0, 0);
        }
        ObjectKind kind = CREATED_BY_FIRST_USE.get(random.nextInt(CREATED_BY_FIRST_USE.size()));
        return new ScriptStep(Action.BIND_UNGENERATED, kind, random.nextInt(UNGENERATED_NAMES), 0);
    }
}
