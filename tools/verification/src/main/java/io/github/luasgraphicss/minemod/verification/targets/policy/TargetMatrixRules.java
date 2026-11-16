package io.github.luasgraphicss.minemod.verification.targets.policy;

import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget.PinnedArtifact;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.policy.ReleaseCatalog.RequiredRelease;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class TargetMatrixRules {
    private static final Pattern SHA1 = Pattern.compile("[0-9a-f]{40}");
    private static final Pattern PINNED_COORDINATE = Pattern.compile("[\\w.-]+:[\\w.-]+:[\\w.-]+");
    private static final String OFFICIAL_HOST_SUFFIX = ".mojang.com";

    private TargetMatrixRules() {}

    public static List<String> findViolations(TargetManifest manifest) {
        List<String> violations = new ArrayList<>();
        if (manifest.schemaVersion() != TargetManifest.SCHEMA_VERSION) {
            violations.add("schemaVersion is " + manifest.schemaVersion() + ", expected " + TargetManifest.SCHEMA_VERSION);
        }

        List<String> requiredIds = ReleaseCatalog.releases().stream().map(RequiredRelease::id).toList();
        List<String> listedIds = manifest.releases().stream().map(ReleaseTarget::id).toList();
        if (!listedIds.equals(requiredIds)) {
            violations.add("releases are " + listedIds + ", expected exactly " + requiredIds);
            return violations;
        }

        for (int index = 0; index < requiredIds.size(); index++) {
            checkRelease(ReleaseCatalog.releases().get(index), manifest.releases().get(index), violations);
        }
        return violations;
    }

    private static void checkRelease(RequiredRelease required, ReleaseTarget release, List<String> violations) {
        String subject = "release " + release.id() + ": ";
        if (!required.family().equals(release.family())) {
            violations.add(subject + "family is " + release.family() + ", expected " + required.family());
        }
        if (release.runtime().majorVersion() != required.javaMajor()) {
            violations.add(subject + "runtime is Java " + release.runtime().majorVersion()
                    + ", expected Java " + required.javaMajor());
        }
        if (release.lwjgl().generation() != required.lwjglGeneration() || release.lwjgl().version().isBlank()) {
            violations.add(subject + "LWJGL binding is " + release.lwjgl()
                    + ", expected generation " + required.lwjglGeneration() + " with a pinned version");
        }
        if (!SHA1.matcher(release.metadata().sha1()).matches()) {
            violations.add(subject + "version metadata has no SHA-1 pin");
        }
        if (release.launch().mainClass().isBlank()) {
            violations.add(subject + "launch profile has no main class");
        }
        checkArtifact(subject + "client", release.client(), violations);
        if (release.clientMappings() != null) {
            checkArtifact(subject + "client mappings", release.clientMappings(), violations);
        }
        checkLoaders(required, release, subject, violations);
    }

    private static void checkArtifact(String subject, PinnedArtifact artifact, List<String> violations) {
        URI location = URI.create(artifact.url());
        boolean official = "https".equals(location.getScheme())
                && location.getHost() != null
                && location.getHost().endsWith(OFFICIAL_HOST_SUFFIX);
        if (!official) {
            violations.add(subject + " is not served from an official Mojang host: " + artifact.url());
        }
        if (!SHA1.matcher(artifact.sha1()).matches() || artifact.size() <= 0) {
            violations.add(subject + " has no SHA-1 and size pin");
        }
    }

    private static void checkLoaders(
            RequiredRelease required, ReleaseTarget release, String subject, List<String> violations) {
        Set<LoaderTarget.Kind> kinds = EnumSet.noneOf(LoaderTarget.Kind.class);
        for (LoaderTarget loader : release.loaders()) {
            if (!kinds.add(loader.loader())) {
                violations.add(subject + "loader " + loader.loader() + " is listed twice");
            }
            checkLoader(required, release, loader, subject, violations);
        }
        if (!kinds.contains(required.baseLoader())) {
            violations.add(subject + "base loader " + required.baseLoader() + " is missing");
        }
        if (kinds.contains(LoaderTarget.Kind.CLIENT_BOOTSTRAP) && kinds.size() > 1) {
            violations.add(subject + "client bootstrap must not be combined with an existing loader: " + kinds);
        }
    }

    private static void checkLoader(
            RequiredRelease required,
            ReleaseTarget release,
            LoaderTarget loader,
            String subject,
            List<String> violations) {
        String loaderSubject = subject + "loader " + loader.loader() + " ";
        LoaderTarget expected = expectedShape(required, release, loader.loader());
        if (loader.bootstrap() != expected.bootstrap() || loader.toolchain() != expected.toolchain()) {
            violations.add(loaderSubject + "uses " + loader.bootstrap() + "/" + loader.toolchain()
                    + ", expected " + expected.bootstrap() + "/" + expected.toolchain());
        }
        if (loader.loader() == LoaderTarget.Kind.CLIENT_BOOTSTRAP) {
            if (required.baseLoader() != LoaderTarget.Kind.CLIENT_BOOTSTRAP) {
                violations.add(loaderSubject + "is reserved for releases without a loader");
            }
            if (loader.artifact() != null || loader.channel() != null || loader.mappings() != null) {
                violations.add(loaderSubject + "must not reference an external artifact");
            }
            return;
        }
        if (loader.artifact() == null || !PINNED_COORDINATE.matcher(loader.artifact()).matches()) {
            violations.add(loaderSubject + "has no exact artifact coordinate: " + loader.artifact());
        }
        if (loader.channel() == null) {
            violations.add(loaderSubject + "has no release channel");
        }
        if (expected.mappings() != null && !expected.mappings().equals(loader.mappings())) {
            violations.add(loaderSubject + "maps with " + loader.mappings() + ", expected " + expected.mappings());
        }
    }

    private static LoaderTarget expectedShape(RequiredRelease required, ReleaseTarget release, LoaderTarget.Kind kind) {
        return switch (kind) {
            case FORGE -> new LoaderTarget(
                    kind, null, null, required.forgeBootstrap(), null, LoaderTarget.Toolchain.FORGEGRADLE);
            case NEOFORGE -> new LoaderTarget(
                    kind, null, null, LoaderTarget.Bootstrap.MODLAUNCHER, null, LoaderTarget.Toolchain.NEOGRADLE);
            case FABRIC -> new LoaderTarget(
                    kind,
                    null,
                    null,
                    LoaderTarget.Bootstrap.FABRIC_KNOT,
                    "net.fabricmc:intermediary:" + release.id(),
                    LoaderTarget.Toolchain.FABRIC_LOOM);
            case CLIENT_BOOTSTRAP -> LoaderTarget.clientBootstrap();
        };
    }
}
