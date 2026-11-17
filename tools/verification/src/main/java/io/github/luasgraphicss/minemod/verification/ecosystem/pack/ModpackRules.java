package io.github.luasgraphicss.minemod.verification.ecosystem.pack;

import io.github.luasgraphicss.minemod.verification.ecosystem.pack.Modpack.Expectation;
import io.github.luasgraphicss.minemod.verification.ecosystem.pack.Modpack.PackMod;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class ModpackRules {
    private static final Pattern EXACT_VERSION = Pattern.compile("[0-9A-Za-z][0-9A-Za-z._+-]*");
    private static final Set<ModRole> COMPOSED_SUITE =
            EnumSet.of(ModRole.INTERFACE_OVERLAY, ModRole.CUSTOM_MODELS, ModRole.MAP, ModRole.TECHNICAL_CONTENT);

    private ModpackRules() {}

    public static List<String> findViolations(Map<String, Modpack> packsByPath, TargetManifest targets) {
        List<String> violations = new ArrayList<>();
        Set<String> suites = new HashSet<>();
        packsByPath.forEach((path, pack) -> {
            String subject = path + ": ";
            if (pack.schemaVersion() != Modpack.SCHEMA_VERSION || !path.endsWith("/" + pack.id() + ".json")) {
                violations.add(subject + "does not carry schema " + Modpack.SCHEMA_VERSION + " and the id of its file");
            }
            checkMods(pack, subject, violations);
            ReleaseTarget release = targets.releases().stream()
                    .filter(candidate -> candidate.id().equals(pack.release()))
                    .findFirst()
                    .orElse(null);
            boolean pinned = release != null
                    && pack.loader() != LoaderTarget.Kind.CLIENT_BOOTSTRAP
                    && release.loaders().stream().anyMatch(loader -> loader.loader() == pack.loader());
            if (!pinned) {
                violations.add(subject + pack.loader() + " on " + pack.release() + " is not a pinned mod loader target");
            } else if (pack.expectation() == Expectation.RUNS && pack.roles().containsAll(COMPOSED_SUITE)) {
                suites.add(release.family() + " " + pack.loader());
            }
        });
        missingSuites(targets, suites).forEach(
                missing -> violations.add("no composed suite runs on " + missing + " with " + COMPOSED_SUITE));
        return violations;
    }

    private static void checkMods(Modpack pack, String subject, List<String> violations) {
        Set<String> seen = new HashSet<>();
        if (pack.mods().isEmpty()) {
            violations.add(subject + "installs no mod");
        }
        for (PackMod mod : pack.mods()) {
            if (mod.id().isBlank() || !EXACT_VERSION.matcher(mod.version()).matches()) {
                violations.add(subject + "mod " + mod.id() + " has no exact version: " + mod.version());
            }
            if (!seen.add(mod.id())) {
                violations.add(subject + "mod " + mod.id() + " is installed twice");
            }
        }
    }

    private static Set<String> missingSuites(TargetManifest targets, Set<String> suites) {
        Set<String> required = new TreeSet<>();
        for (ReleaseTarget release : targets.releases()) {
            for (LoaderTarget loader : release.loaders()) {
                if (loader.loader() != LoaderTarget.Kind.CLIENT_BOOTSTRAP) {
                    required.add(release.family() + " " + loader.loader());
                }
            }
        }
        required.removeAll(suites);
        return required;
    }
}
