package io.github.luasgraphicss.minemod.verification.frame;

import io.github.luasgraphicss.minemod.verification.adapters.layout.ModuleFiles;
import io.github.luasgraphicss.minemod.verification.adapters.storage.AdapterPlanFile;
import io.github.luasgraphicss.minemod.verification.check.Check;
import io.github.luasgraphicss.minemod.verification.frame.FrameTargets.FrameTarget;
import io.github.luasgraphicss.minemod.verification.frame.catalog.StandIns;
import io.github.luasgraphicss.minemod.verification.frame.policy.FramePlanRules;
import io.github.luasgraphicss.minemod.verification.frame.storage.FramePlanFile;
import io.github.luasgraphicss.minemod.verification.frame.storage.FramePlanFile.Committed;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class FramePlansCheck implements Check {
    private static final String REFRESH_HINT = "; write it with \"frames refresh\"";

    @Override
    public String name() {
        return "frame-plans";
    }

    @Override
    public List<String> findViolations(Path repositoryRoot) throws IOException {
        List<String> violations = new ArrayList<>();
        for (FrameTarget target : FrameTargets.of(TargetManifestFile.inRepository(repositoryRoot).read())) {
            Optional<Committed> game = committedIn(repositoryRoot, target.build().gameAdapter(), violations);
            Optional<Committed> loader = committedIn(repositoryRoot, target.build().loaderAdapter(), violations);
            Path presentation = ModuleFiles.planOf(repositoryRoot, target.build().loaderAdapter());
            if (!Files.isRegularFile(presentation)) {
                violations.add(target.build().loaderAdapter() + " has no release plan to present the frame with");
            } else if (game.isPresent() && loader.isPresent()) {
                violations.addAll(FramePlanRules.findViolations(
                        target,
                        game.get(),
                        loader.get(),
                        AdapterPlanFile.read(presentation),
                        StandIns.ofLwjglGeneration(target.release().lwjgl().generation())));
            }
        }
        return violations;
    }

    private static Optional<Committed> committedIn(Path repositoryRoot, String module, List<String> violations) {
        Path file = FramePlanFile.of(repositoryRoot, module);
        if (!Files.isRegularFile(file)) {
            violations.add(module + " has no frame plan" + REFRESH_HINT);
            return Optional.empty();
        }
        try {
            return Optional.of(FramePlanFile.read(file));
        } catch (IOException malformed) {
            violations.add(module + ": " + malformed.getMessage());
            return Optional.empty();
        }
    }
}
