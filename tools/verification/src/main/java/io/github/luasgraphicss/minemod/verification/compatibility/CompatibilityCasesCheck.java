package io.github.luasgraphicss.minemod.verification.compatibility;

import io.github.luasgraphicss.minemod.verification.check.Check;
import io.github.luasgraphicss.minemod.verification.compatibility.evidence.EvidenceFiles;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityManifest;
import io.github.luasgraphicss.minemod.verification.compatibility.policy.CompatibilityCaseRules;
import io.github.luasgraphicss.minemod.verification.compatibility.storage.CompatibilityManifestFile;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CompatibilityCasesCheck implements Check {
    @Override
    public String name() {
        return "compatibility-cases";
    }

    @Override
    public List<String> findViolations(Path repositoryRoot) throws IOException {
        CompatibilityManifest manifest = CompatibilityManifestFile.readFrom(repositoryRoot);
        TargetManifest targets = TargetManifestFile.inRepository(repositoryRoot).read();
        List<String> violations = new ArrayList<>(CompatibilityCaseRules.findViolations(manifest, targets));
        violations.addAll(EvidenceFiles.findViolations(repositoryRoot, manifest));
        return violations;
    }
}
