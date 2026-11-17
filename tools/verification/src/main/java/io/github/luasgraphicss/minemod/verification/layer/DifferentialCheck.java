package io.github.luasgraphicss.minemod.verification.layer;

import io.github.luasgraphicss.minemod.verification.check.Check;
import io.github.luasgraphicss.minemod.verification.differential.DifferentialSuite;
import io.github.luasgraphicss.minemod.verification.differential.simulated.SimulatedComparison;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class DifferentialCheck implements Check {
    @Override
    public String name() {
        return DifferentialSuite.NAME;
    }

    @Override
    public List<String> findViolations(Path repositoryRoot) throws IOException {
        return DifferentialSuite.run(SimulatedComparison::ofProfile).differences();
    }
}
