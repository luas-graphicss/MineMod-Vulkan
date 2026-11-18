package io.github.luasgraphicss.minemod.verification.differential;

import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DifferentialSuite {
    public static final String NAME = "gl-differential";

    private static final long BASE_SEED = 0x676C64696666L;
    private static final int SCRIPTS_PER_PROFILE = 500;
    private static final int STEPS_PER_SCRIPT = 60;

    private DifferentialSuite() {}

    public static DifferentialResult run(ComparedProvider provider) throws IOException {
        Set<String> providers = new LinkedHashSet<>();
        List<String> differences = new ArrayList<>();
        for (ContextProfile profile : ContextProfile.values()) {
            try (ScriptComparison comparison = provider.compareOn(profile)) {
                providers.add(comparison.provider());
                for (int index = 0; index < SCRIPTS_PER_PROFILE; index++) {
                    long seed = BASE_SEED + index;
                    List<ScriptStep> script = ScriptGenerator.generate(seed, profile, STEPS_PER_SCRIPT);
                    comparison
                            .firstDifference(script)
                            .ifPresent(difference -> differences.add(profile + " seed " + seed + ": " + difference));
                }
            }
        }
        return new DifferentialResult(
                SCRIPTS_PER_PROFILE * ContextProfile.values().length, String.join(" and ", providers), differences);
    }
}
