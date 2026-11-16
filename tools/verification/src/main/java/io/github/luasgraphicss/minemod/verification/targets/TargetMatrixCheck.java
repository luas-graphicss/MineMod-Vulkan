package io.github.luasgraphicss.minemod.verification.targets;

import io.github.luasgraphicss.minemod.verification.check.Check;
import io.github.luasgraphicss.minemod.verification.targets.policy.TargetMatrixRules;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class TargetMatrixCheck implements Check {
    @Override
    public String name() {
        return "target-matrix";
    }

    @Override
    public List<String> findViolations(Path repositoryRoot) throws IOException {
        return TargetMatrixRules.findViolations(TargetManifestFile.inRepository(repositoryRoot).read());
    }
}
