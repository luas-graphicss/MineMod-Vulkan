package io.github.luasgraphicss.minemod.verification.targets.policy;

import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import java.util.List;
import java.util.stream.Stream;

public final class ReleaseCatalog {
    public record RequiredRelease(String id, String family, int javaMajor) {
        public LoaderTarget.Kind baseLoader() {
            return switch (id) {
                case "1.12.2", "1.13.2" -> LoaderTarget.Kind.FORGE;
                case "1.13", "1.13.1" -> LoaderTarget.Kind.CLIENT_BOOTSTRAP;
                default -> LoaderTarget.Kind.FABRIC;
            };
        }

        public int lwjglGeneration() {
            return id.equals("1.12.2") ? 2 : 3;
        }

        public LoaderTarget.Bootstrap forgeBootstrap() {
            return id.equals("1.12.2") ? LoaderTarget.Bootstrap.LAUNCHWRAPPER : LoaderTarget.Bootstrap.MODLAUNCHER;
        }
    }

    private static final List<RequiredRelease> RELEASES = Stream.of(
                    family("1.12", 8, "1.12.2"),
                    family("1.13", 8, "1.13", "1.13.1", "1.13.2"),
                    family("1.14", 8, "1.14", "1.14.1", "1.14.2", "1.14.3", "1.14.4"),
                    family("1.15", 8, "1.15", "1.15.1", "1.15.2"),
                    family("1.16", 8, "1.16", "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5"),
                    family("1.17", 16, "1.17", "1.17.1"),
                    family("1.18", 17, "1.18", "1.18.1", "1.18.2"),
                    family("1.19", 17, "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4"),
                    family("1.20", 17, "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4"),
                    family("1.20", 21, "1.20.5", "1.20.6"),
                    family("1.21", 21, "1.21", "1.21.1"))
            .flatMap(List::stream)
            .toList();

    private ReleaseCatalog() {}

    public static List<RequiredRelease> releases() {
        return RELEASES;
    }

    private static List<RequiredRelease> family(String family, int javaMajor, String... ids) {
        return Stream.of(ids).map(id -> new RequiredRelease(id, family, javaMajor)).toList();
    }
}
