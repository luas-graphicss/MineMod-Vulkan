package io.github.luasgraphicss.minemod.verification.hooks;

import io.github.luasgraphicss.minemod.verification.check.Check;
import io.github.luasgraphicss.minemod.verification.hooks.policy.PresentationHookRules;
import io.github.luasgraphicss.minemod.verification.hooks.storage.PresentationHooksFile;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class PresentationHooksCheck implements Check {
    @Override
    public String name() {
        return "presentation-hooks";
    }

    @Override
    public List<String> findViolations(Path repositoryRoot) throws IOException {
        return PresentationHookRules.findViolations(
                PresentationHooksFile.inRepository(repositoryRoot).read(),
                TargetManifestFile.inRepository(repositoryRoot).read());
    }
}
