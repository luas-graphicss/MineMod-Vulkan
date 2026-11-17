package io.github.luasgraphicss.minemod.compatibility.runtime.fixtures;

import io.github.luasgraphicss.minemod.compatibility.provider.artifact.ArtifactDigest;
import io.github.luasgraphicss.minemod.compatibility.provider.artifact.ProviderPin;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderBundle;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StagedProvider {
    public static final String ENTRY = "libEGL.so.1";
    private static final String DISPATCH = "libGLdispatch.so.0";
    private static final String API = "libglapi.so.0";
    private static final String VENDOR = "libEGL_mesa.so.0";
    private static final String DRIVERS_PREFIX = "libgallium-";
    private static final String DRIVERS_SUFFIX = ".so";

    private StagedProvider() {
    }

    public static ProviderBundle stage(Path systemLibraries, Path target) throws IOException {
        String drivers = driversLibrary(systemLibraries);
        List<String> dependencies = new ArrayList<>();
        dependencies.add(DISPATCH);
        if (Files.exists(systemLibraries.resolve(API))) {
            dependencies.add(API);
        }
        dependencies.add(drivers);
        dependencies.add(VENDOR);

        Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rwx------"));
        Map<String, ArtifactDigest> digests = new LinkedHashMap<>();
        for (String dependency : dependencies) {
            digests.put(dependency, ArtifactDigest.ofFile(copy(systemLibraries, target, dependency)));
        }
        ArtifactDigest entryDigest = ArtifactDigest.ofFile(copy(systemLibraries, target, ENTRY));
        String mesaVersion = drivers.substring(DRIVERS_PREFIX.length(), drivers.length() - DRIVERS_SUFFIX.length());
        return new ProviderBundle(target, ENTRY, new ProviderPin(mesaVersion, entryDigest), digests, softwareZink());
    }

    private static Map<String, String> softwareZink() {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("MESA_LOADER_DRIVER_OVERRIDE", "zink");
        settings.put("GALLIUM_DRIVER", "zink");
        settings.put("LIBGL_ALWAYS_SOFTWARE", "1");
        return settings;
    }

    private static Path copy(Path systemLibraries, Path target, String library) throws IOException {
        return Files.copy(systemLibraries.resolve(library), target.resolve(library));
    }

    private static String driversLibrary(Path systemLibraries) throws IOException {
        try (DirectoryStream<Path> candidates =
                Files.newDirectoryStream(systemLibraries, DRIVERS_PREFIX + "*" + DRIVERS_SUFFIX)) {
            for (Path candidate : candidates) {
                return candidate.getFileName().toString();
            }
        }
        throw new IOException("no Mesa driver library in " + systemLibraries);
    }
}
