package io.github.luasgraphicss.minemod.verification.machines;

import io.github.luasgraphicss.minemod.verification.check.Check;
import io.github.luasgraphicss.minemod.verification.machines.policy.HardwareCoverage;
import io.github.luasgraphicss.minemod.verification.machines.storage.MachineRegistryFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class HardwareEvidenceCheck implements Check {
    @Override
    public String name() {
        return "hardware-evidence";
    }

    @Override
    public List<String> findViolations(Path repositoryRoot) throws IOException {
        return HardwareCoverage.findGaps(MachineRegistryFile.readFrom(repositoryRoot));
    }
}
