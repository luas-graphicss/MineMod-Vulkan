package io.github.luasgraphicss.minemod.verification.matrix.plan;

import io.github.luasgraphicss.minemod.verification.matrix.model.AdapterBuild;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import java.util.ArrayList;
import java.util.List;

public final class AdapterBuildPlanner {
    private static final String ARTIFACT_PREFIX = "minemod-vulkan";
    private static final String GAME_FAMILY = "minecraft";

    private AdapterBuildPlanner() {}

    public static List<AdapterBuild> plan(TargetManifest manifest) {
        List<AdapterBuild> builds = new ArrayList<>();
        for (ReleaseTarget release : manifest.releases()) {
            for (LoaderTarget loader : release.loaders()) {
                builds.add(buildOf(release, loader));
            }
        }
        return List.copyOf(builds);
    }

    private static AdapterBuild buildOf(ReleaseTarget release, LoaderTarget loader) {
        String loaderFamily = familyOf(loader.loader());
        return new AdapterBuild(
                release.id(),
                loader.loader(),
                String.join("-", ARTIFACT_PREFIX, loaderFamily, release.id()),
                release.runtime().majorVersion(),
                loader.toolchain(),
                moduleOf(GAME_FAMILY, release),
                moduleOf(loaderFamily, release));
    }

    private static String moduleOf(String family, ReleaseTarget release) {
        return "adapters/" + family + "/mc-" + release.id();
    }

    private static String familyOf(LoaderTarget.Kind loader) {
        return switch (loader) {
            case FORGE -> "forge";
            case FABRIC -> "fabric";
            case NEOFORGE -> "neoforge";
            case CLIENT_BOOTSTRAP -> "bootstrap";
        };
    }
}
