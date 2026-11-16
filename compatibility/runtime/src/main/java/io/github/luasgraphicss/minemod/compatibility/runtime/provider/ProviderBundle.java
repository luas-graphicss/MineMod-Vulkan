package io.github.luasgraphicss.minemod.compatibility.runtime.provider;

import io.github.luasgraphicss.minemod.compatibility.provider.artifact.ArtifactDigest;
import io.github.luasgraphicss.minemod.compatibility.provider.artifact.ProviderPin;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ProviderBundle {
    private final Path directory;
    private final String entryLibrary;
    private final ProviderPin pin;
    private final Map<String, ArtifactDigest> dependencies;
    private final Map<String, String> driverSettings;

    public ProviderBundle(
            Path directory,
            String entryLibrary,
            ProviderPin pin,
            Map<String, ArtifactDigest> dependenciesInLoadOrder,
            Map<String, String> driverSettings) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.entryLibrary = Objects.requireNonNull(entryLibrary, "entryLibrary");
        this.pin = Objects.requireNonNull(pin, "pin");
        this.dependencies = Collections.unmodifiableMap(new LinkedHashMap<>(dependenciesInLoadOrder));
        this.driverSettings = Collections.unmodifiableMap(new LinkedHashMap<>(driverSettings));
    }

    public Path directory() {
        return directory;
    }

    public String entryLibrary() {
        return entryLibrary;
    }

    public ProviderPin pin() {
        return pin;
    }

    public Map<String, ArtifactDigest> dependencies() {
        return dependencies;
    }

    public Map<String, String> driverSettings() {
        return driverSettings;
    }
}
