package io.github.luasgraphicss.minemod.verification.scenario.target;

import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record GameTarget(String loader, String release) {
    private static final Pattern ID = Pattern.compile("([a-z]+)-([0-9]+(?:\\.[0-9]+)+)");
    private static final String ADAPTERS = "adapters/";
    private static final String RELEASE_MODULE = "/mc-";

    public static GameTarget parse(String id) {
        Matcher parts = ID.matcher(id);
        if (!parts.matches()) {
            throw new IllegalArgumentException(
                    "a target names a loader and a release, as in fabric-1.21.1, found: " + id);
        }
        return new GameTarget(parts.group(1), parts.group(2));
    }

    public static GameTarget reference() {
        return new GameTarget("fabric", "1.21.1");
    }

    public String id() {
        return loader + "-" + release;
    }

    public String loaderAdapter() {
        return ADAPTERS + loader + RELEASE_MODULE + release;
    }

    public boolean isListedIn(TargetManifest manifest) {
        return manifest.releases().stream()
                .filter(listed -> listed.id().equals(release))
                .flatMap(listed -> listed.loaders().stream())
                .anyMatch(listed -> listed.loader().name().toLowerCase(Locale.ROOT).equals(loader));
    }
}
