package io.github.luasgraphicss.minemod.verification.machines;

import io.github.luasgraphicss.minemod.verification.check.Check;
import io.github.luasgraphicss.minemod.verification.machines.policy.MachineRegistryRules;
import io.github.luasgraphicss.minemod.verification.machines.storage.MachineRegistryFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class MachineRegistryCheck implements Check {
    @Override
    public String name() {
        return "reference-machines";
    }

    @Override
    public List<String> findViolations(Path repositoryRoot) throws IOException {
        return MachineRegistryRules.findViolations(MachineRegistryFile.readFrom(repositoryRoot));
    }
}
