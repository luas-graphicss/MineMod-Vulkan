package io.github.luasgraphicss.minemod.verification.compatibility.policy;

import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.Evidence;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.ModRelease;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityCase.Outcome;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityManifest;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class CompatibilityCaseRules {
    private static final Pattern EXACT_VERSION = Pattern.compile("[0-9A-Za-z][0-9A-Za-z._+-]*");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    private CompatibilityCaseRules() {}

    public static List<String> findViolations(CompatibilityManifest manifest, TargetManifest targets) {
        List<String> violations = new ArrayList<>();
        if (manifest.schemaVersion() != CompatibilityManifest.SCHEMA_VERSION) {
            violations.add("schemaVersion is " + manifest.schemaVersion()
                    + ", expected " + CompatibilityManifest.SCHEMA_VERSION);
        }

        Set<String> seenIds = new HashSet<>();
        for (CompatibilityCase entry : manifest.cases()) {
            if (entry.id().isBlank()) {
                violations.add("a case has no id");
                continue;
            }
            if (!seenIds.add(entry.id())) {
                violations.add("case " + entry.id() + " is listed twice");
            }
            checkCase(entry, targets, violations);
        }
        return violations;
    }

    private static void checkCase(CompatibilityCase entry, TargetManifest targets, List<String> violations) {
        String subject = "case " + entry.id() + ": ";
        checkTarget(entry, targets, subject, violations);
        checkMods(entry, subject, violations);
        if (entry.categories().isEmpty()) {
            violations.add(subject + "names no homologation category");
        }
        if (entry.capability().isBlank() || entry.scenario().isBlank()) {
            violations.add(subject + "does not name its capability and scenario");
        }
        if (entry.reason().isBlank()) {
            violations.add(subject + "gives no reason for " + entry.outcome());
        }
        if (entry.outcome() == Outcome.VERIFIED && entry.evidence().isEmpty()) {
            violations.add(subject + "is verified without evidence");
        }
        for (Evidence evidence : entry.evidence()) {
            if (evidence.path().isBlank() || !SHA256.matcher(evidence.sha256()).matches()) {
                violations.add(subject + "evidence has no path and SHA-256 pin: " + evidence.path());
            }
        }
    }

    private static void checkTarget(
            CompatibilityCase entry, TargetManifest targets, String subject, List<String> violations) {
        Optional<ReleaseTarget> release = targets.releases().stream()
                .filter(candidate -> candidate.id().equals(entry.release()))
                .findFirst();
        if (release.isEmpty()) {
            violations.add(subject + "targets " + entry.release() + ", which is not a pinned release");
            return;
        }
        if (entry.loader() == LoaderTarget.Kind.CLIENT_BOOTSTRAP) {
            violations.add(subject + "targets the client bootstrap, which loads no mods");
            return;
        }
        boolean loaderIsPinned = release.get().loaders().stream()
                .anyMatch(loader -> loader.loader() == entry.loader());
        if (!loaderIsPinned) {
            violations.add(subject + "targets " + entry.loader() + " on " + entry.release()
                    + ", which the target manifest does not list");
        }
    }

    private static void checkMods(CompatibilityCase entry, String subject, List<String> violations) {
        if (entry.mods().isEmpty()) {
            violations.add(subject + "names no mod release");
        }
        for (ModRelease mod : entry.mods()) {
            if (mod.id().isBlank() || !EXACT_VERSION.matcher(mod.version()).matches()) {
                violations.add(subject + "mod " + mod.id() + " has no exact version: " + mod.version());
            }
        }
    }
}
